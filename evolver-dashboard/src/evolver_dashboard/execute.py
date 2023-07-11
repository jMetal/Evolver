import os
import subprocess
from pathlib import Path

class JavaException(Exception):
    pass

def create_command(java_class: str,
    *,
    jar: Path = None,
    args: list[str] = [],
    enable_logs: bool = False,):
    """
    Prepares the Evolver command to be executed.

    :param jar: The path to the JAR file.
    :param java_class: The name of the Java class to execute.
    :param args: The arguments to pass to the JAR file.
    :param enable_logs: Whether to enable the Evolver logs.
    :return: The command to execute the JAR file.
    """
    # If the JAR file is not specified, use the default JAR file
    if jar is None:
        jar = (
            Path(os.environ.get("XDG_CACHE_HOME", "~/.cache"))
            / "Evolver"
            / "Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar"
        )
    jar = jar.expanduser().resolve()

    if not jar.is_file():
        raise JavaException(f"JAR file {jar} does not exist.")

    # Construct the command to run the JAR file
    command = ["java", "-cp", str(jar), java_class]
    if not enable_logs:
        command.insert(
            1, "-Djava.util.logging.config.file=resources/evolver.log.config"
        )
    command.extend(args)

    return command

def execute_evolver(
    java_class: str,
    *,
    jar: Path = None,
    args: list[str] = [],
    enable_logs: bool = False,
):
    """
    Executes a JAR file and returns the output.

    :param jar: The path to the JAR file.
    :param java_class: The name of the Java class to execute.
    :param args: The arguments to pass to the JAR file.
    :param enable_logs: Whether to enable the Evolver logs.
    :return: The output of the JAR file.
    """
    command = create_command(java_class, jar, args, enable_logs)

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
    enable_logs: bool = False,
):
    """
    Executes a JAR file and yields the output line by line.

    :param jar: The path to the JAR file.
    :param java_class: The name of the Java class to execute.
    :param args: The arguments to pass to the JAR file.
    :param enable_logs: Whether to enable the Evolver logs.
    :yield: Each line of the JAR file's output.
    """
    command = create_command(java_class, jar, args, enable_logs)

    # Run the JAR file using a subprocess
    with subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True) as process:
        for line in process.stderr:
            yield line.strip()

        process.wait()

        if process.returncode != 0:
            error_output = process.stderr.read().strip()
            raise JavaException(
                f"JAR execution failed with return code {process.returncode}. Error message:\n{error_output}"
            )
