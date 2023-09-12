import json
import multiprocessing
import os
import time
from pathlib import Path
from tempfile import TemporaryFile
from zipfile import ZIP_DEFLATED, ZipFile

import streamlit as st
import streamlit.components.v1 as st_components
from evolver.components import (
    configurable_algorithms,
    meta_optimizers,
    problems,
    quality_indicators,
    referenceFront,
)
from evolver.execute import execute_evolver_streaming_to_disk
from evolver.logs import get_logger
from evolver.utils import (
    EvolverDashboardException,
    download_link,
    extract_plot,
    github_logo,
    is_process_running,
    zip_directory,
)

# Configure logger
logger = get_logger()

# Read environment variables, and define constants
EVOLVER_JAR = Path(
    os.environ.get(
        "EVOLVER_JAR", default="target/Evolver-1.0-jar-with-dependencies.jar"
    )
)
BASE_PATH = Path(os.environ.get("EVOLVER_BASE_PATH", default="./evolver-data"))
RESOURCES_PATH = Path(os.environ.get("EVOLVER_RESOURCES_PATH", default="./resources"))
EVOLVER_STATE = None


# Define auxiliary functions
def get_experiment_folders() -> list[str]:
    """Get the list of experiment folders."""
    return [f.name for f in BASE_PATH.iterdir()]


def default_session_state(key: str, value):
    """Set a default value for a session state variable

    Args:
        key (str): The key to set the default value for
        value (Any): The default value
    """
    if key not in st.session_state:
        st.session_state[key] = value


def create_new_state():
    """Create a new Evolver state and change the current Evolver state to the newly
    created one.
    """
    new_state = st.session_state["new_state"]
    # Create the new state folder
    (BASE_PATH / new_state).mkdir(parents=True, exist_ok=True)
    st.session_state["state"] = new_state
    st.session_state["new_state"] = ""


def load_session_state(evolver_state: Path, filename: str) -> None:
    """Load the session state from a json file. This must be called before
    the widgets are created.

    Args:
        evolver_state (Path): The state folder for a experiment of Evolver
        filename (str): The json file name

    Raises:
        EvolverDashboardException: If the file does not exist
    """
    if (json_file := evolver_state / filename).exists():
        with open(json_file, "r") as fd:
            loaded_state = json.load(fd)

        logger.debug(f"Loaded session state: {loaded_state}")
        for key, value in loaded_state.items():
            st.session_state[key] = value
    else:
        raise EvolverDashboardException(f"File {json_file} does not exist.")


def dump_session_state(evolver_state: Path, filename: str) -> None:
    """Dump the session state to a json file.

    Args:
        evolver_state (Path): The state folder for a experiment of Evolver
        filename (str): The json file name

    Raises:
        EvolverDashboardException: If the file does not exist
    """
    json_state = {key: value for key, value in st.session_state.items()}
    # Delete unwanted keys
    del json_state["state"]
    del json_state["new_state"]
    with open(evolver_state / filename, "w") as fd:
        json.dump(json_state, fd, indent=2)


# From here on, the dashboard starts
st.set_page_config(
    page_title="Evolver dashboard",
    page_icon=":sunglasses:",
    layout="wide",
    menu_items={"About": None},
)

# Initialize session state
default_session_state("global_cpu_cores", multiprocessing.cpu_count())
default_session_state("global_plotting_frequency", 10)
default_session_state("meta_optimizer_algorithm", "NSGA-II")
default_session_state("meta_optimizer_population_size", 50)
default_session_state("meta_optimizer_max_evaluations", 3000)
default_session_state(
    "meta_optimizer_indicators_names", ["Normalized Hypervolume", "Epsilon"]
)
default_session_state("configurable_algorithm_algorithm", "NSGA-II")
default_session_state("configurable_algorithm_population_size", 100)
default_session_state("configurable_algorithm_independent_runs", 3)
default_session_state("configurable_algorithm_problems", ["ZDT1", "ZDT4"])
default_session_state("configurable_algorithm_max_number_of_evaluations", [8000, 16000])

