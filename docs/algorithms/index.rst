.. _algorithm_guides:

Algorithm guides
================

One guide per base-level algorithm of Evolver: where it comes from, the variants Evolver offers,
its parameter space and default configurations, how to run it, and, backed by a reproducible
experiment, **where it works well and where it works poorly**. The tutorials teach how to use
Evolver; these guides teach the algorithms.

Every guide follows the same outline:

#. **Idea and origin**: the paper and the central mechanism.
#. **Variants in Evolver**: which parameter selects them.
#. **Parameter space**: what is specific to the algorithm, and the number of parameters.
#. **Default configurations**: the files of ``defaultConfigurations``.
#. **Running it**: from Java (the guide's class) and from the command line.
#. **Where it works well** and 7. **where it works poorly**: an experiment with several runs, with
   quality indicators, statistical tests and fronts, against a reference algorithm.
#. **Tuning notes**: which parameters matter most.
#. **References**.

The code of each guide is a class in package ``org.uma.evolver.example.algorithms``, checked by an
integration test.

.. list-table::
   :header-rows: 1
   :widths: 20 30 50

   * - Algorithm
     - Encodings
     - Guide
   * - NSGA-II
     - Double, Binary, Permutation
     - planned
   * - NSGA-III
     - Double
     - planned
   * - MOEA/D
     - Double, Binary, Permutation
     - planned
   * - SMS-EMOA
     - Double, Binary, Permutation
     - planned
   * - MOPSO
     - Double
     - planned
   * - RDEMOEA
     - Double, Permutation
     - planned
   * - RVEA
     - Double
     - :doc:`RVEA, RVEA* and iRVEA <rvea>`
   * - AGE-MOEA
     - Double
     - planned
   * - SSMOEA
     - Double
     - planned; see :ref:`ssmoea` meanwhile
   * - PAES
     - Double, Binary, Permutation
     - planned; see :ref:`paes` meanwhile

.. toctree::
   :hidden:

   rvea
