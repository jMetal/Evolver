.. _tutorial_automating_with_the_cli:

E15. Automating Evolver with the CLI
====================================

:Level: Advanced
:Version: 1.0 (2026-10-06)
:Time: about 30 minutes, plus the runs of the examples (about 3 minutes in all)
:Timings measured on: Apple M5 Pro (18 cores, 14 of them used by the batch of Step 5), 64 GB of
   RAM, macOS 26.6.2, Java 21.0.12 (Oracle JDK)
:Prerequisites: :doc:`E3. Meta-optimization workflow <meta_optimization_workflow>`,
   :doc:`E4. Evolver in 10 minutes <../quick_start>`

Until now the tutorials ran from Java, or from a single command that you typed. This one is about
using Evolver **from another program**: a script, a scheduled job, a notebook or a tool such as
Evolver-Studio, without writing Java. Evolver offers for that a small contract made of files: you
write a *request*, start a process, read a *status* file while it runs and a *results* file when it
ends. The tutorial explains the contract, shows each piece by hand, and then uses it to run a batch
of 24 requests in parallel and gather their results in one table.

The reference of every field is in :doc:`../utilities/cli_tools`; here the goal is to show how the
pieces fit together. The commands run from the root of the Evolver repository, with a jar built by
``mvn -DskipTests package``.

Step 1: the contract
--------------------

Evolver has two runners, with the same shape:

.. list-table::
   :header-rows: 1
   :widths: 24 38 38

   * - Runner
     - It runs
     - Its request has
   * - ``cli.training.TrainingRunnerMain``
     - A meta-optimization (a training)
     - ``baseLevel``, ``metaSearch``, ``outputDirectory``
   * - ``cli.solving.SolveRunnerMain``
     - One algorithm with one configuration on one problem, several independent runs
     - Algorithm, configuration, problem, budget, indicators, ``outputDirectory``

Both are run as ``java -cp <jar> <runner> request.yaml [status.yaml]`` and both leave:

- ``status.yaml``: updated while the process runs. ``status`` is ``RUNNING``, ``FINISHED`` or
  ``FAILED`` (with an ``errorMessage``), together with ``evaluationsDone`` and ``maxEvaluations``;
- ``results.yaml``, next to the request, when it ends well: where the output files are;
- the output files themselves (``INDICATORS.csv``, ``METADATA.txt``, ...), in ``outputDirectory``.

There is no server to keep alive: a run is just a process that writes files. That makes it simple to
debug (read the files), to run for hours (nothing to reconnect to) and to start from anything that can
write a YAML file and start a process. It has three consequences that you will see below, and that any
program that automates Evolver has to take into account:

- the status file is **overwritten in place**, not replaced atomically, so a reader may find it empty
  or half written and must try again;
- there is no ``CANCELLED`` state: a process that is killed leaves ``RUNNING`` behind;
- the exit code of the process is 0 when it finishes and 1 when it fails.

Step 2: ask Evolver what it can run
-----------------------------------

Before writing requests, a program needs to know the names that it may use. ``DescribeMain`` prints
a *manifest* in YAML and runs nothing else:

.. code-block:: bash

    java -cp target/Evolver-<version>-jar-with-dependencies.jar \
        org.uma.evolver.cli.training.DescribeMain > manifest.yaml

Its top-level keys are ``baseAlgorithms`` (18 pairs of algorithm and encoding),
``metaAlgorithms`` (the 6 meta-optimizers of E12), ``problems`` and ``problemCatalogue`` (107 problems,
with the encoding, the number of objectives and variables and the arguments of each one),
``indicators``, ``resourceDirectories`` (the files that exist under ``parameterSpaces/``,
``defaultConfigurations/``, ...) and ``schemas`` (the fields of each kind of request, with their
types, whether they are required and their defaults).

A few lines of Python answer questions that would otherwise need the documentation:

.. code-block:: python

    import yaml

    manifest = yaml.safe_load(open("manifest.yaml"))

    # The problems a permutation algorithm can solve
    [p["name"] for p in manifest["problemCatalogue"] if p["encoding"] == "Permutation"]
    # ['EuclidAB300', 'KroAB100TSP', 'KroABC100TSP', ..., 'KroAE100TSP']

    # The algorithms with a binary encoding
    sorted({a["name"] for a in manifest["baseAlgorithms"] if a["encoding"] == "Binary"})
    # ['MOEAD', 'NSGA-II', 'PAES', 'SMS-EMOA']

