.. _meta-optimization-approach:

Meta-Optimization Approach
==========================

Evolver automates the process of finding optimal configurations for multi-objective metaheuristics through meta-optimization, which treats the algorithm configuration task itself as a multi-objective optimization problem.

Components
----------
The approach is based on the following components:

- **Base-Level Metaheuristic**: This is the multi-objective optimization algorithm we want to configure, such as NSGA-II, MOEA/D, or MOPSO. The meta-optimization process will automatically adjust its parameters to achieve the best performance.

- **Meta-Optimizer**: The algorithm that searches for optimal configurations of the base-level metaheuristic. Evolver uses standard multi-objective algorithms (like NSGA-II or MOEA/D) at this level to explore the space of possible configurations. 

- **Parameter Space**: Defines all possible configurations of the base-level metaheuristic. This includes:
    - Numerical parameters (e.g., population size, mutation rate)
    - Categorical parameters (e.g., selection operator type)
    - Conditional parameters that depend on other parameters' values (e.g., the distribution index of the SBX operator)

- **Training Set**: A collection of optimization problems used to evaluate configurations. The meta-optimizer seeks configurations that perform well across all problems in this set.

- **Quality Indicators**: Metrics that assess the performance of a configuration from the fronts obtained by running the base-level metaheuristic with that configuration on the training set. Common indicators include NHV, EP, IGD, and IGD+.

- **Meta-Optimization Problem**: Problem that the meta-optimizer solves to find the best configuration of the base-level metaheuristic. 

How It Works
------------
The goal of a meta-optimization process is, given a base-level multi-objective metaheuristic and a training set of optimization problems, to find the best configuration of the base-level metaheuristic to efficiently solve the training set. The first steps are, then, to define these two components.

Base-Level Metaheuristic Selection and Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The base-level metaheuristic is characterized by a parameter space, i.e, a set of parameters that can be adjusted to improve its performance. The next code snippet shows how to configure a base-level NSGA-II algorithm for solving continuous optimization problems:

.. code-block:: java

   String yamlParameterSpaceFile = "NSGAIIDouble.yaml" ;
   var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
   
   int populationSize = 100;
   var baseAlgorithm = new DoubleNSGAII(populationSize, parameterSpace);
    
In this example, ``NSGAIIDouble.yaml`` is a YAML file that defines the parameter space of the NSGA-II algorithm for solving continuous optimization problems. The file is contained in the resources folder of the Evolver project. You are free to modify this file to refine the parameter space of the NSGA-II algorithm.

After creating the parameter space, the *DoubleNSGAII* class is used to configure the base-level metaheuristic. It takes as parameters the population size and the parameter space. 

Training Set Definition
~~~~~~~~~~~~~~~~~~~~~~~
The traning set is simply a collection of optimization problems that must be accompanied by files containing the reference front of each problem. This is a requirement as most of quality indicators need reference fronts to compute their values. 

The next code snippet shows how to define a training set assuming that we are interested in finding a configuration of NSGA-II for solving the ZDT benchmark problem family:

.. code-block:: java

   List<Problem<DoubleSolution>> trainingSet =
      List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6());

   List<String> referenceFrontFileName =
          List.of("resources/referenceFronts/ZDT1.csv",
                  "resources/referenceFronts/ZDT2.csv",
                  "resources/referenceFronts/ZDT3.csv",
                  "resources/referenceFronts/ZDT4.csv",
                  "resources/referenceFronts/ZDT6.csv") ;

Quality Indicators Selection
~~~~~~~~~~~~~~~~~~~~~~~~~~

Quality indicators are metrics that assess the performance of a configuration from the fronts obtained by running the base-level metaheuristic with that configuration on the training set. As Evolver is based on jMetal, which assumess that all objective functions of optimizations problems have to be minimized, care must be taken when selecting quality indicators such as HV, which is a maximization indicator. If we are interested in the HV, the normalized HV or NHV can be used instead. 

The next code snippet shows how to select the EP and NHV indicators, so the resulting meta-optimization problem will have two objectives:

.. code-block:: java

   List<Indicator<DoubleSolution>> qualityIndicators =
      List.of(new Epsilon(), new NormalizedHypervolume());


Suggestions for selecting quality indicators can be found in :doc:`concepts/objective_functions`.


1. **Problem Definition**:
   - Define the parameter space of your base-level metaheuristic
   - Select a training set of optimization problems
   - Choose quality indicators to optimize

2. **Configuration Encoding**:
   - Each configuration is encoded as a real-valued vector in [0,1]^n
   - The encoding maps to specific parameter values in the defined space

3. **Meta-Optimization Loop**:
   - The meta-optimizer generates new configurations
   - Each configuration is evaluated by running the base-level metaheuristic on all training problems
   - Solution quality is measured using the selected indicators
   - The process repeats, with the meta-optimizer using feedback to improve configurations

4. **Result Analysis**:
   - The output is a set of Pareto-optimal configurations
   - Each configuration represents a different trade-off between the quality indicators
   - The user can select the most suitable configuration based on their needs

Practical Example: Tuning NSGA-II
--------------------------------
Let's say you want to optimize NSGA-II for solving a set of benchmark problems:

1. **Define Parameter Space**:
   - Population size: 50-200
   - Crossover probability: 0.7-1.0
   - Mutation probability: 1/n (where n is problem dimension)
   - Selection operator: Tournament or random selection

2. **Select Training Problems**:
   - ZDT1, ZDT2, ZDT3 (for 2-objective problems)
   - DTLZ1, DTLZ2 (for 3+ objectives)

3. **Choose Quality Indicators**:
   - IGD (for convergence and diversity)
   - Spread (for solution distribution)
   - Hypervolume (for overall performance)

4. **Run Meta-Optimization**:
   - Evolver will automatically find configurations that balance these indicators
   - The process typically takes several hours to days, depending on problem complexity

5. **Analyze Results**:
   - Compare different configurations in the obtained Pareto front
   - Select the one that best fits your requirements
   - Optionally, validate on unseen test problems

Key Benefits
------------
- **Automated Tuning**: Saves time compared to manual parameter tuning
- **Multi-Objective Optimization**: Considers multiple performance criteria simultaneously
- **Flexible**: Works with any base-level metaheuristic that can be parameterized
- **Extensible**: New algorithms, problems, and quality indicators can be easily added
3. Choose a meta-optimizer (e.g., SMPSO, NSGA-II)
4. Run the meta-optimization process
5. Analyze and validate the resulting configurations

For implementation details and examples, see the :doc:`/examples` section.
