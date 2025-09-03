.. _base-level-metaheuristics:

Base-Level Metaheuristics
========================

Base-level metaheuristics in Evolver are multi-objective optimization algorithms that can be automatically configured through the meta-optimization process. This section describes the supported algorithms, how they are implemented, and their configuration options.

Supported Algorithms
--------------------

### NSGA-II (Non-dominated Sorting Genetic Algorithm II)
- **Type**: Evolutionary Algorithm
- **Key Features**: Fast non-dominated sorting, crowding distance
- **Parameters**: Population size, crossover/mutation probabilities, selection operators
- **Reference**: Deb, K., et al. (2002). A Fast and Elitist Multiobjective Genetic Algorithm: NSGA-II. *IEEE Transactions on Evolutionary Computation*.

### MOEA/D (Multi-Objective Evolutionary Algorithm Based on Decomposition)
- **Type**: Decomposition-based Evolutionary Algorithm
- **Key Features**: Decomposes MOP into scalar subproblems
- **Parameters**: Neighborhood size, aggregation function, update strategy

### SMS-EMOA (S-metric Selection Evolutionary Multi-Objective Algorithm)
- **Type**: Indicator-based Evolutionary Algorithm
- **Key Features**: Uses hypervolume contribution for selection
- **Parameters**: Reference point, population size

### MOPSO (Multi-Objective Particle Swarm Optimization)
- **Type**: Swarm Intelligence
- **Key Features**: Particle movement based on personal and global best
- **Parameters**: Inertia weight, cognitive/social coefficients

### RDEMOEA (Ranking and Density Estimator Multi-Objective Evolutionary Algorithm)
- **Type**: Hybrid Evolutionary Algorithm
- **Key Features**: Combines ranking and density estimation
- **Parameters**: Ranking weights, density estimation parameters

Configuration
------------

### Parameter Space Definition
Base-level metaheuristics are configured through YAML files that define their parameter spaces. Example:

.. code-block:: yaml

    populationSize:
      type: integer
      range: [20, 200]
    
    variationOperator:
      type: categorical
      values:
        sbx:
          conditionalParameters:
            distributionIndex:
              type: double
              range: [5.0, 400.0]
        de:
          conditionalParameters:
            cr:
              type: double
              range: [0.0, 1.0]
            f:
              type: double
              range: [0.1, 1.0]

### Common Parameters
Most algorithms support these common parameter groups:

1. **Population**
   - `populationSize`: Number of solutions
   - `offspringPopulationSize`: Number of offspring generated

2. **Variation Operators**
   - Crossover type and parameters
   - Mutation type and parameters
   - Selection mechanisms

3. **Termination Criteria**
   - Maximum evaluations/generations
   - Quality thresholds

4. **Archive**
   - Archive size
   - Archive update strategy

Implementation Details
---------------------

### Algorithm Interface
All base-level metaheuristics implement the `BaseMetaheuristic` interface:

.. code-block:: java

    public interface BaseMetaheuristic {
        void parse(String[] parameters);
        List<DoubleSolution> run();
        String getName();
        Map<String, Object> getParameters();
    }

### Configuration Example

.. code-block:: java

    // Load parameter space
    var parameterSpace = new YAMLParameterSpace("NSGAIIDouble.yaml");
    
    // Create and configure algorithm
    var algorithm = new NSGAIIBuilder(problem, parameterSpace)
        .setPopulationSize(100)
        .setMaxEvaluations(25000)
        .build();
    
    // Run optimization
    List<DoubleSolution> population = algorithm.run();

Best Practices
--------------

1. **Start Simple**: Begin with a small set of key parameters
2. **Use Sensible Ranges**: Define reasonable bounds for numerical parameters
3. **Balance Exploration/Exploitation**: Adjust variation operator parameters accordingly
4. **Monitor Progress**: Track convergence metrics during optimization
5. **Validate Configurations**: Test configurations on multiple problem instances
