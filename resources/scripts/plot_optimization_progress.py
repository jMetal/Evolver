#!/usr/bin/env python3
"""
Visualization Tool for Multi-Objective Optimization Progress

This script generates a static plot showing the progression of multi-objective optimization
runs. It reads a series of FUN.*.csv files that contain objective values from different
evaluation points during the optimization process.

File Naming Convention:
    FUN.<algorithm>.<problem>.<x_label>.<y_label>.<evaluation_number>.csv
    Example: FUN.NSGA-II.DTLZ3.EP.NHV.150.csv
        - NSGA-II: Algorithm used
        - DTLZ3: Problem/benchmark name
        - EP: X-axis label (e.g., 'EP' for 'Evaluation Points')
        - NHV: Y-axis label (e.g., 'NHV' for 'Normalized Hypervolume')
        - 150: Evaluation number

Output:
    Generates a high-quality PNG image showing the progression of the Pareto front
    with different colors representing different evaluation points.

Example Usage:
    # Basic usage with default output name
    python plot_optimization_progress.py RESULTS/SPEA2/NSGAII/DTLZ3 200
    
    # Specify custom output file
    python plot_optimization_progress.py RESULTS/SPEA2/NSGAII/DTLZ3 200 --output optimization_progress.png

Dependencies:
    - Python 3.6+
    - numpy
    - matplotlib
    - pathlib
"""

import os
import re
import argparse
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path
from typing import List, Tuple, Optional
import matplotlib.cm as cm

