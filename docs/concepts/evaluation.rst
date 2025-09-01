.. _evaluation:

Solution Evaluation
==================

Evaluation is a critical component of the meta-optimization process in Evolver. This document explains how configurations are evaluated and how their quality is assessed.

Evaluation Process
------------------
1. **Decoding**: Convert the encoded solution into actual parameter values
2. **Configuration**: Set up the base-level metaheuristic with the decoded parameters
3. **Execution**: Run the configured metaheuristic on the target problems
4. **Assessment**: Compute quality indicators for the obtained solutions
5. **Aggregation**: Combine results across multiple runs and problems

### Multiple Runs
Due to the stochastic nature of metaheuristics, each configuration is typically evaluated multiple times (N) per problem. The number of runs (N) is a parameter that affects both reliability and computational cost.

### Quality Indicators
Evolver supports various quality indicators to assess solution quality:

#### Convergence Metrics
- **Inverted Generational Distance (IGD)**: Measures both convergence and diversity
- **Additive Epsilon (EP)**: Indicates how much a front needs to be modified to dominate the reference front
- **Hypervolume (HV)**: Measures the volume of the objective space dominated by the solutions

#### Diversity Metrics
- **Spread (Δ)**: Measures the distribution of solutions along the Pareto front
- **Spacing**: Quantifies how evenly solutions are distributed

#### Normalization
Some indicators like Hypervolume are typically maximized, but Evolver requires minimization. These are transformed:
- **NHV (Normalized Hypervolume)**: `1 - HV_f/HV_rf`
  - `HV_f`: Hypervolume of the front
  - `HV_rf`: Hypervolume of the reference front

### Aggregation Methods
Results are aggregated across multiple runs and problems:

1. **Per Problem**: For each problem, compute the median of N runs
2. **Across Problems**: For each indicator, compute the mean of problem-wise medians

### Parallel Evaluation
To improve efficiency, Evolver supports parallel evaluation of:
- Multiple configurations
- Multiple runs of the same configuration
- Multiple problems

### Example Evaluation Pipeline
```
For each configuration:
    For each problem in problem_set:
        results = []
        For i = 1 to N:
            result = run_metaheuristic(configuration, problem)
            results.append(compute_indicators(result))
        problem_median = median(results)
    overall_score = mean(problem_medians)
    return overall_score
```

### Best Practices
1. **Choose Appropriate N**: Balance between statistical significance and computation time
2. **Select Relevant Indicators**: Choose indicators that reflect your optimization goals
3. **Monitor Progress**: Track evaluation metrics over time
4. **Handle Failures**: Implement proper error handling for invalid configurations
5. **Cache Results**: Store evaluation results to avoid redundant computations

For more details on specific quality indicators, see the :doc:`/api_reference` section.
