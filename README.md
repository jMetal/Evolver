# Evolver: Automated meta-optimization of multi-objective metaheuristics

[![Tests](https://github.com/jMetal/Evolver/actions/workflows/tests.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/tests.yml)
[![Integration Tests](https://github.com/jMetal/Evolver/actions/workflows/integration-tests.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/integration-tests.yml)
[![Build](https://github.com/jMetal/Evolver/actions/workflows/build.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/build.yml)
[![Docs](https://github.com/jMetal/Evolver/actions/workflows/docs.yml/badge.svg)](https://github.com/jMetal/Evolver/actions/workflows/docs.yml)
[![ReadTheDocs](https://readthedocs.org/projects/Evolver/badge/?version=latest)](https://Evolver.readthedocs.io/?badge=latest)

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
- Async NSGA-II
- Async Genetic Algorithm
- SMPSO
- SPEA2
- Random Search

## Requirements

- Java 21+
- Maven 3.6+

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

var outputResults = new OutputResults("NSGA-II", problem, "DTLZ1", indicators, "RESULTS/NSGAII/DTLZ1");
metaNSGAII.observable().register(new WriteExecutionDataToFilesObserver(1, outputResults));

metaNSGAII.run();
```

After running, the best configuration is written to the output folder as a `VAR.*.txt` file.
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
- Quick start and examples
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

### v2.1-SNAPSHOT

- Add derivation tree encoding (`org.uma.evolver.encoding`): `DerivationTreeSolution`, `TreeNode`,
  `SubtreeCrossover` (STGP), `TreeMutation`, `TreeMetaOptimizationProblem`,
  `TreeSolutionGenerator`, and `GrammarConverter`.
- Add configurable NSGA-III, RVEA, AGE-MOEA, SSMOEA, and PAES.
- Add Binary and Permutation encodings for MOEA/D, SMS-EMOA, and PAES.
- Add Async Genetic Algorithm and Random Search meta-optimizers.
- Add `ConfigurationFileReader` to read algorithm configurations from text files.
- Remove hard-coded parameter space classes; all parameter spaces now use `YAMLParameterSpace`.
- Restructure package layout: `algorithm`, `meta`, `trainingset`, `irace`, `example`.

### v2.0 (2025-09-09)

- Complete rewrite of the original Evolver framework.
- New architecture for improved flexibility and maintainability.
- Enhanced support for meta-optimization of multi-objective metaheuristics.
- Improved documentation and examples.

## License

This project is licensed under the GNU General Public License — see the [LICENSE](LICENSE) file
for details.
