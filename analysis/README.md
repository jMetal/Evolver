# Evolver Analysis Tools

Python-based analysis tools for meta-optimization results from Evolver experiments.

## Overview

This module provides tools for analyzing the output of meta-optimization experiments, including:

- **Correlation Analysis**: Identify which parameters most strongly correlate with performance
- **PCA Analysis**: Reduce dimensionality and visualize the configuration space
- **Convergence Analysis**: Track how parameters and objectives evolve over evaluations
- **Parameter Importance Ranking**: Rank parameters by their impact on algorithm performance

## Installation

### Using pip (recommended)

```bash
cd analysis
pip install -e .
```

### Using pip with optional dependencies

```bash
# For interactive visualizations
pip install -e ".[interactive]"

# For development
pip install -e ".[dev]"
```

## Usage

### Command Line Script

```bash
# Analyze results from a specific experiment
python meta_optimization_analysis.py --input ../RESULTS/NSGAII/DTLZ3 --output ./output

# Or using the installed command
evolver-analyze --input ../RESULTS/NSGAII/DTLZ3 --output ./output
```

### Jupyter Notebook

For interactive analysis:

```bash
cd analysis/notebooks
jupyter lab meta_optimization_analysis.ipynb
```

## Input Data Format

The analysis tools expect the following file structure in the results directory:

```
RESULTS/ALGORITHM/PROBLEM/
├── FUN.*.{evaluation}.csv          # Objective values (Epsilon, NormHV)
├── VAR.*.{evaluation}.csv          # Encoded parameter values
├── VAR.*.Conf.{evaluation}.txt     # Human-readable configuration
└── VAR.*.Conf.DoubleValues.{evaluation}.csv  # Configuration with headers
```

## Output

The analysis generates:

| File | Description |
|------|-------------|
| `parameter_correlations.csv` | Spearman correlations between parameters and objectives |
| `pca_loadings.csv` | PCA component loadings for all parameters |
| `correlation_heatmap.png` | Visual correlation matrix |
| `pca_variance.png` | Scree plot and cumulative variance |
| `pca_scatter.png` | Configuration space in PC1-PC2 |
| `parameter_convergence.png` | Evolution of top parameters |
| `objective_convergence.png` | Objective convergence over evaluations |
| `categorical_analysis.png` | Impact of categorical parameters |
| `analysis_report.md` | Summary report |

## Example Results

After running the analysis, you'll get insights like:

```
Top 5 most influential parameters:
  1. offspringPopulationSize: |corr| = 0.523
  2. sbxDistributionIndex: |corr| = 0.412
  3. mutationProbabilityFactor: |corr| = 0.387
  4. crossoverProbability: |corr| = 0.356
  5. uniformMutationPerturbation: |corr| = 0.298

PCA: 8 components explain 90% of variance (out of 25 parameters)
```

## Integration with Evolver (Java)

These Python tools are designed to work alongside the Java-based Evolver framework:

1. **Run experiments** using Evolver (Java) → generates `RESULTS/` data
2. **Analyze results** using these Python tools
3. **Insights** feed back into experiment design

For automated analysis from Java:

```java
ProcessBuilder pb = new ProcessBuilder(
    "python", "analysis/meta_optimization_analysis.py",
    "--input", "RESULTS/NSGAII/DTLZ3",
    "--output", "analysis/output"
);
pb.inheritIO();
pb.start().waitFor();
```

## Dependencies

- Python >= 3.10
- NumPy, Pandas, Matplotlib, Seaborn
- scikit-learn (for PCA)
- SciPy (for statistical tests)
- Jupyter (for interactive notebooks)

See `pyproject.toml` for full dependency list.
