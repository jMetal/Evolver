.. _project_structure:

Project Structure
=================

This section provides an overview of the Evolver project's directory structure and key components.

Directory Layout
----------------

The Evolver project is structured as follows:

.. code-block:: text

    Evolver/
    ├── docs/                # Project documentation
    ├── resources/           # Resource files including reference fronts and weight vectors
    │   ├── referenceFronts/ # Reference Pareto fronts for benchmark problems
    referenceFrontsTSP/ # Reference Pareto fronts for multi-objective TSP problems
    │   ├── weightVectors/   # Weight vectors for benchmark problems
    │   ├── tspInstances/    # TSP instances for benchmark problems
    │   └── scripts/         # Utility scripts for analysis and visualization
    ├── src/                 # Source code
    │   ├── main/            # Main source code
    │   │   ├── java/        # Java source files
    │   │   └── resources/   # Resources for the main application
    │   │       └── parameterSpaces/ # YAML files defining algorithm parameter spaces
    │   │       └── irace/ # irace configuration files
    │   └── test/            # Test source code
    │       ├── java/        # Test source files
    │       └── resources/   # Test resources
    └── pom.xml              # Maven build configuration

Source Code Organization
------------------------

The main source code is organized into the following key packages:

- ``org.uma.evolver``: Core framework components and interfaces
- ``org.uma.evolver.algorithm``: Implementation of base- and meta-optimization algorithms
- ``org.uma.evolver.parameter``: Parameter handling and parameter space management
- ``org.uma.evolver.metaoptimizationproblem``: Meta-optimization problem definitions
- ``org.uma.evolver.util``: Utility classes and helper functions

Resource Files
--------------

The ``resources/`` directory contains important configuration and data files:

- ``parameterSpaces/``: YAML files defining the search space for algorithm parameters
- ``irace/``: irace configuration files to find configurations for base-level metaheuristics

