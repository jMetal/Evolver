CLI Tools
=========

The ``org.uma.evolver.cli`` packages let an external tool (or a terminal) launch and inspect jobs
without recompiling anything: ``cli.training`` runs meta-optimization training jobs, and
``cli.solving`` runs a configurable algorithm on a problem. A job is described entirely by YAML
files, and the entry points below are plain Java classes with a ``main`` method, runnable straight
from the packaged jar.

All the tools are built with the rest of the project:

.. code-block:: bash

   mvn clean package
   # produces target/Evolver-<version>-jar-with-dependencies.jar

TrainingRunnerMain
-------------------

Runs a single training job end to end: resolves the base-level algorithm and the meta-optimizer
engine, runs the meta-optimization, and writes its progress and results to disk so an external
process can poll them instead of parsing log output.

Usage
~~~~~

.. code-block:: bash

   java -cp target/Evolver-<version>-jar-with-dependencies.jar \
       org.uma.evolver.cli.training.TrainingRunnerMain <request.yaml> [status.yaml]

``status.yaml`` is optional; when omitted, it defaults to a file named ``status.yaml`` next to
``request.yaml``.

Example
~~~~~~~

.. code-block:: bash

   java -cp target/Evolver-<version>-jar-with-dependencies.jar \
       org.uma.evolver.cli.training.TrainingRunnerMain \
       src/main/resources/cli/training/nsgaii-zdt4-request.yaml

That file configures NSGA-II as both the base-level algorithm (tuned on ZDT4) and the
meta-optimizer:

.. code-block:: yaml

   baseLevel: Zdt4NSGAIIBaseLevel.yaml
   metaSearch: MetaNSGAIIFlatConfiguration.yaml
   outputDirectory: results/nsgaii/ZDT4
   writeFrequency: 100
   statusFrequency: 100
   frontPlotFrequency: 100   # optional; remove this line to run headless

``baseLevel`` and ``metaSearch`` are *names*, resolved against the reusable recipes bundled under
``src/main/resources/baseLevelConfigurations/`` and ``src/main/resources/metaOptimizerConfigurations/``
(or an absolute path to a standalone file of your own). ``request.yaml`` itself accepts these
fields:

.. list-table:: request.yaml fields
   :header-rows: 1
   :widths: 25 15 15 45

   * - Field
     - Required
     - Default
     - Description
   * - ``baseLevel``
     - yes
     - --
     - Name (or path) of a base-level configuration file
   * - ``metaSearch``
     - yes
     - --
     - Name (or path) of a meta-optimizer configuration file
   * - ``outputDirectory``
     - yes
     - --
     - Where ``METADATA.txt``/``INDICATORS.csv``/``CONFIGURATIONS.csv`` are written
   * - ``writeFrequency``
     - no
     - 100
     - Evaluations between writes of ``CONFIGURATIONS.csv``/``INDICATORS.csv``
   * - ``statusFrequency``
     - no
     - 100
     - Evaluations between updates of ``status.yaml``/the log
   * - ``frontPlotFrequency``
     - no
     - none (headless)
     - When present, opens a window that plots the meta-optimizer's population in the space of the
       first two indicators, updated every that many evaluations. Its title names the
       meta-optimizer and the base-level algorithm, with the progress against the stopping
       condition (``Evaluations: 500 of 2000``, or ``Time: 12.3 of 60 min (500 evaluations)``).
       Closing the window ends the run
   * - ``writePopulation``
     - no
     - ``false``
     - When ``true``, also writes the whole population of the meta-optimizer at every checkpoint to
       ``POPULATION_INDICATORS.csv`` and ``POPULATION_CONFIGURATIONS.csv``

Bounding the meta-optimizer by time
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The meta-optimizer stops after a number of meta-evaluations (``metaMaxEvaluations`` in its
configuration file) **or** after a computing time, in minutes (``metaMaxComputingTimeMinutes``,
with decimals allowed, for example ``7.5``). The two keys are mutually exclusive: give exactly one
of them. For example, a copy of ``MetaNSGAIIFlatConfiguration.yaml`` that runs for an hour and a
half replaces ``metaMaxEvaluations: 2000`` by:

.. code-block:: yaml

   metaMaxComputingTimeMinutes: 90

