"""
CS Scholar AI - S3/MinIO Storage Service Client
Downloads document objects from object storage for ingestion.
"""
import logging
import os
import tempfile
from contextlib import contextmanager
from typing import Generator
import boto3
from botocore.client import Config
from botocore.exceptions import ClientError

from app.core.config import settings
from app.ingestion.exceptions import StorageFetchError

logger = logging.getLogger(__name__)


class StorageClient:
    """Manages interactions with S3-compatible object storage (MinIO/AWS S3)."""

    def __init__(self):
        s3_config = Config(
            s3={"addressing_style": "path" if settings.s3_path_style_access else "auto"},
            signature_version="s3v4"
        )
        self.client = boto3.client(
            "s3",
            endpoint_url=settings.s3_endpoint,
            aws_access_key_id=settings.s3_access_key,
            aws_secret_access_key=settings.s3_secret_key,
            region_name=settings.s3_region,
            config=s3_config
        )
        self.bucket = settings.s3_bucket

    @contextmanager
    def download_to_tempfile(self, storage_key: str, file_extension: str = "") -> Generator[str, None, None]:
        """
        Download an S3 object into a temporary file on the local filesystem.
        Automatically removes the temporary file when exiting the context block.
        """
        ext = f".{file_extension.lower().lstrip('.')}" if file_extension else ""
        temp_file = tempfile.NamedTemporaryFile(delete=False, suffix=ext)
        temp_file_path = temp_file.name
        temp_file.close()

        try:
            logger.info("Downloading s3://%s/%s to temporary file %s", self.bucket, storage_key, temp_file_path)
            self.client.download_file(self.bucket, storage_key, temp_file_path)
            yield temp_file_path
        except ClientError as e:
            error_code = e.response.get("Error", {}).get("Code", "Unknown")
            raise StorageFetchError(
                f"Failed to fetch object '{storage_key}' from bucket '{self.bucket}': {error_code} - {e}",
                {"storage_key": storage_key, "bucket": self.bucket, "error_code": error_code}
            )
        except Exception as e:
            raise StorageFetchError(
                f"Unexpected error while downloading object '{storage_key}': {e}",
                {"storage_key": storage_key, "bucket": self.bucket}
            )
        finally:
            if os.path.exists(temp_file_path):
                try:
                    os.remove(temp_file_path)
                    logger.debug("Removed temporary ingestion file: %s", temp_file_path)
                except Exception as cleanup_err:
                    logger.warning("Failed to clean up temporary file %s: %s", temp_file_path, cleanup_err)


storage_client = StorageClient()

