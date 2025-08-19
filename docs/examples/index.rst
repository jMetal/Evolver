.. _examples:

Examples
========

This section contains example code and tutorials for using Evolver.

.. toctree::
   :maxdepth: 2
   :caption: Contents:

   quickstart
   configuration
   custom_problem
   custom_algorithm

Quick Start
-----------

A simple example to get started with Evolver:

.. code-block:: java

   // Create a simple problem
   var problem = new MyProblem();
   
   // Configure the algorithm
   var configuration = """
   algorithm: {
     name: NSGAII
     parameters: {
       populationSize: 100
       maxEvaluations: 10000
     }
   }
   """;
   
   // Create and run the algorithm
   var algorithm = new ConfigurableNSGAII(problem, configuration);
   algorithm.run();
   
   // Get results
   var population = algorithm.getResult();

Configuration Examples
---------------------

### NSGA-II Configuration
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

### MOEA/D Configuration
.. code-block:: yaml

   algorithm:
     name: MOEAD
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
       tNeighborhood: 20
       neighborhoodSelectionProbability: 0.9

Creating a Custom Problem
------------------------
See :ref:`getting_started` for an example of creating a custom optimization problem.

Creating a Custom Algorithm
--------------------------
To create a custom algorithm, extend the `ConfigurableAlgorithm` class and implement the required methods.

More Examples
-------------
For more examples, please see the `examples <https://github.com/jMetal/Evolver/tree/main/examples>`_ directory in the repository.
