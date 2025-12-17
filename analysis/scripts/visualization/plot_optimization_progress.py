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

def get_fun_files(data_dir: str, frequency: Optional[int] = None) -> List[Tuple[int, str]]:
    """
    Get all FUN.*.csv files in the directory and extract their evaluation numbers.
    
    Args:
        data_dir: Directory containing the FUN.*.csv files
        frequency: If provided, checks that files exist for multiples of this frequency
        
    Returns:
        List of tuples (evaluation_number, file_path) sorted by evaluation number
        
    Raises:
        ValueError: If required frequency multiples are missing
    """
    print(f"\nSearching for files in: {data_dir}")
    print(f"Requested frequency: {frequency} evaluations")
    
    pattern = re.compile(r'FUN\..*\.(\d+)\.csv$')
    files = []
    evaluation_numbers = set()
    
    # First pass: collect all files and their evaluation numbers
    for f in Path(data_dir).glob('FUN.*.csv'):
        match = pattern.search(str(f))
        if match:
            eval_num = int(match.group(1))
            evaluation_numbers.add(eval_num)
            files.append((eval_num, str(f)))
    
    if not files:
        return []
        
    # If frequency is provided, filter files to only include the specified frequency points
    if frequency is not None and frequency > 0:
        print("\nAll available evaluation points:", sorted(evaluation_numbers))
        first_eval = min(evaluation_numbers)
        filtered_files = []
        
        # Always include the first evaluation
        if first_eval in evaluation_numbers:
            file_path = next(f[1] for f in files if f[0] == first_eval)
            filtered_files.append((first_eval, file_path))
            print(f"Including first evaluation: {first_eval} - {file_path}")
        
        # Include files at the specified frequency intervals
        max_eval = max(evaluation_numbers)
        for eval_num in range(frequency, max_eval + 1, frequency):
            if eval_num in evaluation_numbers and eval_num != first_eval:  # Skip if it's the first eval we already added
                file_path = next(f[1] for f in files if f[0] == eval_num)
                filtered_files.append((eval_num, file_path))
                print(f"Including frequency point: {eval_num} - {file_path}")
        
        # If the last evaluation doesn't align with the frequency, include it
        last_eval = max(evaluation_numbers)
        if last_eval % frequency != 0 and last_eval != first_eval:
            if last_eval in evaluation_numbers:
                file_path = next(f[1] for f in files if f[0] == last_eval)
                filtered_files.append((last_eval, file_path))
                print(f"Including last evaluation: {last_eval} - {file_path}")
        
        files = filtered_files
        print(f"\nTotal files selected: {len(files)}")
        
        # Verify we have the expected number of files
        expected_count = (max_eval // frequency) + 1  # +1 for the first evaluation
        if len(files) < expected_count - 1:  # Allow for some flexibility with the last point
            print(f"Warning: Expected around {expected_count} files, but only found {len(files)} files"
                  f" for frequency {frequency} up to evaluation {max_eval}")
    
    # Sort by evaluation number
    files.sort(key=lambda x: x[0])
    
    return files

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
            data = data.reshape(1, -1)  # Convert to 2D with shape (1, n_objectives)
        
        # Print debug info
        print(f"\nFile: {Path(file_path).name}")
        print(f"Data shape: {data.shape}")
        if len(data) > 0:
            print(f"First point: {data[0]}")
            if len(data) > 1:
                print(f"Last point: {data[-1]}")
        else:
            print("No data points in file")
            
        return data
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return np.array([]).reshape(0, 2)  # Return empty 2D array

def plot_fronts(fronts_data: List[Tuple[int, np.ndarray]], ax):
    """Plot all fronts with different colors and consistent point style."""
    # Use a colormap for smooth color transitions
    colors = cm.rainbow(np.linspace(0, 1, len(fronts_data)))
    
    # Plot each front with a different color but same marker
    for i, (eval_num, objectives) in enumerate(fronts_data):
        if len(objectives) > 0:
            ax.scatter(objectives[:, 0], objectives[:, 1], 
                      color=colors[i], 
                      alpha=0.7,
                      s=40,
                      marker='o',  # Consistent circle marker
                      edgecolors='w',
                      linewidth=0.5,
                      label=f'Eval {eval_num}')

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
        
    Raises:
        ValueError: If frequency doesn't match the evaluation numbers in filenames
    """
    # Get and validate files based on frequency
    try:
        files = get_fun_files(data_dir, frequency)
    except ValueError as e:
        print(f"Error: {e}")
        print("Please ensure the frequency matches the evaluation numbers in the filenames.")
        return
    if not files:
        print(f"No FUN.*.csv files found in {data_dir}")
        return
    
    # Parse filename info from the first file
    algorithm, training_set, x_label, y_label = parse_filename_info(files[0][1])
    
    # Read all data - files are already filtered by frequency in get_fun_files
    fronts_data = [(eval_num, read_objectives(f)) for eval_num, f in files]
    
    # Skip if no valid data
    if not any(len(obj) > 0 for _, obj in fronts_data):
        print("No valid data found in any of the files.")
        return
    
    # Set up the figure with adjusted size and margins
    plt.figure(figsize=(14, 10))
    ax = plt.gca()
    
    # Adjust layout to make room for legend
    plt.subplots_adjust(right=0.75, left=0.1, top=0.95, bottom=0.1)
    
    # Filter out empty data points before plotting
    valid_fronts = [(eval_num, obj) for eval_num, obj in fronts_data if len(obj) > 0]
    if not valid_fronts:
        print("No valid data points to plot.")
        return
    
    # Plot only valid fronts
    plot_fronts(valid_fronts, ax)
    
    # Set axis labels and title
    ax.set_xlabel(x_label)
    ax.set_ylabel(y_label)
    ax.set_title(f'Optimization Progress\nAlgorithm: {algorithm} | Training Set: {training_set}')
    ax.grid(True, alpha=0.3)
    
    # Get handles and labels from the plot
    handles, labels = ax.get_legend_handles_labels()
    
    # Only create legend if we have valid points
    if handles and labels:
        ax.legend(handles, labels, 
                 title='Evaluation Points',
                 loc='center left',
                 bbox_to_anchor=(1.05, 0.5),
                 frameon=True,
                 framealpha=0.9,
                 fancybox=True)
    else:
        print("No valid data points to show in legend.")
    
    # Adjust layout to make room for the legend
    plt.tight_layout(rect=[0, 0, 0.85, 1])  # Adjust the right margin
    
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
