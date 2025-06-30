Evolver: A Meta-Optimization Framework for Multi-Objective Metaheuristics
========================================================================

Overview
--------
Evolver is a framework that formulates the automatic configuration and design of multi-objective metaheuristics as a multi-objective optimization problem. It employs a meta-optimization approach where one metaheuristic is used to optimize the parameters of another metaheuristic.

The current version of the tool is Evolver 2.0 beta, which is a complete rewrite of the original Evolver framework.

Evolver implements a two-level optimization architecture, depicted in :ref:`fig-meta-optimization-approach`:

.. _fig-meta-optimization-approach:

.. figure:: docs/images/metaOptimizationApproach.png
   :alt: Meta-optimization approach of Evolver
   :align: center
   :width: 600px

1. **Base-level Components**

   * A set of **Base-level Problems** serving as training instances
   * A **Base-level Multi-objective Metaheuristic** to be configured
   * A parameter space defining the algorithm's configurable parameters and their constraints

2. **Meta-level Components**

   * A **Meta-optimization Problem** that evaluates base-level configurations
   * Quality indicators (e.g., Epsilon, Normalized Hypervolume) as optimization objectives
   * A **Meta-optimization Multi-objective Metaheuristic** that searches for optimal configurations of a meta-optimization problem

This hierarchical approach enables the automatic discovery of high-performance parameter settings tailored to specific problem domains and performance criteria.

Key Features
------------
- **Flexible Meta-optimization**: Supports various metaheuristics for both meta and base levels
- **Multiple Problem Domains**: Works with continuous, combinatorial, and mixed-variable optimization problems
- **Extensible Architecture**: Easy to add new algorithms, problems, and performance metrics

Installation
------------
1. **Prerequisites**:
   - Java JDK 17 or higher
   - Maven 3.6 or higher

2. **Build from source**:
   .. code-block:: bash

      git clone https://github.com/jMetal/Evolver.git
      cd Evolver
      mvn clean install

Quick Start
-----------
The following example demonstrates how to use Evolver to optimize the parameters of an RDEMOEA algorithm for solving the DTLZ3 problem:

.. code-block:: java

   // 1. Define the target problem
   List<Problem<DoubleSolution>> trainingSet = List.of(new DTLZ3());
   List<String> referenceFrontFileNames = List.of("resources/referenceFronts/DTLZ3.3D.csv");

   // 2. Set up the algorithm to be configured
   var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
   var baseAlgorithm = new NSGAIIDouble(100);
   var maximumNumberOfEvaluations = List.of(10000);

   // 3. Create the meta-optimization problem
    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            baseAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            maximumNumberOfEvaluations,
            numberOfIndependentRuns);

   // 4. Configure and run the meta-optimizer
int maxEvaluations = 2000;
    int numberOfCores = 8;

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = 
        new MetaNSGAIIBuilder(metaOptimizationProblem)
            .setMaxEvaluations(maxEvaluations)
            .setNumberOfCores(numberOfCores)
            .build();

   moea.run();

Documentation
-------------
Detailed documentation is available in the `docs` directory, including:
- User Guide
- Developer Documentation
- API Reference
- Tutorials and Examples

Citing Evolver
--------------
If you use Evolver in your research, please cite:

.. code::

   @article{AND23,
    title = {Evolver: Meta-optimizing multi-objective metaheuristics},
    journal = {SoftwareX},
    volume = {23},
    pages = {101551},
    year = {2024},
    issn = {2352-7110},
   }

Changelog
---------

v2.0 beta (2024-06-30)
^^^^^^^^^^^^^^^^^^^^^
* Complete rewrite of the original Evolver framework
* New architecture for improved flexibility and maintainability
* Enhanced support for meta-optimization of multi-objective metaheuristics
* Improved documentation and examples
* The Docker images are not available for this version
* The GUI-based dashboard has been removed

License
-------
This project is licensed under the GNU General Public License - see the `LICENSE <LICENSE>`_ file for details.