def parse_args() -> argparse.Namespace:
    """Parse command line arguments."""
    parser = argparse.ArgumentParser(
        description='Generate a static plot showing the progression of multi-objective optimization runs.',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument(
        'data_dir',
        type=str,
        help='Directory containing the FUN.*.csv files from the optimization run'
    )
    parser.add_argument(
        'frequency',
        type=int,
        help='Plot every N evaluations (e.g., 100 will plot every 100th evaluation)'
    )
    parser.add_argument(
        '--output',
        type=str,
        default='optimization_progress.png',
        help='Output filename (supports .png, .jpg, .pdf, .svg)'
    )
    return parser.parse_args()

def get_fun_files(data_dir: str) -> List[Tuple[int, str]]:
    """
    Get all FUN.*.csv files in the directory and extract their evaluation numbers.
    
    Args:
        data_dir: Directory containing the FUN.*.csv files
        
    Returns:
        List of tuples (evaluation_number, file_path) sorted by evaluation number
    """
    pattern = re.compile(r'FUN\..*\.(\d+)\.csv$')
    files = []
    
    for f in Path(data_dir).glob('FUN.*.csv'):
        match = pattern.search(str(f))
        if match:
            eval_num = int(match.group(1))
            files.append((eval_num, str(f)))
    
    # Sort by evaluation number
    return sorted(files, key=lambda x: x[0])

def read_objectives(file_path: str) -> np.ndarray:
    """
    Read objective values from a FUN.*.csv file.
    
    Args:
        file_path: Path to the FUN.*.csv file
        
    Returns:
        Numpy array of objective values with shape (n_solutions, n_objectives)
    """
    try:
        data = np.loadtxt(file_path, delimiter=',')
        # Ensure data is 2D: (n_solutions, n_objectives)
        if data.ndim == 1:
            return data.reshape(1, -1)  # Convert to 2D with shape (1, n_objectives)
        return data
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return np.array([]).reshape(0, 2)  # Return empty 2D array

def plot_fronts(fronts_data: List[Tuple[int, np.ndarray]], ax):
    """Plot all fronts with different colors."""
    # Create a colormap with enough distinct colors
    colors = cm.rainbow(np.linspace(0, 1, len(fronts_data)))
    
    for (eval_num, objectives), color in zip(fronts_data, colors):
        if len(objectives) > 0:
            ax.scatter(objectives[:, 0], objectives[:, 1], 
                      color=color, alpha=0.6, 
                      label=f'Eval {eval_num}')
    
    # Add a colorbar to show the progression
    sm = plt.cm.ScalarMappable(cmap='rainbow', 
                              norm=plt.Normalize(vmin=0, vmax=len(fronts_data)-1))
    sm.set_array([])
    cbar = plt.colorbar(sm, ax=ax, label='Optimization Progress')
    cbar.set_ticks([0, len(fronts_data)-1])
    cbar.set_ticklabels(['Start', 'End'])

def parse_filename_info(filename: str) -> tuple:
    """Parse algorithm name, training set, x-label, and y-label from filename."""
    parts = Path(filename).name.split('.')
    if len(parts) >= 6:  # FUN.algorithm.training.x_label.y_label.xxx.csv
        algorithm = parts[1]
        training_set = parts[2]
        x_label = parts[3].replace('_', ' ')
        y_label = parts[4].replace('_', ' ')
        return algorithm, training_set, x_label, y_label
    # Default values if filename doesn't match expected format
    return "Algorithm", "Training Set", "Objective 1", "Objective 2"

def plot_optimization_progress(data_dir: str, frequency: int, output_file: str):
    """
    Create a static plot of the optimization progress.
    
    Args:
        data_dir: Directory containing FUN.*.csv files
        frequency: Plot every N evaluations
        output_file: Output file name for the plot
    """
    # Get and filter files based on frequency
    files = get_fun_files(data_dir)
    if not files:
        print(f"No FUN.*.csv files found in {data_dir}")
        return
    
    # Always include first and last files, and others based on frequency
    filtered_files = [files[0]]  # Always include first file
    for f in files[1:-1]:  # Skip first and last
        if f[0] % frequency == 0:
            filtered_files.append(f)
    if len(files) > 1:  # Always include last file if it exists
        filtered_files.append(files[-1])
    
    # Parse filename info from the first file
    algorithm, training_set, x_label, y_label = parse_filename_info(filtered_files[0][1])
    
    # Read all data
    fronts_data = [(eval_num, read_objectives(f)) for eval_num, f in filtered_files]
    
    # Skip if no valid data
    if not any(len(obj) > 0 for _, obj in fronts_data):
        print("No valid data found in any of the files.")
        return
    
    # Set up the figure
    plt.figure(figsize=(12, 8))
    ax = plt.gca()
    
    # Plot all fronts
    plot_fronts(fronts_data, ax)
    
    # Set axis labels and title
    ax.set_xlabel(x_label)
    ax.set_ylabel(y_label)
    ax.set_title(f'Optimization Progress\nAlgorithm: {algorithm} | Training Set: {training_set}')
    ax.grid(True, alpha=0.3)
    
    # Add a legend with a subset of the labels to avoid overcrowding
    handles, labels = ax.get_legend_handles_labels()
    
    # Reverse the order of handles and labels to show earliest evaluations first in legend
    handles = handles[::-1]
    labels = labels[::-1]
    
    if len(handles) > 10:  # If too many fronts, show only some key points
        step = max(1, len(handles) // 5)
        handles = handles[::step] + [handles[-1]] if handles[-1] not in handles[::step] else handles[::step]
        labels = labels[::step] + [labels[-1]] if len(labels) > 1 and labels[-1] not in labels[::step] else labels[::step]
    
    ax.legend(handles, labels, title='Evaluation', bbox_to_anchor=(1.05, 1), loc='upper left')
    
    # Adjust layout to make room for the legend
    plt.tight_layout()
    
    # Save the figure
    try:
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        print(f"Plot saved to {output_file}")
    except Exception as e:
        print(f"Error saving plot: {e}")
    
    plt.close()

def main():
    args = parse_args()
    
    if not os.path.isdir(args.data_dir):
        print(f"Error: Directory '{args.data_dir}' does not exist.")
        return
    
    if args.frequency <= 0:
        print("Error: Frequency must be a positive integer.")
        return
    
    print(f"Plotting optimization progress from {args.data_dir}")
    print(f"Plotting every {args.frequency} evaluations")
    
    # Ensure output file has .png extension
    if not args.output.endswith(('.png', '.jpg', '.jpeg', '.pdf', '.svg')):
        args.output = f"{os.path.splitext(args.output)[0]}.png"
    
    plot_optimization_progress(args.data_dir, args.frequency, args.output)

if __name__ == "__main__":
    main()
