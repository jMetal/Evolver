<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/figures/logo/evolver-logo-dark.svg">
    <img src="docs/figures/logo/evolver-logo.svg" alt="Evolver" width="480">
  </picture>
</p>

# Evolver: Automated meta-optimization of multi-objective metaheuristics

[![Tests](https://github.com/jMetal/Evolver/actions/workflows/tests.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/tests.yml)
[![Integration Tests](https://github.com/jMetal/Evolver/actions/workflows/integration-tests.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/integration-tests.yml)
[![Build](https://github.com/jMetal/Evolver/actions/workflows/build.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/build.yml)
[![Docs](https://github.com/jMetal/Evolver/actions/workflows/docs.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/docs.yml)
[![ReadTheDocs](https://readthedocs.org/projects/Evolver/badge/?version=latest)](https://Evolver.readthedocs.io/?badge=latest)

Full documentation is available at [evolver.readthedocs.io](https://evolver.readthedocs.io).

Evolver is a Java framework that formulates the automatic configuration of multi-objective
metaheuristics as a multi-objective optimization problem and solves it using the same class of
algorithms — a *meta-optimization* approach. It relies on the
[jMetal](https://github.com/jMetal/jMetal) framework for optimization problems, algorithms, and
quality indicators.

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

The flow is:
1. The meta-optimizer generates configurations for a base-level algorithm.
2. Each configuration is evaluated using quality indicators (Epsilon, Hypervolume, …) as objectives.
3. The process repeats until the stopping criterion is met.

## Key features

- **Automated configuration** — finds accurate parameter settings for metaheuristics automatically.
- **Flexible architecture** — supports various metaheuristics at both base and meta levels, with
  multiple encodings (Double, Binary, Permutation).
- **Multi-objective meta-level** — optimizes multiple quality indicators simultaneously.
- **YAML parameter spaces** — parameter spaces are defined in YAML and loaded via
  `YAMLParameterSpace`.
- **Derivation tree encoding** — models configurations as derivation trees, eliminating the
  inactive-variable problem of flat encodings. Includes typed subtree crossover (STGP) and a tree
  mutation operator.
- **irace integration** — base-level configuration search can also be performed with irace.
- **Usable on its own** — the configurable algorithms (`org.uma.evolver.algorithm`,
  `org.uma.evolver.parameter`) do not depend on the meta level, so Evolver can also be used as an
  alternative to jMetal to configure and run algorithms from a parameter space and a
  configuration string.

## Supported algorithms

### Base-level algorithms

Configurable parameters per algorithm and encoding, shown as **total (top-level)**:

| Algorithm | Double | Binary | Permutation |
|---|---:|---:|---:|
| NSGA-II | 32 (5) | 12 (5) | 12 (5) |
| NSGA-III | 32 (5) | — | — |
| MOEA/D | 40 (8) | 17 (8) | 17 (8) |
| SMS-EMOA | 28 (4) | 9 (4) | 9 (4) |
| MOPSO | 41 (14) | — | — |
| RDEMOEA | 40 (8) | — | 20 (8) |
| RVEA | 28 (4) | — | — |
| AGE-MOEA | 33 (6) | — | — |
| SSMOEA | 43 (6) | — | — |
| PAES | 14 (4) | 6 (4) | 6 (4) |

The figure is the number of configurable parameters (flattened, including conditional
sub-parameters — i.e. the search-space dimensionality); the value in parentheses is the number of
top-level parameters. `—` means the encoding is not available. Counts are derived from the YAML
parameter spaces in `src/main/resources/parameterSpaces/` (snapshot as of 2026-06-22; regenerate if
the spaces change).

### Meta-level algorithms

- NSGA-II
- AGE-MOEA
- Async NSGA-II
- Async Genetic Algorithm
- SMPSO
- SPEA2
- Random Search

## Requirements

- Java 21+ (JDK 21 recommended)
- Maven 3.6+

JDK 21, an LTS release, is the version used by the CI workflows. Newer JDKs can compile the
project, but some build plugins may not support them yet: SpotBugs, run by `mvn verify`, fails with
JDK 26, for instance. If several JDKs are installed, make `JAVA_HOME` point to JDK 21 (check it
with `mvn -v`).

The core framework needs nothing else. Python is optional, required only to generate analysis
figures and HTML validation reports — see [Analysis and reports](#analysis-and-reports-optional).

## Installation

```bash
git clone https://github.com/jMetal/Evolver.git
cd Evolver
mvn clean install
```

## Build and test

```bash
# Unit tests
mvn test

# Integration tests
mvn integration-test

# All tests
mvn verify
```

## Quick start

### Configuring and running an algorithm

The configurable algorithms can be used on their own. The following example runs NSGA-II on ZDT1
with a configuration chosen from the `NSGAIIDouble.yaml` parameter space:

```java
String[] configuration =
    ("--algorithmResult population --createInitialSolutions default "
        + "--offspringPopulationSize 100 --variation crossoverAndMutationVariation "
        + "--crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds "
        + "--sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 "
        + "--mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 "
        + "--selection tournament --selectionTournamentSize 2")
        .split(" ");

var parameterSpace = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
var nsgaii = new DoubleNSGAII(new ZDT1(), 100, 20000, parameterSpace);
nsgaii.parse(configuration);

EvolutionaryAlgorithm<DoubleSolution> algorithm = nsgaii.build();
algorithm.run();
```

See `org.uma.evolver.example.baselevel` for more examples, including configurations found by
meta-optimization (`example.baselevel.tuned`).

### Meta-optimizing an algorithm

The following example configures NSGA-II (base level) for DTLZ1 using NSGA-II as meta-optimizer.

```java
// 1. Define the YAML parameter space and the training set
var parameterSpace = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
List<Problem<DoubleSolution>> trainingSet = List.of(new DTLZ1());
List<String> referenceFronts = List.of("resources/referenceFronts/DTLZ1.3D.csv");

// 2. Set up the base-level algorithm to configure
var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
var baseAlgorithm = new DoubleNSGAII(100, parameterSpace);
EvaluationBudgetStrategy budget = new FixedEvaluationsStrategy(List.of(15000));

// 3. Create the meta-optimization problem
MetaOptimizationProblem<DoubleSolution> problem =
    new MetaOptimizationProblem<>(baseAlgorithm, trainingSet, referenceFronts, indicators, budget, 1);

// 4. Build and run the meta-optimizer
EvolutionaryAlgorithm<DoubleSolution> metaNSGAII =
    new MetaNSGAIIBuilder(problem, parameterSpace)
        .setMaxEvaluations(2000)
        .setNumberOfCores(8)
        .build();

var outputResults =
    new ConsolidatedOutputResults("NSGA-II", problem, "DTLZ1", indicators, "RESULTS/NSGAII/DTLZ1");
metaNSGAII.observable().register(new WriteExecutionDataToFilesObserver(1, outputResults));

metaNSGAII.run();
```

After running, the output folder holds `METADATA.txt`, `INDICATORS.csv` (the indicator values of
each configuration) and `CONFIGURATIONS.csv` (the configurations themselves).
See the examples in `org.uma.evolver.example` for complete runnable code.

## Parameter spaces

Algorithm parameter spaces are defined in YAML files under
`src/main/resources/parameterSpaces/` (e.g., `NSGAIIDouble.yaml`).
Pre-tuned default configurations live in
`src/main/resources/defaultConfigurations/`.

## Analysis and reports (optional)

The Java side writes results as CSV files (e.g., `FUN.csv` from the validation runners in
`org.uma.evolver.example.validation`). Turning those into figures and HTML reports uses the Python
scripts in [`scripts/`](scripts/):

```bash
# Option A — conda (creates the 'evolver' environment)
conda env create -f environment.yml
conda activate evolver

# Option B — virtualenv
python -m venv .venv
source .venv/bin/activate
pip install -r scripts/requirements.txt
```

See [`scripts/README.md`](scripts/README.md) for the available analyses.

## Documentation

Full documentation is available at <https://evolver.readthedocs.io>, including:

- Installation guide
- Quick start ("Evolver in 10 minutes") and step-by-step tutorials
- Examples
- Concepts (parameter spaces, evaluation strategies, base-level and meta-level metaheuristics)
- API reference
- irace integration
- FAQ and glossary

## Citation

If you use Evolver in your research, please cite:

```bibtex
@article{AND23,
  title   = {Evolver: Meta-optimizing multi-objective metaheuristics},
  journal = {SoftwareX},
  volume  = {23},
  pages   = {101551},
  year    = {2024},
  issn    = {2352-7110},
  doi     = {10.1016/j.softx.2023.101551},
}
```

## Changelog

### v2.2-SNAPSHOT

- Add `SolveRunnerMain` (`org.uma.evolver.cli.solving`), which runs a configurable algorithm on a
  problem from a YAML request (configuration inline or from a file, independent runs, reproducible
  seeds) and writes the fronts and quality indicators of each run. The registries shared with the
  training tools move to `org.uma.evolver.cli`.
- Add tutorial E4 (Evolver in 10 minutes), which replaces the former quick start: build Evolver,
  run a configurable algorithm, tune it with a short training run, and run it with the
  configuration found, all from the command line.
- Add tutorial E3 (meta-optimization workflow): tuning NSGA-II for ZDT4 from Java and from the
  command line, choosing a configuration from the training results, and comparing its front with
  that of the default configuration.
- Add `scripts/plot_fronts.py`, which plots several labelled bi-objective fronts against a reference
  front, and `scripts/plot_training_convergence.py`, which plots how each meta-objective of one or
  several training runs converges over the meta-evaluations.
- Clean up `scripts/`, which keeps only active, reusable scripts: remove the experiment-specific
  analyses (`analysis_A_hv_evolution/`, `compare_moead_vs_paes.py`, `generate_cd_plots.py`) and the
  PAES vs MOEA/D validation examples with their report script, and trim the Python dependencies
  to what the remaining scripts use.

### v2.1 (2026-09-24)

- Add derivation tree encoding (`org.uma.evolver.meta.encoding`): `DerivationTreeSolution`, `TreeNode`,
  `SubtreeCrossover` (STGP), `TreeMutation`, `TreeMetaOptimizationProblem`,
  `TreeSolutionGenerator`, and `GrammarConverter`.
- Add configurable NSGA-III, RVEA, AGE-MOEA, SSMOEA, and PAES.
- Add Binary and Permutation encodings for MOEA/D, SMS-EMOA, and PAES.
- Add Async Genetic Algorithm and Random Search meta-optimizers.
- Add `ConfigurationFileReader` to read algorithm configurations from text files.
- Remove hard-coded parameter space classes; all parameter spaces now use `YAMLParameterSpace`.
- Restructure package layout: `algorithm`, `meta`, `trainingset`, `irace`, `example`.
- Separate the configurable core (`algorithm`, `parameter`, `util`) from the meta level: the
  derivation tree encoding, training sets and training output classes move under `meta`
  (`meta.encoding`, `meta.trainingset`, `meta.output`), and meta-only algorithms under
  `meta.algorithm`. Remove unused classes, including `OutputResults` (superseded by
  `ConsolidatedOutputResults`).
- Add `cli.training`, a command-line training runner driven by YAML files: `TrainingRunnerMain`
  runs a training job described by a `request.yaml` (reusable base-level and meta-search
  configuration files) and reports its progress in a status file; `DescribeMain` prints a manifest
  of the algorithms, problems and indicators it can use. It supports the flat and tree encodings,
  Double and Permutation base-level algorithms, and any jMetal problem by class name.
- Meta-optimizers: add AGE-MOEA (flat and tree encodings); NSGA-II, AGE-MOEA and Random Search can
  now be used with the tree encoding, and SPEA2, SMPSO, Async NSGA-II and Random Search from
  `cli.training`. Meta-optimizers always generate as many offspring as their population size,
  return their final population, and use a population of 50 by default.
- Add a Tutorials section to the documentation, with runnable code in `example.tutorial`:
  E1 (parameter spaces) and E2 (base-level algorithms). The planned tutorials are listed in
  `docs/proposals/tutorials.md`.
- Add the Evolver logo, and recommend JDK 21 (the version used by CI).
- Evolver-Studio, a companion Python/Streamlit application, builds on `cli.training` and
  `DescribeMain` to explore parameter spaces, launch and monitor training runs, and follow
  interactive tutorials without writing Java code.

### v2.0 (2025-09-09)

- Complete rewrite of the original Evolver framework.
- New architecture for improved flexibility and maintainability.
- Enhanced support for meta-optimization of multi-objective metaheuristics.
- Improved documentation and examples.

## License

This project is licensed under the GNU General Public License — see the [LICENSE](LICENSE) file
for details.
