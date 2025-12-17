# Optimization Progress Visualization

This directory contains scripts for visualizing multi-objective optimization progress.

## plot_optimization_progress.py

A Python script that generates static plots showing the progression of multi-objective optimization runs using the new structured data format.

### Requirements

```bash
pip install numpy matplotlib pandas
```

### Usage

#### Basic Usage
```bash
python plot_optimization_progress.py results/nsgaii/ZDT 200
```

#### List Available Indicators
```bash
python plot_optimization_progress.py results/nsgaii/ZDT 100 --list-indicators
```

#### Custom Indicators and Output
```bash
python plot_optimization_progress.py results/nsgaii/DTLZ3D 300 \
    --indicator1 EP \
    --indicator2 "IGD+" \
    --output dtlz3d_progress.png
```

#### With Confidence Intervals
```bash
# Show quartile ranges (25th-75th percentile)
python plot_optimization_progress.py results/nsgaii/ZDT 200 \
    --show-range --range-type quartiles

# Show full min-max range
python plot_optimization_progress.py results/nsgaii/ZDT 200 \
    --show-range --range-type minmax

# Show standard deviation intervals
python plot_optimization_progress.py results/nsgaii/ZDT 200 \
    --show-range --range-type std
```

### Arguments

- `data_dir`: Directory containing INDICATORS.csv, CONFIGURATIONS.csv, and METADATA.txt files
- `frequency`: Plot every N evaluations (e.g., 200 will plot every 200th evaluation)
- `--output`: Output filename (default: optimization_progress.png)
- `--indicator1`: Quality indicator for left Y-axis (default: EP)
- `--indicator2`: Quality indicator for right Y-axis (default: NHV)
- `--aggregation`: Central tendency method for multiple values per evaluation (default: median)
- `--show-range`: Show confidence intervals/error bars representing the range of values
- `--range-type`: Type of range to show: minmax, quartiles, std (default: quartiles)
- `--list-indicators`: List available indicators and exit

### Data Format

The script expects the new structured data format:

- **INDICATORS.csv**: Contains quality indicator values (EP, NHV, IGD+, etc.) for each evaluation and solution
- **CONFIGURATIONS.csv**: Contains algorithm parameter configurations for each solution
- **METADATA.txt**: Contains experiment metadata (algorithm, training set, etc.)
- **VAR_CONF.txt**: Contains variable configurations

### Output

Generates a high-quality PNG image showing:
- Time series plot with evaluation numbers on X-axis
- Two quality indicators on dual Y-axes (left and right)
- Clear trend lines showing optimization progress over time
- Optional confidence intervals (shaded areas) showing variability when multiple solutions exist
- Algorithm and training set information in the title
- Legend identifying each indicator and axis

#### Confidence Intervals
When `--show-range` is enabled, the plot shows:
- **Median line**: Central tendency of indicator values at each evaluation
- **Shaded areas**: Confidence intervals showing the range/variability of values
- **Range types**:
  - `quartiles`: 25th to 75th percentile (recommended for most cases)
  - `minmax`: Full range from minimum to maximum values
  - `std`: Mean ± one standard deviation

### Examples

#### ZDT Training Set (EP and NHV over time)
```bash
# Basic plot with median values
python plot_optimization_progress.py results/nsgaii/ZDT 200

# With quartile confidence intervals
python plot_optimization_progress.py results/nsgaii/ZDT 200 --show-range
```

#### DTLZ3D Training Set (EP and IGD+ over time)
```bash
# Basic plot
python plot_optimization_progress.py results/nsgaii/DTLZ3D 300 \
    --indicator1 EP --indicator2 "IGD+"

# With full range intervals
python plot_optimization_progress.py results/nsgaii/DTLZ3D 300 \
    --indicator1 EP --indicator2 "IGD+" --show-range --range-type minmax
```

### Key Features

#### Time Series Visualization
- **X-axis**: Evaluation numbers showing optimization progress over time
- **Dual Y-axes**: Two quality indicators plotted simultaneously
- **Scalable**: Handles thousands of evaluations without cluttered legends
- **Clear trends**: Shows convergence patterns and optimization dynamics

#### Smart Data Processing
- **Best value tracking**: Shows minimum (best) indicator values at each evaluation
- **Frequency filtering**: Plot every N evaluations to reduce noise
- **Automatic discovery**: Lists available indicators in datasets

### Migration from Old Format

The script has been completely refactored twice:

1. **First refactor**: Support new structured data format (INDICATORS.csv) instead of old `FUN.*.csv` files
2. **Second refactor**: Time series visualization with dual Y-axes instead of scatter plots

**Benefits of new approach**:
- Scalable to thousands of evaluations
- Clear trend visualization over time
- No cluttered legends with hundreds of points
- Better understanding of optimization dynamics
- Dual indicators show trade-offs and convergence patterns
- **Confidence intervals**: Show variability and uncertainty in optimization process
- **Multiple aggregation methods**: Choose between min, max, mean, median, std
- **Flexible range visualization**: Quartiles, min-max, or standard deviation intervals

If you have old `FUN.*.csv` files, you'll need to convert them to the new format or use an older version of this script.