.. _changelog:

Changelog
=========

All notable changes to Evolver will be documented in this file.

2.3-SNAPSHOT
------------

Added
~~~~~

- Solve requests (``cli.solving``) accept ``statusFrequency``: every that many evaluations
  ``status.yaml`` is updated while a run is in progress (``SolveProgressObserver``), counting the
  evaluations of all the independent runs, so that a GUI can show a progress bar. Absent, the status
  is updated only when a run ends, as before. Updating more often slows the run down: see
  ``docs/utilities/cli_tools.rst`` for the cost
- Solve requests accept ``frontFrequency``: every that many evaluations of a run, the non-dominated
  solutions of the run in progress are written to ``CURRENT_FRONT.csv`` in the output directory
  (``SolveFrontObserver``), overwriting the previous ones and removed when the runs end, so that a GUI
  can plot how the front evolves; with ``writePopulation: true`` the whole population is written
  instead, each solution marked as non-dominated or not. Written more often than every 100
  evaluations it slows the run down noticeably: see ``docs/utilities/cli_tools.rst``
- ``Spread`` and ``GeneralizedSpread`` can be used in the ``indicatorNames`` of training and solve
  requests (``IndicatorRegistry``), and are listed in ``DescribeMain``'s manifest. ``Spread``, Deb's
  diversity indicator, is defined only for two objectives: a request that uses it on a problem with
  any other number of objectives is rejected before the run starts
  (``IndicatorRegistry.checkApplicable``), suggesting ``GeneralizedSpread``
- ``IraceParameterDescriptionGenerator`` has a ``main`` that generates the irace parameter file of
  any parameter space: it takes the YAML file (bundled, or a file of your own) and the parameter
  factory that reads it (``Double``, ``Binary``, ``Permutation`` or ``MOPSO``), so it also covers the
  algorithms that had no generator (AGE-MOEA, NSGA-III, PAES, RVEA, SMS-EMOA and SSMOEA). It also
  gains ``description(ParameterSpace)``, which returns the text instead of printing it
- Tutorial E9, *Validating a configuration* (``docs/tutorials/validating_a_configuration.rst``,
  ``example.tutorial.ValidationTutorial``, ``tutorial-e9-request.yaml``): repeats with Evolver the
  study of Nebro et al. (GECCO 2019 Companion), NSGA-II tuned for the bi-objective WFG problems and
  validated against NSGA-II and SMPSO, to explain each analysis of a validation study. The
  configuration found is bundled in ``src/main/resources/tunedConfigurations/NSGAIIWFG2D.txt``
- Tutorial E6, *Designing your own parameter space* (``docs/tutorials/designing_parameter_spaces.rst``,
  ``example.tutorial.ParameterSpaceDesignTutorial``, ``tutorial-e6-request.yaml``): reducing and
  extending a space, the limits that the algorithms set, and the space of the GECCO 2019 irace study
  (``NSGAIIDoubleGECCO2019.yaml``, a new bundled parameter space) compared with the full one by
  validating the configurations found in each
- ``scripts/plot_parameter_space.py --stats`` prints the size of a parameter space: its parameters
  (the genes of the flat encoding), its structures (combinations of categorical values) and its depth
