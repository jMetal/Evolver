import argparse
import os
import subprocess
import sys

from evolver.logs import get_logger
from evolver.utils import is_installed



def start_streamlit():
    """
    Start streamlit dashboard.

    We use a subprocess instead of importing the streamlit package
    because it messes with the logging configuration.
    """
    # Set up environment variables and arguments
    environment = {}

    command = [
        sys.executable, # Run the same python interpreter
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
    "--checkpoint-folder",
    type=str,
    default="checkpoints",
    help=(
        "Path of the folder where the checkpoints will be stored."
        "Defaults to './checkpoint'"
    ),
)
parser.add_argument(
    "-l",
    "--log-level",
    choices=["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"],
    help="Set the logging level. Defaults to INFO",
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
    streamlit_process = start_streamlit()
    streamlit_process.wait()
except KeyboardInterrupt:
    logger.info("Interrupted by user")
except Exception as err:
    logger.fatal(err)
finally:
    # Make sure to stop the streamlit dashboard to avoid zombie processes
    logger.info("Stopping streamlit dashboard")
    streamlit_process.terminate()
