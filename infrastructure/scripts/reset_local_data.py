#!/usr/bin/env python3
"""
CS Scholar AI — Local Development Data Purge & Clean-State Reset Script

Objective:
    Safely and completely purge local development and test data from PostgreSQL,
    MinIO object storage, and Redis queues, returning the system to a clean,
    empty initial state while preserving database schemas, Flyway migrations,
    and static computer science curriculum reference data.

Usage:
    python infrastructure/scripts/reset_local_data.py [--confirm] [--dry-run]
"""

import sys
import os
import argparse
from typing import Dict, List, Tuple
from urllib.parse import urlparse

try:
    import psycopg2
    import redis
    import boto3
    from botocore.client import Config
except ImportError as e:
    print(f"Error: Missing required dependency: {e}")
    print("Please run this script inside the ai-service virtual environment (ai-service/.venv).")
    sys.exit(1)


# ==============================================================================
# Configuration & Safety Thresholds (Local Dev ONLY)
# ==============================================================================

ALLOWED_HOSTS = {"localhost", "127.0.0.1", "::1"}
EXPECTED_DB = "cs_scholar_db"

PG_HOST = os.getenv("POSTGRES_HOST", "localhost")
PG_PORT = int(os.getenv("POSTGRES_PORT", "5432"))
PG_DB = os.getenv("POSTGRES_DB", "cs_scholar_db")
PG_USER = os.getenv("POSTGRES_USER", "postgres")
PG_PASS = os.getenv("POSTGRES_PASSWORD", "postgres_dev_password")

REDIS_HOST = os.getenv("REDIS_HOST", "localhost")
REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))

S3_ENDPOINT = os.getenv("S3_ENDPOINT", "http://localhost:9000")
S3_ACCESS_KEY = os.getenv("S3_ACCESS_KEY", "minioadmin")
S3_SECRET_KEY = os.getenv("S3_SECRET_KEY", "minioadmin")
S3_BUCKET = os.getenv("S3_BUCKET", "cs-scholar-documents")


# Tables that store user-generated or test data (ordered by foreign key dependency)
USER_DATA_TABLES: List[str] = [
    # 1. Learning Studio (attempts, questions, flashcards)
    "quiz_attempt_answers",
    "quiz_attempts",
    "quiz_question_options",
    "quiz_questions",
    "quizzes",
    "flashcard_review_history",
    "flashcard_review_state",
    "flashcards",
    "flashcard_decks",
    "learning_sessions",
    # 2. Shared Library (reports, logs, tags, resources)
    "library_resource_reports",
    "library_moderation_logs",
    "library_resource_tags",
    "knowledge_topic_resources",
    "library_resources",
    # 3. Chat & Conversations
    "message_sources",
    "messages",
    "conversations",
    # 4. Ingestion, Documents & Embeddings
    "chunk_embeddings",
    "document_chunks",
    "embedding_jobs",
    "ingestion_outbox",
    "processing_jobs",
    "documents",
    # 5. Workspaces
    "workspace_members",
    "workspaces",
    # 6. Auth & Users
    "refresh_tokens",
    "users",
]

# Static reference data that MUST be preserved for application functionality
STATIC_REFERENCE_TABLES: Dict[str, int] = {
    "academic_subjects": 13,
    "knowledge_topics": 65,
    "knowledge_topic_learning_objectives": 64,
    "knowledge_topic_relationships": 81,
    "knowledge_topic_tags": 98,
    "flyway_schema_history": 15,
}

# Redis key patterns/names to purge
REDIS_TARGET_KEYS = [
    "celery",
    "_kombu.binding.celery",
    "_kombu.binding_permanent.celery",
    "phase0_test",
]
REDIS_PREFIXES_TO_SCAN = [
    "cs:*",
    "celery-task-meta-*",
]


def validate_environment():
    """Verify that all target endpoints strictly point to the local development environment."""
    print("=" * 70)
    print("CS Scholar AI — Pre-Purge Environment Safety Validation")
    print("=" * 70)

    # 1. Database validation
    if PG_HOST not in ALLOWED_HOSTS:
        raise ValueError(f"CRITICAL SAFETY VIOLATION: Database host '{PG_HOST}' is not in local allowed hosts {ALLOWED_HOSTS}!")
    if PG_DB != EXPECTED_DB:
        raise ValueError(f"CRITICAL SAFETY VIOLATION: Database name '{PG_DB}' != expected local '{EXPECTED_DB}'!")
    print(f" [PASS] Database endpoint: {PG_USER}@{PG_HOST}:{PG_PORT}/{PG_DB} (Verified Local)")

    # 2. Redis validation
    if REDIS_HOST not in ALLOWED_HOSTS:
        raise ValueError(f"CRITICAL SAFETY VIOLATION: Redis host '{REDIS_HOST}' is not in local allowed hosts {ALLOWED_HOSTS}!")
    print(f" [PASS] Redis endpoint: {REDIS_HOST}:{REDIS_PORT} (Verified Local)")

    # 3. MinIO / S3 validation
    s3_parsed = urlparse(S3_ENDPOINT)
    if s3_parsed.hostname not in ALLOWED_HOSTS:
        raise ValueError(f"CRITICAL SAFETY VIOLATION: S3 endpoint host '{s3_parsed.hostname}' is not in local allowed hosts {ALLOWED_HOSTS}!")
    print(f" [PASS] S3 endpoint: {S3_ENDPOINT} (Verified Local)")
    print("=" * 70)


