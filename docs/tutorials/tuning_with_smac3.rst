.. _tutorial_tuning_with_smac3:

E16. Tuning with SMAC3
======================

:Level: Advanced
:Version: 1.0 (2026-10-13)
:Time: about 40 minutes, of which SMAC3 takes about 26 minutes (7 with Step 6's option)
:Timings measured on: Apple M5 Pro (18 cores, 16 of them used by the evaluation server), 64 GB of
   RAM, macOS 26.6.2, Java 21.0.12 (Oracle JDK), Python 3.11, SMAC3 2.4.1
:Prerequisites: :doc:`E15. Tuning with irace <tuning_with_irace>` (its Step 7, the evaluation server)

`SMAC3 <https://github.com/automl/SMAC3>`_ is a configurator based on Bayesian optimization: a
random forest learns how good a configuration is from the ones already evaluated, and proposes the
next ones where that model expects to find good ones. It is, with irace, one of the configurators
most used in the literature, and comparing what each one finds is a research question in itself.
This tutorial tunes the NSGA-II of tutorial E15 with SMAC3, on the same problems and with the same
evaluation of each configuration as a training of Evolver, so that the three tools can be compared
on equal terms.

Evolver provides the three pieces that SMAC3 needs:

- ``ConfigSpaceGenerator``, which writes a parameter space of Evolver as a configuration space of
  ConfigSpace, the library that SMAC3 uses to describe what it tunes (as
  ``IraceParameterDescriptionGenerator`` writes the parameter file of irace);
- the **evaluation server** of tutorial E15, which evaluates each configuration that SMAC3 proposes;
- ``scripts/smac/evolver_smac.py``, a client that connects them.

Step 1: the environment
-----------------------

SMAC3 is a Python package with dependencies of its own, so it gets an environment of its own,
separate from the one of the analysis scripts. With conda:

.. code-block:: bash

    conda create -n smac3 python=3.11 swig
    conda activate smac3
    pip install -r scripts/smac/requirements.txt

``swig`` is needed to build one of the dependencies of SMAC3. The file pins the versions this
tutorial was run with (SMAC3 2.4.1).

Step 2: the configuration space
-------------------------------

``ConfigSpaceGenerator`` reads a parameter space and its encoding and prints the configuration
space as JSON:

.. code-block:: bash

    java -cp target/Evolver-<version>-jar-with-dependencies.jar \
        org.uma.evolver.smac.ConfigSpaceGenerator NSGAIIDouble.yaml Double > NSGAIIDouble.json

.. code-block:: none

    {"type": "categorical", "name": "algorithmResult",
     "choices": ["population", "externalArchive"], "default_value": "population"},
    {"type": "uniform_int", "name": "populationSizeWithArchive", "lower": 10, "upper": 200, ...},

Every parameter becomes a hyperparameter of ConfigSpace: a categorical one, or an integer or real
one with the range of the parameter. The structure of the space becomes **conditions**: a
conditional parameter (``populationSizeWithArchive``) is only active when its parent takes the
value that declares it (``algorithmResult = externalArchive``), and a global sub-parameter whenever
its parent is active, so SMAC3 never proposes values for parameters that a configuration does not
use. A third argument, a default configuration such as
``defaultConfigurations/NSGAIIDoubleDefault.txt``, sets the default value of each hyperparameter to
its value in that configuration.

The client of Step 4 runs the generator itself, so this step is only to see what SMAC3 receives.

Step 3: the evaluation server
-----------------------------

As in Step 7 of tutorial E15, the server evaluates configurations of NSGA-II on the five ZDT
problems, with a population of 100 and 8000 evaluations, as a training of Evolver does
(``TutorialIraceZdtBaseLevel.yaml``). From the root of the repository:

.. code-block:: bash

    SERVER="java -cp target/Evolver-<version>-jar-with-dependencies.jar \
        org.uma.evolver.cli.evaluation.EvaluationServerMain"
    $SERVER start TutorialIraceZdtBaseLevel.yaml --threads 16 --state-dir results/tutorial-smac/server

``--threads`` is the number of evaluations that run at the same time, and it must be the number of
trials that SMAC3 runs at the same time (``--workers`` in Step 4): with more trials than threads
some wait, with fewer some threads are idle.

Step 4: running SMAC3
---------------------

In the ``smac3`` environment, from the root of the repository:

.. code-block:: bash

    python scripts/smac/evolver_smac.py --jar target/Evolver-<version>-jar-with-dependencies.jar \
        --trials 2000 --workers 16 --output-dir results/tutorial-smac/run

The client asks the server what it evaluates (``GET /status``): the parameter space, its encoding,
the problems and the quality indicators of its base-level file. Then:

- it writes the configuration space with ``ConfigSpaceGenerator``
  (``results/tutorial-smac/run/configspace.json``);
- each **trial** of SMAC3 is one run of a configuration on one problem, sent to the server, as an
  experiment of irace; the problems are the *instances* of SMAC3, and 2000 trials are the budget;
- the objectives are the indicators of the base-level file, EP and NHV, which SMAC3 handles with
  ParEGO (it minimizes a weighted combination of them whose weights change at every iteration);
- the first 50 configurations are random (``--initial-configurations``), as the first population
  of a meta-optimizer of Evolver.

When it ends, it prints the configurations of the Pareto front of what it evaluated, as
command-line arguments of Evolver:

.. code-block:: none

    2 configurations on the Pareto front of the trials
    --algorithmResult population --createInitialSolutions cauchy --offspringPopulationSize 1
      --selection tournament --variation crossoverAndMutationVariation --crossover SBX
      --mutation nonUniform --selectionTournamentSize 7 --crossoverProbability 0.5268 ...
    --algorithmResult externalArchive --createInitialSolutions latinHypercubeSampling ...

The trials, the configurations and SMAC3's own state are in ``results/tutorial-smac/run/smac3_output``,
and the server's log (``results/tutorial-smac/server/evaluations-8765.csv``) has a line per trial,
with its indicators and its time. Stop the server when you are done:

.. code-block:: bash

    $SERVER stop --state-dir results/tutorial-smac/server

At the very end, SMAC3 may print ``TimeoutError: timed out after 60 s`` while it closes the workers
of dask, its parallel engine: the results are already written, and it can be ignored.

Step 5: using a configuration
-----------------------------

Each printed line is a configuration of NSGA-II, valid in ``NSGAIIDouble.yaml``. Saved to a file,
one per line, it can be validated as in tutorial :doc:`E8 <validating_a_configuration>`, run with
``cli.solving`` (tutorial :doc:`E17 <automating_with_the_cli>`), or compared with the configurations
that irace (tutorial E15) and Evolver's own meta-optimizers find with the same budget. Because the
server evaluates every configuration as a training does, the values of the three are directly
comparable.

Step 6: running time
--------------------

The 2000 runs of NSGA-II took 190 ms each on average: **24 seconds** of computation on the 16 cores.
SMAC3 took 1486 seconds until the last trial, and its time grows with the trials:

.. list-table::
   :header-rows: 1
   :widths: 25 25 25 25

   * - Trials
     - Time (s)
     - Time with ``--retrain-after 50`` (s)
     - Time of its runs on 16 cores (s)
   * - 500
     - 81
     - 36
     - 9
   * - 1000
     - 347
     - 108
     - 13
   * - 1500
     - 822
     - 211
     - 19
   * - 2000
     - 1486
     - 353
     - 24

The time is SMAC3's, not the evaluations': its optimizer is sequential, and every few trials it
fits its random forest again on the whole history and searches it for the next configurations, so
each trial costs more than the previous one. With cheap runs, as here, that dominates everything;
with runs of minutes it would be a small part of the total. For comparison, irace took 663 seconds
for 10000 experiments of the same kind with the server (tutorial E15), and a training of Evolver of
the same budget runs in a few minutes.

``--retrain-after`` sets how many trials pass between two fits of the model (8 by default, SMAC3's
own default). With 50 the same budget took 353 seconds, four times less, because the model is fitted
six times less often; the configurations it proposes in between come from an older model. In this
run the front it found was not worse (best mean NHV 0.160 against 0.204), but one run of each is not
enough to tell how the option affects the quality of the result: that is a question to study with
replications (tutorial :doc:`E19 <independent_replications>`), not something to take from here.

This is a property of SMAC3, and Evolver does not try to change it. When SMAC3 is compared with other
configurators, report its wall-clock time with the results, and keep in mind that a comparison by
number of evaluations and a comparison by time can give different answers.

Try it yourself
---------------

- Change the size of the random initial design (``--initial-configurations 20`` or ``100``):
  how does it change the time and the front found with the same budget?
- Run SMAC3 three times with ``--retrain-after 8`` and three with ``50``, and compare their fronts.
- Tune MOEA/D instead: start the server with a base-level file of MOEA/D (with its
  ``weightVectorFilesDirectory``) and run the same client.

What's next
-----------

- :doc:`E15 <tuning_with_irace>` tunes the same algorithm with irace, with and without the server.
- :doc:`E19 <independent_replications>` explains how to run the replications of a study, also on a
  cluster.
- :doc:`../utilities/cli_tools` describes the server's API, for configurators other than irace and
  SMAC3.
