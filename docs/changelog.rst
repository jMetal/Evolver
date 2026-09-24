.. _changelog:

Changelog
=========

All notable changes to Evolver will be documented in this file.

2.1-SNAPSHOT
------------

Added
~~~~~
- Add a class (``ConfigurationFileReader``) to read algorithm configurations stored in text files
- Add a Python script for visualizing the progression of meta-level multi-objective optimization runs.
- Add permutation and binary base-level SMSEMOA
- Add configurable NSGA-III for double-encoded problems (``DoubleNSGAIII``)
- Add configurable AGE-MOEA for double-encoded problems (``DoubleAGEMOEA``)
- Add configurable PAES (Pareto Archived Evolution Strategy) for double-encoded problems
  (``DoublePAES``). Population size is fixed at 1. Variation is mutation-only (no crossover).
  The bounded archive size (``numberOfSolutionsToFind``) is fixed and passed via the constructor.
  Density archive types: ``crowdingDistanceArchive``, ``hypervolumeArchive``,
  ``spatialSpreadDeviationArchive``. A configurable ``archiveSelectionProbability`` chooses the
  mutation parent between the current solution and a random archive member (0.0 = classic PAES).
  Three new components: ``MutationOnlyVariation``, ``PAESSelection``, and ``PAESReplacement``.
  The initial solution is a single random solution (no configurable initialisation strategy,
  as population-diversity strategies are meaningless for a 1+1 ES).
  Parameter space defined in ``PAESDouble.yaml`` (13 parameters, 4 top-level).
- Add configurable SSMOEA (Steady-State MOEA) for double-encoded problems (``DoubleSSMOEA``).
  Supports two variation branches (crossover+mutation or differential evolution) and two
  replacement strategies (``rankingAndDensityEstimator`` or ``singleSolutionReplacement``).
  The offspring population size is fixed at 1. Parameter space defined in ``SSMOEADouble.yaml``
  (43 parameters, 6 top-level).
- Add ``singleSolutionReplacement`` to ``ReplacementParameter``, enabling one-to-one DEMO-style
  replacement based on dominance comparison
- Add AGE-MOEA as a meta-optimizer, for both the flat and the tree encodings
- Support NSGA-II, AGE-MOEA and Random Search as tree-encoding meta-optimizers
  (``TreeNSGAII``, ``TreeAGEMOEA``), configured from ``NSGAIIMetaTree.yaml``/``AGEMOEAMetaTree.yaml``
- Add ``cli.training``, a command-line training runner driven by YAML files
  (``TrainingRunnerMain``: a ``request.yaml`` referencing reusable base-level and meta-search
  configuration files, with progress reported in a status file) and ``DescribeMain``, a manifest
  of the algorithms, problems and indicators it can use. Supports the flat and tree encodings,
  Double and Permutation base-level algorithms, and any jMetal problem by class name
  (see :doc:`utilities/cli_tools`)
- Register SPEA2, SMPSO, Async NSGA-II and Random Search as ``cli.training`` meta-optimizers
- Add a Tutorials section to the documentation, with runnable code in the
  ``org.uma.evolver.example.tutorial`` package: :doc:`E1. Parameter spaces
  <tutorials/parameter_spaces>` and :doc:`E2. Base-level algorithms <tutorials/base_level_algorithms>`
- Add the Evolver logo (README, documentation sidebar and favicon)
- Add ``PackageLayeringTest``, which keeps the configurable core free of dependencies on the meta level
- Evolver-Studio, a companion Python/Streamlit application, builds on ``cli.training`` and
  ``DescribeMain`` to explore parameter spaces, launch and monitor training runs, and follow
  interactive tutorials (paired with the documentation's tutorials) without writing Java code

Changed
~~~~~~~

- Meta-optimizers always generate as many offspring as their population size and return their
  final population (never an external archive); ``offspringPopulationSize`` and
  ``metaOffspringSize`` are no longer configurable
- The default meta population size is 50 for every meta-optimizer and both encodings
- Tree-encoding meta-optimizer configuration files use operator flags, like the flat ones, and
  must declare the selection (``selection``, ``selectionTournamentSize``)
- JDK 21, the version used by the CI workflows, is the recommended JDK
- The Javadoc under ``docs/_static/javadoc`` is regenerated for 2.1-SNAPSHOT, and stale
  documentation pages (quick start, examples, parameter spaces, meta-optimizers, irace) are updated
- Separate the configurable core (``algorithm``, ``parameter``, ``util``) from the meta level:
  the derivation tree encoding, training sets and training output classes move to
  ``meta.encoding``, ``meta.trainingset`` and ``meta.output``, and meta-only algorithms to
  ``meta.algorithm``

Removed
~~~~~~~

- ``OutputResults`` (use ``ConsolidatedOutputResults``), ``TrainingSetRunner``,
  ``ExtremePointsEstimator``, ``EstimatedReferenceFrontGenerator``, ``SingleObjectiveWrapper``,
  ``ProbabilityParameter``, ``DifferentialEvolutionSelectionParameter`` and
  ``DoubleSelectionParameter``, which were not used

Fixed
~~~~~

- Fix a bug in class MOEADCommonParameterSpace
- Fix ``ReplacementParameter.getReplacement()`` to handle a null ``removalPolicy`` sub-parameter,
  defaulting to ``ONE_SHOT`` (required for steady-state configurations without a configurable
  removal policy)
- Restore ``crowdingDistanceArchive`` as a valid ``archiveType`` option in ``NSGAIIDouble.yaml``,
  alongside ``unboundedArchive``, ``spatialSpreadDeviationArchive``, ``knnDistanceArchive`` and
  ``angleArchive``
- Extend ``ExternalArchiveParameter`` (shared by NSGA-II, AGE-MOEA, MOEA/D, MOPSO, RDEMOEA, RVEA and
  SMS-EMOA) to build ``knnDistanceArchive`` and ``angleArchive`` instances; selecting either value
  previously threw ``JMetalException: Archive type does not exist`` at evaluation time
- Record the default meta population size, instead of 0, in ``METADATA.txt`` when a meta-search
  configuration omits it
- Fix stale ``NSGAIIDoubleFull.yaml`` references (renamed to ``NSGAIIDouble.yaml`` in a previous
  commit) in several base-level and validation example classes


2.0 (2025-09-09)
----------------

Added
~~~~~

- Documentation
- Examples

Changed
~~~~~~~

- Complete rewrite of the original Evolver framework
- New architecture for improved flexibility and maintainability
- Enhanced support for meta-optimization of multi-objective metaheuristics
- Improved documentation and examples
- The Docker images are not available for this version
- The GUI-based dashboard has been removed

Fixed
~~~~~

- Minor bug fixes and improvements

