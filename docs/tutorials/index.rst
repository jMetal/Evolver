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
   * - :doc:`E7. Training sets, indicators and budgets <training_sets_indicators_budgets>`
     - Designing a training run (training set, meta-objectives, budgets, independent runs) and
       validating its result against other algorithms, on problems seen and not seen during the
       training.
   * - :doc:`E17. Budgets: evaluations or time <budgets>`
     - The two budgets of a training: the evaluations of each base-level run, and the stopping
       condition of the meta-optimizer, by number of configurations or by computing time; when to
       use each and what they change in the results.
   * - :doc:`E8. Analyzing training results <analyzing_training_results>`
     - Reading the output files of a training run, its convergence and the population of the
       meta-optimizer, choosing a configuration from the final front, and validating the choice.
   * - :doc:`E9. Validating a configuration <validating_a_configuration>`
     - Designing a validation study and analyzing it: medians and IQRs, boxplots, the Wilcoxon test,
       effect sizes, the Friedman test with Holm's procedure, critical difference plots and a
       Bayesian test, repeating with Evolver the first study of automatic configuration with jMetal.

.. toctree::
   :hidden:

   training_sets_indicators_budgets
   budgets
   analyzing_training_results
   validating_a_configuration

Advanced
--------

.. list-table::
   :header-rows: 1
   :widths: 30 70

   * - Tutorial
     - What you will learn
   * - :doc:`E14. Tuning with irace <tuning_with_irace>`
     - Tuning an Evolver algorithm with irace: generating the parameter file from a YAML space, the
       target runner, the scenario, running irace, and applying the configuration it finds.

More tutorials are planned for the intermediate and advanced levels.

.. toctree::
   :hidden:

   tuning_with_irace
