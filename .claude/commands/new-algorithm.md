Add a new configurable algorithm to the Evolver project following the established pattern.

The algorithm to add is: $ARGUMENTS

## Steps

### 1. Identify the reference pattern

Before creating any file, read the most similar algorithm already implemented:
- `src/main/java/org/uma/evolver/algorithm/agemoea/BaseAGEMOEA.java`
- `src/main/java/org/uma/evolver/algorithm/agemoea/DoubleAGEMOEA.java`

Identify what is specific to that algorithm in `createReplacement()` and `setNonConfigurableParameters()`, then replicate it with the logic of the new algorithm.

### 2. Create the Java classes

**Package:** `org.uma.evolver.algorithm.<lowercasename>`

Create two files:

**`Base<Name>.java`** — abstract class implementing `BaseLevelAlgorithm<S>`:
- Two constructors: `(int populationSize, ParameterSpace)` and `(Problem<S>, int populationSize, int maximumNumberOfEvaluations, ParameterSpace)`
- Implements `parameterSpace()`, `build()`, and the protected methods: `createInitialSolutions()`, `createVariation()`, `createSelection()`, `createEvaluation()`, `createReplacement()`, `createTermination()`
- Declares `setNonConfigurableParameters()` as abstract

**`Double<Name>.java`** — concrete class for `DoubleSolution`:
- Extends `Base<Name><DoubleSolution>`
- Implements `createInstance(Problem<DoubleSolution>, int)` returning `new Double<Name>(..., parameterSpace.createInstance())`
- Implements `setNonConfigurableParameters()` injecting `numberOfProblemVariables` into the mutation

### 3. Create the parameter space YAML

**Path:** `src/main/resources/parameterSpaces/<Name>Double.yaml`

Use `AGEMOEADouble.yaml` as a starting point. Always include:
- `algorithmResult` (population / externalArchive with sub-parameters)
- `createInitialSolutions`
- `offspringPopulationSize`
- `variation` (with `crossover` and `mutation` as conditional parameters)
- `selection`

Add algorithm-specific parameters at the top of the file.

### 4. Create the default configuration

**Path:** `src/main/resources/defaultConfigurations/<Name>DoubleDefault.txt`

A single line with the standard configuration:
```
--algorithmResult population --createInitialSolutions default --variation crossoverAndMutationVariation --offspringPopulationSize 100 --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2
```
Prepend the algorithm-specific parameters with their recommended default values.

### 5. Create the unit test

**Path:** `src/test/java/org/uma/evolver/algorithm/<lowercasename>/<Name>DoubleTest.java`

Follow §13 of `JAVA_CODING_GUIDELINES.md` exactly. The test must have:

- `@DisplayName("Unit tests for class Double<Name>")`
- `@BeforeEach void setUp()` instantiating with `ZDT1`, populationSize=100, maxEvals=20000
- `@Nested` "When the class constructor is called":
  - Verifies the total number of flattened parameters
  - Verifies the number of top-level parameters
- `@Nested` "When calling the parse() method":
  - Verifies parsing of the default configuration
  - Verifies algorithm-specific parameters
  - If there are variants, tests each one

To find the correct parameter counts, run the test with a placeholder value and fix it with the actual number.

### 6. Create the algorithm integration test

**Path:** `src/test/java/org/uma/evolver/algorithm/<lowercasename>/<Name>DoubleIT.java`

- `@DisplayName("Integration tests for class Double<Name>")`
- At least one `@Test` with `@Tag("integration")` that runs the algorithm on ZDT1 with the default configuration
- Verifies that the hypervolume exceeds a reasonable minimum (use 0.62 as a reference for ZDT1)
- Extract the run to a private method if there are multiple variants
- **If the algorithm includes `algorithmResult=externalArchive` with `archiveType=unboundedArchive`**,
  add an extra test that:
  - Runs the algorithm on `DTLZ1` (3 objectives) with `algorithmResult=externalArchive --archiveType unboundedArchive`
  - Verifies that `algorithm.result().size() == populationSize` (exactly 100 solutions)
  - This checks that the unbounded external archive applies distance-based subset selection
    (`BestSolutionsArchive`) rather than returning all non-dominated solutions found

### 7. Create the meta-problem integration test

**Path:** `src/test/java/org/uma/evolver/algorithm/<lowercasename>/<Name>MetaOptimizationIT.java`

Verifies that the algorithm works correctly as a base-level algorithm in both meta-problem encodings:
`MetaOptimizationProblem` (flat double vector) and `TreeMetaOptimizationProblem` (derivation tree).

Follow the pattern in `PAESMetaOptimizationIT.java`. Create one `@Nested` class per combination
(solution encoding × meta-problem encoding), each containing at least one test that:
- Builds the meta-problem with `FixedEvaluationsStrategy`
- Calls `createSolution()` + `evaluate()`
- Asserts the objective falls in a valid range (≥ 0 and ≤ 1 for normalized indicators)

**For the Double variant** (required for every algorithm):
```java
// Flat encoding (MetaOptimizationProblem)
var algo = new Double<Name>(100, new YAMLParameterSpace("<Name>Double.yaml", new DoubleParameterFactory()));
var metaProblem = new MetaOptimizationProblem<>(algo,
    List.of(new ZDT1()), List.of("resources/referenceFronts/ZDT1.csv"),
    List.of(new NormalizedHypervolume()), new FixedEvaluationsStrategy(List.of(10000)), 1);
DoubleSolution solution = metaProblem.createSolution();
metaProblem.evaluate(solution);
// assertTrue(solution.objectives()[0] >= 0.0 && solution.objectives()[0] <= 1.0)

// Tree encoding (TreeMetaOptimizationProblem) — same but with TreeSolutionGenerator
var parameterSpace = new YAMLParameterSpace("<Name>Double.yaml", new DoubleParameterFactory());
var solutionGenerator = new TreeSolutionGenerator(parameterSpace);
var metaProblem = new TreeMetaOptimizationProblem<>(algo, ..., solutionGenerator);
DerivationTreeSolution solution = metaProblem.createSolution();
metaProblem.evaluate(solution);
```

**For the Permutation variant** (if it exists):
- Use `KroAB100TSP`, front `resources/referenceFrontsTSP/KroAB100TSP.csv`, indicator `Epsilon`, 1000 evals.
- Test both `MetaOptimizationProblem` and `TreeMetaOptimizationProblem`.

**For the Binary variant** (if it exists):
- No standard reference fronts for binary problems exist in the project; skip these tests.

Run `mvn integration-test -Dit.test="<Name>MetaOptimizationIT"` and confirm all tests pass before continuing.

### 8. Create the example runner

**Path:** `src/main/java/org/uma/evolver/example/configuration/<Name>ForZDT1Example.java`

Follow the structure of `AGEMOEAForZDT1Example.java`:
1. Declare `yamlParameterSpaceFile` and `referenceFrontFileName`
2. Define `parameters` as a text block (triple quotes)
3. Create an instance with `ZDT1`, populationSize=100, maxEvals=20000
4. `.parse(parameters)` → `.build()` → `.run()`
5. Save results to `VAR.csv` and `FUN.csv`
6. Print quality indicators with `QualityIndicatorUtils.printQualityIndicators`

### 9. Verify

Run `mvn test -Dtest=<Name>DoubleTest` and fix any errors before declaring the work done.