# Create the experiment selector on the sidebar
with st.sidebar:
    st.header("Choose experiment")
    experiments = get_experiment_folders()

    current_state = st.selectbox(
        "Available experiments",
        experiments,
        help="Current experiment being shown in the main window",
        key="state",
    )

    if current_state:
        EVOLVER_STATE = Path(current_state)

    # Form to create a new experiment
    new_state = st.text_input(
        "Create new experiment",
        help="Name for the new experiment",
        key="new_state",
    )

    st.button(
        "Create",
        help="Create a new experiment with the given name",
        disabled=not bool(new_state),
        on_click=create_new_state,
    )

st.header("Evolver")
st.markdown(
    f"Evolver's source code and documentation can be found at "
    f"[{github_logo()} **jMetal/Evolver**](https://github.com/jMetal/Evolver).",
    unsafe_allow_html=True,
)

# Check if an experiment is selected
if EVOLVER_STATE is None:
    st.info("Please select an experiment on the sidebar to continue.")
    # We can stop the load here, because if not the widgets don't load a default value.

# Read configuration values
with st.spinner("Reading configuration..."):
    if EVOLVER_STATE:
        # Check if the experiment has already been executed
        # by checking for the lock file
        has_executed = (BASE_PATH / EVOLVER_STATE / ".executed").exists()
        if has_executed:
            # Load the session state from disk
            load_session_state(BASE_PATH / EVOLVER_STATE, "config.json")
    else:
        # No experiment selected yet, disable everything
        has_executed = True

# General configuration
st.subheader("General configuration")
st.number_input(
    "Number of CPU cores",
    help=(
        "Number of cores to be used by the meta-optimizer "
        "(ignored if the meta-optimizer cannot be parallelized)"
    ),
    key="global_cpu_cores",
    min_value=1,
    disabled=has_executed,
)
st.number_input(
    "Plotting frequency",
    help=(
        "Frequency to plot the Pareto front approximation obtained by "
        "the meta-optimizer during the search. It must be a multiple of "
        "the meta-optimizer population size"
    ),
    key="global_plotting_frequency",
    min_value=1,
    disabled=has_executed,
)

# Meta-optimizer and configurable algorithm configurations
meta_optimizer_column, configurable_algorithm_column = st.columns(2)
with meta_optimizer_column:
    st.subheader("Meta-optimizer configuration")
    st.selectbox(
        "Algorithm",
        meta_optimizers.keys(),
        help=(
            "Meta-optimizer algorithm to be used. It is advisable to "
            "choose among those that can be run in parallel"
        ),
        key="meta_optimizer_algorithm",
        disabled=has_executed,
    )
    if st.session_state["meta_optimizer_algorithm"] == "Generational Genetic Algorithm":
        st.warning(
            "GGA is not a parallel algorithm. It will ignore the number of cores."
        )

    st.number_input(
        "Population size",
        help="Population size for the meta-topimizer",
        key="meta_optimizer_population_size",
        min_value=1,
        disabled=has_executed,
    )
    st.number_input(
        "Maximum number of evaluations",
        help="Maximum number of evaluations for the meta-optimizer",
        key="meta_optimizer_max_evaluations",
        min_value=1,
        disabled=has_executed,
    )
    st.multiselect(
        "Indicators",
        quality_indicators.keys(),
        help=(
            "Quality indicators to be used by the "
            "meta-optimizer to evaluate each configuration"
        ),
        key="meta_optimizer_indicators_names",
        disabled=has_executed,
    )
