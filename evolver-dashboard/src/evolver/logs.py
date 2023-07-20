import logging.config
import os
from pathlib import Path


def configure_logging(log_file: str = None, config: dict | None = None) -> None:
    """Configure logging for the application.

    Args:
        log_file (str): Path to the log file.
        config (dict|None): Logging configuration. If not provided, uses a default
            console logger.
    """
    if log_file is None:
        log_path = os.environ.get("XDG_STATE_HOME", "~/.local/state")
        log_file = Path(f"{log_path}/evolver/dashboard.log").expanduser().resolve()
        if not log_file.is_file():
            log_file.parent.mkdir(parents=True, exist_ok=True)

    DEFAULT_LOGGING_CONFIG = {
        "version": 1,
        "disable_existing_loggers": False,
        "formatters": {
            "basic": {
                "format": "[%(asctime)s] [%(name)s] [%(levelname)s] %(message)s",
                "datefmt": "%Y-%m-%d %H:%M:%S",
            }
        },
        "handlers": {
            "console": {
                "formatter": "basic",
                "class": "logging.StreamHandler",
            },
            "rotate_file": {
                "formatter": "basic",
                "class": "logging.handlers.RotatingFileHandler",
                "filename": f"{log_file}",
                "encoding": "utf8",
                "maxBytes": 100000,
                "backupCount": 1,
            },
        },
        "loggers": {
            "evolver": {
                "level": "INFO",
                "handlers": ["console"],
            },
        },
    }

    try:
        logging.config.dictConfig(config or DEFAULT_LOGGING_CONFIG)
    except FileNotFoundError as err:
        logging.error(f"Error while configuring logging: {err}")


def get_logger(module: str = "evolver", name: str = None) -> logging.Logger:
    """Get a logger for the given module.

    Args:
        module (str): Name of the module to get the logger for.
        name (str): Name of the logger to get.

    Returns:
        logging.Logger: Logger for the given module.
    """
    logger_name = module
    if name is not None:
        logger_name += "." + name
    return logging.getLogger(logger_name)
