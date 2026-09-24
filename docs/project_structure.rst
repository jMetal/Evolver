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

The main source code is organized in two layers. The **configurable core** can be used on its own,
as an alternative to jMetal for configuring and running multi-objective algorithms; the **meta
level** uses the core to tune those algorithms automatically. The core never depends on the meta
level (``PackageLayeringTest`` enforces it).

Configurable core:

- ``org.uma.evolver.algorithm``: Configurable algorithms (``BaseLevelAlgorithm``): NSGA-II, MOEA/D, SMS-EMOA, …
- ``org.uma.evolver.parameter``: Parameter space definition and YAML parsing
- ``org.uma.evolver.util``: ``ConfigurationFileReader`` and ``HypervolumeMinus``

Meta level:

- ``org.uma.evolver.meta``: Meta-optimization

    - ``org.uma.evolver.meta.algorithm``: Meta-only algorithms: ``RandomSearch``, ``TreeNSGAII``, ``TreeAGEMOEA``
    - ``org.uma.evolver.meta.builder``: Meta-optimizer builders (``MetaNSGAIIBuilder``, ``MetaSPEA2Builder``, …)
    - ``org.uma.evolver.meta.problem``: ``AbstractMetaOptimizationProblem`` (shared evaluation pipeline), ``MetaOptimizationProblem`` (flat double encoding), ``TreeMetaOptimizationProblem`` (tree encoding)
    - ``org.uma.evolver.meta.strategy``: ``FixedEvaluationsStrategy``, ``RandomRangeEvaluationsStrategy``
    - ``org.uma.evolver.meta.encoding``: Derivation tree encoding (``solution``: ``DerivationTreeSolution``, ``TreeNode``; ``operator``: ``SubtreeCrossover``, ``TreeMutation``; ``util``: ``TreeSolutionGenerator``, ``GrammarConverter``; ``parameter``: tree operator parameters and ``TreeParameterFactory``)
    - ``org.uma.evolver.meta.trainingset``: Training set definitions for benchmark problems
    - ``org.uma.evolver.meta.output``: ``ConsolidatedOutputResults``, ``WriteExecutionDataToFilesObserver``, ``MetaOptimizerConfig``, ``TreeOutputResults``
- ``org.uma.evolver.cli.training``: Request/status/result runner for training jobs
- ``org.uma.evolver.irace``: irace integration

Runnable examples:

- ``org.uma.evolver.example``: Runnable example programs

    - ``org.uma.evolver.example.baselevel``: Configurable algorithms used on their own
    - ``org.uma.evolver.example.training``: Meta-optimization training examples
    - ``org.uma.evolver.example.validation``: Validation programs and experimental studies

Resource Files
--------------

The ``resources/`` directory contains important configuration and data files:

- ``parameterSpaces/``: YAML files defining the search space for algorithm parameters
- ``irace/``: irace configuration files to find configurations for base-level metaheuristics

