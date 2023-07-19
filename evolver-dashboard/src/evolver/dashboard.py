import json
import multiprocessing
import os
import time
from datetime import datetime
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
    download_link,
    extract_plot,
    github_logo,
    is_process_running,
    zip_directory,
)

# Configure logger
logger = get_logger()


# DASHBOARD
st.set_page_config(
    page_title="Evolver dashboard",
    page_icon=":sunglasses:",
    layout="wide",
    menu_items={"About": None},
)

# Read environment variables
evolver_jar = Path(
    os.environ.get(
        "EVOLVER_JAR", default="target/Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar"
    )
)

base_path = Path("/tmp/evolver")


# Session state
# Evolver states are stored as folders
def default_session_state(key: str, value):
    if key not in st.session_state:
        st.session_state[key] = value


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


def load_session_state(state: Path, filename: str) -> None:
    if (json_file := base_path / state / filename).exists():
        with open(json_file, "r") as fd:
            loaded_state = json.load(fd)

        for key, value in loaded_state.items():
            st.session_state[key] = value
    else:
        raise ValueError(f"File {json_file} does not exist.")


def dump_session_state(state: Path, filename: str) -> None:
    json_state = {key: value for key, value in st.session_state.items()}
    del json_state["state"]
    with open(base_path / state / filename, "w") as fd:
        json.dump(json_state, fd, indent=2)


with st.sidebar:
    st.header("Choose experiment")
    experiments = [f.name for f in base_path.iterdir()]

    if not experiments:
        experiments = [None]

    state = st.selectbox(
        "Available experiments",
        experiments,
        key="state",
    )
    if state:
        state = Path(state)

    new_state = st.text_input(
        "New experiment",
    )

    if st.button("Create new experiment", disabled=not bool(new_state)):
        # Ensure that the folder exists
        (base_path / new_state).mkdir(parents=True, exist_ok=True)

        st.experimental_rerun()

st.header("Evolver")
st.markdown(
    f"Evolver's source code and documentation can be found at "
    f"[{github_logo()} **jMetal/Evolver**](https://github.com/jMetal/Evolver).",
    unsafe_allow_html=True,
)

if state is None:
    st.info("Please select an experiment on the sidebar to continue.")
    # We can stop the load here, because if not the widgets don't load a default value.

# Read configuration values
with st.spinner("Reading configuration..."):
    if state:
        has_executed = (base_path / state / ".executed").exists()
        if has_executed:
            load_session_state(state, "config.json")
    else:
        # No experiment selected yet, disable everything
        has_executed = True

st.subheader("General configuration")
st.number_input(
    "Number of CPU cores",
    key="global_cpu_cores",
    min_value=1,
    disabled=has_executed,
)
st.number_input(
    "Plotting frequency",
    key="global_plotting_frequency",
    min_value=1,
    disabled=has_executed,
)

col1, col2 = st.columns(2)
with col1:
    st.subheader("Meta-optimizer configuration")
    st.selectbox(
        "Algorithm",
        meta_optimizers.keys(),
        key="meta_optimizer_algorithm",
        disabled=has_executed,
    )
    if st.session_state["meta_optimizer_algorithm"] == "Generational Genetic Algorithm":
        st.warning(
            "GGA is not a parallel algorithm. It will ignore the number of cores."
        )

    st.number_input(
        "Population size",
        key="meta_optimizer_population_size",
        min_value=1,
        disabled=has_executed,
    )
    st.number_input(
        "Maximum number of evaluations",
        key="meta_optimizer_max_evaluations",
        min_value=1,
        disabled=has_executed,
    )
    st.multiselect(
        "Indicators",
        quality_indicators.keys(),
        key="meta_optimizer_indicators_names",
        disabled=has_executed,
    )