The manifest is generated from the same registries that resolve a request, so it cannot get out of
date with them: a program that builds its menus from it keeps working when Evolver adds an algorithm.
This is how Evolver-Studio fills its selectors.

Step 3: a solve request by hand
-------------------------------

The simplest request runs the default NSGA-II on ZDT1, three times:

.. code-block:: yaml

    algorithmName: NSGA-II
    encoding: Double
    populationSize: 100
    yamlParameterSpaceFile: NSGAIIDouble.yaml
    configurationFile: defaultConfigurations/NSGAIIDoubleDefault.txt
    problem: ZDT1
    referenceFrontFileName: resources/referenceFronts/ZDT1.csv
    maxEvaluations: 200000
    numberOfIndependentRuns: 3
    seed: 1
    indicatorNames: [Epsilon, NormalizedHypervolume]
    statusFrequency: 20000
    outputDirectory: results/e15/solve

The budget is only for illustration: 200000 evaluations are far more than ZDT1 needs (25000 is the
usual one, as in the batch of Step 6), but with fewer the run would end in a fraction of a second,
before there is anything to watch. Save it as ``results/e15/solve-request.yaml`` and run it.
``statusFrequency`` asks for an update of the status every 20000 evaluations:

.. code-block:: bash

    java -cp target/Evolver-<version>-jar-with-dependencies.jar \
        org.uma.evolver.cli.solving.SolveRunnerMain results/e15/solve-request.yaml &
    while ! grep -qE "FINISHED|FAILED" results/e15/status.yaml 2>/dev/null; do
        cat results/e15/status.yaml; sleep 1
    done

These are the updates it printed here, one per second (the run takes about 4 seconds):

.. code-block:: none

    {status: RUNNING, evaluationsDone: 60000, maxEvaluations: 600000, updatedAt: '2026-10-06T12:07:45.954730'}
    {status: RUNNING, evaluationsDone: 160000, maxEvaluations: 600000, updatedAt: '2026-10-06T12:07:46.641858'}
    {status: RUNNING, evaluationsDone: 380000, maxEvaluations: 600000, updatedAt: '2026-10-06T12:07:48.156916'}
    {status: FINISHED, evaluationsDone: 600000, maxEvaluations: 600000, updatedAt: '2026-10-06T12:07:49.675384'}

``maxEvaluations`` is the total of the three runs. When it ends, ``results.yaml`` tells where
everything is, and ``INDICATORS.csv`` has a row per run:

.. code-block:: none

    Run,Seed,TimeMs,EP,NHV
    1,1,1470,0.010481563096465224,0.008283071405551956
    2,2,1339,0.01142715268726091,0.008559462654954664
    3,3,1372,0.01451508676383173,0.00887654572896135

**An error that says what is wrong.** A request that does not make sense stops before running, with
``status: FAILED``, an ``errorMessage`` and exit code 1. For example, NSGA-II for permutations
(``yamlParameterSpaceFile: NSGAIIPermutation.yaml``, the default configuration of that encoding) on
ZDT1, which has real variables:

.. code-block:: yaml

    status: FAILED
    errorMessage: 'Problem ZDT1 is Double-encoded, but the algorithm was configured with the
      Permutation encoding: use a problem of that encoding (for instance EuclidAB300), or the
      algorithm for Double problems'

The message names both encodings and suggests a way out, so a program can show it to its user as it
is. The ``problemCatalogue`` of Step 2 is where to look first to avoid it.

Step 4: a training request by hand
----------------------------------

A training request is shorter, because the work is in the two files it names, resolved against the
recipes under ``src/main/resources/``. The request of tutorial E17, with the output directory
changed:

.. code-block:: yaml

    baseLevel: TutorialZdt4BaseLevel.yaml
    metaSearch: TutorialTimeNSGAIIMetaSearch.yaml    # stops after 2 minutes
    outputDirectory: results/e15/train/output
    writeFrequency: 50
    statusFrequency: 50

Copy it to its own directory (``results/e15/train/request.yaml``): the runner writes ``status.yaml``
and ``results.yaml`` next to the request, so two trainings must not share a directory. Run it as in
Step 3 with ``TrainingRunnerMain``. Because the training is bounded by time, its status has the limit
and the time elapsed, which is what a program needs to compute the progress:

.. code-block:: none

    {status: RUNNING, evaluationsDone: 350, maxEvaluations: 0, maxComputingTimeMinutes: 2.0, elapsedMinutes: 0.4, ...}
    ...
    {status: FINISHED, evaluationsDone: 1050, maxEvaluations: 0, maxComputingTimeMinutes: 2.0, elapsedMinutes: 2.1, ...}

