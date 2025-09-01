.. _objective-functions:

Objective Functions
==================

Quality indicators are essential components in Evolver's meta-optimization process, serving as objective functions that guide the search for optimal algorithm configurations. This document describes the available quality indicators and their usage in Evolver.

Available Quality Indicators
----------------------------

### Convergence Metrics

#### Inverted Generational Distance (IGD)
- **Description**: Measures both convergence and diversity by calculating the average distance from each point in the reference front to the nearest point in the obtained front.
- **Range**: [0, ∞), lower is better
- **Reference**: Coello Coello, C. A., & Reyes-Sierra, M. (2006). Multi-Objective Particle Swarm Optimizers: A Survey of the State-of-the-Art. *International Journal of Computational Intelligence Research*.

#### Additive Epsilon (EP)
- **Description**: Indicates the minimum value by which a front needs to be modified in objective space to dominate the reference front.
- **Range**: [0, ∞), lower is better

### Diversity Metrics

#### Spread (Δ)
- **Description**: Measures the distribution of solutions along the Pareto front.
- **Range**: [0, 1], lower is better

#### Spacing (SP)
- **Description**: Quantifies how evenly solutions are distributed in the objective space.
- **Range**: [0, ∞), lower is better

### Combined Metrics

#### Hypervolume (HV)
- **Description**: Measures the volume of the objective space dominated by the obtained solutions, bounded by a reference point.
- **Range**: [0, ∞), higher is better
- **Note**: In Evolver, we typically use Normalized Hypervolume (NHV) for minimization:
  - `NHV = 1 - HV_f/HV_rf`
  - Where `HV_f` is the hypervolume of the front and `HV_rf` is the hypervolume of the reference front
  - Range: [0, 1], lower is better

#### R2 Indicator
- **Description**: Measures the average distance of the solutions to a set of weight vectors in the objective space.
- **Range**: [0, ∞), lower is better

Usage in Evolver
---------------

### Configuration
Quality indicators are specified in the meta-optimization configuration. Example:

.. code-block:: yaml

    objectives:
      - type: igd
        referenceFront: path/to/reference/front
      - type: nhv
        referenceFront: path/to/reference/front
        referencePoint: [1.1, 1.1, 1.1]  # For 3 objectives

### Best Practices
1. **Reference Front**: Always use a well-distributed reference front that represents the true Pareto front if available.
2. **Multiple Indicators**: Combine convergence and diversity indicators for better results.
3. **Normalization**: Consider normalizing objectives when they have different scales.
4. **Computational Cost**: Be aware that some indicators (like HV) can be computationally expensive for many objectives.

### Custom Indicators
Evolver allows the integration of custom quality indicators. To add a new indicator:

1. Implement the `QualityIndicator` interface
2. Register it in the indicator factory
3. Update the configuration parser to support the new indicator type

Example Implementation
----------------------

.. code-block:: java

    public class MyCustomIndicator implements QualityIndicator {
        @Override
        public double compute(double[][] front, double[][] referenceFront) {
            // Implementation here
            return 0.0;
        }
        
        @Override
        public String getName() {
            return "MyCustomIndicator";
        }
    }
