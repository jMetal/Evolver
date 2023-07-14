import multiprocessing
from datetime import datetime
from pathlib import Path
from tempfile import TemporaryFile
from zipfile import ZIP_DEFLATED, ZipFile

import streamlit as st
import streamlit.components.v1 as st_components
from evolver.execute import execute_evolver_streaming
from evolver.utils import download_link, extract_plot, zip_directory

# TODO: Extract all this in run.sh and add it as parameter to the dashboard
meta_optimizers = {
    "NSGA-II": "NSGAII",
    "Asynchronous NSGA-II": "ASYNCNSGAII",
    "Generational Genetic Algorithm": "GGA",
    "Speed-constrained Multi-objective Particle Swarm Optimization": "SMPSO",
}

configurable_algorithms = {
    "NSGA-II": "NSGAII",
    "NSGA-II with Diferential": "NSGAIIDE",
    "MOPSO": "MOPSO",
    "MOEA/D": "MOEAD",
    "SMS-EMOA": "SMSEMOA",
}

problems = {
    "ZDT1": "org.uma.jmetal.problem.multiobjective.zdt.ZDT1",
    "ZDT2": "org.uma.jmetal.problem.multiobjective.zdt.ZDT2",
    "ZDT3": "org.uma.jmetal.problem.multiobjective.zdt.ZDT3",
    "ZDT4": "org.uma.jmetal.problem.multiobjective.zdt.ZDT4",
    "ZDT6": "org.uma.jmetal.problem.multiobjective.zdt.ZDT6",
    "DTLZ1": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1",
    "DTLZ2": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2",
    "DTLZ3": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3",
    "DTLZ4": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4",
    "DTLZ5": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5",
    "DTLZ6": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6",
    "DTLZ7": "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7",
    "WFG1": "org.uma.jmetal.problem.multiobjective.wfg.WFG1",
    "WFG2": "org.uma.jmetal.problem.multiobjective.wfg.WFG2",
    "WFG3": "org.uma.jmetal.problem.multiobjective.wfg.WFG3",
    "WFG4": "org.uma.jmetal.problem.multiobjective.wfg.WFG4",
    "WFG5": "org.uma.jmetal.problem.multiobjective.wfg.WFG5",
    "WFG6": "org.uma.jmetal.problem.multiobjective.wfg.WFG6",
    "WFG7": "org.uma.jmetal.problem.multiobjective.wfg.WFG7",
    "WFG8": "org.uma.jmetal.problem.multiobjective.wfg.WFG8",
    "WFG9": "org.uma.jmetal.problem.multiobjective.wfg.WFG9",
    "UF1": "org.uma.jmetal.problem.multiobjective.uf.UF1",
    "UF2": "org.uma.jmetal.problem.multiobjective.uf.UF2",
    "UF3": "org.uma.jmetal.problem.multiobjective.uf.UF3",
    "UF4": "org.uma.jmetal.problem.multiobjective.uf.UF4",
    "UF5": "org.uma.jmetal.problem.multiobjective.uf.UF5",
    "UF6": "org.uma.jmetal.problem.multiobjective.uf.UF6",
    "UF7": "org.uma.jmetal.problem.multiobjective.uf.UF7",
    "UF8": "org.uma.jmetal.problem.multiobjective.uf.UF8",
    "UF9": "org.uma.jmetal.problem.multiobjective.uf.UF9",
    "UF10": "org.uma.jmetal.problem.multiobjective.uf.UF10",
}