def get_pg_counts(conn) -> Dict[str, int]:
    """Return row counts for all tables in the public schema."""
    counts = {}
    with conn.cursor() as cur:
        # Check all user tables
        for tbl in USER_DATA_TABLES:
            cur.execute(f"SELECT count(*) FROM {tbl};")
            counts[tbl] = cur.fetchone()[0]
        # Check static tables
        for tbl in STATIC_REFERENCE_TABLES:
            cur.execute(f"SELECT count(*) FROM {tbl};")
            counts[tbl] = cur.fetchone()[0]
    return counts


def get_minio_inventory(s3_client) -> Dict[str, List[str]]:
    """Return list of object keys per bucket."""
    inventory = {}
    try:
        buckets = s3_client.list_buckets().get("Buckets", [])
        for b in buckets:
            bname = b["Name"]
            objs = s3_client.list_objects_v2(Bucket=bname).get("Contents", [])
            inventory[bname] = [o["Key"] for o in objs]
    except Exception as e:
        print(f" Warning: Could not inspect MinIO inventory: {e}")
    return inventory


def get_redis_keys(r_client) -> List[str]:
    """Return application-specific keys in Redis."""
    found = set()
    for key in REDIS_TARGET_KEYS:
        if r_client.exists(key):
            found.add(key)
    for prefix in REDIS_PREFIXES_TO_SCAN:
        for k in r_client.keys(prefix):
            found.add(k.decode("utf-8") if isinstance(k, bytes) else str(k))
    return sorted(list(found))


def purge_postgresql(conn, dry_run: bool) -> int:
    """Purge all user-generated records inside a single transaction."""
    total_deleted = 0
    with conn.cursor() as cur:
        # Disable immutability trigger temporarily on library_moderation_logs for purge
        if not dry_run:
            cur.execute("ALTER TABLE library_moderation_logs DISABLE TRIGGER trg_prevent_moderation_log_modification;")
        
        try:
            for tbl in USER_DATA_TABLES:
                cur.execute(f"SELECT count(*) FROM {tbl};")
                cnt = cur.fetchone()[0]
                if cnt > 0:
                    print(f"   - Deleting {cnt:>5} rows from table '{tbl}'...")
                    if not dry_run:
                        cur.execute(f"DELETE FROM {tbl};")
                    total_deleted += cnt
                else:
                    print(f"   - Table '{tbl}': 0 rows (already clean)")
        finally:
            # Always re-enable immutability trigger
            if not dry_run:
                cur.execute("ALTER TABLE library_moderation_logs ENABLE TRIGGER trg_prevent_moderation_log_modification;")
                
    if not dry_run:
        conn.commit()
    return total_deleted


def purge_minio(s3_client, inventory: Dict[str, List[str]], dry_run: bool) -> int:
    """Delete all test objects from application buckets while preserving buckets."""
    total_deleted = 0
    for bname, keys in inventory.items():
        if not keys:
            print(f"   - Bucket '{bname}': 0 objects (already clean)")
            continue
        print(f"   - Purging {len(keys)} object(s) from bucket '{bname}'...")
        for key in keys:
            print(f"     * Deleting object: {key}")
            if not dry_run:
                s3_client.delete_object(Bucket=bname, Key=key)
            total_deleted += 1
    return total_deleted


def purge_redis(r_client, keys: List[str], dry_run: bool) -> int:
    """Delete application-specific Redis keys."""
    if not keys:
        print("   - Redis: 0 application keys found (already clean)")
        return 0
    print(f"   - Deleting {len(keys)} application key(s) from Redis...")
    deleted = 0
    for key in keys:
        print(f"     * Deleting Redis key: {key}")
        if not dry_run:
            r_client.delete(key)
        deleted += 1
    return deleted


