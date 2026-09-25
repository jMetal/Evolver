# Scripts for Evolver

Python scripts that turn Evolver's results into figures. The core Evolver framework (Java + Maven)
needs no Python: these scripts are optional. Only active, reusable scripts live here; one-off
analyses of a particular experiment belong with that experiment's data, not in this directory.

## Environment setup

Run the setup **from the repository root**:

```bash
# Option A — conda (creates the 'evolver' environment)
conda env create -f environment.yml
conda activate evolver

# Option B — virtualenv
python -m venv .venv
source .venv/bin/activate
pip install -r scripts/requirements.txt
```

## Scripts

| Script | What it does |
|---|---|
| `plot_front.py` | Plots one front (a `FUN.csv`) against its reference front: 2D, 3D, or parallel coordinates for more objectives. Static PNG (matplotlib). |
| `plot_front_interactive.py` | Same as `plot_front.py`, as an interactive Plotly figure (in the browser, or a self-contained HTML file). |
| `plot_fronts.py` | Plots several labelled bi-objective fronts against a reference front, e.g. the fronts of different configurations of an algorithm on the same problem: one panel per front with shared axes (default), or all of them on a single panel (`--mode overlay`). |
| `plot_training_convergence.py` | Plots how each meta-objective of a training run converges over the meta-evaluations: median and best–worst band of the configurations on the meta-optimizer's front at each checkpoint, pooled over replications, and the meta-evaluation at which 95% of the improvement is reached. |
| `plot_parameter_space.py` | Prints a parameter space YAML file as a text tree, or draws it as a compact figure. |

Run any script without arguments (or with `--help`) for its full usage.

### Fronts

```bash
# one front over its reference front (overlay by default; --mode side|both for panels)
python scripts/plot_front.py FUN.csv resources/referenceFronts/DTLZ2.3D.csv
python scripts/plot_front_interactive.py FUN.csv resources/referenceFronts/DTLZ2.3D.csv --output front.html

# several labelled fronts over the reference front (bi-objective)
python scripts/plot_fronts.py resources/referenceFronts/ZDT4.csv \
    --front "Default=path/to/default/FUN.csv" --front "Tuned=path/to/tuned/FUN.csv" \
    --output fronts.png
```

The reference front is always passed explicitly: reference fronts follow no single naming
convention (`DTLZ1.3D.csv` vs `RE31.csv`), so the file name is not guessed from the problem.

### Training convergence

```bash
# one training run (the output directory of a training, with INDICATORS.csv)
python scripts/plot_training_convergence.py results/tutorial/E3 --primary NHV

# several replications: a directory with one training output directory per replication
python scripts/plot_training_convergence.py path/to/campaign --primary IGD+ --output-dir plots
```

It writes one `convergence_<indicator>.png` per meta-objective and prints the final value of the
primary indicator and when 95% of its improvement was reached.

### Parameter spaces

```bash
python scripts/plot_parameter_space.py src/main/resources/parameterSpaces/NSGAIIDouble.yaml --depth 3
```

## Dependencies

`requirements.txt` (and `../environment.yml`): pandas, numpy and matplotlib for every script, plus
Plotly for `plot_front_interactive.py` and PyYAML for `plot_parameter_space.py`.
