# Evolver: Meta-optimizing multi-objective metaheuristics
Evolver is a tool based on the formulation of the automatic configuration and design of multi-objective metaheuristics as a multi-objective optimization problem that can be solved by using the same kind of algorithms; applying a meta-optimization approach.

The project is made of two parts:
* [Evolver](src): A Java library built with maven that implements the meta-optimization approach.
* [Evolver Dashboard](evolver-dashboard): A Python dashboard built with Streamlit that allows to configure and execute Evolver from an user-friendly web application.

# Pre-requisites for
* Java 17 or higher
* Maven 3.6.3 or higher
* Python 3.9 and [<3.11](https://github.com/whitphx/streamlit-server-state/issues/187) (Optional, only for the GUI)

Alternative, you can use Evolver through Docker using our pre-built images. More information in the [Docker section](#execute-with-docker)

# Execute Evolver
To execute Evolver, first build the project with Maven:
```console
$ mvn package
```

Then, execute the following command:
```console
$ java -cp target/Evolver-1.0-jar-with-dependencies.jar org.uma.evolver.MetaRunner <path-to-configuration-file>
```

## Execute Evolver with a GUI (Optional)
Additionally, you can use Python to deploy a web-based dashboard for executing evolver. To do so, follow the next steps:
```bash
# Build the latest version of the Evolver jar at target folder
$ mvn package
# Install the evolver python package
$ pip install "./evolver-dashboard"
# Execute the dashboard
$ python -m evolver
```

This will start the dashboard at [http://localhost:8501/](http://localhost:8501/).

For more information in the dashboard, please refer to the [dashboard documentation](evolver-dashboard/README.md).

# Sample configuration
To execute evolver, the parameters are configured in a YAML file.
The following example can use a generic NSGAII to auto-design the NSGAII algorithm to solve the ZDT1, ZDT4, and DTLZ3 problems. You can find more example configurations at [examples](examples).

```yaml
general_config:
    dashboard_mode: false
    output_directory: /tmp/evolver
    cpu_cores: 8
    plotting_frequency: 10

external_algorithm_arguments:
    meta_optimizer_algorithm: NSGAII
    meta_optimizer_population_size: 50
    meta_optimizer_max_evaluations: 3000
    indicators_names: NHV,EP

internal_algorithm_arguments:
    configurable_algorithm: NSGAII
    internal_population_size: 100
    independent_runs: 3
    problem_names: org.uma.jmetal.problem.multiobjective.zdt.ZDT1,org.uma.jmetal.problem.multiobjective.zdt.ZDT4
    reference_front_file_name: resources/referenceFronts/ZDT1.csv,resources/referenceFronts/ZDT4.csv
    max_number_of_evaluations: 8000,16000

optional_specific_arguments:
    # For Configurable-MOEAD only, probably shouldn't be modified
    weight_vector_files_directory: resources/weightVectors
```

# Execute with docker
There are two docker images available for Evolver.

## Main Evolver
Container image: `ghcr.io/jmetal/evolver`

Tags: `latest`

Evolver requires you to include your configuration file and the folder where you want to save the results. The output folder inside the container is the one you define in your execution configuration.

You can do this by mounting volumes in the container.

Usage:
```bash
$ docker run --rm \
-v <local/path/config.yml>:/config.yml \
-v <local/path/folder>:<container/path/from/config> \
ghcr.io/jmetal/evolver:latest org.uma.evolver.MetaRunner /config.yml
```
## Evolver dashboard
Check the [dashboard documentation](evolver-dashboard/README.md#execute-the-dashboard-with-docker) for more information.