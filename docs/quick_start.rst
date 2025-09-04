.. _getting_started:

Quick Start
===========

This guide will help you get started with Evolver by walking you through some simple examples.

The simplest way to use Evolver is to take a look to the examples included in the ``org.uma.evolver.algorithm.example`` package. This package contains two sub-packages:

- ``org.uma.evolver.algorithm.example.base``: Examples of base-level metaheuristics
- ``org.uma.evolver.algorithm.example.meta``: Examples of meta-optimization algorithms

Let us examine an example of each sub-package.

Base-level Metaheuristic Example: NSGA-II
-----------------------------------------
The `NSGAIIZDT4Example <https://github.com/jMetal/Evolver/blob/main/src/main/java/org/uma/evolver/example/base/NSGAIIZDT4Example.java>`_ class shows how to use the base-level NSGA-II algorithm to solve the ZDT4 problem. The contents of this class are as follows:

.. literalinclude:: ../src/main/java/org/uma/evolver/example/base/NSGAIIZDT4Example.java
   :language: java
   :linenos:
   :caption: NSGAIIZDT4Example.java
   :name: nsgaii-zdt4

If you run the class without arguments, the included parameter settings will be used. This configuration corresponds to a standard NSGA-II configuration commonly used in the literature. The resulting solutions and their corresponding function values will be stored in the ``VAR.csv`` and ``FUN.csv`` files in the current directory, respectively.

Alternatively, we can pass the configuration string in the command line. We have first to create a .jar file using Maven:

.. code-block:: bash

    mvn -DskipTests=true clean package

As a result, a file called ``Evolver-2.0.jar-with-dependencies.jar`` will be created in the ``target`` folder. We can run now the program using the following command:

.. code-block:: bash

    java -cp target/Evolver-2.0-SNAPSHOT.jar-with-dependencies.jar org.uma.evolver.example.base.NSGAIIZDT4Example --algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2


Meta-Optimization Example: NSGA-II Optimizing NSGA-II
-----------------------------------------------------
To illustrate a simple example of meta-optimization, let us consider the `NSGAIIOptimizingNSGAIIForProblemZDT4 <https://github.com/jMetal/Evolver/blob/main/src/main/java/org/uma/evolver/example/meta/NSGAIIOptimizingNSGAIIForProblemZDT4.java>`_ class. This class uses NSGA-II to optimize the parameters of NSGA-II for solving a training set composed only of the ZDT4 problem. We include the class next:

.. literalinclude:: ../src/main/java/org/uma/evolver/example/meta/NSGAIIOptimizingNSGAIIForProblemZDT4.java
   :language: java
   :linenos:
   :caption: NSGAIIOptimizingNSGAIIForProblemZDT4.java
   :name: nsgaii-optimizing-nsgaii

   