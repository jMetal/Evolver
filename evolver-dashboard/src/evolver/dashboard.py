import multiprocessing
import os
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
from evolver.execute import execute_evolver_streaming
from evolver.logs import get_logger
from evolver.utils import download_link, extract_plot, github_logo, zip_directory
from streamlit_server_state import no_rerun, server_state, server_state_lock

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
evolver_jar = Path(os.environ.get("EVOLVER_JAR", default="target/Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar"))

# Server-wide State
with server_state_lock["is_running"]:
    if "is_running" not in server_state:
        server_state["is_running"] = False

with server_state_lock["config"]:
    if "config" not in server_state:
        date = datetime.now().strftime('%Y%m%d-%H%M%S')
        server_state["config"] = {
            "output_directory": f"/tmp/evolver/{date}",
            "cpu_cores": multiprocessing.cpu_count(),
            "plotting_frequency": 10,
        }

with server_state_lock["logs"]:
    if "logs" not in server_state:
        server_state["logs"] = ""

with server_state_lock["meta_optimizer"]:
    if "meta_optimizer" not in server_state:
        server_state["meta_optimizer"] = {
            "algorithm": "NSGA-II",
            "population_size": 50,
            "max_evaluations": 3000,
            "independent_runs": 3,
            "indicators_names": ["Normalized Hypervolume", "Epsilon"],
        }

with server_state_lock["configurable_algorithm"]:
    if "configurable_algorithm" not in server_state:
        server_state["configurable_algorithm"] = {
            "algorithm": "NSGA-II",
            "population_size": 100,
            "problems": ["ZDT1", "ZDT4"],
            "max_number_of_evaluations": [8000, 16000],
        }


st.header("Evolver")
st.markdown(
    f"Evolver's source code and documentation can be found at "
    f"[{github_logo()} **jMetal/Evolver**](https://github.com/jMetal/Evolver).",
    unsafe_allow_html=True,
)

st.subheader("General configuration")
with server_state_lock["config"]:
    tmp_folder = st.text_input(
        "Folder to store the results",
        value=server_state["config"]["output_directory"],
        disabled=server_state["is_running"],
    )
    server_state["config"]["output_directory"] = Path(tmp_folder)
    server_state["config"]["cpu_cores"] = st.number_input(
        "Number of CPU cores",
        value=server_state["config"]["cpu_cores"],
        min_value=1,
        disabled=server_state["is_running"],
    )
    server_state["config"]["plotting_frequency"] = st.number_input(
        "Plotting frequency",
        value=server_state["config"]["plotting_frequency"],
        min_value=1,
        disabled=server_state["is_running"],
    )

col1, col2 = st.columns(2)
with col1:
    st.subheader("Meta-optimizer configuration")
    with server_state_lock["meta_optimizer"]:
        algorithm_key = list(meta_optimizers.keys()).index(
            server_state["meta_optimizer"]["algorithm"]
        )
        server_state["meta_optimizer"]["algorithm"] = st.selectbox(
            "Algorithm",
            meta_optimizers.keys(),
            index=algorithm_key,
            disabled=server_state["is_running"],
        )
        if (
            server_state["meta_optimizer"]["algorithm"]
            == "Generational Genetic Algorithm"
        ):
            st.warning(
                "GGA is not a parallel algorithm. It will ignore the number of cores."
            )

        server_state["meta_optimizer"]["population_size"] = st.number_input(
            "Population size",
            value=server_state["meta_optimizer"]["population_size"],
            min_value=1,
            disabled=server_state["is_running"],
        )
        server_state["meta_optimizer"]["max_evaluations"] = st.number_input(
            "Maximum number of evaluations",
            value=server_state["meta_optimizer"]["max_evaluations"],
            min_value=1,
            disabled=server_state["is_running"],
        )
        server_state["meta_optimizer"]["independent_runs"] = st.number_input(
            "Number of independent runs",
            value=server_state["meta_optimizer"]["independent_runs"],
            min_value=1,
            disabled=server_state["is_running"],
        )
        server_state["meta_optimizer"]["indicators_names"] = st.multiselect(
            "Indicators",
            quality_indicators.keys(),
            default=server_state["meta_optimizer"]["indicators_names"],
            disabled=server_state["is_running"],
        )
