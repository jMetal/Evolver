Create a base-level example for the Evolver project: run one configured algorithm on one problem,
save the resulting Pareto front, and plot it.

What to run: $ARGUMENTS

A *base-level* example runs a single configured algorithm on a single problem — no meta-optimization,
no statistical study. Reference pattern:
`src/main/java/org/uma/evolver/example/baselevel/standard/AGEMOEAForZDT1Example.java`.

## Steps

### 1. Gather the inputs

Clarify (or derive from the request / repository):
- **Algorithm and encoding**: `Double<Algo>` (most), `Binary<Algo>`, or `Permutation<Algo>`.
- **Problem** (e.g. `ZDT1`, `DTLZ2`, `KroAB100TSP`) and its **reference front**:
  `resources/referenceFronts/<P>.csv` (or `<P>.3D.csv` for 3-objective DTLZ;
  `resources/referenceFrontsTSP/<P>.csv` for TSP).
- **Configuration string** and the **bucket** it belongs to:
  - `standard/` — the algorithm's typical/canonical configuration.
  - `tuned/` — a meta-optimized configuration (from a training run).
  - `features/` — demonstrates a specific capability (external archive, observers, …).
- Population size and evaluation budget (defaults: 100 / 20000).

### 2. Create the runner

**Path:** `src/main/java/org/uma/evolver/example/baselevel/<bucket>/<Name>Example.java`

Follow `AGEMOEAForZDT1Example.java`. Every value of the example is a local variable at the start of
`main`, in this order, so that they can be seen at a glance; there are no numbers or file names
further down:

1. The problem (`DoubleProblem problem = new ZDT1();`) and `referenceFrontFileName`. A problem with
   parameters names them first (`int numberOfBits = 512;`, then `new OneZeroMax(numberOfBits)`).
   Problems without a reference front (OneZeroMax) have no `referenceFrontFileName`.
2. A blank line, then `yamlParameterSpaceFile` (e.g. `AGEMOEADouble.yaml`), the algorithm's own
   inputs (`weightVectorFilesDirectory`, `referenceVectorDivisions`, …), `populationSize` (or the
   algorithm's name for it, e.g. `numberOfSolutionsToFind`) and `maximumNumberOfEvaluations`.
3. A blank line, then `parameters` as a text block (triple quotes) with the configuration.
4. Only if the example has observers: their frequencies (`evaluationObserverFrequency`,
   `chartUpdateFrequency`, `chartDisplayDelay`).

Then the procedure:

- `var baseAlgo = new Double<Algo>(problem, populationSize, maximumNumberOfEvaluations, new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));`
- `baseAlgo.parse(parameters)` → `EvolutionaryAlgorithm<DoubleSolution> algo = baseAlgo.build()` →
  observers, if any → `algo.run()`.
- Log the execution time and the number of evaluations.
- Save with `SolutionListOutput` to `FUN.csv` and `VAR.csv`.
- Log the random seed and, when there is a reference front, print the indicators:
  `QualityIndicatorUtils.printQualityIndicators(SolutionListUtils.getMatrixWithObjectiveValues(algo.result()), VectorUtils.readVectors(referenceFrontFileName, ","))`.
- `System.exit(0)` only after a run with a chart window, to close it.

For Binary/Permutation encodings use the matching factory and YAML
(`<Algo>Binary.yaml` + `BinaryParameterFactory`, `<Algo>Permutation.yaml` + `PermutationParameterFactory`).

### 3. Plot the front

Existing base-level examples only save `FUN.csv`. This skill adds the plot, using the reusable
`scripts/plot_front.py`:

```bash
python scripts/plot_front.py FUN.csv resources/referenceFronts/<P>.csv
```

It auto-detects 2D/3D (by the number of objective columns) and overlays the reference front when
given, saving a PNG next to the FUN file. To compare several fronts on the same bi-objective
problem (e.g. two configurations), use `scripts/plot_fronts.py` instead. See `scripts/README.md`
for the Python environment.

For interactive inspection instead (rotate a 3D front, hover for values), use
`scripts/plot_front_interactive.py` (Plotly) with the same arguments; it shows the figure or, with
`--output figure.html`, writes a self-contained interactive HTML.

### 4. Verify

- `mvn compile -q`.
- Run the example
  (`mvn exec:java -Dexec.mainClass=org.uma.evolver.example.baselevel.<bucket>.<Name>Example`),
  confirm `FUN.csv` is written and the printed indicators are sensible.
- Run `plot_front.py` and confirm the figure is produced.