The bundled ``MetaNSGAIIFlatComputingTimeConfiguration.yaml`` is such a copy, with a limit of 60
minutes, and ``nsgaii-re3d-computing-time-request.yaml`` uses it.

The limit is checked at the beginning of each generation, so when it is reached the generation in
progress is completed before the run stops: the real time exceeds the limit by up to the time of one
generation (random search works in batches of ``numberOfCores`` evaluations). The initial population
is always evaluated, even if that takes longer than the limit, in which case no generation is run.
All the meta-optimizers support it. The asynchronous ``AsyncNSGA-II`` has no generations: it checks
the limit after every evaluation, once the initial population has been evaluated, so it exceeds the
limit by up to the time of one evaluation, and the evaluations in progress when it stops are
discarded.

Output
~~~~~~

While running, ``status.yaml`` is updated every ``statusFrequency`` evaluations:

.. code-block:: yaml

   status: RUNNING
   evaluationsDone: 800
   maxEvaluations: 2000
   updatedAt: '2026-09-18T09:49:41.123456'

With a time limit, ``maxEvaluations`` is ``0`` (there is no limit on the evaluations) and the file
also has ``maxComputingTimeMinutes`` and ``elapsedMinutes``, from which to compute the progress.

On success it ends with ``status: FINISHED``; on failure, ``status: FAILED`` plus an
``errorMessage`` field. A ``results.yaml`` file is written next to ``request.yaml``, pointing at
the output files:

.. code-block:: yaml

   outputDirectory: results/nsgaii/ZDT4
   metadataFile: results/nsgaii/ZDT4/METADATA.txt
   indicatorsFile: results/nsgaii/ZDT4/INDICATORS.csv
   configurationsFile: results/nsgaii/ZDT4/CONFIGURATIONS.csv

In the output directory, ``METADATA.txt`` has the settings of the run, including the stopping
condition (``Max Evaluations`` or ``Max Computing Time``, and ``Stopping condition``) and, in its
``Execution`` section, the wall-clock time and the meta-evaluations performed; ``INDICATORS.csv``,
``CONFIGURATIONS.csv`` and ``VAR_CONF.txt`` have, every ``writeFrequency`` evaluations, the
**non-dominated** configurations of the meta-optimizer's population, with their indicator values,
their parameter values and their configuration strings. In ``VAR_CONF.txt`` each checkpoint starts
with the meta-evaluations and the computing time of the meta-optimizer in minutes, whatever the
stopping condition:

.. code-block:: none

   # Evaluation: 1000
   # Time (min): 3.200
   EP=0.015577074645407418 NHV=0.014837681855983442 | --algorithmResult externalArchive ...

With ``writePopulation: true``,
``POPULATION_INDICATORS.csv`` and ``POPULATION_CONFIGURATIONS.csv`` have the same columns as
``INDICATORS.csv`` and ``CONFIGURATIONS.csv`` for the **whole** population, which shows its
diversity, and ``results.yaml`` also points at them (``populationIndicatorsFile``,
``populationConfigurationsFile``). ``scripts/plot_meta_population.py`` plots the population at
several checkpoints from them.

.. note::

   ``TrainingRunnerMain`` always calls ``System.exit(0)`` after the run finishes, since the
   ``AsyncNSGA-II`` engine's worker thread pool does not shut down on its own. This is harmless
   for the other meta-optimizer engines, which already terminate naturally.

Base-level algorithms
~~~~~~~~~~~~~~~~~~~~~

The ``algorithmName`` and ``encoding`` of a ``baseLevel`` file choose the algorithm that is tuned
(and the one a solve request runs). These are the ones ``cli.training`` and ``cli.solving`` build,
and what ``DescribeMain`` lists:

.. list-table::
   :header-rows: 1
   :widths: 20 30 50

   * - ``algorithmName``
     - ``encoding``
     - Notes
   * - ``NSGA-II``
     - Double, Binary, Permutation
     -
   * - ``NSGA-III``
     - Double
     -
   * - ``MOEAD``
     - Double, Binary, Permutation
     - Needs ``extraConfig: {weightVectorFilesDirectory: ...}``, the directory of its weight vectors
       (``resources/weightVectors``); the population size must have a file there for the number of
       objectives of the problem
   * - ``SMS-EMOA``
     - Double, Binary, Permutation
     -
   * - ``RDEMOEA``
     - Double, Permutation
     -
   * - ``RVEA``
     - Double
     - Needs ``weightVectorFilesDirectory``, like MOEA/D
   * - ``AGE-MOEA``
     - Double
     -
   * - ``SSMOEA``
     - Double
     - Steady-state: a run is slower than the one of a generational algorithm with the same budget
   * - ``PAES``
     - Double, Binary, Permutation
     - It has no population: ``populationSize`` is the number of solutions to find (the size of its
       archive)

