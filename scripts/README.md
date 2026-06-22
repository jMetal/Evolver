# Scripts for Evolver Experimental Analysis

Python scripts for turning Evolver experiment and validation results into figures and reports. The
core Evolver framework (Java + Maven) needs no Python — these scripts are optional.

## Environment Setup

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

## Front-plotting scripts (general-purpose)

Two reusable scripts plot a single Pareto front (a `FUN.csv`) against its reference front. They take
the same arguments and comparison modes; pick by purpose:

| Script | Engine | Output | Use for |
|---|---|---|---|
| `plot_front.py` | matplotlib | static PNG (headless) | reports, automation, embedding |
| `plot_front_interactive.py` | Plotly | interactive (browser, or self-contained HTML with `--output`) | manual exploration: rotate 3D, hover |

```bash
# static, overlay (default): obtained front over its reference
python scripts/plot_front.py FUN.csv resources/referenceFronts/DTLZ2.3D.csv

# side-by-side panels (clearer in 3D); 'both' adds an overlay panel
python scripts/plot_front.py FUN.csv resources/referenceFronts/DTLZ2.3D.csv --mode side

# interactive, saved as a self-contained HTML
python scripts/plot_front_interactive.py FUN.csv resources/referenceFronts/DTLZ2.3D.csv --output front.html
```

Notes:
- Objectives are auto-detected from the column count (2 → 2D, 3 → 3D, >3 → parallel coordinates).
- `--mode overlay|side|both` (default `overlay`); `side`/`both` share axis ranges across panels.
- The reference front is passed **explicitly** (its exact file). Reference fronts follow no single
  naming convention (`DTLZ1.3D.csv` vs `RE31.csv`), so the filename is not guessed from the problem.
- Run either script without arguments to print the full help.

## Study statistics

- `generate_cd_plots.py` — Critical Difference plots (Friedman + Nemenyi post-hoc) from a study's
  `QualityIndicatorSummary.csv`. Set the `RESULTS_DIR` at the top of the file to the study directory.

## Experiment-specific analysis

### Experiment A: HV evolution

- **Directory**: `analysis_A_hv_evolution/` (has its own `README.md`).
- **Purpose**: hypervolume convergence comparison figures and statistics.
- **Entry points**: `analysis_A_hv_evolution.py`, `generate_figures_and_stats.py`,
  `postprocess_analysis.py`.

## Directory layout

```
scripts/
├── README.md                     # this file
├── requirements.txt              # Python dependencies
├── plot_front.py                 # single-front plotter (matplotlib, static)
├── plot_front_interactive.py     # single-front plotter (Plotly, interactive)
├── generate_cd_plots.py          # Critical Difference plots from a study summary
├── analysis_A_hv_evolution/      # HV evolution analysis (own README)
└── figures/                      # shared figures output
```

## Dependencies

Declared in `requirements.txt` / `environment.yml`:

- **pandas**, **numpy** — data manipulation
- **matplotlib**, **seaborn** — static figures
- **plotly** — interactive figures (`plot_front_interactive.py`)
- **scipy** — statistical tests
- **scikit-learn** — clustering / feature-importance analyses
