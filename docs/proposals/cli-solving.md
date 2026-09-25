# `cli.solving`: running a configurable algorithm on a problem from the command line

**Status:** implemented (2026-09-25) on the Evolver side; the Evolver-Studio page is pending, to be
done together with the Studio solving-track tutorials. User documentation:
`docs/utilities/cli_tools.rst`.

## Motivation

Evolver-Studio has two purposes (see its `CLAUDE.md`): meta-optimization, which it drives through
`cli.training`, and **solving problems** with Evolver's configurable algorithms, in the style of
jMetal's runners — choose a problem, an algorithm and a configuration, run it, inspect the front.
The second one had no command-line entry point: a single run meant writing (or adapting) a Java
`main()` such as the examples under `org.uma.evolver.example.baselevel`. That blocked four Studio
tutorials (S3, S5, S6, S10 in `tutorials.md`) and the Studio page "Run algorithm".

`cli.solving` is to single runs what `cli.training` is to training runs: a structured request, a
runner that reuses the existing code, and a request/status/result file contract an external process
can drive. It adds a way in; it does not replace the Java API or the examples.

## Request

`SolveRunnerMain <request.yaml> [status.yaml]`, with a single, self-contained request file whose
top-level entries are the fields of the `SolveRequest` record (so that `DescribeMain` can describe
it by reflection, as it does for the training records):

```yaml
algorithmName: NSGA-II                  # as in a training run's baseLevel file
encoding: Double                        # optional, default Double
populationSize: 100
yamlParameterSpaceFile: NSGAIIDouble.yaml
extraConfig: {}                         # optional; e.g. {weightVectorFilesDirectory: resources/weightVectors}
configuration: "--algorithmResult population ..."
# or, instead of configuration:
# configurationFile: defaultConfigurations/NSGAIIDoubleDefault.txt   # first configuration is used
problem: ZDT1                           # name, or {class: <FQN>, args: [...]}
referenceFrontFileName: resources/referenceFronts/ZDT1.csv   # optional; required by indicatorNames
maxEvaluations: 25000
numberOfIndependentRuns: 1              # optional, default 1
seed: 1                                 # optional; run i uses seed + i - 1; drawn at random if absent
indicatorNames: [Epsilon, NormalizedHypervolume]             # optional
outputDirectory: results/solve/NSGA-II.ZDT1
```

The field names are those of a training run's base-level configuration wherever the meaning is the
same. `configuration` and `configurationFile` are mutually exclusive: the string is what a GUI
produces when a configuration is edited value by value, or what is copied from `VAR_CONF.txt`; the
file is convenient for the default configurations and for configurations saved after a training
run. Both end up in the same `BaseLevelAlgorithm.parse`.

## Output

In `outputDirectory`: `run-<i>/VAR.csv` and `run-<i>/FUN.csv` for each run; `INDICATORS.csv`, with
one row per run (`Run,Seed,TimeMs,<indicators>`); and `METADATA.txt`, with the settings, the
configuration used and the seeds. The indicators are computed as in a training run (non-dominated
solutions, normalized to the bounds of the reference front), so their values are comparable.

Next to the request file, as `cli.training` does: `status.yaml` (`RUNNING`/`FINISHED`/`FAILED`) and,
on success, `results.yaml` pointing at the files above. Since a single run usually takes a second
or less, the status is updated after each run, not every so many evaluations: `evaluationsDone`
counts the evaluations of the finished runs out of `numberOfIndependentRuns * maxEvaluations`, so
the same progress bar works for both tools.

## Design

- **Package `org.uma.evolver.cli.solving`**: `SolveRequest` (record), `SolveRequestYamlLoader`,
  `SolveRunner`, `SolveRunnerMain`. Independent runs are executed sequentially, each with its own
  seed, so that they are reproducible.
- **Shared pieces in `org.uma.evolver.cli`** (now public): `BaseAlgorithmRegistry`,
  `ProblemRegistry`, `ProblemSpec` (which also parses the YAML form of a problem),
  `IndicatorRegistry` and `RunStatusWriter`, moved from `cli.training`. The entry points
  Evolver-Studio uses (`TrainingRunnerMain`, `DescribeMain`) and the request formats do not change.
- **`DescribeMain`** adds the shape of the solve request (`schemas.solveRequest`).
- **Algorithms available**: whatever `BaseAlgorithmRegistry` resolves — today NSGA-II (Double,
  Permutation) and MOEA/D (Double). Extending the registry to the other algorithms and encodings
  is a separate step, which benefits `cli.training` as well.

## Evolver-Studio side (pending)

The "Run algorithm" page: pick a problem, an algorithm and encoding, and a configuration — a default
one (`defaultConfigurations/`), one found by a training run, or one edited value by value (a new
widget; `evolver_studio/parameter_space.py` already computes the active parameters of a
configuration). Launch `SolveRunnerMain` as a subprocess, as the Training page does, poll
`status.yaml`, and show the fronts and the indicators of the runs.

## Testing

`SolveRequestYamlLoaderTest` (defaults, `configuration` versus `configurationFile`, problems with
arguments, invalid requests, and every bundled request under `src/main/resources/cli/solving/`),
`SolveRunnerTest` (two runs of NSGA-II on ZDT1 with a small budget, reproducibility with the same
seed, `FAILED` status) and a `DescribeMainTest` case for the new schema.

## Out of scope

- Parallel independent runs.
- Progress within a run, and live fronts.
- Registering more algorithms (see above).