The parameter space of each one is ``<Name><Encoding>.yaml`` (``NSGAIIIDouble.yaml``,
``SMSEMOAPermutation.yaml``, ...), without the hyphens of the name. The problems must have the
encoding of the algorithm: the runners fail before running otherwise. MOPSO, which has no
per-encoding classes, is not registered yet.

Bundled examples
~~~~~~~~~~~~~~~~

``src/main/resources/cli/training/`` ships one ``request.yaml`` per reference case, each runnable
as-is:

.. list-table:: Bundled request.yaml examples
   :header-rows: 1
   :widths: 35 65

   * - File
     - What it exercises
   * - ``nsgaii-zdt4-request.yaml``
     - NSGA-II tuning NSGA-II on a single problem (flat encoding)
   * - ``nsgaii-zdt-benchmark-request.yaml``
     - NSGA-II tuning NSGA-II on a multi-problem ZDT training set (flat encoding)
   * - ``nsgaii-re3d-request.yaml``
     - NSGA-II tuning NSGA-II on a named multi-problem training set (flat encoding)
   * - ``nsgaii-re3d-computing-time-request.yaml``
     - The same, with the meta-optimizer bounded by computing time (60 minutes) instead of by
       meta-evaluations
   * - ``nsgaii-two-biobjective-tsp-request.yaml``
     - NSGA-II tuning a **Permutation**-encoded base-level algorithm (``PermutationNSGAII`` on two
       bi-objective TSP instances), instead of the Double encoding of most bundled requests
   * - ``tutorial-encodings-binary-request.yaml``
     - AsyncNSGA-II tuning a **Binary**-encoded base-level algorithm (``BinaryNSGAII`` on ZDT5)
   * - ``moead-zdt4-request.yaml``
     - NSGA-II tuning MOEA/D, a base algorithm with its own extra config (flat encoding)
   * - ``rvea-zdt1-dtlz2-request.yaml``
     - NSGA-II tuning RVEA (RVEA, RVEA* or iRVEA) on problems with two and three objectives, each
       with its own weight vector file (flat encoding)
   * - ``tree-nsgaii-re3d-request.yaml``
     - NSGA-II tuning NSGA-II with the derivation-tree encoding
   * - ``tree-agemoea-re3d-request.yaml``
     - AGE-MOEA as the meta-optimizer engine with the derivation-tree encoding
   * - ``tree-randomsearch-re3d-request.yaml``
     - RandomSearch as the meta-optimizer engine with the derivation-tree encoding
   * - ``async-nsgaii-zdt4-request.yaml``
     - AsyncNSGA-II as the meta-optimizer engine (flat encoding)
   * - ``async-nsgaii-dtlz3d-request.yaml``
     - AsyncNSGA-II over a seven-problem DTLZ training set (flat encoding)
   * - ``smpso-re31-request.yaml``
     - SMPSO as the meta-optimizer engine (flat encoding)
   * - ``agemoea-zdt4-request.yaml``
     - AGE-MOEA as the meta-optimizer engine (flat encoding)
   * - ``spea2-dtlz3-request.yaml``
     - SPEA2 as the meta-optimizer engine (flat encoding)
   * - ``randomsearch-dtlz3d-request.yaml``
     - RandomSearch as the meta-optimizer engine (flat encoding)

SolveRunnerMain
---------------

Runs a configurable algorithm, with a given configuration, on a problem, once or several times
(independent runs), and writes the fronts found and their quality indicators — the command-line
counterpart of writing a Java ``main`` as in :doc:`../tutorials/base_level_algorithms`. It uses the
same registries as ``TrainingRunnerMain``: the algorithms, problems and indicators it accepts are
those listed by ``DescribeMain``.

Usage
~~~~~

.. code-block:: bash

   java -cp target/Evolver-<version>-jar-with-dependencies.jar \
       org.uma.evolver.cli.solving.SolveRunnerMain <request.yaml> [status.yaml]

