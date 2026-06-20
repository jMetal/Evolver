Create a validation study for the Evolver project: a Java runner that executes given algorithm
configurations on a set of problems and saves the Pareto fronts, plus a Python script that builds
an HTML report comparing the fronts.

What to validate: $ARGUMENTS

A *validation* (`*Validation`, run + save fronts) is different from a *study* (`*Study`,
statistical comparison with `ExperimentBuilder`). Use this skill for the run + save + plot pattern.

## Steps

### 1. Gather the inputs

Clarify (ask the user only if not derivable from the request or the repository):
- **Algorithms and their configurations**: each configuration is a hardcoded parameter string.
  Sources are usually:
  - Best configurations from a meta-optimization experiment under
    `experimentation/training/<EXP>/` (look for the configuration with the best indicator, e.g.
    minimum HVMinus at the target meta-evaluation, in the `VAR_CONF.txt` / results files).
  - Default configurations in `src/main/resources/defaultConfigurations/<Algo>DoubleDefault.txt`
    (useful as a reference baseline).
- **Problem set** and number of objectives (e.g. DTLZ1–7 / RE / RWA / ZDT).
- **Evaluation budget** (stopping criterion, e.g. 40 000 evaluations).
- **A short study label** used for the output directory and file names.

### 2. Create the Java runner

**Path:** `src/main/java/org/uma/evolver/example/validation/<Name>Validation.java`

**Reference pattern:** `PAESvsMOEADDTLZValidation.java` (same package).

Structure:
- Constants:
  ```java
  private static final int MAX_EVALUATIONS = 40_000;
  private static final int POPULATION_SIZE = 100;
  private static final String WEIGHT_VECTORS_DIR = "resources/weightVectors"; // only if MOEA/D
  private static final String OUTPUT_DIR = "results/validation/<Name>/";
  ```
- One config string per algorithm/configuration, built with `String.join(" ", "--param value", …)`.
  Add a comment stating where each config comes from (experiment + indicator, or default file).
- A `PROBLEMS` list with the problems instantiated inline:
  ```java
  private static final List<Problem<DoubleSolution>> PROBLEMS =
      List.of(new DTLZ1(), new DTLZ2(), /* … */);
  ```
- `main()` loops over problems and calls `runAndSave(problem, label, config)` once per
  configuration.
- `runAndSave()` selects the algorithm by label and instantiates it. **MOEA/D uses the 5-arg
  constructor** (problem, popSize, maxEvals, weightVectorsDir, parameterSpace); other algorithms
  use the 4-arg constructor (problem, popSize, maxEvals, parameterSpace). Then `parse(config)`,
  `build()`, `run()`, and write only the FUN file:
  ```java
  String dir = OUTPUT_DIR + problem.name() + "/";
  new File(dir).mkdirs();
  new SolutionListOutput(algorithm.result())
      .setFunFileOutputContext(new DefaultFileOutputContext(dir + label + "_FUN.csv", ","))
      .print();
  ```

Output layout: `results/validation/<Name>/<ProblemName>/<label>_FUN.csv`.
(`results/` is gitignored; only the runner under `example/validation` is committed.)

### 3. Create the Python report script

**Path:** `scripts/plot_<name>_validation.py`

**Reference pattern:** `scripts/plot_dtlz_validation.py`.

`scripts/` is versioned, so commit this script together with the Java runner — it is reproducible
tooling, not a throwaway. Requirements (already in `scripts/requirements.txt`): `pandas`, `numpy`,
`matplotlib`, `seaborn`. See the README "Analysis and reports" section for how to set up the
Python environment (conda or venv).

Key points:
- Use a headless backend and embed plots as base64 PNG (avoids the WebGL/Plotly context limit
  for many subplots):
  ```python
  import matplotlib
  matplotlib.use("Agg")
  ```
- One row per problem; columns are **Reference + one per configuration** (the `COLUMNS` list maps
  display label → FUN-file key → color; the first column is the reference front).
- **Auto-detect 2D vs 3D** by the number of columns of the reference front: use a 2D scatter for
  2 objectives and a 3D scatter (`projection="3d"`) for 3. Share per-axis ranges across columns
  using the reference front (`axis_ranges`).
- Reference-front file name depends on the benchmark: 3D DTLZ uses `<name>.3D.csv`; RE/RWA/ZDT use
  `<name>.csv`. Make this explicit in `ref_path`.
- Assemble a single self-contained HTML file (inline CSS, a colored legend, one `<img>` per
  problem). Handle missing FUN files gracefully ("no data").
- CLI:
  ```
  python scripts/plot_<name>_validation.py <results_dir> <ref_dir> [--output report.html]
  ```

### 4. Run the validation and build the report

```bash
# 1. Run the Java validation (generates the FUN.csv files) — no Python needed
mvn compile exec:java -Dexec.mainClass=org.uma.evolver.example.validation.<Name>Validation

# 2. Activate the Python environment (see README "Analysis and reports"), then generate the report
python scripts/plot_<name>_validation.py \
    results/validation/<Name>/ resources/referenceFronts/
# (without activating: conda run -n evolver python scripts/plot_<name>_validation.py …)
```

The report is written to `results/validation/<Name>/report.html`.

### 5. Verify

- Confirm the Java runner compiles: `mvn compile -q`.
- Confirm a FUN file exists for each problem/configuration under the output directory.
- Confirm the HTML report was generated and references every problem.
- Reference each generated file in the summary with clickable paths.
