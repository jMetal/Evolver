.. _base-level-metaheuristics:

Base-Level Metaheuristics
=========================

Base-level metaheuristics in Evolver are multi-objective optimization algorithms that can be automatically configured through the meta-optimization process. This section describes the how to implement the algorithms, the provided solveres, and  their configuration options.

The base-level metaheuristics are implemented in the ``org.uma.evolver.algorithm.base`` package. This package contains the ``BaseLevelAlgorithm`` interface that defines the contract for all configurable metaheuristics in Evolver.

BaseLevelAlgorithm Interface
----------------------------

The ``BaseLevelAlgorithm`` interface serves as the foundation for all configurable metaheuristics. It provides the necessary methods to:

1. Define and access the parameter space of the algorithm
2. Build configured algorithm instances
3. Create new instances with different problem configurations
4. Parse arguments for parameter configuration

Key characteristics:

- **Generic Type Parameter**: ``<S extends Solution<?>>`` ensures type safety for the solutions managed by the algorithm
- **Immutable Configuration**: The interface encourages immutable configuration through the builder pattern
- **Parameter Space Integration**: Tightly integrated with the `ParameterSpace` class for flexible parameter management

Here's the interface definition with detailed method documentation:

.. code-block:: java

  /**
   * Interface representing a configurable evolutionary algorithm.
   * 
   * @param <S> the solution type handled by the algorithm
   */
  public interface BaseLevelAlgorithm<S extends Solution<?>> {
    
    /**
     * Returns the parameter space associated with this algorithm.
     */
    ParameterSpace parameterSpace();

    /**
     * Builds and returns a configured {@link Algorithm} instance.
     */
    Algorithm<List<S>> build();

    /**
     * Creates a new instance of the algorithm for the given problem and maximum number of evaluations.
     */
    BaseLevelAlgorithm<S> createInstance(Problem<S> problem, int maximumNumberOfEvaluations);

    /**
     * Parses the given arguments and configures all top-level parameters in the parameter space.
     * The arguments should be provided as an array of strings in the format
     * ["--param1", "value1", "--param2", "value2", ...].
     * Returns {@code this} for fluent usage.
     *
     * @param args the arguments to parse, in the format ["--param1", "value1", "--param2", "value2", ...]
     * @return this algorithm instance, configured according to the arguments
     */
    default BaseLevelAlgorithm<S> parse(String[] args) {
      for (Parameter<?> parameter : parameterSpace().topLevelParameters()) {
        parameter.parse(args);
      }
      return this;
    }
  }
        