with col2:
    st.subheader("Meta-optimization problem configuration")
    st.selectbox(
        "Configurable algorithm",
        configurable_algorithms.keys(),
        key="configurable_algorithm_algorithm",
        disabled=has_executed,
    )
    st.number_input(
        "Population size",
        key="configurable_algorithm_population_size",
        min_value=1,
        disabled=has_executed,
    )
    st.number_input(
        "Number of independent runs",
        key="configurable_algorithm_independent_runs",
        min_value=1,
        disabled=has_executed,
    )
    st.multiselect(
        "Problems",
        problems.keys(),
        key="configurable_algorithm_problems",
        disabled=has_executed,
    )

    evaluations = []
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
                value=evaluations_value,
                min_value=1,
                disabled=has_executed,
            )
        )

    st.session_state["configurable_algorithm_max_number_of_evaluations"] = evaluations

if state is None:
    st.stop()

with st.expander("Manually change configuration"):
    # This yaml should be used for everything
    str_configuration = f"""general_config:
    dashboard_mode: True # Required to plot graphs in the dashboard
    output_directory: {base_path / state}
    cpu_cores: {st.session_state["global_cpu_cores"]}
    plotting_frequency: {st.session_state["global_plotting_frequency"]}

external_algorithm_arguments:
    meta_optimizer_algorithm: {meta_optimizers[st.session_state["meta_optimizer_algorithm"]]}
    meta_optimizer_population_size: {st.session_state["meta_optimizer_population_size"]}
    meta_optimizer_max_evaluations: {st.session_state["meta_optimizer_max_evaluations"]}
    indicators_names: {",".join([quality_indicators[indicator] for indicator in st.session_state["meta_optimizer_indicators_names"]])}

internal_algorithm_arguments:
    configurable_algorithm: {configurable_algorithms[st.session_state["configurable_algorithm_algorithm"]]}
    internal_population_size: {st.session_state["configurable_algorithm_population_size"]}
    independent_runs: {st.session_state["configurable_algorithm_independent_runs"]}
    problem_names: {",".join([problems[problem] for problem in st.session_state["configurable_algorithm_problems"]])}
    reference_front_file_name: {",".join([referenceFront[problem] for problem in st.session_state["configurable_algorithm_problems"]])}
    max_number_of_evaluations: {",".join([str(num_evaluations) for num_evaluations in st.session_state["configurable_algorithm_max_number_of_evaluations"]])}

optional_specific_arguments:
    # For Configurable-MOEAD only, probably shouldn't be modified
    weight_vector_files_directory: resources/weightVectors"""

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
log_file = base_path / state / "evolver.log"
if not has_executed:
    st.header("Execute Evolver")
    if st.button(
        "Execute",
        disabled=has_executed,
    ):
        temp_file = base_path / state / "evolver-config.yaml"

        with open(temp_file, "w") as f:
            f.write(configuration)

        dump_session_state(state, "config.json")

        java_class = "org.uma.evolver.MetaRunner"
        args = [str(temp_file)]

        pid = execute_evolver_streaming_to_disk(
            java_class,
            log_file,
            args=args,
            jar=evolver_jar,
        )
        with open(base_path / state / ".executed", "w") as execution_lock:
            execution_lock.write(str(pid))
        st.experimental_rerun()


if has_executed:
    st.header("Evolver progress")
    if st.button("Refresh"):
        st.experimental_rerun()

    progress_bar = st.progress(0, "Meta-optimizer progress")
    # Prepare a block to showcase the results later
    results_block = st.empty()

    with st.spinner("Reading logs..."):
        while not log_file.exists():
            time.sleep(0.1)
        with open(log_file, "r") as logs_fd:
            logs = logs_fd.read()
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

        with st.expander("Execution logs"):
            st_components.html(
                f"""<pre>{logs}</pre>""",
                height=600,
                scrolling=True,
            )  # Add it in code block
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

    if not is_finished:
        with results_block.container():
            st.success("Evolver execution finished!")
            st.balloons()

            with TemporaryFile() as tmp:
                with ZipFile(tmp, "w", ZIP_DEFLATED) as zip_file:
                    zip_directory(base_path / state, zip_file)
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