As with ``TrainingRunnerMain``, ``status.yaml`` defaults to a file next to ``request.yaml``.

Example
~~~~~~~

A solve request is a single, self-contained file
(``src/main/resources/cli/solving/nsgaii-zdt1-request.yaml``):

.. code-block:: yaml

   algorithmName: NSGA-II
   encoding: Double
   populationSize: 100
   yamlParameterSpaceFile: NSGAIIDouble.yaml
   configurationFile: defaultConfigurations/NSGAIIDoubleDefault.txt
   problem: ZDT1
   referenceFrontFileName: resources/referenceFronts/ZDT1.csv
   maxEvaluations: 25000
   numberOfIndependentRuns: 5
   seed: 1
   indicatorNames: [Epsilon, NormalizedHypervolume]
   outputDirectory: results/solve/NSGA-II.ZDT1

.. list-table:: Solve request fields
   :header-rows: 1
   :widths: 28 14 58

   * - Field
     - Default
     - Meaning
   * - ``algorithmName``, ``encoding``, ``populationSize``, ``yamlParameterSpaceFile``,
       ``extraConfig``
     - ``encoding``: ``Double``; ``extraConfig``: none
     - The algorithm, as in a training run's ``baseLevel`` file
   * - ``configuration``
     - —
     - The configuration, as a string (``--algorithmResult population ...``)
   * - ``configurationFile``
     - —
     - Instead of ``configuration``: a file with one configuration per line, of which the first is
       used, such as those in ``defaultConfigurations/``. Exactly one of the two must be given
   * - ``problem``
     - required
     - A name listed by ``DescribeMain`` (``ZDT1``, ``ZDT5``, ``KroAB100TSP``), or a fully-qualified class name, optionally
       with constructor arguments: ``{class: org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1,
       args: [7, 3]}``
   * - ``referenceFrontFileName``
     - none
     - The reference front of the problem; required when ``indicatorNames`` is given
   * - ``maxEvaluations``
     - required
     - The evaluation budget of each run
   * - ``numberOfIndependentRuns``
     - 1
     - The number of runs
   * - ``seed``
     - drawn at random
     - The seed of the first run; run *i* uses ``seed + i - 1``, so every run can be reproduced
   * - ``indicatorNames``
     - none
     - The quality indicators computed for each run
   * - ``statusFrequency``
     - none
     - Every how many evaluations ``status.yaml`` is updated while a run is in progress; absent, it
       is updated only when a run ends. The more often, the slower the run (see below)
   * - ``frontFrequency``
     - none
     - Every how many evaluations of a run the current front is written to ``CURRENT_FRONT.csv``
       while the run is in progress (see below); absent, none is written. The more often, the
       slower the run
   * - ``writePopulation``
     - false
     - With ``frontFrequency``, write the whole population to that file instead of only its
       non-dominated solutions
   * - ``frontDelayMillis``
     - none
     - With ``frontFrequency``, the milliseconds the run pauses after writing each front, as the
       display delay of jMetal's chart observers does: a follower that reads the file every
       second or so then sees every front, instead of only the last one of a run that is over in
       a second. It slows the run down by that much for each front written
   * - ``outputDirectory``
     - required
     - Where the results are written

Output
~~~~~~

In ``outputDirectory``:

- ``run-<i>/VAR.csv`` and ``run-<i>/FUN.csv``: the result of each run (``run-1``, ``run-2``, ...);
- ``INDICATORS.csv``: one row per run, with its seed, its computing time and its indicator values:

  .. code-block:: none

     Run,Seed,TimeMs,EP,NHV
     1,1,336,0.010348755426522999,0.009763737439173137
     2,2,242,0.012313320050797077,0.010281578438605998

- ``METADATA.txt``: the settings of the run, including the configuration used and the seeds.

The indicators are computed as in a training run, so their values are comparable: on the
non-dominated solutions of the result, with the front and the reference front normalized to the
bounds of the reference front.

``status.yaml`` has the same fields as for ``TrainingRunnerMain``. It is updated after each run,
counting the evaluations of the runs already finished out of ``numberOfIndependentRuns *
maxEvaluations``; with ``statusFrequency``, also while a run is in progress, every that many
evaluations, so that a GUI can show how far the run is. An algorithm that evaluates a whole
offspring population at a time reports in steps of its size, so a frequency smaller than that gives
the same updates as one equal to it. Updating costs time: on NSGA-II with ZDT1, updating every 100
evaluations slowed a run down by about 4 % when the algorithm reports every evaluation (a
population of one) and by about 5 % at every generation, and every 1000 evaluations by 1 %, but
updating after every single evaluation nearly doubled the time of the first (+85 %).