referenceFront = {
    "ZDT1": "resources/referenceFronts/ZDT1.csv",
    "ZDT2": "resources/referenceFronts/ZDT2.csv",
    "ZDT3": "resources/referenceFronts/ZDT3.csv",
    "ZDT4": "resources/referenceFronts/ZDT4.csv",
    "ZDT6": "resources/referenceFronts/ZDT6.csv",
    "DTLZ1": "resources/referenceFronts/DTLZ1.3D.csv",
    "DTLZ2": "resources/referenceFronts/DTLZ2.3D.csv",
    "DTLZ3": "resources/referenceFronts/DTLZ3.3D.csv",
    "DTLZ4": "resources/referenceFronts/DTLZ4.3D.csv",
    "DTLZ5": "resources/referenceFronts/DTLZ5.3D.csv",
    "DTLZ6": "resources/referenceFronts/DTLZ6.3D.csv",
    "DTLZ7": "resources/referenceFronts/DTLZ7.3D.csv",
    "WFG1": "resources/referenceFronts/WFG1.2D.csv",
    "WFG2": "resources/referenceFronts/WFG2.2D.csv",
    "WFG3": "resources/referenceFronts/WFG3.2D.csv",
    "WFG4": "resources/referenceFronts/WFG4.2D.csv",
    "WFG5": "resources/referenceFronts/WFG5.2D.csv",
    "WFG6": "resources/referenceFronts/WFG6.2D.csv",
    "WFG7": "resources/referenceFronts/WFG7.2D.csv",
    "WFG8": "resources/referenceFronts/WFG8.2D.csv",
    "WFG9": "resources/referenceFronts/WFG9.2D.csv",
    "UF1": "resources/referenceFronts/UF1.csv",
    "UF2": "resources/referenceFronts/UF2.csv",
    "UF3": "resources/referenceFronts/UF3.csv",
    "UF4": "resources/referenceFronts/UF4.csv",
    "UF5": "resources/referenceFronts/UF5.csv",
    "UF6": "resources/referenceFronts/UF6.csv",
    "UF7": "resources/referenceFronts/UF7.csv",
    "UF8": "resources/referenceFronts/UF8.csv",
    "UF9": "resources/referenceFronts/UF9.csv",
    "UF10": "resources/referenceFronts/UF10.csv",
}

quality_indicators = {
    "Hypervolume": "HV",
    "Epsilon": "EP",
    "Spread": "SP",
    "Generational Distance": "GD",
    "Inverted Generational Distance": "IGD",
    "Inverted Generational Distance Plus": "IGD+",
    "Normalized Hypervolume": "NHV",
    "Generalized Spread": "GSPREAD",
}

# DASHBOARD
st.set_page_config(
    page_title="Evolver dashboard",
    page_icon=":sunglasses:",
    layout="wide",
    menu_items={"About": None},
)

st.header("Evolver configuration")
st.subheader("General configuration")
config = {}
tmp_folder = st.text_input("Folder to store the results", value="/tmp/evolver")
config["output_directory"] = Path(tmp_folder) / datetime.now().strftime("%Y%m%d-%H%M%S")
num_cores = multiprocessing.cpu_count()
config["cpu_cores"] = st.number_input("Number of CPU cores", value=num_cores, min_value=1)
config["observer_frequency"] = st.number_input("Observer frequency", value=10, min_value=1)
st.info("Observer frequency will always be equal or a multiple of the population size")

col1, col2 = st.columns(2)
with col1:
    st.subheader("Meta-optimizer configuration")
    meta_optimizer = {}
    meta_optimizer["algorithm"] = st.selectbox("Algorithm", meta_optimizers.keys())
    if meta_optimizer["algorithm"] == "Generational Genetic Algorithm":
        st.warning("GGA is not a parallel algorithm. It will ignore the number of cores.")

    meta_optimizer["population_size"] = st.number_input(
        "Population size", value=50, min_value=1
    )
    meta_optimizer["max_evaluations"] = st.number_input(
        "Maximum number of evaluations", value=3000, min_value=1
    )
    meta_optimizer["independent_runs"] = st.number_input(
        "number of independent runs", value=3, min_value=1
    )
    meta_optimizer["indicators_names"] = st.multiselect(
        "Indicators",
        quality_indicators.keys(),
        default=["Normalized Hypervolume", "Epsilon"],
    )
