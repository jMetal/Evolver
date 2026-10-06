.. _tutorials_index:

Tutorials
=========

Step-by-step tutorials with runnable code. Most come with a class in the
``org.uma.evolver.example.tutorial`` package, so you can run it and experiment with it; the quick
start (E4) only uses the command line. They are grouped in three levels:

- **Introductory**: the basic concepts, needed for everything else.
- **Intermediate**: designing experiments, analyzing and validating their results.
- **Advanced**: meta-optimizers, encodings, alternative tuners and extending Evolver.

The algorithms themselves (what each one does, and where it works well or poorly) are described in
the :ref:`algorithm guides <algorithm_guides>`.

Introductory
------------

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Tutorial
     - What you will learn
   * - :doc:`E1. Parameter spaces <parameter_spaces>`
     - What a parameter space is, the types of parameters and their relations, and how a
       configuration is a point of the space (NSGA-II for continuous and binary problems).
   * - :doc:`E2. Base-level algorithms <base_level_algorithms>`
     - Configuring and running Evolver's algorithms from a parameter space, reading their results,
       and running them on other problems and encodings: Evolver as an alternative to jMetal.
   * - :doc:`E3. Meta-optimization workflow <meta_optimization_workflow>`
     - Base-level algorithm, meta-optimizer, training problem and quality indicators: a complete
       training run, from Java and from the command line, and choosing the configuration it finds.
   * - :doc:`E4. Evolver in 10 minutes <../quick_start>`
     - Build Evolver, run a configurable algorithm, tune it, and run it with the configuration
       found, from the command line.

.. toctree::
   :hidden:

   parameter_spaces
   base_level_algorithms
   meta_optimization_workflow
   ../quick_start

Intermediate
------------

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Tutorial
     - What you will learn
   * - :doc:`E6. Designing your own parameter space <designing_parameter_spaces>`
     - Reducing and extending a YAML parameter space, the limits that the algorithms set, measuring
       the size of a space, and comparing two spaces by validating the configurations they produce
       (the space of the 2019 irace study against the full one).
   * - :doc:`E7. Training sets, indicators and budgets <training_sets_indicators_budgets>`
     - Designing a training run (training set, meta-objectives, budgets, independent runs) and
       validating its result against other algorithms, on problems seen and not seen during the
       training.
   * - :doc:`E8. Analyzing training results <analyzing_training_results>`
     - Reading the output files of a training run, its convergence and the population of the
       meta-optimizer, choosing a configuration from the final front, and validating the choice.
   * - :doc:`E9. Validating a configuration <validating_a_configuration>`
     - Designing a validation study and analyzing it: medians and IQRs, boxplots, the Wilcoxon test,
       effect sizes, the Friedman test with Holm's procedure, critical difference plots and a
       Bayesian test, repeating with Evolver the first study of automatic configuration with jMetal.
   * - :doc:`E10. Problems without a reference front <problems_without_reference_front>`
     - What each indicator needs when a problem has no reference front, estimating extreme points
       for the hypervolume and what goes wrong with bad ones, training with HV− and EP, and
       validating with a reference front built from the study (the bi-objective TSP).
   * - :doc:`E11. Binary and permutation encodings <binary_and_permutation_encodings>`
     - Tuning NSGA-II for a binary problem (ZDT5) and a permutation one (the bi-objective TSP): the
       parameter spaces and operators of each encoding, and validating without a known front.
   * - :doc:`E17. Budgets: evaluations or time <budgets>`
     - The two budgets of a training: the evaluations of each base-level run, and the stopping
       condition of the meta-optimizer, by number of configurations or by computing time; when to
       use each and what they change in the results.

.. toctree::
   :hidden:

   designing_parameter_spaces
   training_sets_indicators_budgets
   analyzing_training_results
   validating_a_configuration
   problems_without_reference_front
   binary_and_permutation_encodings
   budgets

Advanced
--------

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Tutorial
     - What you will learn
   * - :doc:`E12. Choosing the meta-optimizer <choosing_the_meta_optimizer>`
     - What the six meta-optimizers have in common and what sets them apart: operators, flat and tree
       encodings, parallel evaluation and the number of cores, when each one checks the time limit,
       and reasons to choose one.
   * - :doc:`E14. Tuning with irace <tuning_with_irace>`
     - Tuning an Evolver algorithm with irace: generating the parameter file from a YAML space, the
       target runner, the scenario, running irace, and applying the configuration it finds.
   * - :doc:`E15. Automating Evolver with the CLI <automating_with_the_cli>`
     - The request, status and results files of ``cli.training`` and ``cli.solving``: asking
       ``DescribeMain`` what Evolver can run, writing and running requests by hand, what a failed or
       killed run leaves behind, and a batch of requests run in parallel and gathered in one table.

More tutorials are planned for the intermediate and advanced levels.

.. toctree::
   :hidden:

   choosing_the_meta_optimizer
   tuning_with_irace
   automating_with_the_cli
