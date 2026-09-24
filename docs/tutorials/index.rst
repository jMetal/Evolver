.. _tutorials_index:

Tutorials
=========

Step-by-step tutorials with runnable code. Each one comes with a class in the
``org.uma.evolver.example.tutorial`` package, so you can run it and experiment with it. They are
grouped in three levels:

- **Introductory**: the basic concepts, needed for everything else.
- **Intermediate**: designing experiments, analyzing and validating their results.
- **Advanced**: meta-optimizers, encodings, alternative tuners and extending Evolver.

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
   * - E3. Meta-optimization workflow *(coming soon)*
     - Base-level algorithm, meta-optimizer, training problems and results: a complete training run.

More tutorials are planned for the intermediate and advanced levels.

.. toctree::
   :hidden:

   parameter_spaces
   base_level_algorithms