With ``frontFrequency``, ``CURRENT_FRONT.csv`` in the output directory holds the solutions of the
run in progress, so that a GUI can plot how the front evolves. It is overwritten each time (it is
written to a temporary file that is moved over it, so it is never read half-written) and removed
when the runs end. It is a CSV with a row per solution, ``Run,Evaluations,NonDominated,F1,...,Fm``:
the run and its evaluations so far (repeated in each row), 1 if the solution is non-dominated
within the current population and 0 if not, and its objectives. By default only the non-dominated
solutions are written, so ``NonDominated`` is always 1; with ``writePopulation`` the whole
population is, dominated solutions included. Writing the front costs more than updating the status,
since the non-dominated solutions have to be found: on NSGA-II with ZDT1, every 1000 evaluations
slowed a run down by 11 % (generational) or 3 % (population of one), every 100 by 75 % or 12 %, and
every single evaluation by 76 % or, with a population of one, more than ten times.

``results.yaml`` points at the output files:

.. code-block:: yaml

   outputDirectory: results/solve/NSGA-II.ZDT1
   metadataFile: results/solve/NSGA-II.ZDT1/METADATA.txt
   indicatorsFile: results/solve/NSGA-II.ZDT1/INDICATORS.csv
   numberOfIndependentRuns: 5
   runDirectoryPattern: results/solve/NSGA-II.ZDT1/run-<i>

Bundled examples
~~~~~~~~~~~~~~~~

``src/main/resources/cli/solving/`` ships two requests, runnable from the root of the repository:

.. list-table:: Bundled solve requests
   :header-rows: 1
   :widths: 35 65

   * - File
     - What it exercises
   * - ``nsgaii-zdt1-request.yaml``
     - NSGA-II with its default configuration, read from a file, on ZDT1; five runs with a fixed
       seed
   * - ``moead-zdt4-request.yaml``
     - MOEA/D, an algorithm with its own extra config, with an inline configuration, on ZDT4
   * - ``rvea-dtlz2-request.yaml``
     - RVEA with its default configuration, read from a file, on DTLZ2; five runs with a fixed seed

DescribeMain
------------

Prints a single, machine-readable YAML manifest describing everything the CLI tools can
resolve: registered base-level algorithms, meta-optimizer algorithms, training problems,
indicators, the file names available under each reusable resource directory, and the shape of
``request.yaml``/``baseLevel``/``metaSearch`` themselves. It takes no arguments, runs no training
job, and exits as soon as the manifest is written — intended for an external tool (e.g.
Evolver-Studio) to discover what is runnable without reading Java source.

Usage
~~~~~

.. code-block:: bash

   java -cp target/Evolver-<version>-jar-with-dependencies.jar \
       org.uma.evolver.cli.training.DescribeMain

Example output
~~~~~~~~~~~~~~

