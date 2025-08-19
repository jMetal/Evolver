.. _user_guide:

User Guide
=========

This guide provides detailed information about using Evolver.

.. toctree::
   :maxdepth: 2
   :caption: Contents:

   configuration
   algorithms
   problems
   visualization
   advanced

Configuration
-------------

Evolver uses YAML files for configuration. Here's an example configuration for NSGA-II:

.. code-block:: yaml

   algorithm:
     name: NSGAII
     parameters:
       populationSize: 100
       maxEvaluations: 25000
       crossover: SBX
       crossoverProbability: 0.9
       crossoverRepair: true
       crossoverDistributionIndex: 20.0
       mutation: polynomial
       mutationProbability: 0.1
       mutationRepair: true
       mutationDistributionIndex: 20.0

Algorithms
----------

Evolver supports various optimization algorithms:

- NSGA-II
- MOEA/D
- SPEA2
- And more...

Problems
--------

Evolver includes several benchmark problems:

- ZDT
- DTLZ
- WFG
- Custom problems

Visualization
-------------

Evolver provides tools for visualizing optimization results:

- Pareto front visualization
- Convergence plots
- Solution analysis

Advanced Topics
--------------

- Custom operators
- Parallel execution
- Hybrid algorithms
- Constraint handling
