.. image:: https://readthedocs.org/projects/Evolver/badge/?version=latest
   :alt: Documentation Status
   :target: https://Evolver.readthedocs.io/?badge=latest


Evolver: Automated Metaheuristic Configuration Framework
========================================================

Overview
--------
Evolver is a Java-based software tool designed for the automatic configuration of multi-objective metaheuristics. 
Its core approach is meta-optimization, where the process of tuning the parameters of a base-level metaheuristic for a set of problems is framed as a multi-objective problem which
is solvable by a multi-objective optimizer (i.e., the meta-optimization algorithm). In this problem, the variable encoding represents a particular configuration 
of the base-level algorithm and 
evaluating a solution involves a run of the metaheuristic under that configuration; the resulting solution front is evaluated against a combination of 
quality indicators, which are the objective functions of the resulting multi-objective problem. 
Evolver relies on the `jMetal framework <https://github.com/jMetal/jMetal>`_ for the optimization problems, algorithms, and quality indicators.

The next stable version is 2.0, which is a full re-implementation of the original Evolver framework described in the following paper: `Evolver: Meta-optimizing multi-objective metaheuristics <https://doi.org/10.1016/j.softx.2023.101551>`_.

The development of Evolver 2.0 was motivated by two key objectives: enhancing the original framework's capabilities and serving as a case study in AI-assisted software development. Throughout this project, we've extensively utilized generative AI tools including Windsurf, ChatGPT, and Claude to support various aspects of the development process.

Architecture    
^^^^^^^^^^^^
Evolver follows a two-level optimization architecture:

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

The components of the architecture are:

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
- **Flexible Architecture**: Supports various metaheuristics at both meta and base levels and several encodings (double, binary, permutation, etc.)
- **Multi-objective Optimization at the meta level**: Optimizes multiple performance criteria (quality indicators) simultaneously
- **Extensible Design**: Allows the integration of new algorithms, problems, and quality indicators
- **YAML Parameter Space Definition**: The parameter space of base-level metaheuristics can be defined in a YAML file

Other Features
^^^^^^^^^^^^
- **irace Support**: The search of base-level metaheuristic configurations can be performed with irace.

Available algorithms
--------------------
Evolver currently supports the following base-level and meta-optimization algorithms:

- Base-optimization algorithms:

  - NSGA-II (double, binary, permutation encodings)
  - MOEA/D (double, binary, permutation encodings)
  - SMS/EMOA (double encoding)
  - MOPSO – multi-objective particle swarm optimization (double encoding)
  - RDEMOEA – ranking and density estimator MOEA (double, permutation encodings)

- Meta-optimization algorithms:

  - NSGA-II
  - Async NSGA-II
  - SMPSO


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
Let us suppose that we want to optimize the parameters of the NSGA-II algorithm (the base-level metaheuristic) for solving the DTLZ1 problem with NSGA-II (the meta-optimizer).
We first load the parameter space from the `NSGAIIDouble.yaml <https://github.com/jMetal/Evolver/blob/main/src/main/resources/parameterSpaces/NSGAIIDouble.yaml>`_ file in the resources folder. 
Next, we configure the training set with DTLZ1 and its reference front. 
We then set up the epsilon and normalized hypervolume quality indicators (i.e., the objectives to minimize) and initialize the base NSGA-II with a population size of 100 and
a stopping criterion of 15,000 evaluations. 
The meta-optimization is configured with execute a sigle independent run per configuration.
Next, we configure the NSGA-II acting as meta-optimizer with a stopping criterion of 2,000 evaluations and 8 cores for parallel processing. 
Finally, we run the meta-optimizer, which stores results in the RESULTS directory as CSV files.

The following code snippet includes the main steps:

.. code-block:: java

   // 1. Define the YAML parameter space file and the training set
   String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
   var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
   List<Problem<DoubleSolution>> trainingSet = List.of(new DTLZ1());
   List<String> referenceFrontFileNames = List.of("resources/referenceFronts/DTLZ1.3D.csv");

   // 2. Set up the algorithm to be configured
   var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
   int populationSize = 100 ;
   var baseAlgorithm = new DoubleNSGAII(populationSize, parameterSpace);
   var maximumNumberOfEvaluations = List.of(15000);

   // 3. Create the meta-optimization problem
   int numberOfIndependentRuns = 1;
   EvaluationBudgetStrategy evaluationBudgetStrategy = new FixedEvaluationsStrategy(maximumNumberOfEvaluations);

   MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
       new MetaOptimizationProblem<>(
           baseAlgorithm,
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

   // 5. Define an observer to write the execution data to files    
   String outputFolder = "RESULTS/NSGAII/DTLZ1"
   var outputResults =
        new OutputResults(
            "NSGA-II",
            metaOptimizationProblem,
            "DTLZ1",
            indicators,
            outputFolder);

   var writeExecutionDataToFilesObserver =
        new WriteExecutionDataToFilesObserver(1, outputResults);

   nsgaii.observable().register(writeExecutionDataToFilesObserver);
     
   // 6. Run the meta-optimizer  
   nsgaii.run();

After running the meta-optimizer, a configuration is located in the ``VAR.NSGA-II.DTLZ1.EP.NHV.Conf.2000.txt`` file in the RESULTS directory:

.. code-block:: bash

   --algorithmResult externalArchive --populationSizeWithArchive 133 --archiveType unboundedArchive --createInitialSolutions default --offspringPopulationSize 2 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9719337329527943 --crossoverRepairStrategy random --sbxDistributionIndex 133.8313543413145 --mutation uniform --mutationProbabilityFactor 0.5124086272844153 --mutationRepairStrategy random --uniformMutationPerturbation 0.22680609334711863 --selection tournament --selectionTournamentSize 5 

With this configuration, we can run the base-level NSGA-II as follows:

.. code-block:: java

   public class NSGAIIDTLZ3Example {
      public static void main(String[] args) {
         String[] parameters =
            ("--algorithmResult externalArchive " +
                "--populationSizeWithArchive 133 " +
                "--archiveType unboundedArchive " +
                "--createInitialSolutions default " +
                "--offspringPopulationSize 2 " +
                "--variation crossoverAndMutationVariation " +
                "--crossover SBX " +
                "--crossoverProbability 0.9719337329527943 " +
                "--crossoverRepairStrategy random " +
                "--sbxDistributionIndex 133.8313543413145 " +
                "--mutation uniform " +
                "--mutationProbabilityFactor 0.5124086272844153 " +
                "--mutationRepairStrategy random " +
                "--uniformMutationPerturbation 0.22680609334711863 " +
                "--selection tournament " +
                "--selectionTournamentSize 5 \n")
            .split("\\s+");

      var baseNSGAII = new DoubleNSGAII(new DTLZ3(), 100, 40000, new NSGAIIDoubleParameterSpace());
      baseNSGAII.parse(parameters);

      baseNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

      EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();
      nsgaII.run();

      new SolutionListOutput(nsgaII.result())
         .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
         .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
         .print();
      }
   }
   

The obtained front and the one obtained with NSGA-II with default settings are shown in the following figures:

.. list-table::
   :align: center
   :widths: auto

   * - .. image:: resources/scripts/DTLZ3.Evolver.png
          :alt: DTLZ1-Evolver
          :width: 400
     - .. image:: resources/scripts/DTLZ3.NSGAII.png
          :alt: DTLZ1-OriginalNSGAII
          :width: 400

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

v2.0 (2025-09-09)
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