def main():
    parser = argparse.ArgumentParser(description="CS Scholar AI Local Development Data Purge & Clean Reset")
    parser.add_argument("--confirm", action="store_true", help="Confirm execution without interactive prompt")
    parser.add_argument("--dry-run", action="store_true", help="Perform pre-purge inventory without deleting anything")
    args = parser.parse_args()

    # Step 1: Safety validation
    validate_environment()

    # Step 2: Connect to services
    print("\nConnecting to local services...")
    conn = psycopg2.connect(
        host=PG_HOST,
        port=PG_PORT,
        dbname=PG_DB,
        user=PG_USER,
        password=PG_PASS
    )
    r_client = redis.Redis(host=REDIS_HOST, port=REDIS_PORT, db=0)
    s3_client = boto3.client(
        "s3",
        endpoint_url=S3_ENDPOINT,
        aws_access_key_id=S3_ACCESS_KEY,
        aws_secret_access_key=S3_SECRET_KEY,
        config=Config(signature_version="s3v4"),
        region_name="us-east-1"
    )

    # Step 3: Pre-Purge Inventory
    print("\n" + "=" * 70)
    print("PRE-PURGE INVENTORY")
    print("=" * 70)

    initial_counts = get_pg_counts(conn)
    total_user_rows = sum(initial_counts[t] for t in USER_DATA_TABLES)
    print(f"\n[PostgreSQL User/Test Tables] Total rows to delete: {total_user_rows}")
    for tbl in USER_DATA_TABLES:
        cnt = initial_counts[tbl]
        if cnt > 0:
            print(f"  * {tbl:<32}: {cnt} rows")

    print("\n[PostgreSQL Static Reference Tables] Retained permanently:")
    for tbl, expected in STATIC_REFERENCE_TABLES.items():
        actual = initial_counts[tbl]
        print(f"  * {tbl:<36}: {actual} rows (expected: {expected})")

    minio_inv = get_minio_inventory(s3_client)
    total_s3_objs = sum(len(objs) for objs in minio_inv.values())
    print(f"\n[MinIO S3 Buckets] Total objects to delete: {total_s3_objs}")
    for bname, keys in minio_inv.items():
        print(f"  * Bucket '{bname}': {len(keys)} object(s)")
        for k in keys:
            print(f"    - {k}")

    redis_keys = get_redis_keys(r_client)
    print(f"\n[Redis db0] Application keys to delete: {len(redis_keys)}")
    for k in redis_keys:
        k_type = r_client.type(k).decode("utf-8")
        if k_type == "list":
            length = r_client.llen(k)
            print(f"  * {k} (list, {length} items)")
        else:
            print(f"  * {k} ({k_type})")

    print("=" * 70)

    if args.dry_run:
        print("\n[DRY RUN COMPLETE] No data was modified or deleted.")
        conn.close()
        return

    if not args.confirm:
        print("\nWARNING: This will permanently delete all local development users, documents,")
        print("conversations, quizzes, flashcards, MinIO objects, and Redis job queues.")
        answer = input("Are you sure you want to proceed with the local data reset? (yes/no): ")
        if answer.strip().lower() not in ("yes", "y"):
            print("Purge aborted by user.")
            conn.close()
            return

    # Step 4: Execute Purge
    print("\nExecuting Purge...")
    print("\n1. Purging PostgreSQL User/Test Records...")
    deleted_rows = purge_postgresql(conn, dry_run=False)

    print("\n2. Purging MinIO Test Objects...")
    deleted_objs = purge_minio(s3_client, minio_inv, dry_run=False)

    print("\n3. Purging Redis Queues & Keys...")
    deleted_keys = purge_redis(r_client, redis_keys, dry_run=False)

    # Step 5: Post-Purge Verification
    print("\n" + "=" * 70)
    print("POST-PURGE VERIFICATION & AUDIT")
    print("=" * 70)

    post_counts = get_pg_counts(conn)
    failed_tables = []
    for tbl in USER_DATA_TABLES:
        cnt = post_counts[tbl]
        if cnt != 0:
            failed_tables.append((tbl, cnt))

    if failed_tables:
        print(f"\n [ERROR] Some user tables still contain records: {failed_tables}")
    else:
        print(f"\n [PASS] All {len(USER_DATA_TABLES)} user/test tables verified completely EMPTY (0 rows).")

    static_failed = []
    for tbl, expected in STATIC_REFERENCE_TABLES.items():
        actual = post_counts[tbl]
        if actual != expected:
            static_failed.append((tbl, actual, expected))

    if static_failed:
        print(f"\n [WARNING] Static reference count mismatch: {static_failed}")
    else:
        print(f" [PASS] All {len(STATIC_REFERENCE_TABLES)} static reference tables preserved intact.")

    post_minio = get_minio_inventory(s3_client)
    remaining_objs = sum(len(objs) for objs in post_minio.values())
    if remaining_objs == 0:
        print(f" [PASS] All MinIO buckets verified completely EMPTY (0 objects).")
    else:
        print(f" [WARNING] MinIO still contains {remaining_objs} object(s): {post_minio}")

    post_redis = get_redis_keys(r_client)
    if not post_redis:
        print(f" [PASS] Redis application keys and Celery queues verified completely EMPTY (0 keys).")
    else:
        print(f" [WARNING] Redis still contains application keys: {post_redis}")

    conn.close()
    print("\n" + "=" * 70)
    print(f"RESET SUMMARY: Deleted {deleted_rows} PostgreSQL rows, {deleted_objs} MinIO objects, {deleted_keys} Redis keys.")
    print("Local environment is now in a clean, initial state.")
    print("=" * 70)


if __name__ == "__main__":
    main()

