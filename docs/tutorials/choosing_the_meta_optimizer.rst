.. _tutorial_choosing_the_meta_optimizer:

E12. Choosing the Meta-Optimizer
================================

:Level: Advanced
:Version: 1.0 (2026-10-06)
:Time: about 25 minutes of reading; the exercises at the end take a few minutes each
:Prerequisites: :doc:`E3. Meta-optimization workflow <meta_optimization_workflow>`,
   :doc:`E17. Budgets: evaluations or time <budgets>`

In the tutorials so far the meta-optimizer was a given: NSGA-II in E3, the asynchronous NSGA-II in
E7 to E9. Evolver has six, and the choice matters in two practical ways: how well they use the cores
of your machine, and how soon after the limit you gave them they stop. This tutorial explains what
the meta-optimizers have in common (and why), what sets them apart, and how to choose one. It has no
experiment of its own: it compares nothing between meta-optimizers, because Evolver has no
systematic study that does, and what is measured in a single training is too noisy to rank them
(see *What to expect from the choice* below). The exercises let you measure what matters for *your*
machine and problem.

What every meta-optimizer has in common
---------------------------------------

Evaluating one configuration means running the base-level algorithm with it, several times, on every
problem of the training set. That is why the meta-optimizers of Evolver are required to:

- **Evaluate whole populations in parallel.** Only generational algorithms qualify. Steady-state
  ones (MOEA/D, SMS-EMOA) produce one solution per iteration, which would leave all the cores but
  one idle. RVEA, designed for many objectives, is a poor fit for a meta-problem that usually has
  two.
- **Generate as many offspring as the population has.** The offspring population size is always the
  meta population size, and it is not configurable, so that each generation is evaluated in
  parallel as a whole. The default population is 50 (``metaPopulationSize``).
- **Return the final population, with no external archive.** The fronts of the meta level hold very
  few solutions, so an archive would add nothing.
- **Expose their population to observers**, which write the configurations and their indicator
  values (the files of tutorial E8).

These rules are fixed by the builders and cannot be changed from a configuration file.

The six meta-optimizers
-----------------------

The name is the value of ``algorithm`` in the meta-optimizer configuration file of ``cli.training``
(the files in ``src/main/resources/metaOptimizerConfigurations/``).

.. list-table::
   :header-rows: 1
   :widths: 16 9 9 36 30

   * - ``algorithm``
     - Flat
     - Tree
     - Operators you can set
     - Parallel evaluation
   * - ``NSGA-II``
     - yes
     - yes
     - Crossover, mutation, selection (flat: ``NSGAIIMetaDouble.yaml``; tree:
       ``NSGAIIMetaTree.yaml``)
     - Synchronous: the generation is evaluated in parallel, then the algorithm goes on
   * - ``AGE-MOEA``
     - yes
     - yes
     - The same as NSGA-II, plus its environmental selection variant (``agemoeaVariant``)
     - Synchronous
   * - ``SPEA2``
     - yes
     - no
     - Only ``mutationProbabilityFactor``: the rest (SBX, polynomial mutation, strength ranking,
       k-nearest-neighbours density, tournament) is fixed
     - Synchronous
   * - ``SMPSO``
     - yes
     - no
     - None: the swarm, with its perturbation and velocity update, is built by jMetal's
       ``SMPSOBuilder``
     - Synchronous
   * - ``AsyncNSGA-II``
     - yes
     - yes
     - Crossover and mutation (``AsyncNSGAIIMetaDouble.yaml``)
     - Asynchronous: a core that finishes gets a new configuration at once
   * - ``RandomSearch``
     - yes
     - yes
     - None: it samples the space uniformly
     - Batches of ``numberOfCores`` evaluations

Three details of the table:

- **SMPSO and SPEA2 are flat-only.** SMPSO needs a continuous (``DoubleProblem``) meta-problem, and
  the tree encoding is not one; SPEA2 is built with ``DoubleSolution`` operators. The other four
  work with both encodings (tutorial E13, planned, will explain the tree encoding).
