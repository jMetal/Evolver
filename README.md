# Evolver: Meta-optimizing multi-objective metaheuristics
Evolver is a tool based on the formulation of the automatic configuration and design of multi-objective metaheuristics as a multi-objective optimization problem that can be solved by using the same kind of 
algorithms, i.e., Evolver applies a meta-optimization approach.

The basis of Evolver are:
* A multi-objective metaheuristic algorithm in which, given one or several problems used as training set, a configuration of it is sought that solves the training set in an efficient way. This algorithm is referred as to the *Internal Algorithm*.
* A design space associated to the internal algorithm which defines their parameters and components subject to be configured.
* A list of quality indicators used as objectives to minimize when using the internal algorithm to solve a problem of the training set.
* A *meta-optimizer* algorithm which is used to solve the optimization problem defined by minimizing the quality indicators of an internal algorithm given a particular training set.

The project is made of two parts:
* [Evolver](src): A Java library built with maven that implements the meta-optimization approach.
* [Evolver Dashboard](evolver-dashboard): A Python dashboard built with Streamlit that allows to configure and execute Evolver from an user-friendly web application.

# Pre-requisites for
* Java 17 or higher
* Maven 3.6.3 or higher
* Python 3.9 and [<3.11](https://github.com/whitphx/streamlit-server-state/issues/187) (Optional, only for the GUI)

Evolver is based on [jMetal 6.1](https://github.com/jMetal/jMetal), so all the stuff provided by that framework (algorithms, problems, quality indicators, utilities) are available in Evolver.

You can use Evolver through Docker using our pre-built images. More information in the [Docker section](#execute-with-docker)

# Execute Evolver
To execute Evolver, first build the project with Maven:
```console
$ mvn package
```

Then, you have three options:
* Use the ```MetaRunner``` class, which takes as a parameter a configuration file. This is the simplest way but only some parameters of the meta-runner can be set.
* Define a runner program which provides you full control to set the meta-optimizer parameters.
* Use a Graphical User Interface (GUI).

## Using the ```MetaRunner``` class
To run the metarunner class, just execute the following command:
```console
$ java -cp target/Evolver-1.0-jar-with-dependencies.jar org.uma.evolver.MetaRunner <path-to-configuration-file>
```
The configuration file describes an Evolver configuration by using a YAML file. The following example can use a generic NSGAII to auto-design the NSGAII algorithm to solve the ZDT1, ZDT4, and DTLZ3 problems. You can find more example configurations at the [configurationFiles](configurationFiles) folder of the project.

```yaml
general_config:
    dashboard_mode: false
    output_directory: /tmp/evolver
    cpu_cores: 8
    plotting_frequency: 100

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

We comment on these parameters next:
* dashboard_mode: true if the dashboard-based GUI is used; false otherwise.
* output_directory: directory where the output of the meta-optimization is stored. The output include files with the configurations and with the values of the quality indicators. 
* cpu_cores: number of cores to be used by the meta-optimizer (ignored if the meta-optimizer cannot be parallelized).
* plotting_frequency: frequency to plot the Pareto front approximation obtained by the meta-optimizer during the search. It must be a multiple of the meta-optimizer population size.
* meta_optimizer_algorithm: most of the multi-objective algorithms in jMetal can be a meta-optimizer, but it is advisable to choose among those that can be run in parallel.
* meta_optimizer_population_size: we typically use a size of 50 in our experiments.
* meta_optimizer_max_evaluations: stopping condition of the meta-optimizer.
* configurable_algorithm: algorithm whose configuration is searched for. Evolver currently provides configurable implementations of NSGA-II, MOEA/D, MOPSO, and SMS-EMOA.
* internal_population_size: population size of the configurable algorithm.
* independent_runs: number of independent runs when evaluating the configurable algorithm with a particular combination of parameters and components.
* problem_names: problems used as the training set.
* reference_front_file_name: files with the reference fronts of the problems in the training set.
* max_number_of_evaluations: stopping condition of the configurable algorithm per problem.
* weight_vector_files_directory: directory containing the weight vector files for MOEA/D.

## Using a runner program
The [examples package](https://github.com/jMetal/Evolver/tree/main/src/main/java/org/uma/evolver/examples) in the Evolver project contains examples illustrating combinations of meta-optimizers and algorithms to be configured. 

To run the example where NSGA-II is used to find configurations of MOEAD by selecting the DTZ2Minus problems as the training set, the command to execute is:

```console
$ java -cp target/Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar org.uma.evolver.examples.NSGAIIOptimizingMOEADForProblemDTLZ2Minus
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

# Example: Meta-optimizing NSGA-II to solve an engineering problem
To illustrate the use of Evolver, we provide an example where NSGA-II, as meta-optimizer, is used to find a configuration of NSGA-II for the liquid-rocket single element injector design problem described in [Engineering applications of multi-objective evolutionary algorithms: A test suite of box-constrained real-world problems](https://doi.org/10.1016/j.engappai.2023.106192). This problem in included in jMetal in the *org.uma.jmetal.problem.multiobjective.rwa.Goel2007* class.
The code to run this example is in the [NSGAIIOptimizingNSGAIIForProblemGoel2007](https://github.com/jMetal/Evolver/blob/main/src/main/java/org/uma/evolver/examples/NSGAIIOptimizingNSGAIIForProblemGoel2007.java) program in Evolver.
Without entering into details, we set the Epsilon and Inverted Generational Distance Plus as indicators to be minimized and the stopping condition of the meta-optimizer is to compute 2000 function evaluations.

We execute the program in a terminal by using the following command:
```console
$ java -cp target/Evolver-1.0-SNAPSHOT-jar-with-dependencies.jar org.uma.evolver.examples.NSGAIIOptimizingNSGAIIForProblemGoel2007
```

The following chart shows the population of the meta-optimizer after 250 evaluations:
![250iterations](https://github.com/jMetal/Evolver/blob/develop/resources/documentation/goel2007.250evals.png "Population size after 250 evaluations")
