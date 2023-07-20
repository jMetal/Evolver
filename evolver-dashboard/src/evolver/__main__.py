import argparse
import os
import subprocess
import sys

from evolver.logs import get_logger
from evolver.utils import is_installed


def start_streamlit(
    data_path: str, port: int, logger_level: str = "INFO", jar: str = None
) -> subprocess.Popen:
    """
    Start streamlit dashboard.

    We use a subprocess instead of importing the streamlit package
    because it messes with the logging configuration.

    Args:
        data_path: Path to the data directory for Evolver outputs
        port: Port to run the dashboard on
        logger_level: Set the logging level. Defaults to INFO
        jar: Path to the jar file for Evolver. Defaults to None

    Returns:
        subprocess.Popen: The process running the streamlit dashboard
    """
    # Set up environment variables and arguments
    environment = {}
    environment["EVOLVER_BASE_PATH"] = data_path
    environment["STREAMLIT_BROWSER_SERVER_PORT"] = str(port)
    environment["STREAMLIT_SERVER_PORT"] = str(port)
    environment["STREAMLIT_BROWSER_GATHER_USAGE_STATS"] = "FALSE"
    environment["STREAMLIT_LOGGER_LEVEL"] = logger_level.lower()

    if jar:
        environment["EVOLVER_JAR"] = jar

    command = [
        sys.executable,  # Run the same python interpreter
        "-m",
        "streamlit",
        "run",
        f"{os.path.dirname(__file__)}/dashboard.py",
    ]

    # Start the subprocess with the environment variables
    process = subprocess.Popen(command, env=environment)

    return process


# Parse arguments
parser = argparse.ArgumentParser()
parser.add_argument(
    "-f",
    "--data-folder",
    type=str,
    help=("Path to the data directory for Evolver outputs. Defaults to /tmp/evolver"),
    default="/tmp/evolver",
)
parser.add_argument(
    "-j",
    "--jar",
    type=str,
    help=("Path to the jar file for Evolver"),
)
parser.add_argument(
    "-p",
    "--port",
    type=int,
    help=("Port to run the dashboard on. Defaults to 8501"),
    default=8501,
)
parser.add_argument(
    "-l",
    "--log-level",
    choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
    help="Set the logging level. Defaults to INFO",
    default="INFO",
)


args = parser.parse_args()

# Configure logger
logger = get_logger()
if args.log_level:
    logger.setLevel(args.log_level)


# Check whether the dashboard should be enabled
if not (is_streamlit_installed := is_installed("streamlit")):
    raise RuntimeError(
        "Streamlit is not installed. The dashboard will not start."
        "To install streamlit, run 'pip install streamlit'"
    )

# Start streamlit dashboard
try:
    logger.info("Starting streamlit dashboard")
    streamlit_process = start_streamlit(
        data_path=args.data_folder,
        port=args.port,
        logger_level=args.log_level,
        jar=args.jar,
    )
    streamlit_process.wait()
except KeyboardInterrupt:
    logger.info("Interrupted by user")
except Exception as err:
    logger.fatal(err)
finally:
    # Make sure to stop the streamlit dashboard to avoid zombie processes
    logger.info("Stopping streamlit dashboard")
    streamlit_process.terminate()