.. code-block:: yaml

   baseAlgorithms:
   - name: NSGA-II
     encoding: Double
     requiredExtraConfigKeys: []
   - name: NSGA-II
     encoding: Binary
     requiredExtraConfigKeys: []
   - name: NSGA-II
     encoding: Permutation
     requiredExtraConfigKeys: []
   - name: MOEAD
     encoding: Double
     requiredExtraConfigKeys:
     - weightVectorFilesDirectory
   - name: RVEA
     encoding: Double
     requiredExtraConfigKeys:
     - weightVectorFilesDirectory
   metaAlgorithms:
   - name: NSGA-II
     family: EVOLUTIONARY
     supportsFlat: true
     supportsTree: true
     operatorParameterSpaceFile: NSGAIIMetaDouble.yaml
     hardcodedOperatorFlags: []
   - name: AGE-MOEA
     family: EVOLUTIONARY
     supportsFlat: true
     supportsTree: true
     operatorParameterSpaceFile: AGEMOEAMetaDouble.yaml
     hardcodedOperatorFlags: []
   - name: SPEA2
     family: EVOLUTIONARY
     supportsFlat: true
     supportsTree: false
     operatorParameterSpaceFile: null
     hardcodedOperatorFlags:
     - name: mutationProbabilityFactor
       type: double
       required: false
   - name: AsyncNSGA-II
     family: ASYNCHRONOUS
     supportsFlat: true
     supportsTree: true
     operatorParameterSpaceFile: AsyncNSGAIIMetaDouble.yaml
     hardcodedOperatorFlags: []
   - name: SMPSO
     family: PARTICLE_SWARM
     supportsFlat: true
     supportsTree: false
     operatorParameterSpaceFile: null
     hardcodedOperatorFlags: []
   - name: RandomSearch
     family: RANDOM_SEARCH
     supportsFlat: true
     supportsTree: true
     operatorParameterSpaceFile: null
     hardcodedOperatorFlags: []
   problems:
   - DTLZ1
   - DTLZ2
   # ... every problem ProblemRegistry resolves
   indicators:
   - Epsilon
   - GeneralizedSpread
   - HypervolumeMinus
   - InvertedGenerationalDistancePlus
   - NormalizedHypervolume
   - Spread
   resourceDirectories:
     parameterSpaces: [...]
     baseLevelConfigurations: [...]
     metaOptimizerConfigurations: [...]
     defaultConfigurations: [...]
   schemas:
     request: [...]
     baseLevel: [...]
     metaSearchFlat: [...]
     metaSearchTree: [...]
     solveRequest: [...]

Manifest sections
~~~~~~~~~~~~~~~~~

.. list-table:: Manifest top-level keys
   :header-rows: 1
   :widths: 25 75

   * - Key
     - Contents
   * - ``baseAlgorithms``
     - Every ``(algorithmName, encoding)`` pair accepted by ``BaseLevelConfig``'s
       ``algorithmName``/``encoding`` fields (see *Base-level algorithms* above), and any ``extraConfig`` keys it requires (e.g. MOEA/D's
       ``weightVectorFilesDirectory``)
   * - ``metaAlgorithms``
     - Every meta-optimizer algorithm name accepted by ``metaSearch.algorithm``, its family
       (``EVOLUTIONARY``, ``ASYNCHRONOUS``, ``PARTICLE_SWARM``, ``RANDOM_SEARCH``), whether it
       supports the tree encoding, and its flat-encoding operator configuration (a
       ``ParameterSpace`` YAML file, or a fixed list
       of hardcoded operator flags for algorithms with no parameter space of their own)
   * - ``problems``
     - Every training problem name ``ProblemRegistry`` resolves, usable in
       ``trainingProblemNames``
   * - ``problemCatalogue``
     - One entry per problem of ``problems``, with its ``family``, its ``encoding`` (``Double``,
       ``Binary`` or ``Permutation``: the one the base-level algorithm must have), its
       ``numberOfObjectives`` and ``numberOfVariables`` (left out for a problem that cannot be built,
       such as a TSP instance whose files are not in the working directory) and, for the problems
       that take them, the ``arguments`` of the constructor: a ``name``, a ``type`` (``integer``,
       ``number`` or ``boolean``) and the ``default`` of the problem built with none (left out when
       unknown). A problem is given all its arguments or none: ``{class: DTLZ2, args: [12, 3]}``
   * - ``indicators``
     - Every quality indicator name ``IndicatorRegistry`` resolves, usable in ``indicatorNames``
   * - ``resourceDirectories``
     - The file names actually present under each reusable resource directory
       (``parameterSpaces``, ``baseLevelConfigurations``, ``metaOptimizerConfigurations``,
       ``defaultConfigurations``), so a caller does not have to guess a naming convention
   * - ``schemas``
     - The field shape of ``request.yaml``, ``baseLevel``, the two ``metaSearch`` variants
       (``metaSearchFlat``, ``metaSearchTree``) and the request of ``SolveRunnerMain``
       (``solveRequest``) — name, Java type, whether it is required, and its default when optional

The data behind the manifest comes from the same registries ``TrainingRunner`` itself uses to
resolve a request (``BaseAlgorithmRegistry``, ``MetaAlgorithmRegistry``, ``ProblemRegistry``,
``IndicatorRegistry``), plus reflection over the ``BaseLevelConfig``/``FlatMetaSearchConfig``/
``TreeMetaSearchConfig``/``TrainingRequest``/``SolveRequest`` records — not a second, hand-maintained copy that
could drift from the actual resolution logic.
