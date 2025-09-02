.. _solution-encoding:

Solution Encoding
================

Evolver encodes metaheuristic configurations as real-valued vectors to enable efficient optimization. This document explains the encoding scheme and its implications.


Configurations for Base-Level Metaheuristics
-------------------------------------------
All the base-level metaheuristics in Evolver are configured from a string containing all the parameters. This string has this scheme: "--parameterName1 parameterValue1 --parameterName2 parameterValue2 ... --parameterNameN parameterValueN". This string is parsed and the corresponding parameter values are set in the base-level metaheuristic. 

Using this string as a solution encoding would involve the design and implementation of specific mutation and crossover operators. Our approach, on the other hand, adopts a very simple scheme: each parameter is encoded as a real number. This approach simplifies the meta-optimization problem by making it continuous, allowing us to use any metaheuristic included in jMetal that can solve continuous optimization problems as a meta-optimizer. This includes not only evolutionary algorithms like NSGA-II, but also particle swarm optimization algorithms like SMPSO.

Encoding Scheme
---------------





All parameters are encoded as real numbers in the range [0.0, 1.0], regardless of their original type. This uniform encoding allows the use of any continuous metaheuristic as a meta-optimizer.

### Type Conversion

#### Boolean Parameters
- `[0.0, 0.5)` → `false`
- `[0.5, 1.0]` → `true`

#### Categorical Parameters
For a parameter with N possible values:
- The range [0.0, 1.0] is divided into N equal-sized bins
- Each bin corresponds to one category

Example with 3 categories (A, B, C):
- [0.0, 0.333) → A
- [0.333, 0.666) → B
- [0.666, 1.0] → C

#### Numerical Parameters
For parameters with range [min, max]:
- `value = min + (max - min) * encoded_value`

### Example
Consider a configuration with:
- A boolean parameter (e.g., `useArchive`)
- A categorical parameter with 3 values (e.g., `crossover` = {SBX, BLX, PCX})
- A numerical parameter in [0, 100] (e.g., `populationSize`)

An encoded solution `[0.7, 0.4, 0.25]` would decode to:
- `useArchive = true` (0.7 ≥ 0.5)
- `crossover = BLX` (0.4 falls in second bin)
- `populationSize = 25` (0 + (100-0)*0.25)

### Implications
1. **Parameter Independence**: The encoding treats all parameters as independent, which may not always reflect their relationships in the actual algorithm.
2. **Mutation Effects**: Small changes in encoded values may or may not change the decoded parameter values, especially for boolean and categorical parameters.
3. **Constraint Handling**: The encoding doesn't enforce constraints between parameters. Invalid configurations must be handled during decoding or evaluation.

### Best Practices
1. **Parameter Ordering**: Place related parameters close to each other in the encoding for better performance with some meta-optimizers.
2. **Parameter Scaling**: Consider the scale of numerical parameters to ensure balanced optimization.
3. **Validation**: Always validate decoded configurations before evaluation.
4. **Logging**: Log both encoded and decoded parameter values for debugging.

For more details on how these encodings are used in practice, see the :doc:`evaluation` documentation.
