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

Fixed
~~~~~

- Fix a bug in class MOEADCommonParameterSpace
- Fix ``ReplacementParameter.getReplacement()`` to handle a null ``removalPolicy`` sub-parameter,
  defaulting to ``ONE_SHOT`` (required for steady-state configurations without a configurable
  removal policy)


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

