# AGENTS.md

This file provides guidance to AI agents when working on code in this repository.

## What this repo is
Evolver is a Java framework for automatically configuring multi-objective metaheuristics using a two-level (meta/base) optimization approach. It builds on jMetal and exposes example mains. See `README.rst` for a fuller narrative.

## Quick facts
- Language/Tooling: Java 21, Maven
- Key deps: jMetal `${jmetal.version}` (currently 7.0), SnakeYAML
- Packaging: jar (library)
- Coding standards: see `JAVA_CODING_GUIDELINES.md` (project-specific guidance)

## Build/Lint/Test Commands
```bash
# Clean and compile
mvn clean compile

# Unit tests only (Surefire)
mvn test

# Unit + integration tests (Failsafe runs *IT.java during verify)
mvn verify

# Package the jar (skips tests)
mvn -DskipTests package

# Run a single test class
mvn -Dtest=NSGAIIDoubleTest test

# Run a single test method
mvn -Dtest=NSGAIIDoubleTest#methodName test

# Run a single IT class
mvn -Dit.test=NSGAIIDoubleIT verify

# Run a single IT method
mvn -Dit.test=NSGAIIDoubleIT#methodName verify

# Run multiple test classes
mvn -Dtest="NSGAIIDoubleTest,MOEADDoubleTest" test

# Skip tests and static analysis, just package
mvn -DskipTests -Dcheckstyle.skip -Dpmd.skip -Dspotbugs.skip package
```

Static analysis (configured in pom.xml):
```bash
mvn checkstyle:check    # google_checks.xml (import order, naming, etc.)
mvn pmd:check          # code style, best practices
mvn spotbugs:check     # bug detection
mvn javadoc:javadoc    # generate API docs
```

## Code Style Guidelines

### Import Organization (Google Java Style)
Order imports with blank lines between groups:
1. `com.google.*`
2. `org.apache.commons.*`, `org.apache.logging.*`
3. `jakarta.*`, `javax.*`
4. `org.uma.jmetal.*` (jMetal framework)
5. `org.uma.evolver.*` (this project)
6. `java.*`, `javafx.*`

### Java 21+ Features - USE THESE:
- **Records** for DTOs and immutable value objects
- **Sealed classes/interfaces** for controlled type hierarchies
- **Pattern matching** with switch expressions
- **Optional<T>** instead of null for absent values
- **Streams API** for collection transformations
- **Try-with-resources** for all AutoCloseable resources
- **`var`** for local variables when type is obvious

### Naming Conventions
- Classes/Records: `PascalCase` (e.g., `YAMLParameterSpace`, `MetaOptimizationProblem`)
- Methods/Variables: `camelCase` (e.g., `buildAlgorithm()`, `parameterSpace`)
- Constants: `SCREAMING_SNAKE_CASE` (e.g., `MAX_ITERATIONS`)
- Test classes: `ClassNameTest` or `ClassNameIT` (e.g., `NSGAIIDoubleTest`, `NSGAIIDoubleIT`)
- Test methods: `givenContext_whenAction_thenResult()` (e.g., `givenValidYaml_whenLoading_thenParametersCreated()`)

### DO:
- Use `@DisplayName` and `@Nested` for organized JUnit 5 tests
- Follow Given-When-Then naming: `givenValidInput_whenAction_thenResult()`
- Write complete Javadoc for all public APIs
- Make fields `final` when possible
- Use immutable collections: `List.of()`, `Set.of()`, `Map.of()`
- Keep methods under 20 lines with single responsibility
- Use specific custom exceptions over generic RuntimeException
- Use builder pattern for complex object construction (see `MetaNSGAIIBuilder`)

### DON'T:
- Create verbose classes when records suffice
- Use null returns; return Optional instead
- Close resources manually with finally blocks
- Use if-else chains with instanceof checks
- Write "god methods" doing multiple things
- Leave public APIs undocumented

### Error Handling
- Throw specific exceptions: `IllegalArgumentException`, `IllegalStateException`, custom exceptions
- Include descriptive messages with context
- Use `@throws` in Javadoc for documented exceptions
- Never swallow exceptions silently

## Architecture (Package Structure)

### Two-level optimization
- **Meta level**: `org.uma.evolver.meta.*` (builders, problem, strategy)
- **Base level**: `org.uma.evolver.algorithm.*` (nsgaii, moead, smsemoa, mopso, rdemoea)

### Parameter modeling
- Package: `org.uma.evolver.parameter.*` (Parameter, ConditionalParameter, ParameterSpace)
- YAML loader: `parameter.yaml.YAMLParameterSpace`
- YAML files: `src/main/resources/parameterSpaces/*.yaml`
- Factories: `DoubleParameterFactory`, `BinaryParameterFactory`, `PermutationParameterFactory`

### Training sets
- `org.uma.evolver.trainingset.*`: Training problem sets and reference fronts

### Example mains
- Training: `org.uma.evolver.example.training.*`
- Configuration: `org.uma.evolver.example.configuration.*`
- Validation: `org.uma.evolver.example.validation.*`

### Irace integration
- `org.uma.evolver.irace.*`: irace parameter description generators and runner

### Utilities
- `org.uma.evolver.util.*`: config I/O, result writers, reference-front estimation

## Testing:
- Framework: JUnit 5 (junit-jupiter), Mockito for mocking
- Unit tests: `src/test/java/**/*Test.java`
- Integration tests: `src/test/java/**/*IT.java` (executed during `verify`)
- Run example mains from your IDE (no maven-exec plugin configured)

## Notes
- When adding parameters or algorithms, mirror existing patterns for YAML compatibility
- New algorithms should extend jMetal components and be configurable via ParameterSpace
- Prefer editing `JAVA_CODING_GUIDELINES.md` for style decisions

## Pointers for future agents
1. All parameter spaces are YAML files in `src/main/resources/parameterSpaces/`
2. Use builders (e.g., `MetaNSGAIIBuilder`) for complex object construction
3. jMetal quality indicators are used as optimization objectives (Epsilon, NormalizedHypervolume, etc.)