with configurable_algorithm_column:
    st.subheader("Meta-optimization problem configuration")
    st.selectbox(
        "Configurable algorithm",
        configurable_algorithms.keys(),
        help="Configurable algorithm to be optimized for a set of problems",
        key="configurable_algorithm_algorithm",
        disabled=has_executed,
    )
    st.number_input(
        "Population size",
        help="Population size for the configurable algorithm",
        key="configurable_algorithm_population_size",
        min_value=1,
        disabled=has_executed,
    )
    st.number_input(
        "Number of independent runs",
        help=(
            "Number of independent runs for the configurable algorithm. "
            "The median of all execution will be used to evaluate the configuration"
        ),
        key="configurable_algorithm_independent_runs",
        min_value=1,
        disabled=has_executed,
    )
    st.multiselect(
        "Problems",
        problems.keys(),
        help="Problems to be used as the target to optimize the configurable algorithm",
        key="configurable_algorithm_problems",
        disabled=has_executed,
    )

    evaluations = []
    # Ensure the length of the evaluations list is the same as the number of problems
    for i, problem in enumerate(st.session_state["configurable_algorithm_problems"]):
        if i < len(
            st.session_state["configurable_algorithm_max_number_of_evaluations"]
        ):
            evaluations_value = st.session_state[
                "configurable_algorithm_max_number_of_evaluations"
            ][i]
        else:
            evaluations_value = 8000

        evaluations.append(
            st.number_input(
                f"Maximum number of evaluations for {problem}",
                help=f"Maximum number of evaluations for the {problem} problem",
                value=evaluations_value,
                min_value=1,
                disabled=has_executed,
            )
        )

    st.session_state["configurable_algorithm_max_number_of_evaluations"] = evaluations

# Here we stop the load if no state is choosen
if EVOLVER_STATE is None:
    st.stop()

# Add option to manually change the configuration
with st.expander("Manually change configuration"):
    # Extract long values to maintain readability
    _meta_opt_alg = meta_optimizers[st.session_state["meta_optimizer_algorithm"]]
    _meta_opt_ind = ",".join(
        [
            quality_indicators[indicator]
            for indicator in st.session_state["meta_optimizer_indicators_names"]
        ]
    )
    _int_alg_alg = configurable_algorithms[
        st.session_state["configurable_algorithm_algorithm"]
    ]
    _int_alg_pop = st.session_state["configurable_algorithm_population_size"]
    _int_alg_prob = ",".join(
        [
            problems[problem]
            for problem in st.session_state["configurable_algorithm_problems"]
        ]
    )
    _int_alg_ref = ",".join(
        [
            str(RESOURCES_PATH / referenceFront[problem])
            for problem in st.session_state["configurable_algorithm_problems"]
        ]
    )
    _int_alg_eval = ",".join(
        [
            str(num_evaluations)
            for num_evaluations in st.session_state[
                "configurable_algorithm_max_number_of_evaluations"
            ]
        ]
    )
    # TODO: This yaml should be used for load and store the state to disk
    str_configuration = f"""general_config:
    dashboard_mode: True # Required to plot graphs in the dashboard
    output_directory: {BASE_PATH / EVOLVER_STATE}
    cpu_cores: {st.session_state["global_cpu_cores"]}
    plotting_frequency: {st.session_state["global_plotting_frequency"]}

external_algorithm_arguments:
    meta_optimizer_algorithm: {_meta_opt_alg}
    meta_optimizer_population_size: {st.session_state["meta_optimizer_population_size"]}
    meta_optimizer_max_evaluations: {st.session_state["meta_optimizer_max_evaluations"]}
    indicators_names: {_meta_opt_ind}

internal_algorithm_arguments:
    configurable_algorithm: {_int_alg_alg}
    internal_population_size: {_int_alg_pop}
    independent_runs: {st.session_state["configurable_algorithm_independent_runs"]}
    problem_names: {_int_alg_prob}
    reference_front_file_name: {_int_alg_ref}
    max_number_of_evaluations: {_int_alg_eval}

optional_specific_arguments:
    # For Configurable-MOEAD only, probably shouldn't be modified
    weight_vector_files_directory: {RESOURCES_PATH}/weightVectors"""

    configuration = st.text_area(
        "MetaRunner configuration",
        value=str_configuration,
        height=600,
        disabled=has_executed,
    )
    config_link = download_link(
        "here", str_configuration, file_name="config.yaml", mime="text/yaml"
    )

    st.markdown(
        "##### This configuration for Evolver's `MetaRunner`"
        f" can be downloaded {config_link}.",
        unsafe_allow_html=True,
    )


