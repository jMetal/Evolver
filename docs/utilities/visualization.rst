.. _visualization_utilities:

Visualization Utilities
=======================

This section contains visualization tools for analyzing and presenting optimization results.

plot_optimization_progress.py
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A Python script for visualizing the progression of multi-objective optimization runs.

**Location**::

    resources/scripts/plot_optimization_progress.py

**Description**:
This script generates a static plot showing the progression of optimization runs by reading a series of FUN.*.csv files that contain objective values from different evaluation points.

**File Naming Convention**:
The script expects files in the format::

    FUN.<algorithm>.<problem>.<x_label>.<y_label>.<evaluation_number>.csv

For example: ``FUN.NSGA-II.DTLZ3.EP.NHV.150.csv``

- ``NSGA-II``: Algorithm used
- ``DTLZ3``: Problem/benchmark name
- ``EP``: X-axis label (e.g., 'EP' for 'Evaluation Points')
- ``NHV``: Y-axis label (e.g., 'NHV' for 'Normalized Hypervolume')
- ``150``: Evaluation number

**Usage**::

    python resources/scripts/plot_optimization_progress.py <data_directory> <plot_frequency> [--output FILENAME]

**Arguments**:

* ``data_directory``: Directory containing the FUN.*.csv files
* ``plot_frequency``: Plot every N evaluations (e.g., 100 will plot every 100th evaluation)
* ``--output``: Output filename (default: optimization_progress.png)

**Examples**::

    # Basic usage with default output name
    python resources/scripts/plot_optimization_progress.py RESULTS/SPEA2/NSGAII/DTLZ3 200
    
    # Specify custom output file
    python resources/scripts/plot_optimization_progress.py RESULTS/SPEA2/NSGAII/DTLZ3 200 --output my_plot.png

**Dependencies**:
- Python 3.6+
- numpy
- matplotlib
- pathlib

**Output**:
Generates a high-quality image showing the progression of the Pareto front with different colors representing different evaluation points.
