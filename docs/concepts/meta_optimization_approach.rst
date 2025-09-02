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
The goal of a meta-optimization process is, given a base-level multi-objective metaheuristic and a training set of optimization problems, to find the best configuration of the base-level metaheuristic to efficiently solve the training set. The first step is, then, to define these two components.

Defining the Base-Level Metaheuristic
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The base-level metaheuristic is characterized by a parameter space, i.e, a set of parameters that can be adjusted to improve its performance. The next code snippet shows how to configure a base-level NSGA-II algorithm for solving continuous optimization problems:

.. code-block:: java

   String yamlParameterSpaceFile = "NSGAIIDouble.yaml" ;
   var parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());
   
   int populationSize = 100;
   var baseAlgorithm = new DoubleNSGAII(populationSize, parameterSpace);
    




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