with col2:
    st.subheader("Meta-optimization problem configuration")
    with server_state_lock["configurable_algorithm"]:
        configurable_algorithm_key = list(configurable_algorithms.keys()).index(
            server_state["configurable_algorithm"]["algorithm"]
        )
        server_state["configurable_algorithm"]["algorithm"] = st.selectbox(
            "Configurable algorithm",
            configurable_algorithms.keys(),
            index=configurable_algorithm_key,
            disabled=server_state["is_running"],
        )
        server_state["configurable_algorithm"]["population_size"] = st.number_input(
            "Population size",
            value=server_state["configurable_algorithm"]["population_size"],
            min_value=1,
            disabled=server_state["is_running"],
        )
        server_state["configurable_algorithm"]["problems"] = st.multiselect(
            "Problems",
            problems.keys(),
            default=server_state["configurable_algorithm"]["problems"],
            disabled=server_state["is_running"],
        )
        if (
            len_evaluations := len(
                server_state["configurable_algorithm"]["max_number_of_evaluations"]
            )
        ) < (len_problems := len(server_state["configurable_algorithm"]["problems"])):
            for _ in range(len_problems - len_evaluations):
                server_state["configurable_algorithm"][
                    "max_number_of_evaluations"
                ].append(8000)
        evaluations = []
        for i, problem in enumerate(server_state["configurable_algorithm"]["problems"]):
            evaluations.append(
                st.number_input(
                    f"Maximum number of evaluations for {problem}",
                    value=server_state["configurable_algorithm"][
                        "max_number_of_evaluations"
                    ][i],
                    min_value=1,
                    disabled=server_state["is_running"],
                )
            )
        server_state["configurable_algorithm"][
            "max_number_of_evaluations"
        ] = evaluations