- Analysis scripts for validation studies, all reading jMetal's ``QualityIndicatorSummary.csv``:
  ``scripts/boxplots.py``, ``scripts/effect_size_tables.py`` (Vargha-Delaney A12),
  ``scripts/friedman_holm_tables.py`` (Friedman test and Holm's procedure with a control) and
  ``scripts/bayesian_plots.py`` (Bayesian sign test with a ROPE), with the shared
  ``scripts/study_summary.py``

Removed
~~~~~~~

- The package ``irace.generator``: its nine
  ``Irace<Algorithm><Encoding>ParameterDescriptionGenerator`` classes, one per parameter space, are
  replaced by the ``main`` of
  ``IraceParameterDescriptionGenerator`` (for instance, ``IraceParameterDescriptionGenerator
  NSGAIIDouble.yaml Double`` replaces ``IraceNSGAIIDoubleParameterDescriptionGenerator``), which
  moves to ``org.uma.evolver.irace`` and no longer has a type parameter, which it did not use

Fixed
~~~~~

- Errors explain what went wrong. A configuration that fails during a training names the problem,
  the cause and the configuration, in the exception and in ``status.yaml``; it used to say only
  what the failing component said. The operator parameters name the parameter that causes an
  error: a tournament larger than the population (``selectionTournamentSize``), a probability
  outside [0, 1] (``crossoverProbability``, ``mutationProbability``), a ``mutationProbabilityFactor``
  that gives a probability larger than 1 for the number of variables of the problem (with the valid
  range), an ``offspringPopulationSize`` of 0. Exceptions that wrap another one keep its message
  and its cause (``JMetalExceptions``): jMetal 7.6 loses them, so they used to be ``null``. A failing
  configuration still makes the training fail: ``docs/proposals/failing-configurations.md`` records
  the decision
- The binary mutation accepts a ``mutationProbabilityFactor`` of 0 (no mutation), which is in the
  range of the binary parameter spaces: it was rejected, which aborted a training whenever the
  meta-optimizer reached the bound of the range
- Each indicator gets its own copy of the normalized reference front in
  ``AbstractMetaOptimizationProblem``: the configurations are evaluated in parallel and share those
  fronts, and an indicator that sorts the reference front in place (jMetal's ``Spread``) corrupted
  it for the other threads
- The output of the tests no longer floods the build log: ``WriteExecutionDataToFilesObserver`` no
  longer logs ``EVAlS -> n`` at every checkpoint, ``new TrainingRunner(false)`` skips the evaluation
  progress log (the integration tests use it), the tests log only warnings
  (``src/test/resources/logging.properties``), and failsafe writes the output of the integration
  tests to ``target/failsafe-reports/*-output.txt``

2.2 (2026-10-02)
----------------

Added
~~~~~

- The meta-optimizers can be bounded by computing time, in minutes with decimals, instead of by
  meta-evaluations (the two limits are mutually exclusive): ``setMaxComputingTimeMinutes`` in
  ``MetaNSGAIIBuilder``, ``MetaSPEA2Builder``, ``MetaSMPSOBuilder``, ``MetaRandomSearchBuilder``,
  ``MetaAsyncNSGAIIBuilder`` and ``MetaAsyncGeneticAlgorithmBuilder``, and
  ``metaMaxComputingTimeMinutes`` in the meta-optimizer configuration files of ``cli.training``
  (flat and tree encodings, all the meta-optimizers). The condition is checked at the beginning of
  each generation, so the generation in progress is completed (the asynchronous meta-optimizers check
  it after every evaluation, once the initial population is evaluated); ``status.yaml`` gets
  ``maxComputingTimeMinutes`` and ``elapsedMinutes``. See ``docs/proposals/meta-termination-by-time.md``
- ``AsyncNSGA-II`` supports the tree encoding in ``cli.training`` (``encoding: tree``): jMetal's
  ``AsynchronousMultiThreadedNSGAII`` on derivation trees, with subtree crossover and tree mutation
  (``AsyncNSGAIIMetaTree.yaml``; its selection and replacement are fixed by the algorithm), bounded
  by meta-evaluations or by computing time. Bundled configuration:
  ``MetaAsyncNSGAIITreeConfiguration.yaml``
- ``example.training.dtlz.AsyncNSGAIIOptimizingRVEAForProblemDTLZ3Minus``: the tree-encoded
  AsyncNSGA-II tuning RVEA on DTLZ3Minus for 5 minutes on 12 cores
- ``example.training.dtlz.AsyncNSGAIIOptimizingRVEAForBenchmarkDTLZMinus``: the same, tuning RVEA on
  DTLZ1Minus, DTLZ2Minus and DTLZ3Minus (inverted fronts) for 20 minutes on 18 cores
- Add :doc:`tutorial E17, budgets: evaluations or time <tutorials/budgets>`
  (``example.tutorial.BudgetsTutorial``, ``tutorial-e17-request.yaml``,
  ``TutorialTimeNSGAIIMetaSearch.yaml``): the two budgets of a training, and the training of E3
  stopped after two minutes, with how to read what the output files record about the stop
- Bundled example of a training bounded by computing time:
  ``MetaNSGAIIFlatComputingTimeConfiguration.yaml`` (60 minutes) and
  ``nsgaii-re3d-computing-time-request.yaml``
- ``METADATA.txt`` states the stopping condition of the meta-optimizer (``Max Evaluations`` or
  ``Max Computing Time``, and ``Stopping condition``) and, in its ``Execution`` section, the
  meta-evaluations performed; the runs of ``cli.training`` now write that section too, with the
  wall-clock time
- Each checkpoint of ``VAR_CONF.txt`` has, after ``# Evaluation: <n>``, the line
  ``# Time (min): <minutes>`` with the computing time of the meta-optimizer, whatever the stopping
  condition
- ``scripts/plot_training_convergence.py --x time`` plots the convergence over the computing time
  of the meta-optimizer (unit chosen from the length of the run; replications pooled on a common
  time grid), and ``scripts/plot_meta_population.py`` adds the time of each checkpoint to its
  panels. The times come from ``VAR_CONF.txt``; ``scripts/tests`` has their pytest tests
- Add the reference front of MaF08 with three objectives (``MaF08.3D.csv``), the image of its
  Pareto set (a triangle in the decision space), used by the RVEA guide
- Add the :ref:`algorithm guides <algorithm_guides>`, one per base-level algorithm, with where it
  works well and where it works poorly backed by an experiment; the first is the guide of
  :doc:`RVEA, RVEA* and iRVEA <algorithms/rvea>` (``example.algorithms.RVEAGuide``), and
  ``docs/proposals/algorithm-guides.md`` tracks the rest
- Add the :doc:`NSGA-II guide <algorithms/nsgaii>` (``example.algorithms.NSGAIIGuide``): the
  standard version against the steady-state one on ZDT1, a crowding distance archive on ZDT4 and an
  unbounded archive on DTLZ2, with the spread of the fronts measured by the generalized spread
- RVEA can be tuned and run from the command line and Evolver-Studio: it is registered in
  ``BaseAlgorithmRegistry`` as ``RVEA`` (Double encoding), with the extra configuration
  ``weightVectorFilesDirectory`` as MOEA/D. Bundled requests: ``rvea-zdt1-dtlz2-request.yaml``
  (training on problems with two and three objectives) and ``rvea-dtlz2-request.yaml`` (solving)
- RVEA covers RVEA, RVEA* and iRVEA, as in jMetal 7.6's ``AutoRVEA``: the ``replacement``
  parameter of ``RVEADouble.yaml`` selects the variant, with ``alpha`` and ``fr`` (and, for iRVEA,
  ``numberOfSubregions``, ``lateStageFraction`` and ``epsilonKappa``) as its sub-parameters, and the
  mating ``selection`` (random or tournament) is configurable. ``RVEADoubleDefault.txt``,
  ``RVEAStarDoubleDefault.txt`` and ``IRVEADoubleDefault.txt`` hold the standard configuration of
  each variant, and ``IRVEADTLZ7Example`` runs iRVEA
- Add :doc:`tutorial E14, tuning with irace <tutorials/tuning_with_irace>`, which replaces the
  former irace page: generating irace's parameter file from a YAML parameter space, the target
  runner, the scenario, running irace, and applying the configuration it finds to the ZDT problems
  (``example.tutorial.IraceTutorial``)
- Add ``docs/proposals/tree-mutation.md``, an analysis of ``TreeMutation`` with its open questions
  (distribution index, integer parameters with small ranges, ordinal parameters, mutation strength)
- Add ``docs/proposals/irace-vs-evolver.md``, which describes the comparison of irace with Evolver's
  meta-optimization as an open research line
- Add :doc:`tutorial E8, analyzing training results <tutorials/analyzing_training_results>`:
  the output files, convergence and population of a training run that tunes NSGA-II for ZDT1-6
  with five runs per configuration, choosing a configuration from its final front, and validating
  the candidates against the standard NSGA-II
- Add ``AbstractMetaOptimizationProblem.evaluateConfiguration``, which evaluates a configuration
  given as a configuration string exactly as the meta-optimizer evaluates its solutions
- Add :doc:`tutorial E7, training sets, indicators and budgets
  <tutorials/training_sets_indicators_budgets>`: tuning NSGA-II for DTLZ1-7 with a fifth of the
  validation budget, and validating the configuration found against NSGA-II, NSGA-III, MOEA/D,
  SMS-EMOA and AGE-MOEA on DTLZ1-7 and WFG1-9, with its validation study
  (``example.tutorial.TrainingSetsValidationTutorial``)
- Add the optional ``writePopulation`` field to training requests (and to ``TrainingRequest``):
  it also writes the whole population of the meta-optimizer at every checkpoint to
  ``POPULATION_INDICATORS.csv`` and ``POPULATION_CONFIGURATIONS.csv``, not only its non-dominated
  configurations; ``scripts/plot_meta_population.py`` plots it
- Add ``scripts/plot_median_fronts.py``, which plots the fronts with the median HV of each
  algorithm and problem of a jMetal validation study
- Add ``scripts/critical_difference_plots.py``, which draws critical difference plots (with SAES)
  of a jMetal ``QualityIndicatorSummary.csv``
- Add ``scripts/wilcoxon_pivot_tables.py``, which writes Wilcoxon pivot tables (with SAES) of a
  jMetal ``QualityIndicatorSummary.csv``, with the tuned configuration as pivot
- Add ``org.uma.evolver.cli.solving.SolveRunnerMain``, which runs a configurable algorithm, with a
  configuration given inline or as a file, on a problem, with several independent runs and
  reproducible seeds, and writes the fronts and quality indicators of each run (see
  :doc:`utilities/cli_tools`); ``DescribeMain`` adds the shape of its request
- Add :doc:`tutorial E4, Evolver in 10 minutes <quick_start>`, which replaces the former quick
  start: build Evolver, run a configurable algorithm, tune it with a short training run, and run it
  with the configuration found, all from the command line
- Add a test that loads every request file bundled under ``src/main/resources/cli/training``
- Add :doc:`tutorial E3, meta-optimization workflow <tutorials/meta_optimization_workflow>`:
  tuning NSGA-II for ZDT4 from Java and from the command line, choosing a configuration from the
  training results, and comparing its front with that of the default configuration
- Add ``scripts/plot_fronts.py``, which plots several labelled bi-objective fronts against a
  reference front
- Add ``scripts/plot_training_convergence.py``, which plots how each meta-objective of one or
  several training runs converges over the meta-evaluations (median and best-worst band at each
  checkpoint, and the meta-evaluation at which 95% of the improvement is reached)

Changed
~~~~~~~

- The time limit of the meta-optimizer (``metaMaxComputingTimeMinutes``) is introduced where
  ``metaMaxEvaluations`` first appears: the README example, the quick start (E4) and tutorial E3
  (v1.1). The discussion of the training budget of tutorial E7 moves to the new tutorial E17, and E7
  keeps the budgets of its case study
- The live front plot of ``cli.training`` (``frontPlotFrequency``) names the meta-optimizer and the
  base-level algorithm in its title, with the progress against the stopping condition:
  ``NSGA-II optimizing RVEA. Evaluations: 500 of 2000``, or, bounded by computing time,
  ``NSGA-II optimizing RVEA. Time: 12.3 of 60 min (500 evaluations)``; it showed only the
  meta-optimizer and the evaluations
- The error of a parameter space that lacks a parameter the algorithm requires (for example a
  top-level parameter removed from ``NSGAIIDouble.yaml``) now says that the space must define it and
  lists the defined parameters; the behaviour is unchanged: there are no default values
- ``DoubleRVEA`` no longer takes ``alpha`` and ``fr`` (they are parameters of the parameter
  space), builds the algorithm from jMetal's components instead of ``RVEABuilder`` (with the same
  result, which a test checks), and can read its reference vectors, for each problem, from a
  directory of weight vector files as MOEA/D does, so that it can be trained on problems with
  different numbers of objectives. ``offspringPopulationSize`` in ``RVEADouble.yaml`` takes the
  values of ``AutoRVEA`` (10 to 200)
- jMetal 7.6 instead of 7.5
- The flat encoding decodes integer parameters giving every integer of the range an interval of the
  same width: the upper bound was only decoded from the value 1.0 exactly, so it was almost never
  chosen (e.g. a tournament size of 10 in [2, 10])
- ``TreeMutation`` mutates categorical parameters with integer values, such as
  ``offspringPopulationSize``: they were never mutated (their node had no valid values), so the
  tree meta-optimizers only changed them through the initial population and crossover. They are
  mutated as nominal parameters: another value, chosen uniformly. ``GrammarConverter.validate``
  also checks their values
- ``TreeMutation`` always changes the tree: integer values are mutated on ``[lower - 0.5, upper +
  0.5]`` and rounded, and moved one unit if they do not change (with small ranges, such as the
  tournament size [2, 10], about three quarters of the mutations used to leave the value unchanged
  and spend a meta-evaluation on a copy of the parent); a double value at a bound, where half of the
  polynomial steps left it unchanged, is mutated again; categorical nodes with a single value are
  never selected
- The meta-optimizer configurations with the tree encoding (``MetaNSGAIITreeConfiguration.yaml``,
  ``MetaAGEMOEATreeConfiguration.yaml``) and ``TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D`` use a
  distribution index of 5 instead of 20 in ``TreeMutation``, for larger steps in the mutation of
  numeric parameters; the value is provisional and still to be studied
  (``docs/proposals/tree-mutation.md``)
- The irace resources (``src/main/resources/irace``) are updated: irace 4.4.3 instead of 4.2.0,
  ``parameters-NSGAII.txt`` regenerated from the current ``NSGAIIDouble.yaml``, a scenario that
  runs ``org.uma.evolver.irace.AutoNSGAIIIraceHVEP`` (it referred to a class that no longer exists)
  on the ZDT problems with 8000 evaluations, and ``run.sh`` with a configurable number of cores
  (``N_CPUS``)
- The irace target runners ``AutoNSGAIIIraceHV`` and ``AutoNSGAIIIraceHVEP`` apply the seed irace
  passes (``--randomGeneratorSeed``), so that their experiments are reproducible
- The quick start of the README shows the command-line route and a meta-optimization example based
  on ``TrainingRunner`` and YAML configurations; the changelog is kept only in the documentation
- Tutorial E4 appears under the introductory tutorials in the sidebar of the documentation
- ``AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ`` and the bundled ``DTLZ3DNSGAIIBaseLevel.yaml``
  use NHV and EP as meta-objectives (instead of HV− and EP) and 10000 evaluations per problem
  (instead of 16000); the class uses 16 cores, writes the whole population of the meta-optimizer,
  writes to ``results/tutorial-e7/training``, and no longer calls ``System.exit`` at the end, so
  that its live plot stays open with the final population until the window is closed
- ``BaseAlgorithmRegistry``, ``ProblemRegistry``, ``ProblemSpec``, ``IndicatorRegistry`` and
  ``RunStatusWriter`` move from ``cli.training`` to ``org.uma.evolver.cli``, shared by the training
  and solving tools; request files and entry points do not change
- ``scripts/`` keeps only active, reusable scripts, and is no longer ignored by git; the Python
  dependencies (``scripts/requirements.txt``, ``environment.yml``) are trimmed to what they use
- The examples of ``example.baselevel.standard`` follow one structure: every value (problem,
  reference front, parameter space, population size, evaluations, configuration and observer
  frequencies) is a local variable at the start of ``main``, the configuration is a text block, and
  they all end by logging the time, the evaluations and the seed, writing ``VAR.csv``/``FUN.csv``
  and printing the quality indicators when there is a reference front. The ``new-baselevel``
  command describes the structure

Fixed
~~~~~

- The last checkpoint of ``VAR_CONF.txt``, ``INDICATORS.csv`` and ``CONFIGURATIONS.csv`` was written
  twice when the last meta-evaluation fell on a periodic checkpoint (for instance, 2000
  meta-evaluations with a write frequency of 50), because the final front is written once more at
  the end of the run. It is now written once, in both encodings
- RVEA's external archive (``algorithmResult`` = ``externalArchive``) was filled only with the
  final population, so it could not add any solution to it and the parameter had almost no effect.
  It is now fed with every evaluated solution, as in the other algorithms
  (``SequentialEvaluationWithArchive``), and ``DoubleRVEA.build()`` returns an
  ``EvolutionaryAlgorithm``
- ``BinarySMSEMOA`` and ``PermutationSMSEMOA`` could not be built from their parameter spaces: the
  algorithm reads the selection as ``gaSelection`` (renamed in ``SMSEMOADouble.yaml``), but
  ``SMSEMOABinary.yaml`` and ``SMSEMOAPermutation.yaml`` still called it ``selection``, and their
  parameter factories did not create it. The spaces now call it ``gaSelection``
- ``PermutationMOEAD`` could not be built from ``MOEADPermutation.yaml``, where the mutation is a
  global sub-parameter of the variation: ``PermutationVariationParameter`` now looks for it there
  as well, as ``BinaryVariationParameter`` does
- ``MOEADDTLZ2Example`` used ``MOEADDoubleFull.yaml``, renamed to ``MOEADDouble.yaml``, and
  ``SMSEMOABiObjectiveTSPExample`` used the binary space and operators on a permutation problem;
  the chart titles of ``MOPSOSMPSOZDT4Example``, ``SMSEMOAExample`` and
  ``RDEMOEASPEA2DTLZ2Example`` named another algorithm

Removed
~~~~~~~

- ``parameterSpaces/NSGAIIDouble.irace`` and ``parameterSpaces/MOEADouble.irace``, outdated and
  unused: the generators in ``org.uma.evolver.irace.generator`` produce irace's parameter files
- The experiment-specific analysis scripts ``analysis_A_hv_evolution/``,
  ``compare_moead_vs_paes.py`` and ``generate_cd_plots.py``
- The ``PAESvsMOEADValidation`` and ``PAESvsMOEADDTLZValidation`` examples, with their report
  script ``plot_paes_vs_moead_validation.py``

2.1 (2026-09-24)
----------------

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

