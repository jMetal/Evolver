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
    --x-indicator EP \
    --y-indicator "IGD+" \
    --output dtlz3d_progress.png
```

### Arguments

- `data_dir`: Directory containing INDICATORS.csv, CONFIGURATIONS.csv, and METADATA.txt files
- `frequency`: Plot every N evaluations (e.g., 200 will plot every 200th evaluation)
- `--output`: Output filename (default: optimization_progress.png)
- `--x-indicator`: Quality indicator for X-axis (default: EP)
- `--y-indicator`: Quality indicator for Y-axis (default: NHV)
- `--list-indicators`: List available indicators and exit

### Data Format

The script expects the new structured data format:

- **INDICATORS.csv**: Contains quality indicator values (EP, NHV, IGD+, etc.) for each evaluation and solution
- **CONFIGURATIONS.csv**: Contains algorithm parameter configurations for each solution
- **METADATA.txt**: Contains experiment metadata (algorithm, training set, etc.)
- **VAR_CONF.txt**: Contains variable configurations

### Output

Generates a high-quality PNG image showing:
- Quality indicator values plotted as scatter points
- Different colors for different evaluation points
- Legend showing evaluation numbers
- Algorithm and training set information in the title

### Examples

#### ZDT Training Set (EP vs NHV)
```bash
python plot_optimization_progress.py results/nsgaii/ZDT 200
```

#### DTLZ3D Training Set (EP vs IGD+)
```bash
python plot_optimization_progress.py results/nsgaii/DTLZ3D 300 \
    --x-indicator EP --y-indicator "IGD+"
```

### Migration from Old Format

The script has been completely refactored to work with the new structured data format instead of the old `FUN.*.csv` files. The new format provides:

- Better organization of data
- Support for multiple quality indicators
- Metadata about experiments
- Algorithm configuration tracking

If you have old `FUN.*.csv` files, you'll need to convert them to the new format or use an older version of this script.