# Execute evolver
log_file = BASE_PATH / EVOLVER_STATE / "evolver.log"
if not has_executed:
    st.header("Execute Evolver")
    if st.button(
        "Execute",
        disabled=has_executed,
    ):
        # Store configuration values in a temporary file to pass to Evolver
        temp_file = BASE_PATH / EVOLVER_STATE / "evolver-config.yaml"

        with open(temp_file, "w") as f:
            f.write(configuration)

        # Save session state to disk
        dump_session_state(BASE_PATH / EVOLVER_STATE, "config.json")

        java_class = "org.uma.evolver.MetaRunner"
        args = [str(temp_file)]

        # Execute Evolver jar
        pid = execute_evolver_streaming_to_disk(
            java_class,
            log_file,
            args=args,
            jar=EVOLVER_JAR,
        )

        # Create lock to prevent multiple executions
        with open(BASE_PATH / EVOLVER_STATE / ".executed", "w") as execution_lock:
            execution_lock.write(str(pid))
        st.experimental_rerun()

# Once the execution starts, track the progress
if has_executed:
    st.header("Evolver progress")
    if st.button("Refresh"):
        st.experimental_rerun()

    progress_bar = st.progress(0, "Meta-optimizer progress")
    # Prepare a block to showcase the results later
    results_block = st.empty()

    with st.spinner("Reading logs..."):
        # Wait for the log file to be created after Evolver starts
        while not log_file.exists():
            time.sleep(0.1)

        # Read the log file
        with open(log_file, "r") as logs_fd:
            logs = logs_fd.read()

        # Extract from the logs if the execution is finished and the latest front
        is_finished = is_process_running(logs)
        plot, progress = extract_plot(logs)
        if plot:
            max_evals = st.session_state["meta_optimizer_max_evaluations"]
            if progress / max_evals > 1:
                logger.warning(
                    f"Progress is greater than 100%:\n"
                    f"Progress: {progress}\n"
                    f"Max evaluations:"
                    f"{max_evals}"
                )
            progress_percentage = min(
                progress / max_evals,
                1,
            )
            progress_bar.progress(progress_percentage, "Meta-optimizer progress")

            st.vega_lite_chart(plot, use_container_width=True)
        else:
            min_evaluations = max(
                st.session_state["global_plotting_frequency"],
                st.session_state["meta_optimizer_population_size"],
            )
            st.info(
                "No plot available yet. First results will be ready when "
                f"{min_evaluations} evaluations are done"
            )

        # Show a expander with the logs
        with st.expander("Execution logs"):
            st_components.html(
                f"""<pre>{logs}</pre>""",
                height=600,
                scrolling=True,
            )  # TODO: Add it in code block and support Dark theme
            with open(log_file, "r") as logs_fd:
                logs_link = download_link(
                    "here",
                    logs,
                    file_name="evolver-logs.txt",
                    mime="text/plain",
                )
                st.markdown(
                    "##### Execution logs can be " f"downloaded {logs_link}.",
                    unsafe_allow_html=True,
                )

    # Show the results if the execution is finished
    if not is_finished:
        with results_block.container():
            st.success("Evolver execution finished!")
            st.balloons()

            # Zip the whole state folder
            with TemporaryFile() as tmp:
                with ZipFile(tmp, "w", ZIP_DEFLATED) as zip_file:
                    zip_directory(BASE_PATH / EVOLVER_STATE, zip_file)
                tmp.seek(0)

                # The oficial streamlit download button refreshes the page,
                # so we use a custom one
                # st.download_button(
                #   "Download artifacts", tmp.read(),
                #   file_name="evolver.zip", mime="application/zip"
                # )

                zip_link = download_link(
                    "here", tmp.read(), file_name="evolver.zip", mime="application/zip"
                )

                st.markdown(
                    f"##### Execution artifacts can be downloaded {zip_link}.",
                    unsafe_allow_html=True,
                )
