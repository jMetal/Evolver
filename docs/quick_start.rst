.. _getting_started:

E4. Evolver in 10 Minutes
=========================

:Level: Introductory
:Time: about 10 minutes, of which building takes about 20 seconds and the training about 35 seconds
:Timings measured on: Apple M5 Pro (18 cores, 8 of them used by the training), 64 GB of RAM,
   macOS 26.6.2, Java 21.0.12 (Oracle JDK), Maven 3.9.16
:Prerequisites: a terminal, git, a JDK and Maven

This quick start gets Evolver working on your machine and shows its two uses, from the command
line and without writing any code:

- running one of its **configurable algorithms** on a problem;
- **tuning** that algorithm automatically, and running it with the configuration found.

Each step links to the tutorial that explains it in depth.

Step 1: check the requirements
------------------------------

Evolver needs **Java 21 or later** (JDK 21 recommended) and **Maven 3.6 or later**:

.. code-block:: bash

    java -version     # should report version 21 or later
    mvn -v            # should report Maven 3.6 or later, running on Java 21 or later

If you have several JDKs installed, make ``JAVA_HOME`` point to JDK 21: ``mvn -v`` shows which one
Maven uses. See :ref:`installation` for more details.

Step 2: get and build Evolver
-----------------------------

.. code-block:: bash

    git clone https://github.com/jMetal/Evolver.git
    cd Evolver
    mvn -DskipTests package

``-DskipTests`` skips the tests to make the build quicker (``mvn clean install`` runs them). The
build produces a single JAR file with Evolver and all its dependencies in ``target/``. Keep its
name in a variable, so that the following commands work whatever the version:

.. code-block:: bash

    JAR=$(ls target/Evolver-*-jar-with-dependencies.jar)

Run every command of this guide **from the root of the Evolver repository**: the examples read
their reference fronts from ``resources/referenceFronts/`` with relative paths.

Step 3: run a configurable algorithm
------------------------------------

Evolver's algorithms are configurable: each one is described by a parameter space, and a
configuration chooses a value for each of its parameters. The example ``NSGAIIForZDT1Example`` runs
NSGA-II, with its standard configuration, on the ZDT1 problem:

.. code-block:: bash

    java -cp "$JAR" org.uma.evolver.example.baselevel.standard.NSGAIIForZDT1Example

It runs in less than a second, writes the solutions found to ``VAR.csv`` (their variables) and
``FUN.csv`` (their objective values), and prints several quality indicators of the front against
the reference front of ZDT1, among them:

.. code-block:: none

    INFO: EP: 0.011759127031143582
    INFO: NHV: 0.013234132557747302

Both are to be minimized. The example does not fix the random seed, so your values will be slightly
different. If you have set up the optional Python environment (see ``scripts/README.md``), you can
plot the front:

.. code-block:: bash

    python scripts/plot_front.py FUN.csv resources/referenceFronts/ZDT1.csv

To learn more: :doc:`tutorials/parameter_spaces` explains parameter spaces, and
:doc:`tutorials/base_level_algorithms` how to configure and run the algorithms from Java.

Step 4: tune the algorithm
--------------------------

Now let Evolver find a configuration of NSGA-II for the ZDT4 problem, a harder one, on which the
standard configuration struggles. A **meta-optimizer** (NSGA-II as well) searches the parameter
space of NSGA-II: it tries 500 configurations, runs NSGA-II with each of them on ZDT4, and minimizes
two quality indicators of the fronts found, NHV and EP.

The training is described by a request file, bundled with Evolver. Copy it to a working directory,
since the run writes its status and results next to it, and run it:

.. code-block:: bash

    mkdir -p results/quick-start
    cp src/main/resources/cli/training/tutorial-e4-request.yaml results/quick-start/request.yaml
    java -cp "$JAR" org.uma.evolver.cli.training.TrainingRunnerMain results/quick-start/request.yaml

It takes about 35 seconds with 8 cores. The meta-optimizer uses 8 cores to evaluate configurations
in parallel: if your machine has a different number of cores, change ``numberOfCores`` in
``src/main/resources/metaOptimizerConfigurations/TutorialQuickNSGAIIMetaSearch.yaml`` and build
again.

While it runs, ``results/quick-start/status.yaml`` shows its progress. When it finishes:

.. code-block:: none

    {status: FINISHED, evaluationsDone: 500, maxEvaluations: 500, ...}

and the results are in ``results/quick-start/training/``:

- ``METADATA.txt``: the settings of the run;
- ``INDICATORS.csv`` and ``CONFIGURATIONS.csv``: the indicator values and the parameters of the
  configurations on the meta-optimizer's front, at every checkpoint;
- ``VAR_CONF.txt``: the same configurations as configuration strings, one per line, preceded by
  their indicator values (``EP=... NHV=... | --algorithmResult ...``).

To learn more: :doc:`tutorials/meta_optimization_workflow` explains each piece of a training run,
how to run it from Java, and how to read its results.

Step 5: use the configuration found
-----------------------------------

The last block of ``VAR_CONF.txt`` is the meta-optimizer's final front. This command keeps the
configuration with the lowest NHV, the main objective, and saves it to a file:

.. code-block:: bash

    awk '/^# Evaluation/ {block = ""} / \| / {block = block $0 "\n"} END {printf "%s", block}' \
        results/quick-start/training/VAR_CONF.txt \
      | sed 's/.*NHV=\([^ ]*\) | \(.*\)/\1 \2/' | sort -g | head -1 | cut -d' ' -f2- \
      > results/quick-start/best-configuration.txt

The example ``NSGAIIZDT4WithArchiveExample`` runs NSGA-II on ZDT4 with 25000 evaluations, and
accepts a configuration as its arguments. Run it with the configuration found, and with the default
configuration of NSGA-II, stored in ``src/main/resources/defaultConfigurations/``:

.. code-block:: bash

    java -cp "$JAR" org.uma.evolver.example.baselevel.features.NSGAIIZDT4WithArchiveExample \
        $(cat results/quick-start/best-configuration.txt)

    java -cp "$JAR" org.uma.evolver.example.baselevel.features.NSGAIIZDT4WithArchiveExample \
        $(cat src/main/resources/defaultConfigurations/NSGAIIDoubleDefault.txt)

In three runs of each, the configuration found obtained NHV values between 0.0073 and 0.0077, and
the default one between 0.011 and 0.016. Your values will differ, since the training and the runs
are random, but the tuned configuration should be clearly better on ZDT4. A proper comparison needs
more runs and a statistical test, which later tutorials cover.

Working from an IDE
-------------------

Evolver is a standard Maven project: open the repository root in IntelliJ IDEA, Eclipse or VS Code
(with its Java extensions) as a Maven project. You can then run the examples and tutorials, under
``org.uma.evolver.example``, from their ``main`` methods; set the working directory to the root of
the repository, so that they find the reference fronts.

What's next
-----------

- The :ref:`tutorials_index` explain each part in depth, starting with
  :doc:`tutorials/parameter_spaces`.
- :doc:`utilities/cli_tools` describes the request files of ``TrainingRunnerMain`` and the
  ``DescribeMain`` manifest.
- :doc:`examples/index` lists the runnable examples.
