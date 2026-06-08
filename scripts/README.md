# Scripts for Evolver Experimental Analysis

This directory contains Python scripts for analyzing the results of Evolver meta-optimization experiments, designed for scientific publication.

## Environment Setup

```bash
# Activate conda environment
conda activate evolver

# Install/update dependencies
pip install -r requirements.txt
```

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
- **scipy**: Statistical tests and scientific computing
- **scikit-learn**: Machine learning analyses (clustering, feature importance)
