import logging
import sys
from app.core.config import settings


def setup_logging() -> logging.Logger:
    """Configures structured logging format across the FastAPI AI service."""
    log_level = getattr(logging, settings.log_level.upper(), logging.INFO)

    logging.basicConfig(
        level=log_level,
        format="%(asctime)s | %(levelname)-7s | [%(name)s] %(message)s",
        datefmt="%Y-%m-%d %H:%M:%S",
        handlers=[logging.StreamHandler(sys.stdout)],
        force=True,
    )

    logger = logging.getLogger("cs-scholar.ai")
    logger.setLevel(log_level)
    return logger


logger = setup_logging()

