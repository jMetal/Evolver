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
    │   ├── referenceFrontsTSP/ # Reference Pareto fronts for multi-objective TSP problems
    │   ├── weightVectors/   # Weight vectors for benchmark problems
    │   └── tspInstances/    # TSP instances for benchmark problems
    ├── src/                 # Source code
    │   ├── main/            # Main source code
    │   │   ├── java/        # Java source files
    │   │   └── resources/   # Resources for the main application
    │   │       └── parameterSpaces/ # YAML files defining algorithm parameter spaces
    │   │       └── irace/   # irace configuration files
    │   └── test/            # Test source code
    │       ├── java/        # Test source files
    │       └── resources/   # Test resources
    └── pom.xml              # Maven build configuration

Source Code Organization
------------------------

The main source code is organized into the following packages:

- ``org.uma.evolver``: Core framework components and interfaces
- ``org.uma.evolver.algorithm``: Configurable base-level algorithms (NSGA-II, MOEA/D, SMS-EMOA, …)
- ``org.uma.evolver.encoding``: Derivation tree encoding for meta-optimization

    - ``org.uma.evolver.encoding.solution``: ``DerivationTreeSolution`` and ``TreeNode``
    - ``org.uma.evolver.encoding.operator``: ``SubtreeCrossover`` and ``TreeMutation``
    - ``org.uma.evolver.encoding.util``: ``TreeSolutionGenerator``, ``GrammarConverter``, ``TreeOutputResults``
- ``org.uma.evolver.example``: Runnable example programs

    - ``org.uma.evolver.example.training``: Meta-optimization training examples
    - ``org.uma.evolver.example.configuration``: Base-level algorithm configuration examples
    - ``org.uma.evolver.example.validation``: Validation programs and experimental studies
- ``org.uma.evolver.meta``: Meta-optimization problem and builder classes

    - ``org.uma.evolver.meta.problem``: ``AbstractMetaOptimizationProblem`` (shared evaluation pipeline), ``MetaOptimizationProblem`` (flat double encoding), ``TreeMetaOptimizationProblem`` (tree encoding)
    - ``org.uma.evolver.meta.strategy``: ``FixedEvaluationsStrategy``, ``RandomRangeEvaluationsStrategy``
- ``org.uma.evolver.parameter``: Parameter space definition and YAML parsing
- ``org.uma.evolver.trainingset``: Training set definitions for benchmark problems
- ``org.uma.evolver.irace``: irace integration
- ``org.uma.evolver.util``: Utilities: ``ConfigurationFileReader``, ``OutputResults``, observers

Resource Files
--------------

The ``resources/`` directory contains important configuration and data files:

- ``parameterSpaces/``: YAML files defining the search space for algorithm parameters
- ``irace/``: irace configuration files to find configurations for base-level metaheuristics