(``maxEvaluations: 0`` means that there is no limit on evaluations.) The run took 2 minutes and 6
seconds in all, and its ``METADATA.txt`` records the 1050 meta-evaluations it performed and why it
stopped, as tutorial E17 explains.

**Stopping a run.** Evolver has no command to cancel: you kill the process. We did it 15 seconds
into another training, and this is what was left:

.. code-block:: none

    {status: RUNNING, evaluationsDone: 200, maxEvaluations: 0, maxComputingTimeMinutes: 2.0, elapsedMinutes: 0.2, ...}

with no ``results.yaml``. The status is a lie: the process is dead, but nobody wrote ``FAILED``.
A program that starts the runs has to remember what it has done: the process id, and a cancellation
that it asked for itself. When it did not ask for it, a ``RUNNING`` status whose ``updatedAt`` has
not changed for much longer than the update frequency, or whose process no longer exists, means a
lost run.

Step 5: reusable requests
-------------------------

Two details make a request a thing to reuse instead of copy:

- ``baseLevel`` and ``metaSearch`` are *names* of recipes bundled with Evolver (the lists are
  ``resourceDirectories`` in the manifest), or paths to files of your own. A set of experiments
  can share one base-level file and vary the meta-optimizer, as the exercises of E12 do.
- ``outputDirectory`` is relative to the directory the process runs in. Give every run its own: a
  request, a status and an output directory per experiment, side by side, is what makes a batch
  easy to inspect and to repeat.

For repeatability, fix ``seed`` in solve requests (run *i* uses ``seed + i - 1``). A training has no
seed in the request.

Step 6: a batch of requests
---------------------------

With a program that writes requests and starts processes, running a study stops being manual work.
The script
`scripts/cli_batch.py <https://github.com/jMetal/Evolver/blob/develop/scripts/cli_batch.py>`_ does it
for a common case: the **default configuration of every algorithm of an encoding, on some problems**,
with a summary of the medians of the indicators. It follows the steps of this tutorial:

1. ``read_manifest`` runs ``DescribeMain`` and parses its manifest. Which algorithms have a default
   configuration, and which problems an encoding admits, come from it (Step 2): nothing is listed in
   the script.
2. ``make_jobs`` writes a ``request.yaml`` for each algorithm and problem (``build_request``),
   in a directory of its own, following the file names of Evolver (``NSGA-II`` and ``Double`` give
   ``NSGAIIDouble.yaml`` and ``defaultConfigurations/NSGAIIDoubleDefault.txt``). It adds the weight
   vectors for the algorithms that require them (MOEA/D, RVEA: ``requiredExtraConfigKeys`` in the
   manifest), and it refuses an algorithm without a default configuration or a problem of another
   encoding before running anything.
3. ``run_batch`` starts one ``SolveRunnerMain`` process per request, at most ``--processes`` at a
   time (each one uses a core), and reports each one that ends.
4. ``collect_medians`` reads the ``INDICATORS.csv`` of each one and takes the median of every
   indicator over its runs, written to ``medians.csv``.

It runs as:

.. code-block:: bash

    python scripts/cli_batch.py target/Evolver-<version>-jar-with-dependencies.jar \
        --problems ZDT1,ZDT4,ZDT6 --evaluations 25000 --runs 10 --processes 14 \
        --output-dir results/e15-batch

The eight algorithms of the Double encoding that have a default configuration (NSGA-II, NSGA-III,
MOEA/D, SMS-EMOA, RVEA, AGE-MOEA, SSMOEA and PAES) on the three problems are 24 requests, of 10 runs
of 25000 evaluations each. The batch took **15 seconds** with 14 processes, and printed:

