CLI Tools
=========

The ``org.uma.evolver.cli.training`` package lets an external tool (or a terminal) launch and
inspect meta-optimization training jobs without recompiling anything: a job is described entirely
by YAML files, and the two entry points below are plain Java classes with a ``main`` method,
runnable straight from the packaged jar.

Both tools are built with the rest of the project:

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
     - When present, shows a live Pareto front plot every that many evaluations

Output
~~~~~~

While running, ``status.yaml`` is updated every ``statusFrequency`` evaluations:

.. code-block:: yaml

   status: RUNNING
   evaluationsDone: 800
   maxEvaluations: 2000
   updatedAt: '2026-09-18T09:49:41.123456'

On success it ends with ``status: FINISHED``; on failure, ``status: FAILED`` plus an
``errorMessage`` field. A ``results.yaml`` file is written next to ``request.yaml``, pointing at
the output files:

.. code-block:: yaml

   outputDirectory: results/nsgaii/ZDT4
   metadataFile: results/nsgaii/ZDT4/METADATA.txt
   indicatorsFile: results/nsgaii/ZDT4/INDICATORS.csv
   configurationsFile: results/nsgaii/ZDT4/CONFIGURATIONS.csv

.. note::

   ``TrainingRunnerMain`` always calls ``System.exit(0)`` after the run finishes, since the
   ``AsyncNSGA-II`` engine's worker thread pool does not shut down on its own. This is harmless
   for the other meta-optimizer engines, which already terminate naturally.

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
   * - ``nsgaii-two-biobjective-tsp-request.yaml``
     - NSGA-II tuning a **Permutation**-encoded base-level algorithm (``PermutationNSGAII`` on two
       bi-objective TSP instances), instead of the Double encoding every other bundled request uses
   * - ``moead-zdt4-request.yaml``
     - NSGA-II tuning MOEA/D, a base algorithm with its own extra config (flat encoding)
   * - ``tree-nsgaii-re3d-request.yaml``
     - NSGA-II tuning NSGA-II with the derivation-tree encoding
   * - ``async-nsgaii-zdt4-request.yaml``
     - AsyncNSGA-II as the meta-optimizer engine (flat encoding)
   * - ``async-nsgaii-dtlz3d-request.yaml``
     - AsyncNSGA-II over a seven-problem DTLZ training set (flat encoding)
   * - ``smpso-re31-request.yaml``
     - SMPSO as the meta-optimizer engine (flat encoding)
   * - ``spea2-dtlz3-request.yaml``
     - SPEA2 as the meta-optimizer engine (flat encoding)
   * - ``randomsearch-dtlz3d-request.yaml``
     - RandomSearch as the meta-optimizer engine (flat encoding)

DescribeMain
------------

Prints a single, machine-readable YAML manifest describing everything ``cli.training`` can
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
     encoding: Permutation
     requiredExtraConfigKeys: []
   - name: MOEAD
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
   - name: SPEA2
     family: EVOLUTIONARY
     supportsFlat: true
     supportsTree: false
     operatorParameterSpaceFile: null
     hardcodedOperatorFlags:
     - name: offspringPopulationSize
       type: int
       required: false
     - name: mutationProbabilityFactor
       type: double
       required: false
   - name: AsyncNSGA-II
     family: ASYNCHRONOUS
     supportsFlat: true
     supportsTree: false
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
     supportsTree: false
     operatorParameterSpaceFile: null
     hardcodedOperatorFlags: []
   problems:
   - DTLZ1
   - DTLZ2
   # ... every problem ProblemRegistry resolves
   indicators:
   - Epsilon
   - HypervolumeMinus
   - InvertedGenerationalDistancePlus
   - NormalizedHypervolume
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

Manifest sections
~~~~~~~~~~~~~~~~~

.. list-table:: Manifest top-level keys
   :header-rows: 1
   :widths: 25 75

   * - Key
     - Contents
   * - ``baseAlgorithms``
     - Every ``(algorithmName, encoding)`` pair accepted by ``BaseLevelConfig``'s
       ``algorithmName``/``encoding`` fields (e.g. NSGA-II is registered for both ``Double`` and
       ``Permutation``), and any ``extraConfig`` keys it requires (e.g. MOEA/D's
       ``weightVectorFilesDirectory``)
   * - ``metaAlgorithms``
     - Every meta-optimizer algorithm name accepted by ``metaSearch.algorithm``, its family
       (``EVOLUTIONARY``, ``ASYNCHRONOUS``, ``PARTICLE_SWARM``), whether it supports the tree
       encoding, and its operator configuration (a ``ParameterSpace`` YAML file, or a fixed list
       of hardcoded operator flags for algorithms with no parameter space of their own)
   * - ``problems``
     - Every training problem name ``ProblemRegistry`` resolves, usable in
       ``trainingProblemNames``
   * - ``indicators``
     - Every quality indicator name ``IndicatorRegistry`` resolves, usable in ``indicatorNames``
   * - ``resourceDirectories``
     - The file names actually present under each reusable resource directory
       (``parameterSpaces``, ``baseLevelConfigurations``, ``metaOptimizerConfigurations``,
       ``defaultConfigurations``), so a caller does not have to guess a naming convention
   * - ``schemas``
     - The field shape of ``request.yaml``, ``baseLevel``, and the two ``metaSearch`` variants
       (``metaSearchFlat``, ``metaSearchTree``) — name, Java type, whether it is required, and its
       default when optional

The data behind the manifest comes from the same registries ``TrainingRunner`` itself uses to
resolve a request (``BaseAlgorithmRegistry``, ``MetaAlgorithmRegistry``, ``ProblemRegistry``,
``IndicatorRegistry``), plus reflection over the ``BaseLevelConfig``/``FlatMetaSearchConfig``/
``TreeMetaSearchConfig``/``TrainingRequest`` records — not a second, hand-maintained copy that
could drift from the actual resolution logic.