- **The asynchronous genetic algorithm** (``MetaAsyncGeneticAlgorithmBuilder``) exists, but only
  from Java: ``cli.training`` does not accept it.
- **Which operators.** The flat NSGA-II and AGE-MOEA are built on Evolver's own configurable
  algorithms, so their operators come from a parameter space, like those of any base-level
  algorithm. What the configuration file may contain is checked against it (``DescribeMain`` lists
  the fields, see :doc:`../utilities/cli_tools`).

Parallel evaluation and the number of cores
-------------------------------------------

``numberOfCores`` is the number of configurations evaluated at the same time. Each evaluation
occupies one core for as long as the base-level runs it contains take, so the training scales
almost linearly until the cores run out. Tutorials E3 and E17 use 14 cores of an 18-core machine,
E7 to E9 use 16: set it to what your machine has, leaving a core or two for the system and for
anything else you are running (Evolver-Studio, the browser).

The synchronous meta-optimizers (NSGA-II, AGE-MOEA, SPEA2, SMPSO) behave exactly as their
sequential versions would: the generation is evaluated as a whole, in waves of ``numberOfCores``
configurations, and only when the last one finishes does the algorithm select and reproduce. This
has two consequences:

- **Cores can idle at the end of each wave.** The last wave of a generation is partly empty unless
  the population is a multiple of the cores. With the default population of 50:

  .. list-table::
     :header-rows: 1
     :widths: 20 25 55

     * - Cores
       - Waves per generation
       - Slots used, at best
     * - 10
       - 5
       - 50 of 50 (100 %)
     * - 14
       - 4
       - 50 of 56 (89 %)
     * - 16
       - 4
       - 50 of 64 (78 %)

  The figures are the best case: they assume that all the evaluations of a wave last the same.
  Choosing ``metaPopulationSize`` as a multiple of ``numberOfCores`` (for example 48 with 16 cores,
  or 56 with 14) removes this loss.
- **Each wave lasts as long as its slowest evaluation.** The cost of a configuration varies a lot:
  one that makes the base-level algorithm use a costly density estimator or a large population takes
  many times longer than a cheap one. The cores that finished early wait.

``AsyncNSGA-II`` removes both. It keeps every core busy: when a configuration is evaluated, the
result is used at once to produce a new one for that core, without waiting for the rest of a
generation. Its behavior therefore differs from the sequential NSGA-II, which has generations, and it is the choice that
pays off when the evaluation times are very uneven or the number of cores is high; E7 uses it for
that reason. On a few cores with similar evaluation times, the loss of the synchronous ones is
small.

``RandomSearch`` has no population: it samples configurations in batches of ``numberOfCores``.

When the meta-optimizer stops
-----------------------------

Tutorial E17 explains the two stopping conditions, ``metaMaxEvaluations`` and
``metaMaxComputingTimeMinutes``. What differs between meta-optimizers is *when* the time limit is
checked, and so how much the real time of the run can exceed the limit:

.. list-table::
   :header-rows: 1
   :widths: 25 40 35

   * - Meta-optimizer
     - The limit is checked
     - The run exceeds the limit by up to
   * - NSGA-II, AGE-MOEA, SPEA2, SMPSO
     - At the beginning of each generation (iteration); the one in progress is completed
     - The time of one generation: its waves, each as long as the slowest evaluation in it
   * - ``AsyncNSGA-II``
     - After every evaluation, once the initial population has been evaluated; the evaluations in
       progress when the limit is met are discarded
     - The time of one evaluation
   * - ``RandomSearch``
     - Before each batch of ``numberOfCores`` evaluations; the batch in progress is completed
     - The time of one batch

In all of them the initial population is always evaluated, even if that takes longer than the limit.

With a time limit, two runs of different meta-optimizers are compared fairly only if you also look
at how many meta-evaluations each one managed in that time. ``METADATA.txt`` records it, together
with the stopping condition and the real time (tutorial E17). A synchronous meta-optimizer that
loses 22 % of its slots (16 cores, population 50) simply performs fewer evaluations in the same
minutes.

When to use each
----------------

