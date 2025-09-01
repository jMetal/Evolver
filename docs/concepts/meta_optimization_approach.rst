.. _meta-optimization-approach:

Meta-Optimization Approach
==========================

Evolver's meta-optimization approach automates the configuration of multi-objective metaheuristics by treating the configuration process itself as a multi-objective optimization problem.

Core Concept
------------
The meta-optimization process involves:

1. **Base-Level Metaheuristic**: The algorithm being configured (e.g., NSGA-II, MOEA/D)
2. **Meta-Optimizer**: The algorithm used to search for optimal configurations
3. **Configuration Space**: The set of all possible parameter configurations
4. **Objectives**: Quality indicators used to evaluate configurations

Process Flow
------------
1. Define the parameter space for the base-level metaheuristic
2. Encode configurations as real-valued vectors
3. Use a meta-optimizer to search the configuration space
4. Evaluate configurations by running the base-level metaheuristic
5. Assess solution quality using multiple metrics
6. Iteratively refine configurations based on performance

Key Features
------------
- **Multi-Objective Optimization**: Considers multiple, often conflicting objectives
- **Flexible Configuration**: Supports various parameter types and constraints
- **Extensible**: Can incorporate new meta-optimizers and quality indicators
- **Parallel Execution**: Supports parallel evaluation of configurations

Example Use Case
----------------
Configuring NSGA-II to solve a set of benchmark problems:

1. Define NSGA-II's parameter space (population size, operators, etc.)
2. Select quality indicators (e.g., IGD, HV, Spread)
3. Choose a meta-optimizer (e.g., SMPSO, NSGA-II)
4. Run the meta-optimization process
5. Analyze and validate the resulting configurations

For implementation details and examples, see the :doc:`/examples` section.