.. code-block:: none

    Algorithm Problem  TimeMs       EP      NHV
      NSGA-II    ZDT1   301.0 0.013139 0.010470
      NSGA-II    ZDT4   263.0 0.016068 0.016207
      NSGA-II    ZDT6   261.0 0.018624 0.040808
     NSGA-III    ZDT1   285.5 0.008248 0.008675
     NSGA-III    ZDT4   288.5 0.043881 0.021068
     NSGA-III    ZDT6   266.0 0.016744 0.047461
        MOEAD    ZDT1   256.0 0.039922 0.018395
        MOEAD    ZDT4   165.0 0.030608 0.035682
        MOEAD    ZDT6   161.0 0.034891 0.085001
     SMS-EMOA    ZDT1   281.0 0.005551 0.006328
     SMS-EMOA    ZDT4   128.5 0.065948 0.020436
     SMS-EMOA    ZDT6   155.0 0.009329 0.022143
         RVEA    ZDT1   279.0 0.047705 0.081542
         RVEA    ZDT4   197.5 0.384106 0.240170
         RVEA    ZDT6   195.0 0.198484 0.448369
     AGE-MOEA    ZDT1   854.0 0.009991 0.007876
     AGE-MOEA    ZDT4   825.5 0.011850 0.013264
     AGE-MOEA    ZDT6   874.5 0.015782 0.031097
       SSMOEA    ZDT1  1057.5 0.005899 0.006795
       SSMOEA    ZDT4  1054.0 0.010168 0.013668
       SSMOEA    ZDT6  1054.5 0.008888 0.021596
         PAES    ZDT1    62.0 0.501974 0.373534
         PAES    ZDT4    43.0 0.230577 0.174223
         PAES    ZDT6    69.0 0.006939 0.011215

``TimeMs`` is the median computing time of a run in milliseconds. These are medians of 10 runs of
configurations that nobody tuned, and the table is not a comparison to draw conclusions from: that
needs the statistical tests of tutorial :doc:`E9 <validating_a_configuration>`. What the table shows
is what the batch gives you for 15 seconds of work: a table of every algorithm that you can change
by editing the command, the base of a study or a regression check after a change to Evolver.

The script applies the lessons of Steps 1 to 4. It reads the status of a job tolerating a missing or
half-written file, it takes the final state from ``status.yaml`` and not only from the exit code, and
it keeps the log of each process (``runner.log``) next to its request, so that a ``FAILED`` job
can be diagnosed. It needs the reference front of each problem to be called ``<problem>.csv`` in
``resources/referenceFronts/`` (or ``referenceFrontsTSP/``): without one, the request has no
indicators.

Step 7: other tools
-------------------

Nothing in the contract is tied to Python: a shell loop, a Makefile, a workflow engine or a
scheduler can write the request and read the files. A few ideas:

- **A shell script**: ``yq`` reads and writes the YAML of requests and status files
  (``yq .status results/e15/status.yaml``), and ``xargs -P`` runs requests in parallel.
- **A notebook**: ``pandas.read_csv`` on ``INDICATORS.csv`` or ``CONFIGURATIONS.csv`` is the
  quickest way to look at a training, with the plots of tutorial E8 as a starting point.
- **A graphical interface**: Evolver-Studio is a client of this contract. It writes the request, starts the
  runner, polls the status and reads the results; its selectors come from ``DescribeMain``. If you
  want a tool for your own group, the same pieces are available to you.

Whatever the tool, the points of Step 1 apply: read the status tolerating a bad file, do not trust a
``RUNNING`` status of a dead process, and give each run its own directory.

What the CLI does not do yet
----------------------------

- There is **no runner for validation studies**: those are written in Java with jMetal's
  ``ExperimentBuilder`` (tutorials E7 and E9), and their results are read with the scripts of
  ``scripts/``.
- The asynchronous genetic algorithm (``MetaAsyncGeneticAlgorithmBuilder``) can only be used from
  Java, and MOPSO is not among the algorithms that the CLI builds (E12 and the manifest list what is).
- There is no cancel or pause: a run ends, fails or is killed.

Try it yourself
---------------

- Run the batch with ``--encoding Permutation`` and ``--problems KroAB100TSP,KroAC100TSP`` (use
  ``--indicators HypervolumeMinus``, since the Permutation fronts have the extreme points of E10).
  Which algorithms have a default configuration for that encoding?
- Add a column with the evaluations of each request, or write the table with ``to_markdown`` for your
  own report.
- Start two copies of the training of Step 4 at the same time, each with its own directory. How many
  meta-evaluations does each one perform in the 2 minutes, compared with one alone?
- Write a solve request for a problem that takes arguments, ``problem: {class: DTLZ2, args: [12, 3]}``,
  and find in the manifest what each argument is.
- Write a function that waits for a run to end and returns ``"LOST"`` when the status stays
  ``RUNNING`` and ``updatedAt`` does not change for a minute.

What's next
-----------

- :doc:`../utilities/cli_tools` is the reference of every field of the requests, the status and the
  results.
- :doc:`E12 <choosing_the_meta_optimizer>` explains how many cores a training uses, which matters when
  you run several processes at once.
- :doc:`E9 <validating_a_configuration>` explains how to analyze a study properly, once your batch has
  produced the runs.
