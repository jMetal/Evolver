.. _meta-optimization-level-metaheuristics:

Meta-Optimization-Level Metaheuristics
======================================

In Evolver, the meta-optimization process is performed by a meta-optimization metaheuristic, which is a multi-objective optimization algorithm that searches for the best configuration of a base-level metaheuristic by solving a meta-optimization problem.

Requirements for a Meta-Optimizer
---------------------------------

Evaluating a single solution of the meta-optimization problem requires one or more independent runs of the base-level metaheuristic on every problem of the training set, so a sequential meta-optimizer would lead to unfeasible running times. Evolver's meta-optimizers are therefore required to:

- **Evaluate whole populations in parallel.** Only generational algorithms qualify; steady-state ones such as MOEA/D or SMS-EMOA are excluded, since they produce a single solution per iteration.
- **Generate as many offspring as the population size.** The offspring population size always equals the meta population size, so that each generation can be evaluated in parallel as a whole. It is not configurable.
- **Return the final population.** Meta-level fronts usually hold very few solutions, so meta-optimizers never use an external archive.
- **Expose their population to observers**, which store the configurations and their indicator values at each generation (see :doc:`meta_optimization_approach`).

The default meta population size is 50.

Available Meta-Optimizers
-------------------------

.. list-table::
   :header-rows: 1
   :widths: 18 16 16 50

   * - Meta-optimizer
     - Flat encoding
     - Tree encoding
     - Notes
   * - NSGA-II
     - Yes
     - Yes
     - Built on Evolver's own configurable NSGA-II (``DoubleNSGAII`` or ``TreeNSGAII``), with its operators chosen from ``NSGAIIMetaDouble.yaml``/``NSGAIIMetaTree.yaml``
   * - AGE-MOEA
     - Yes
     - Yes
     - Built on Evolver's configurable AGE-MOEA (``DoubleAGEMOEA`` or ``TreeAGEMOEA``); same operators as NSGA-II plus its environmental selection variant
   * - SPEA2
     - Yes
     - No
     - ``MetaSPEA2Builder``: an RDEMOEA configured as SPEA2 with fixed operators; only the mutation probability factor is configurable
   * - SMPSO
     - Yes
     - No
     - ``MetaSMPSOBuilder``; requires a continuous (``DoubleProblem``) meta-problem
   * - Async NSGA-II
     - Yes
     - No
     - ``MetaAsyncNSGAIIBuilder``; asynchronous parallel evaluation (see below)
   * - Random Search
     - Yes
     - Yes
     - Samples random configurations; no population and no operators
   * - Async Genetic Algorithm
     - Yes
     - No
     - ``MetaAsyncGeneticAlgorithmBuilder``; available from Java, not yet from ``cli.training``

The flat encoding represents a configuration as a vector in [0,1]\ :sup:`n`; the tree encoding represents it as a derivation tree of the base-level parameter space grammar (see :doc:`solution_encoding`).

NSGA-II, AGE-MOEA, SPEA2 and SMPSO use a synchronous parallel scheme: all the solutions (or particles) of a generation are evaluated in parallel, so the algorithm behaves exactly as its sequential version. Scalability may be limited when the number of cores is high, because the rest of each generation runs sequentially. Async NSGA-II instead sends new solutions to workers as soon as one is free; its behavior differs from the sequential version, but it scales to a higher number of cores.

Building a Meta-Optimizer
-------------------------

The simplest way to run a meta-optimization is through ``cli.training``, where the meta-optimizer is chosen by name in a meta-optimizer configuration file (see :doc:`../utilities/cli_tools`). From Java, the ``Meta*Builder`` classes in ``org.uma.evolver.meta.builder`` create meta-optimizers with typical parameter settings. For example, class ``MetaNSGAIIBuilder`` can be used in this way:

.. code-block:: java

    int maxEvaluations = 2000;
    int numberOfCores = 8 ;

    EvolutionaryAlgorithm<DoubleSolution> nsgaii =
        new MetaNSGAIIBuilder(metaOptimizationProblem, parameterSpace)
            .setMaxEvaluations(maxEvaluations)
            .setNumberOfCores(numberOfCores)
            .build();

At this point, it should be remarked that finding the best parameter settings for the meta-optimizers is a task that deserves further research. Adding another level of meta-optimization, in which meta-level metaheuristics are treated as base-level metaheuristics, would introduce an additional layer of computational complexity that would require a vast amount of resources.
