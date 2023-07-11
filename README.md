# Evolver: Meta-optimizing multi-objective metaheuristics
Evolver is a tool based on the formulation of the automatic configuration and design of multi-objective metaheuristics as a multi-objective optimization problem that can be solved by using the same kind of algorithms; applying a meta-optimization approach.

The project is made of two parts:
* [Evolver](src): A Java library built with maven that implements the meta-optimization approach.
* [Evolver Dashboard](evolver-dashboard): A Python dashboard built with Streamlit that allows to configure and execute Evolver from an user-friendly web application.

## Sample configuration
To execute evolver, the parameters are configured in a YAML file.
The following example can use a generic NSGAII to auto-design the NSGAII algorithm to solve the ZDT1, ZDT4, and DTLZ3 problems.

```yaml

  external_algorithm_arguments:
    meta_optimizer_algorithm: NSGAII
    meta_optimizer_population_size: 50
    meta_optimizer_max_evaluations: 3000
    independent_runs: 3
    indicators_names: NHV,EP
    output_directory: TEST/DIRECTORY

  internal_algorithm_arguments:
    configurable_algorithm: NSGAII
    internal_population_size: 100
    problem_names: org.uma.jmetal.problem.multiobjective.zdt.ZDT1,org.uma.jmetal.problem.multiobjective.zdt.ZDT4,org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3
    reference_front_file_name: resources/referenceFronts/ZDT1.csv,resources/referenceFronts/ZDT4.csv,resources/referenceFronts/DTLZ3.3D.csv
    max_number_of_evaluations: 8000,8000,8000

  optional_specific_arguments:
    # For Configurable-MOEAD only, probably shouldn't be modified
    weight_vector_files_directory: resources/weightVectors
```

## Execute Evolver
To execute Evolver, first build the project with Maven:
```console
$ mvn package
```

Then, execute the following command:
```console
$ java -cp target/evolver-1.0-SNAPSHOT-jar-with-dependencies.jar org.uma.evolver.MetaRunner <path-to-configuration-file>
```

Addionally, you can use Python to run Evolver's GUI:
```console
$ ./run.sh
```

This will start the dashboard at [http://localhost:8501/](http://localhost:8501/).