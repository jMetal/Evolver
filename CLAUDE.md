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

Requirements: Java 21+, Maven 3.6+.

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

| Package | Role |
|---|---|
| `org.uma.evolver.parameter` | Parameter space definition and YAML parsing. Supports integer, double, categorical, binary, and conditional (hierarchical) parameters. |
| `org.uma.evolver.algorithm` | Configurable base-level algorithms: NSGA-II, MOEA/D, SMS-EMOA, MOPSO, RDEMOEA, RVEA. Each supports multiple encodings (Double, Binary, Permutation). |
| `org.uma.evolver.encoding` | Derivation tree encoding: `DerivationTreeSolution`, `SubtreeCrossover`, `TreeMutation`, `TreeSolutionGenerator`, `GrammarConverter`. |
| `org.uma.evolver.meta` | Meta-optimizer builders (`MetaNSGAIIBuilder`, `MetaSMPSOBuilder`, etc.) and problem classes: `AbstractMetaOptimizationProblem`, `MetaOptimizationProblem` (flat encoding), `TreeMetaOptimizationProblem` (tree encoding). |
| `org.uma.evolver.trainingset` | Training set management wrapping jMetal benchmark problems. |
| `org.uma.evolver.irace` | irace integration for alternative parameter tuning. |
| `org.uma.evolver.util` | Utilities: `ConfigurationFileReader`, `OutputResults`, observers for writing evolution data. |
| `org.uma.evolver.example` | Runnable examples for training, configuration, and validation. |

### Parameter Spaces

Algorithm parameter spaces are defined in YAML files under `src/main/resources/parameterSpaces/` (e.g., `NSGAIIDouble.yaml`). These specify parameter names, types, ranges, and conditional dependencies. The `YAMLParameterSpace` class parses them.

Pre-tuned default configurations live in `src/main/resources/defaultConfigurations/` as plain text files (e.g., `NSGAIIDoubleDefault.txt`). These are read by `ConfigurationFileReader`.

### Solution Encodings

Algorithms come in encoding-specific variants: `DoubleNSGAII`, `BinaryNSGAII`, `PermutationNSGAII`, etc. The base abstract class is `BaseLevelAlgorithm`.

### Dependencies

Built on [jMetal 7.3](https://github.com/jMetal/jMetal) (`jmetal-core`, `jmetal-algorithm`, `jmetal-component`, `jmetal-parallel`, `jmetal-lab`, `jmetal-problem`). YAML parsing uses SnakeYAML 2.4.

## Testing

- Unit tests: `*Test.java` under `src/test/java`, run with `mvn test`
- Integration tests: `*IT.java`, run with `mvn integration-test`
- Test parameter space YAML fixtures are in `src/test/resources/parameterSpaces/`
- Follow the conventions in `JAVA_CODING_GUIDELINES.md` §14: JUnit 5 (Jupiter), Given-When-Then method names, AAA internal structure (`// Arrange / Act / Assert`), `@DisplayName`, and `@Nested` for grouping.

## Temporary Directories

The following directories are generated at runtime and should not be tracked or modified:

- `experimentation/` — output from meta-optimization runs (configurations, indicators, logs)
- `results/` — aggregated results and analysis artifacts
- `scripts/` — auto-generated or ad-hoc scripts used during experiments

Do not commit files from these directories. They are listed in `.gitignore`.

## Git and Commit Guidelines

This project follows the [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Message format

```
<type>: <short imperative description>
```

### Allowed types

| Type       | When to use                                          |
|------------|------------------------------------------------------|
| `feat`     | A new feature or public method                       |
| `fix`      | A bug fix                                            |
| `test`     | Adding or correcting tests (no production code)      |
| `refactor` | Code change that is neither a fix nor a new feature  |
| `docs`     | Documentation only (`README.md`, `AGENTS.md`, etc.)  |
| `chore`    | Build config, dependencies, `.gitignore`, etc.       |

### Atomic commits

Each commit must represent **one single logical change**:

- If the commit message needs "and" to describe what it does, split it into two commits.
- Ensure the project builds and all tests pass before committing (`mvn clean test`).
- Never mix production code changes with test changes in the same commit.
- Never mix code changes with documentation changes in the same commit.
