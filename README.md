# Evolver: Automated meta-optimization of multi-objective metaheuristics

Evolver is a Java framework that formulates the automatic configuration of multi-objective
metaheuristics as a multi-objective optimization problem and solves it using the same class of
algorithms — a *meta-optimization* approach.

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

## Supported base-level algorithms

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

## Requirements

- Java 21+
- Maven 3.6+

The core framework needs nothing else. Python is optional, required only to generate analysis
figures and HTML validation reports — see [Analysis and reports](#analysis-and-reports-optional).

## Build and test

```bash
# Build
mvn clean install

# Unit tests
mvn test

# Integration tests
mvn integration-test

# All tests
mvn verify
```

## Parameter spaces

Algorithm parameter spaces are defined in YAML files under
`src/main/resources/parameterSpaces/` (e.g., `NSGAIIDouble.yaml`).
Pre-tuned default configurations live in
`src/main/resources/defaultConfigurations/`.

## Analysis and reports (optional)

The Java side runs everything and writes results as CSV — for example, the validation runners
under `org.uma.evolver.example.validation` produce `FUN.csv` files. Turning those into figures and
HTML reports uses the Python scripts in [`scripts/`](scripts/), which need a Python environment
(only for this optional step):

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

## Citation

If you use Evolver in your research, please cite:

> Evolver: Meta-optimizing multi-objective metaheuristics.
> SoftwareX, 2023. <https://doi.org/10.1016/j.softx.2023.101551>