with col2:
    st.subheader("Internal configuration")
    internal_configuration = {}
    internal_configuration["algorithm"] = st.selectbox(
        "Algorithm", configurable_algorithms.keys()
    )
    internal_configuration["population_size"] = st.number_input(
        "Population size", value=100, min_value=1
    )
    internal_configuration["problems"] = st.multiselect(
        "Problems", problems.keys(), default=["ZDT1", "ZDT4"]
    )
    evaluations = []
    for problem in internal_configuration["problems"]:
        evaluations.append(
            st.number_input(
                f"Maximum number of evaluations for {problem}", value=8000, min_value=1
            )
        )
    internal_configuration["max_number_of_evaluations"] = evaluations


with st.expander("Manually change configuration"):
    configuration = st.text_area(
        "config",
        value=""
        f"""general_config:
    dashboard_mode: True # Required to plot graphs in the dashboard
    output_directory: {config["output_directory"]}
    cpu_cores: {config["cpu_cores"]}
    observer_frequency: {config["observer_frequency"]}

external_algorithm_arguments:
    meta_optimizer_algorithm: {meta_optimizers[meta_optimizer["algorithm"]]}
    meta_optimizer_population_size: {meta_optimizer["population_size"]}
    meta_optimizer_max_evaluations: {meta_optimizer["max_evaluations"]}
    independent_runs: {meta_optimizer["independent_runs"]}
    indicators_names: {",".join([quality_indicators[indicator] for indicator in meta_optimizer["indicators_names"]])}

internal_algorithm_arguments:
    configurable_algorithm: {configurable_algorithms[internal_configuration["algorithm"]]}
    internal_population_size: {internal_configuration["population_size"]}
    problem_names: {",".join([problems[problem] for problem in internal_configuration["problems"]])}
    reference_front_file_name: {",".join([referenceFront[problem] for problem in internal_configuration["problems"]])}
    max_number_of_evaluations: {",".join([str(num_evaluations) for num_evaluations in internal_configuration["max_number_of_evaluations"]])}

optional_specific_arguments:
    # For Configurable-MOEAD only, probably shouldn't be modified
    weight_vector_files_directory: resources/weightVectors""",
    )


# Execute evolver
st.header("Execute Evolver")
if st.button("Execute"):
    base_path = Path(config["output_directory"])
    base_path.mkdir(parents=True, exist_ok=True)
    temp_file = base_path / "evolver-config.yaml"

    with open(temp_file, "w") as f:
        f.write(configuration)

    java_class = "org.uma.evolver.MetaRunner"
    args = [str(temp_file)]

    execution = execute_evolver_streaming(
        java_class,
        args=args,
        jar=Path("target/Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar"),
        enable_logs=True,
    )

    # Prepare a block to show progress plot
    plot_block = st.empty()

    # Prepare a block to showcase the results later
    results_block = st.empty()

    logs = ""
    with st.spinner("Executing Evolver..."):
        with st.expander("Execution logs"):
            logs_block = st.empty()
            for log_line in execution:
                if "Evolver dashboard front plot" in log_line:
                    plot = extract_plot(log_line)
                    with plot_block.container():
                        st.vega_lite_chart(plot, use_container_width=True)
                else:
                    logs += log_line + "\n"
                    with logs_block.container():
                        st_components.html(
                            f"""<pre>{logs}</pre>""", height=600, scrolling=True
                        )  # Add it in code block
                        logs_link = download_link(
                            "here",
                            logs,
                            file_name="evolver-logs.txt",
                            mime="text/plain",
                        )
                        st.markdown(
                            f"##### Execution logs can be downloaded {logs_link}.",
                            unsafe_allow_html=True,
                        )

    with results_block.container():
        st.success("Evolver execution finished!")
        st.balloons()

        with TemporaryFile() as tmp:
            with ZipFile(tmp, "w", ZIP_DEFLATED) as zip_file:
                zip_directory(base_path, zip_file)
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
