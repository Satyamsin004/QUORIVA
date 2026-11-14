import boto3
import sys

def test_minio_s3():
    endpoint = "http://localhost:9000"
    access_key = "minioadmin"
    secret_key = "minioadmin"
    bucket_name = "cs-scholar-test-bucket"
    object_key = "phase0_audit/test_document.txt"
    test_content = b"CS Scholar AI - Phase 0 MinIO S3 Audit Content"

    print(f"Connecting to MinIO S3 API at {endpoint}...")
    s3 = boto3.client(
        "s3",
        endpoint_url=endpoint,
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        region_name="us-east-1"
    )

    # 1. Bucket Creation
    print(f"1. Creating bucket '{bucket_name}'...")
    try:
        s3.create_bucket(Bucket=bucket_name)
        print("   Bucket created successfully.")
    except s3.exceptions.BucketAlreadyOwnedByYou:
        print("   Bucket already exists and owned by user.")

    # 2. Upload
    print(f"2. Uploading object '{object_key}'...")
    s3.put_object(Bucket=bucket_name, Key=object_key, Body=test_content)
    print("   Object uploaded successfully.")

    # 3. Download and verify
    print(f"3. Downloading object '{object_key}'...")
    response = s3.get_object(Bucket=bucket_name, Key=object_key)
    downloaded_content = response["Body"].read()
    assert downloaded_content == test_content, f"Content mismatch: {downloaded_content} != {test_content}"
    print(f"   Object verified successfully! Content: {downloaded_content.decode('utf-8')}")

    # 4. List Buckets & Objects
    buckets = [b["Name"] for b in s3.list_buckets().get("Buckets", [])]
    print(f"4. Listed buckets: {buckets}")
    assert bucket_name in buckets, f"{bucket_name} not found in {buckets}"

    print("All S3 operations PASSED.")

if __name__ == "__main__":
    test_minio_s3()