There is no best meta-optimizer in general, and no study to cite that ranks them: the meta level of
Evolver is itself a metaheuristic whose parameters have not been tuned (that would be a third level
of meta-optimization). What follows are reasons for a choice, not results.

.. list-table::
   :header-rows: 1
   :widths: 25 75

   * - Meta-optimizer
     - Reason to use it
   * - ``NSGA-II``
     - The default and the best known. Equivalent to its sequential version, with operators you can
       change. A good first choice, and the one the reference tutorials (E3, E17) use
   * - ``AsyncNSGA-II``
     - Many cores, or evaluation times that differ a lot between configurations (a parameter space
       that includes both cheap and costly components): no core waits for the others. The choice of
       E7 to E9
   * - ``AGE-MOEA``
     - An alternative with a different environmental selection (based on the geometry of the front),
       to try when NSGA-II stalls, or to check that a result does not depend on the meta-optimizer.
      
   * - ``SPEA2``, ``SMPSO``
     - Other families (strength ranking, swarm), with fixed operators, to compare against NSGA-II
       with the flat encoding. They offer less to tune and neither supports the tree encoding
   * - ``RandomSearch``
     - **A baseline, and worth running.** If a meta-optimizer does not beat random search with the
       same budget, the budget is too small for it to learn anything, or the parameter space is so
       forgiving that any configuration is good. Either is useful to know

For the tree encoding (E13) the choice is among NSGA-II, AGE-MOEA, ``AsyncNSGA-II`` and
``RandomSearch``.

What to expect from the choice
------------------------------

Whatever the meta-optimizer, a training is a stochastic search: two runs of the same one give
different configurations (tutorial E3 discusses how many trainings to run), and the differences between meta-optimizers are usually
smaller than that variation unless the budget is tiny. A claim such as "AGE-MOEA finds better
configurations than NSGA-II" needs several training runs of each, compared with the statistical
tests of E9, at the same time budget; one run of each shows nothing. The practical differences you
can count on are the ones above: cores used well, how far past the limit the run goes, and which
encodings and operators are available.

Try it yourself
---------------

Each exercise copies a bundled request and changes only the meta-optimizer. Start from tutorial
E17's training (``tutorial-e17-request.yaml`` and the meta-optimizer configuration it names,
stopped by time) and set ``numberOfCores`` to your machine's value.

- **Equal time, different meta-optimizers.** Run ``NSGA-II`` and ``AsyncNSGA-II``
  (``MetaAsyncNSGAIIFlatConfiguration.yaml`` with ``metaMaxComputingTimeMinutes: 2``) on the same
  base level. In ``METADATA.txt``, how many meta-evaluations did each perform, and how much did the
  real time exceed 2 minutes?
- **Wasted slots.** Run NSGA-II with ``metaPopulationSize: 50`` and again with a multiple of your
  ``numberOfCores``, with the same time limit. Does the second perform more evaluations?
- **A baseline.** Run ``RandomSearch`` (``MetaRandomSearchFlatConfiguration.yaml``) for the same
  time and compare its final front, in the space of the two indicators, with NSGA-II's (the plots of
  E8). How much better is the search than sampling?
- **Fixed operators.** Run ``SPEA2`` and ``SMPSO`` (``MetaSPEA2FlatConfiguration.yaml``,
  ``MetaSMPSOFlatConfiguration.yaml``) and read their configuration files: what can you set?
- **A tiny limit.** With ``metaMaxComputingTimeMinutes: 0.001``, which meta-optimizer finishes the
  soonest, and why? (Think about what each one evaluates before it checks the limit.)

What's next
-----------

- :doc:`E17 <budgets>` explains the two stopping conditions and what the output files record about
  them.
- :doc:`E8 <analyzing_training_results>` explains the files and plots with which to compare the
  results of two meta-optimizers.
- :doc:`../concepts/meta_optimization_level_metaheuristics` is the reference page on the
  requirements and the available meta-optimizers.
- :doc:`../utilities/cli_tools` describes the fields of the meta-optimizer configuration files.
