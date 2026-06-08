# Evolver: Automated meta-optimization of multi-objective metaheuristics

Evolver is a Java framework that formulates the automatic configuration of multi-objective
metaheuristics as a multi-objective optimization problem and solves it using the same class of
algorithms — a *meta-optimization* approach.

## How it works

```
Meta-level Optimizer (e.g., MetaNSGAII)
  └─> AbstractMetaOptimizationProblem
       ├─> MetaOptimizationProblem      (flat double encoding)
       └─> TreeMetaOptimizationProblem  (derivation tree encoding)
            └─> Base-level Algorithm (runs with the decoded configuration)
                 └─> Training Set of Problems (ZDT, WFG, DTLZ, RE, RWA…)
```

The meta-optimizer treats parameter configurations as solutions and their quality indicator
values on a training set as objectives to minimize.

## Supported algorithms

| Algorithm | Encodings |
|---|---|
| NSGA-II | Double, Binary, Permutation |
| MOEA/D | Double |
| SMS-EMOA | Double |
| MOPSO | Double |
| RDEMOEA | Double, Permutation |
| RVEA | Double |

## Requirements

- Java 21+
- Maven 3.6+

## Build and test

```bash
# Build
mvn clean install

# Unit tests
mvn test

# Integration tests
mvn integration-test

# All tests
mvn verify
```

## Parameter spaces

Algorithm parameter spaces are defined in YAML files under
`src/main/resources/parameterSpaces/` (e.g., `NSGAIIDouble.yaml`).
Pre-tuned default configurations live in
`src/main/resources/defaultConfigurations/`.

## Citation

If you use Evolver in your research, please cite:

> Evolver: Meta-optimizing multi-objective metaheuristics.
> SoftwareX, 2023. <https://doi.org/10.1016/j.softx.2023.101551>
