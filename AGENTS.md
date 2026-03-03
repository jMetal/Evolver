# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## What this repo is
Evolver is a Java framework for automatically configuring multi-objective metaheuristics using a two-level (meta/base) optimization approach. It builds on jMetal and exposes example mains. See `README.rst` for a fuller narrative.

## Quick facts
- Language/Tooling: Java 21, Maven
- Key deps: jMetal `${jmetal.version}` (currently 7.0), SnakeYAML
- Packaging: jar (library). Training examples with `public static void main` live under `src/main/java/org/uma/evolver/example/training/**`, base-level configuration examples under `org/uma/evolver/example/configuration/**`, and validation/experimental studies under `org/uma/evolver/example/validation/**`.
- Coding standards: see `JAVA_CODING_GUIDELINES.md` (project-specific guidance). Prefer those over generic style rules.

## Commands you'll use most
Build and test
```bash
# Clean and compile (unit tests not executed)
mvn clean compile

# Unit tests only (Surefire)
mvn test

# Unit + integration tests (Failsafe runs *IT.java during verify)
mvn verify

# Package the jar (skips tests)
mvn -DskipTests package
```
Run a single test
```bash
# One unit test class
mvn -Dtest=NSGAIIDoubleTest test
# One unit test method
mvn -Dtest=NSGAIIDoubleTest#methodName test
# One IT class (runs during verify)
mvn -Dit.test=NSGAIIDoubleIT verify
# One IT method
mvn -Dit.test=NSGAIIDoubleIT#methodName verify
```
Static analysis (configured in pom.xml)
```bash
mvn checkstyle:check    # google_checks.xml
mvn pmd:check
mvn spotbugs:check
```
Documentation
```bash
mvn javadoc:jar         # build API docs jar
```

Notes
- No maven-exec plugin is configured. To run example mains, use your IDE or add `exec-maven-plugin` locally if needed.
- Integration tests are identified by `*IT.java` and are executed by Failsafe during `verify`.

## Big-picture architecture (where to look)
Two-level optimization
- Meta level: defines a meta-optimization problem over base-level configurations, then solves it with a MO algorithm (e.g., NSGA-II, SMPSO, SPEA2).
  - Key packages: `org.uma.evolver.meta.builder` (builders such as `MetaNSGAIIBuilder`, `MetaSMPSOBuilder`, `MetaSPEA2Builder`), `org.uma.evolver.meta.problem` (`MetaOptimizationProblem`), `org.uma.evolver.meta.strategy` (evaluation budget strategies)
- Base level: configurable MO algorithms (NSGA-II, MOEA/D, SMSEMOA, MOPSO, RDEMOEA) assembled from jMetal components.
  - Key packages: `org.uma.evolver.algorithm.*` and subpackages per algorithm (`nsgaii`, `moead`, `smsemoa`, `mopso`, `rdemoea`)
  - Example: `BaseNSGAII` builds a jMetal `EvolutionaryAlgorithm` using a `ParameterSpace` and optional external archives.

Parameter modeling and YAML loading
- Package: `org.uma.evolver.parameter.*`
  - Core abstractions: `Parameter`, `ConditionalParameter`, `ParameterSpace`
  - YAML loader: `parameter.yaml.YAMLParameterSpace` (+ processors per type) to build `ParameterSpace` from YAML
  - Types/factories: `parameter.type.*`, `parameter.factory.*`
- YAML files: `src/main/resources/parameterSpaces/*.yaml` (e.g., `NSGAIIDouble.yaml`, `MOEADDouble.yaml`, etc.)

Training sets
- Training sets: `org.uma.evolver.trainingset.*` (e.g., `DTLZ3DTrainingSet`, `RE3DTrainingSet`, `WFG2DTrainingSet`)

Examples (runnable mains)
- Meta-optimization training examples: `org.uma.evolver.example.training.*` (e.g., `NSGAIIOptimizingNSGAIIForBenchmarkDTLZ`)
- Base-level configuration examples: `org.uma.evolver.example.configuration.*` (e.g., `MOEAD_DTLZ2`, `NSGAIIZDT4Example`)
- Validation & experimental studies: `org.uma.evolver.example.validation.*` (e.g., `REStudy`, `RWAStudy`, `DTLZWFG3DStudy`, `RDEMOEAValidation`, `RDEMOEAValidationDTLZ`)

Irace integration
- `org.uma.evolver.irace.*`: irace parameter description generators and runner

Utilities and outputs
- `org.uma.evolver.util.*`: config I/O (`ConfigurationFileReader`), result writers (`WriteExecutionDataToFilesObserver`, `OutputResults`), reference-front estimation

## Tests
- Framework: JUnit 5 (`junit-jupiter`), Mockito for mocking
- Layout: unit tests under `src/test/java/**`, ITs suffixed with `IT` (executed by Failsafe during `verify`)

## Pointers for future agents
- Prefer editing/consulting `JAVA_CODING_GUIDELINES.md` for style decisions in this repo.
- When adding configurable parameters or new algorithms, mirror existing patterns in `parameter.*` and `algorithm.*` to ensure they are compatible with `ParameterSpace` and YAML loaders.
- All parameter spaces are defined via YAML files in `src/main/resources/parameterSpaces/` and loaded with `YAMLParameterSpace`.
