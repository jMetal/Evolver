# Scripts for Evolver Experimental Analysis

This directory contains Python scripts for analyzing the results of Evolver meta-optimization experiments, designed for scientific publication.

## Environment Setup

The core Evolver framework (Java + Maven) needs no Python. These scripts are only for turning
experiment and validation results into figures and reports. Run the setup **from the repository
root**:

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

## Available Analyses

### Experiment A: HV Evolution Analysis
- **Script**: `analysis_A_hv_evolution.py`
- **Purpose**: Generate hypervolume convergence comparison figures
- **Output**: `figures/hv_comparison_convergence.png`
- **Related**: Experiment A in `../experimentation/EXPERIMENTAL_DESIGN.md`

### Future Scripts (Planned)
- `analysis_B_statistical.py` - Statistical comparison between reference front types
- `analysis_C_convergence.py` - Convergence threshold analysis
- `analysis_D_configurations.py` - Configuration parameter analysis (D1-D5)
- `analysis_E_representative.py` - Representative configuration extraction
- `analysis_F_cost_quality.py` - Cost-quality trade-off analysis

## Directory Structure

```
scripts/
├── README.md                    # This file
├── requirements.txt             # Core Python dependencies
├── analysis_A_hv_evolution/     # HV evolution analysis
│   ├── README.md               # Analysis-specific documentation
│   ├── analysis_A_hv_evolution.py
│   ├── hv_comparison_convergence.png
│   └── requirements.txt        # Analysis-specific dependencies
├── analysis_B_statistical/     # Statistical comparison (planned)
├── analysis_C_convergence/     # Convergence analysis (planned)
├── analysis_D_configurations/   # Configuration analysis (planned)
├── analysis_E_representative/  # Representative configurations (planned)
├── analysis_F_cost_quality/     # Cost-quality analysis (planned)
├── figures/                     # Legacy figures directory
│   └── README.md
└── utils/                       # Shared utilities (future)
    ├── data_parser.py
    ├── visualization.py
    └── statistical_tests.py
```

## Data Sources

All scripts expect data in the standardized structure:
- **Training data**: `../experimentation/training/referenceFronts/` and `../experimentation/training/extremePoints/`
- **Reference fronts**: `../experimentation/config/referenceFronts/`
- **Results**: Output saved to `figures/` subdirectory

## Usage Pattern

```bash
# Run individual analysis
cd analysis_A_hv_evolution
python analysis_A_hv_evolution.py

# Run all analyses (when implemented)
python run_all_analyses.py
```

## Publication Notes

- All figures are generated at 300 DPI for journal submission
- Color schemes are colorblind-friendly
- Statistical significance follows the standards in the field
- Scripts are version-controlled for reproducibility

## Dependencies

Core libraries:
- **pandas**: Data manipulation and aggregation
- **numpy**: Numerical computations
- **matplotlib**: Publication-quality figures
- **plotly**: Interactive figures (`plot_front_interactive.py`)
- **scipy**: Statistical tests and scientific computing
- **scikit-learn**: Machine learning analyses (clustering, feature importance)
