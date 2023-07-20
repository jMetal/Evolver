import os
import subprocess
from pathlib import Path


class JavaException(Exception):
    """Custom exception for raising Java-related errors."""

    pass


def create_command(
    java_class: str,
    *,
    jar: Path = None,
    args: list[str] = [],
    log_config: str = None,
) -> list[str]:
    """Prepares the Evolver command to be executed.

    Args:
        java_class (str): The name of the Java class to execute.
        jar (Path, optional): The path to the JAR file. Defaults to None.
        args (list[str], optional): The arguments to pass to the JAR file.
            Defaults to [].
        log_config (str, optional): Java configuration file for the logs.
            Defaults to None.

    Returns:
        list[str]: The command to execute the JAR file.
    """
    # If the JAR file is not specified, use the default JAR file
    if jar is None:
        jar = (
            Path(os.environ.get("XDG_CACHE_HOME", "~/.cache"))
            / "Evolver"
            / "Evolver-1.0-jar-with-dependencies.jar"
        )
    jar = jar.expanduser().resolve()

    if not jar.is_file():
        raise JavaException(f"JAR file {jar} does not exist.")

    # Construct the command to run the JAR file
    command = ["java", "-cp", str(jar), java_class]
    if log_config:
        command.insert(1, f"-Djava.util.logging.config.file={log_config}")
    command.extend(args)

    return command


def execute_evolver(
    java_class: str,
    *,
    jar: Path = None,
    args: list[str] = [],
    log_config: str = None,
) -> str:
    """Executes a JAR file, waits for its completion and returns the output.

    Args:
        java_class (str): The name of the Java class to execute.
        jar (Path, optional): The path to the JAR file. Defaults to None.
        args (list[str], optional): The arguments to pass to the JAR file.
            Defaults to [].
        log_config (str, optional): Java configuration file for the logs.
            Defaults to None.

    Returns:
        str: The stdout of the JAR file.
    """
    command = create_command(java_class, jar=jar, args=args, log_config=log_config)

    # Run the JAR file using a subprocess
    try:
        output = subprocess.check_output(command, stderr=subprocess.PIPE)
        logs = output.decode("utf-8")
    except subprocess.CalledProcessError as e:
        raise JavaException(
            f"JAR execution failed with return code {e.returncode}. "
            f"Error message:\n{e.stderr.decode('utf-8')}"
        )

    return logs


def execute_evolver_streaming(
    java_class: str,
    *,
    jar: Path = None,
    args: list[str] = [],
    log_config: str = None,
):
    """Executes a JAR file and returns a generator to its logs, while executing
    in the background.

    Args:
        java_class (str): The name of the Java class to execute.
        jar (Path, optional): The path to the JAR file. Defaults to None.
        args (list[str], optional): The arguments to pass to the JAR file.
            Defaults to [].
        log_config (str, optional): Java configuration file for the logs.
            Defaults to None.

    Yields:
        str: The stdout of the JAR file line by line.
    """
    command = create_command(java_class, jar=jar, args=args, log_config=log_config)

    # Run the JAR file using a subprocess
    with subprocess.Popen(
        command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True
    ) as process:
        for line in process.stderr:
            yield line.strip()

        process.wait()

        if process.returncode != 0:
            error_output = process.stderr.read().strip()
            raise JavaException(
                f"JAR execution failed with return code {process.returncode}."
                f" Error message:\n{error_output}"
            )


def execute_evolver_streaming_to_disk(
    java_class: str,
    log_file: Path,
    *,
    jar: Path = None,
    args: list[str] = [],
):
    """Executes a JAR file, storing to disk it's output by configuring Evolver logs.

    Args:
        java_class (str): The name of the Java class to execute.
        log_file (Path): The path where Evolver log file will be written.
        jar (Path, optional): The path to the JAR file. Defaults to None.
        args (list[str], optional): The arguments to pass to the JAR file.
            Defaults to [].

    Returns:
        int: The PID of the process executing the JAR file.
    """
    logger_info = f"""# Set the log level
.level = INFO

# Configure the FileHandler
handlers = java.util.logging.FileHandler
java.util.logging.FileHandler.level = ALL
java.util.logging.FileHandler.pattern = {log_file}
java.util.logging.FileHandler.formatter = java.util.logging.SimpleFormatter
"""
    # Create log configuration file to pass to Evolver
    log_config = log_file.with_suffix(log_file.suffix + ".config")
    with open(log_config, "w") as f:
        f.write(logger_info)
    command = create_command(java_class, jar=jar, args=args, log_config=log_config)

    # Run the JAR file using a subprocess
    process = subprocess.Popen(command)

    return process.pid