with st.expander("Manually change configuration"):
    with server_state_lock["config"], server_state_lock[
        "meta_optimizer"
    ], server_state_lock["configurable_algorithm"]:
        str_configuration = f"""general_config:
    dashboard_mode: True # Required to plot graphs in the dashboard
    output_directory: {server_state["config"]["output_directory"]}
    cpu_cores: {server_state["config"]["cpu_cores"]}
    plotting_frequency: {server_state["config"]["plotting_frequency"]}

external_algorithm_arguments:
    meta_optimizer_algorithm: {meta_optimizers[server_state["meta_optimizer"]["algorithm"]]}
    meta_optimizer_population_size: {server_state["meta_optimizer"]["population_size"]}
    meta_optimizer_max_evaluations: {server_state["meta_optimizer"]["max_evaluations"]}
    independent_runs: {server_state["meta_optimizer"]["independent_runs"]}
    indicators_names: {",".join([quality_indicators[indicator] for indicator in server_state["meta_optimizer"]["indicators_names"]])}

internal_algorithm_arguments:
    configurable_algorithm: {configurable_algorithms[server_state["configurable_algorithm"]["algorithm"]]}
    internal_population_size: {server_state["configurable_algorithm"]["population_size"]}
    problem_names: {",".join([problems[problem] for problem in server_state["configurable_algorithm"]["problems"]])}
    reference_front_file_name: {",".join([referenceFront[problem] for problem in server_state["configurable_algorithm"]["problems"]])}
    max_number_of_evaluations: {",".join([str(num_evaluations) for num_evaluations in server_state["configurable_algorithm"]["max_number_of_evaluations"]])}

optional_specific_arguments:
    # For Configurable-MOEAD only, probably shouldn't be modified
    weight_vector_files_directory: resources/weightVectors"""

    with server_state_lock["is_running"]:
        configuration = st.text_area(
            "MetaRunner configuration",
            value=str_configuration,
            height=600,
            disabled=server_state["is_running"],
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
st.header("Execute Evolver")
with server_state_lock["is_running"]:
    if st.button("Execute", disabled=server_state["is_running"]):
        with server_state_lock["config"]:
            base_path = Path(server_state["config"]["output_directory"])
        base_path.mkdir(parents=True, exist_ok=True)
        temp_file = base_path / "evolver-config.yaml"

        with open(temp_file, "w") as f:
            f.write(configuration)

        java_class = "org.uma.evolver.MetaRunner"
        args = [str(temp_file)]

        with server_state_lock["execution"]:
            if "execution" not in server_state:
                server_state["execution"] = execute_evolver_streaming(
                    java_class,
                    args=args,
                    jar=evolver_jar,
                    enable_logs=True,
                )
                server_state["is_running"] = True
                st.experimental_rerun()

concurrency_warning = st.empty()

# Placeholder to show warning if focus is taken by other instance
with server_state_lock["is_running"]:
    if server_state["is_running"]:
        with concurrency_warning.container():
            st.warning(
                "If you can't see the execution logs, "
                "please check if another instance of Evolver is running.\n"
                "If there is no other, this will update as soon as the next checkpoint "
                "is reached."
            )

with server_state_lock["execution"]:
    if "execution" in server_state:
        with concurrency_warning.container():
            st.empty()
        spinner_block = st.empty()

        progress_bar = st.progress(0, "Meta-optimizer progress")
        # Prepare a block to showcase the results later
        results_block = st.empty()

        # Prepare a block to show progress plot
        plot_block = st.empty()

        expander_block = st.empty()

        with spinner_block.container():
            with st.spinner("Executing Evolver..."):
                with expander_block.container():
                    with st.expander("Execution logs"):
                        # Prepare a block to show the logs
                        logs_block = st.empty()

                    for log_line in server_state["execution"]:
                        if "Evolver dashboard front plot" in log_line:
                            plot, progress = extract_plot(log_line)
                            with server_state_lock["meta_optimizer"]:
                                max_evals = server_state["meta_optimizer"][
                                    "max_evaluations"
                                ]
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
                                progress_bar.progress(
                                    progress_percentage, "Meta-optimizer progress"
                                )
                            with plot_block.container():
                                st.vega_lite_chart(plot, use_container_width=True)
                        else:
                            with no_rerun:
                                with server_state_lock["logs"]:
                                    server_state["logs"] = (
                                        server_state["logs"] + log_line + "\n"
                                    )
                                    with logs_block.container():
                                        st_components.html(
                                            f"""<pre>{server_state["logs"]}</pre>""",
                                            height=600,
                                            scrolling=True,
                                        )  # Add it in code block
                                        logs_link = download_link(
                                            "here",
                                            server_state["logs"],
                                            file_name="evolver-logs.txt",
                                            mime="text/plain",
                                        )
                                        st.markdown(
                                            "##### Execution logs can be "
                                            f"downloaded {logs_link}.",
                                            unsafe_allow_html=True,
                                        )

        # Prepare a block to reset the execution
        reset_block = st.empty()

        with results_block.container():
            st.success("Evolver execution finished!")
            st.balloons()

            with TemporaryFile() as tmp:
                with ZipFile(tmp, "w", ZIP_DEFLATED) as zip_file:
                    with server_state_lock["config"]:
                        zip_directory(
                            server_state["config"]["output_directory"], zip_file
                        )
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
            with reset_block.container():
                st.subheader("Reset execution to run a new one")
                if st.button("Reset"):
                    with server_state_lock["is_running"]:
                        del server_state["is_running"]
                    with server_state_lock["execution"]:
                        del server_state["execution"]
                    with server_state_lock["logs"]:
                        del server_state["logs"]

                    st.experimental_rerun()
