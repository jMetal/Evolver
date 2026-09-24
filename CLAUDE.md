# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Evolver is a Java framework for **automated meta-optimization of multi-objective metaheuristics**. It tunes algorithm parameters using a two-level optimization approach:

- **Base level**: The algorithm being configured (e.g., NSGA-II, MOEA/D) solves a set of training problems.
- **Meta level**: A meta-optimizer finds optimal parameter configurations by optimizing quality indicators (Hypervolume, Epsilon).

## Build and Test Commands

```bash
# Build
mvn clean install

# Run unit tests
mvn test

# Run a single test class
mvn test -Dtest=ConfigurationFileReaderTest

# Run a single test method
mvn test -Dtest=ConfigurationFileReaderTest#shouldLoadConfigurationFileSuccessfully

# Run integration tests
mvn integration-test

# Run both unit and integration tests
mvn verify
```

Requirements: Java 21+ (JDK 21 recommended, as in CI; SpotBugs in `mvn verify` fails with JDK 26), Maven 3.6+.

## Architecture

### Two-Level Optimization Flow

```
Meta-level Optimizer (e.g., MetaNSGAII)
  └─> AbstractMetaOptimizationProblem  (shared evaluation pipeline)
       ├─> MetaOptimizationProblem      (flat double encoding)
       └─> TreeMetaOptimizationProblem  (derivation tree encoding)
            └─> Base-level Algorithm (runs with the decoded configuration)
                 └─> Training Set of Problems (ZDT, WFG, DTLZ, RE, RWA...)
```

The meta-optimizer treats algorithm parameter configurations as solutions, and their quality indicators on the training set as objectives to minimize.

### Key Packages

The packages form two layers. The **configurable core** (`algorithm`, `parameter`, `util`) can be
used on its own, as an alternative to jMetal for configuring and running algorithms; it never
depends on the **meta level** (`meta`, `cli`, `irace`), which `PackageLayeringTest` enforces.

| Package | Role |
|---|---|
| `org.uma.evolver.algorithm` | *Core.* Configurable algorithms (`BaseLevelAlgorithm`): NSGA-II, NSGA-III, MOEA/D, SMS-EMOA, MOPSO, RDEMOEA, RVEA, AGE-MOEA, SSMOEA, PAES. Each supports multiple encodings (Double, Binary, Permutation), except NSGA-III, MOPSO, RVEA, AGE-MOEA and SSMOEA (Double only) and RDEMOEA (Double and Permutation). |
| `org.uma.evolver.parameter` | *Core.* Parameter space definition and YAML parsing. Supports integer, double, categorical, binary, and conditional (hierarchical) parameters. |
| `org.uma.evolver.util` | *Core.* `ConfigurationFileReader` (reads configurations such as `defaultConfigurations/*.txt`) and `HypervolumeMinus`. |
| `org.uma.evolver.meta` | *Meta level.* `algorithm` (meta-only algorithms: `RandomSearch`, `TreeNSGAII`, `TreeAGEMOEA`), `builder` (`Meta*Builder`), `problem` (`AbstractMetaOptimizationProblem`, `MetaOptimizationProblem` for the flat encoding, `TreeMetaOptimizationProblem` for the tree encoding), `strategy` (evaluation budgets), `encoding` (derivation tree encoding: `DerivationTreeSolution`, `SubtreeCrossover`, `TreeMutation`, `TreeSolutionGenerator`, `GrammarConverter` and the tree operator parameters), `trainingset` (training sets wrapping jMetal benchmark problems) and `output` (`ConsolidatedOutputResults`, `WriteExecutionDataToFilesObserver`, `MetaOptimizerConfig`, `TreeOutputResults`). |
| `org.uma.evolver.cli` | *Meta level.* `cli.training`: request/status/result runner for training jobs, used by Evolver-Studio. |
| `org.uma.evolver.irace` | *Meta level.* irace integration for alternative parameter tuning. |
| `org.uma.evolver.example` | Runnable examples in three packages: `baselevel` (single base-level algorithm runs, split into `standard` for typical configurations, `tuned` for meta-optimized configurations, and `features` for specific-capability demos), `training` (meta-optimization runs), and `validation` (comparative studies). |

### Parameter Spaces

Algorithm parameter spaces are defined in YAML files under `src/main/resources/parameterSpaces/` (e.g., `NSGAIIDouble.yaml`). These specify parameter names, types, ranges, and conditional dependencies. The `YAMLParameterSpace` class parses them.

Pre-tuned default configurations live in `src/main/resources/defaultConfigurations/` as plain text files (e.g., `NSGAIIDoubleDefault.txt`). These are read by `ConfigurationFileReader`.

### Solution Encodings

Algorithms come in encoding-specific variants: `DoubleNSGAII`, `BinaryNSGAII`, `PermutationNSGAII`, etc. The base abstract class is `BaseLevelAlgorithm`.

### Dependencies

Built on [jMetal 7.4](https://github.com/jMetal/jMetal) (`jmetal-core`, `jmetal-algorithm`, `jmetal-component`, `jmetal-parallel`, `jmetal-lab`, `jmetal-problem`). YAML parsing uses SnakeYAML 2.4.

## Testing

- Unit tests: `*Test.java` under `src/test/java`, run with `mvn test`
- Integration tests: `*IT.java`, run with `mvn integration-test`
- Test parameter space YAML fixtures are in `src/test/resources/parameterSpaces/`
- Follow the conventions in `JAVA_CODING_GUIDELINES.md` §13: JUnit 6 (Jupiter), Given-When-Then method names, AAA internal structure (`// Arrange / Act / Assert`), `@DisplayName`, and `@Nested` for grouping.

## Generated Output Directories

These directories hold runtime output and must not be tracked or modified:

- `experimentation/` — output from meta-optimization runs (configurations, indicators, logs)
- `results/` — aggregated results and analysis artifacts (e.g. validation `FUN.csv` files and HTML reports)

Do not commit their contents; they are produced by running the examples.

`scripts/` is **not** in this category. It holds versioned, reusable Python analysis and plotting
tooling (committed). Reusable scripts — including those generated by the `new-validation` command —
should be committed; only genuine one-off throwaway scripts may be left untracked.

## Git and Commit Guidelines

See [`GIT_GUIDELINES.md`](GIT_GUIDELINES.md) for the full conventions (Conventional Commits, atomic commits, allowed types).
