.. image:: https://readthedocs.org/projects/Evolver/badge/?version=latest
   :alt: Documentation Status
   :target: https://Evolver.readthedocs.io/?badge=latest


Evolver: Automated Metaheuristic Configuration Framework
========================================================

DISCLAIMER: we are developing a new version of Evolver (version 2.0). The package is
ready to be used but we are still working on the documentation.

Overview
--------
Evolver is a software tool designed for the automatic configuration of multi-objective metaheuristics. 
Its core approach is meta-optimization, where the process of tuning the parameters of a base-level metaheuristic is framed as a multi-objective problem which
is solvable by a multi-objective optimizer (i.e., the meta-optimization algorithm). In this problem, the variable encoding represents a particular configuration 
of the base-level algorithm and 
evaluating a solution involves a run of the metaheuristic under that configuration; the resulting solution front is evaluated against a combination of 
quality indicators, which are the objective functions of the resulting multi-objective problem.

The next stable version will be 2.0 (https://github.com/jMetal/Evolver).

Evolver 2.0 is a full re-implementation of the original Evolver framework, which is described in the following paper: `Evolver: Meta-optimizing multi-objective metaheuristics <https://doi.org/10.1016/j.softx.2023.101551>`_.

The development of Evolver 2.0 was motivated by two key objectives: enhancing the original framework's capabilities and serving as a case study in AI-assisted software development. Throughout this project, we've extensively utilized generative AI tools including Windsurf, ChatGPT, and Claude to support various aspects of the development process.

Approach    
^^^^^^^^
Evolver follows a two-level optimization approach:

.. code-block:: none

    +---------------------------------------------+
    |        Meta-optimization Level              |
    |  +---------------------------------------+  |
    |  | Meta-optimization Algorithm           |  |
    |  | (e.g., NSGA-II, MOEA/D)               |  |
    |  +------------------+--------------------+  |
    |                     |                       |
    |  +------------------v--------------------+  |
    |  | Meta-optimization Problem             |  |
    |  | - Evaluates base-level configurations |  |
    |  | - Uses quality indicators as          |  |
    |  |   optimization objectives             |  |
    |  +------------------+--------------------+  |
    |                     |                       |
    +---------------------|-----------------------+
                          |
    +---------------------|-----------------------+
    |  Base-level         v                       |
    |  +------------------+--------------------+  |
    |  | Base-level Metaheuristic              |  |
    |  | - Parameter space                     |  |
    |  | - Solves base-level problems          |  |
    |  +------------------+--------------------+  |
    |                     |                       |
    |  +------------------v--------------------+  |
    |  | Base-level Problems                   |  |
    |  | (Training instances)                  |  |
    |  +---------------------------------------+  |
    |                                             |
    +---------------------------------------------+

The components of the approach are:

1. **Base-level Components**

   * A set of **Base-level Problems** serving as training instances
   * A **Base-level Multi-objective Metaheuristic** to be configured
   * A parameter space defining the algorithm's configurable parameters and their constraints

2. **Meta-level Components**

   * A **Meta-optimization Problem** that evaluates base-level configurations
   * Quality indicators (e.g., Epsilon, normalized Hypervolume, etc.) as optimization objectives
   * A **Meta-optimization Multi-objective Metaheuristic** that searches for optimal configurations of a meta-optimization problem


The flow is as follows:

1. Given a set of base-level problems, the meta-optimization algorithm generates configurations for a base-level metaheuristic
2. Each configuration is evaluated in the meta-optimization problem using quality indicators
3. The process repeats until stopping criteria are met


Key Features
^^^^^^^^^^^^
- **Automated Configuration**: Automatically finds accurate parameter settings for metaheuristics
- **Flexible Architecture**: Supports various metaheuristics at both meta and base levels
- **Multi-objective Optimization at the meta level**: Optimizes multiple performance criteria (quality indicators) simultaneously
- **Extensible Design**: Allows the integration of new algorithms, problems, and quality indicators
- **YAML Parameter Space Definition**: The parameter space of base-level metaheuristics can be defined in a YAML file

Other Features
^^^^^^^^^^^^
- **irace Support**: The search of base-level metaheuristic configurations can be performed with irace.


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
Let us suppose that we want to optimize the parameters of the NSGA-II algorithm for solving the DTLZ1 problem. 
First, we load the parameter space of the NSGA-II algorithm from the YAML file named `NSGAIIDouble.yaml<https://github.com/jMetal/Evolver/blob/main/src/main/resources/parameterSpaces/NSGAIIDouble.yaml>`_ located in the resources
folder of the project.

The following example demonstrates how to use Evolver to optimize the parameters of the NSGA-II algorithm for solving the ZDT4 problem:

.. code-block:: java

   // 1. Define the YAML parameter space file and target problem
   String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
   List<Problem<DoubleSolution>> trainingSet = List.of(new DTLZ1());
   List<String> referenceFrontFileNames = List.of("resources/referenceFronts/DTLZ1.3D.csv");

   // 2. Set up the algorithm to be configured
   var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
   var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
   var configurableAlgorithm = new DoubleNSGAII(100, parameterSpace);

   var maximumNumberOfEvaluations = List.of(15000);
   int numberOfIndependentRuns = 1;
   EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

   // 3. Create the meta-optimization problem
   MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
       new MetaOptimizationProblem<>(
           configurableAlgorithm,
           trainingSet,
           referenceFrontFileNames,
           indicators,
           evaluationBudgetStrategy,
           numberOfIndependentRuns);

   // 4. Configure and run the meta-optimizer
   int maxEvaluations = 2000;
   int numberOfCores = 8;

   EvolutionaryAlgorithm<DoubleSolution> nsgaii = 
       new MetaNSGAIIBuilder(metaOptimizationProblem)
           .setMaxEvaluations(maxEvaluations)
           .setNumberOfCores(numberOfCores)
           .build();

   nsgaii.run();

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

v2.0 (2025-08-19)
^^^^^^^^^^^^^^^^^
* Complete rewrite of the original Evolver framework
* New architecture for improved flexibility and maintainability
* Enhanced support for meta-optimization of multi-objective metaheuristics
* Improved documentation and examples
* The Docker images are not available for this version
* The GUI-based dashboard has been removed

License
-------
This project is licensed under the GNU General Public License - see the `LICENSE <LICENSE>`_ file for details.
