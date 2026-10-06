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
| NSGA-II | 34 (5) | 12 (5) | 12 (5) |
| NSGA-III | 32 (5) | — | — |
| MOEA/D | 41 (8) | 17 (8) | 17 (8) |
| SMS-EMOA | 33 (3) | 9 (4) | 9 (4) |
| MOPSO | 41 (14) | — | — |
| RDEMOEA | 40 (8) | — | 20 (8) |
| RVEA | 36 (6) | — | — |
| AGE-MOEA | 33 (6) | — | — |
| SSMOEA | 43 (6) | — | — |
| PAES | 14 (4) | 6 (4) | 6 (4) |

The figure is the number of configurable parameters (flattened, including conditional
sub-parameters — i.e. the search-space dimensionality); the value in parentheses is the number of
top-level parameters. `—` means the encoding is not available. Counts are derived from the YAML
parameter spaces in `src/main/resources/parameterSpaces/` (as of version 2.4): `python
scripts/plot_parameter_space.py <space>.yaml --stats` gives them for any space.

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

`mvn clean install` runs the whole test suite, integration tests included, which takes several
minutes. To just build the JAR with all its dependencies (in `target/`), skip the tests:

```bash
mvn -DskipTests package
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

The quickest way to try Evolver needs no code: the tutorial
[Evolver in 10 minutes](https://evolver.readthedocs.io/en/latest/quick_start.html) builds it, runs
a configurable algorithm and tunes it from the command line. For instance, this runs a short
training run that tunes NSGA-II for ZDT4 (about half a minute with 8 cores), from the root of the
repository:

```bash
JAR=$(ls target/Evolver-*-jar-with-dependencies.jar)
mkdir -p results/quick-start
cp src/main/resources/cli/training/tutorial-quick-start-request.yaml results/quick-start/request.yaml
java -cp "$JAR" org.uma.evolver.cli.training.TrainingRunnerMain results/quick-start/request.yaml
```

From Java, the configurable algorithms and the meta-optimizers are used as the next two examples
show.

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

The following example tunes NSGA-II (base level) for DTLZ1, with NSGA-II as meta-optimizer. A
training run is described by two YAML texts, the same ones the command-line tools read from files:
what to tune and on what training set, and how the meta-optimizer searches. `TrainingRunner` runs
it and writes the results.

```java
// 1. What to tune (NSGA-II and its parameter space), on what training set, and the quality
//    indicators the meta-optimizer minimizes
BaseLevelConfig baseLevel =
    BaseLevelConfigurationReader.loadFromYaml(
        """
        algorithmName: NSGA-II
        populationSize: 100
        numberOfIndependentRuns: 1
        yamlParameterSpaceFile: NSGAIIDouble.yaml
        trainingProblemNames: [DTLZ1]
        trainingReferenceFrontFileNames: [resources/referenceFronts/DTLZ1.3D.csv]
        trainingEvaluations: [15000]
        indicatorNames: [Epsilon, NormalizedHypervolume]
        """);

// 2. The meta-optimizer: NSGA-II, trying 2000 configurations, 8 at a time
MetaSearchConfig metaSearch =
    MetaOptimizerConfigurationReader.loadFromYaml(
        """
        algorithm: NSGA-II
        encoding: flat
        metaMaxEvaluations: 2000        # or metaMaxComputingTimeMinutes: 20, to stop by time
        metaPopulationSize: 50
        numberOfCores: 8
        crossover: SBX
        crossoverProbability: 0.9
        crossoverRepairStrategy: bounds
        sbxDistributionIndex: 20.0
        mutation: polynomial
        mutationProbabilityFactor: 1.0
        mutationRepairStrategy: bounds
        polynomialMutationDistributionIndex: 20.0
        selection: tournament
        selectionTournamentSize: 2
        """);

// 3. Run it, writing the results every 100 meta-evaluations
String outputDirectory = "results/NSGAII/DTLZ1";
var request = new TrainingRequest(baseLevel, metaSearch, outputDirectory, 100, 100, null);
new TrainingRunner().run(request, Path.of(outputDirectory, "status.yaml"));
```

After running, the output folder holds, for the non-dominated configurations found at every
checkpoint: `METADATA.txt` (the settings of the run), `INDICATORS.csv` (their indicator values),
`CONFIGURATIONS.csv` (their parameter values) and `VAR_CONF.txt` (their configuration strings,
ready to be used). The examples in `org.uma.evolver.example.training` follow this pattern, and the
tutorials of the documentation explain each part.

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
- Tuning with irace (tutorial E15)
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

The changes of each version are listed in the
[changelog](https://evolver.readthedocs.io/en/latest/changelog.html) of the documentation
([`docs/changelog.rst`](docs/changelog.rst)).

## License

This project is licensed under the GNU General Public License — see the [LICENSE](LICENSE) file
for details.
