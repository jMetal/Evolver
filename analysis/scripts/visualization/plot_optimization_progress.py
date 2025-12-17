#!/usr/bin/env python3
"""
Visualization Tool for Multi-Objective Optimization Progress

This script generates a static plot showing the progression of multi-objective optimization
runs. It reads data from the new structured format with INDICATORS.csv, CONFIGURATIONS.csv,
and METADATA.txt files that contain quality indicators and algorithm configurations from
different evaluation points during the optimization process.

New Data Format:
    - INDICATORS.csv: Contains quality indicator values (EP, NHV, etc.) for each evaluation
    - CONFIGURATIONS.csv: Contains algorithm parameter configurations for each solution
    - METADATA.txt: Contains experiment metadata (algorithm, training set, etc.)
    - VAR_CONF.txt: Contains variable configurations

Output:
    Generates a high-quality PNG image showing the progression of quality indicators
    with different colors representing different evaluation points.

Example Usage:
    # Basic usage with default output name
    python plot_optimization_progress.py results/nsgaii/ZDT 200
    
    # Specify custom output file and indicators
    python plot_optimization_progress.py results/nsgaii/ZDT 200 --output optimization_progress.png --x-indicator EP --y-indicator NHV

Dependencies:
    - Python 3.6+
    - numpy
    - matplotlib
    - pandas
    - pathlib
"""

import os
import argparse
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
from pathlib import Path
from typing import List, Tuple, Optional, Dict
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
        help='Directory containing INDICATORS.csv, CONFIGURATIONS.csv, and METADATA.txt files'
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
    parser.add_argument(
        '--x-indicator',
        type=str,
        default='EP',
        help='Quality indicator to use for X-axis (default: EP)'
    )
    parser.add_argument(
        '--y-indicator',
        type=str,
        default='NHV',
        help='Quality indicator to use for Y-axis (default: NHV)'
    )
    parser.add_argument(
        '--list-indicators',
        action='store_true',
        help='List available indicators in the data directory and exit'
    )
    return parser.parse_args()

def load_experiment_data(data_dir: str) -> Tuple[pd.DataFrame, Dict[str, str]]:
    """
    Load experiment data from the new structured format.
    
    Args:
        data_dir: Directory containing INDICATORS.csv, CONFIGURATIONS.csv, and METADATA.txt
        
    Returns:
        Tuple of (indicators_df, metadata_dict)
        
    Raises:
        FileNotFoundError: If required files are missing
        ValueError: If data format is invalid
    """
    data_path = Path(data_dir)
    
    # Check for required files
    indicators_file = data_path / 'INDICATORS.csv'
    metadata_file = data_path / 'METADATA.txt'
    
    if not indicators_file.exists():
        raise FileNotFoundError(f"INDICATORS.csv not found in {data_dir}")
    if not metadata_file.exists():
        raise FileNotFoundError(f"METADATA.txt not found in {data_dir}")
    
    print(f"\nLoading data from: {data_dir}")
    
    # Load indicators data
    try:
        indicators_df = pd.read_csv(indicators_file)
        print(f"Loaded indicators data: {len(indicators_df)} rows")
        print(f"Available indicators: {list(indicators_df.columns[2:])}")  # Skip Evaluation and SolutionId
    except Exception as e:
        raise ValueError(f"Error reading INDICATORS.csv: {e}")
    
    # Load metadata
    metadata = {}
    try:
        with open(metadata_file, 'r') as f:
            content = f.read()
            
        # Parse metadata (simple key-value extraction)
        for line in content.split('\n'):
            if ':' in line and not line.startswith('=') and not line.startswith('-'):
                key, value = line.split(':', 1)
                metadata[key.strip()] = value.strip()
                
        print(f"Loaded metadata: Algorithm={metadata.get('Algorithm', 'Unknown')}, "
              f"Training Set={metadata.get('Problem Family', 'Unknown')}")
              
    except Exception as e:
        print(f"Warning: Error reading metadata: {e}")
        metadata = {'Algorithm': 'Unknown', 'Problem Family': 'Unknown'}
    
    return indicators_df, metadata

def filter_data_by_frequency(indicators_df: pd.DataFrame, frequency: int) -> pd.DataFrame:
    """
    Filter indicators data to include only evaluations at specified frequency.
    
    Args:
        indicators_df: DataFrame with indicators data
        frequency: Include every N evaluations
        
    Returns:
        Filtered DataFrame
    """
    if frequency <= 0:
        return indicators_df
        
    available_evaluations = sorted(indicators_df['Evaluation'].unique())
    print(f"\nAll available evaluation points: {available_evaluations}")
    
    # Always include the first evaluation
    selected_evaluations = [available_evaluations[0]] if available_evaluations else []
    
    # Include evaluations at frequency intervals
    max_eval = max(available_evaluations) if available_evaluations else 0
    for eval_num in range(frequency, max_eval + 1, frequency):
        if eval_num in available_evaluations and eval_num not in selected_evaluations:
            selected_evaluations.append(eval_num)
    
    # Include the last evaluation if it doesn't align with frequency
    if available_evaluations and available_evaluations[-1] not in selected_evaluations:
        selected_evaluations.append(available_evaluations[-1])
    
    selected_evaluations.sort()
    print(f"Selected evaluation points: {selected_evaluations}")
    
    # Filter the dataframe
    filtered_df = indicators_df[indicators_df['Evaluation'].isin(selected_evaluations)]
    print(f"Filtered data: {len(filtered_df)} rows from {len(selected_evaluations)} evaluation points")
    
    return filtered_df

def prepare_plot_data(indicators_df: pd.DataFrame, x_indicator: str, y_indicator: str) -> List[Tuple[int, np.ndarray]]:
    """
    Prepare data for plotting by grouping by evaluation and extracting indicator values.
    
    Args:
        indicators_df: DataFrame with indicators data
        x_indicator: Name of indicator for X-axis
        y_indicator: Name of indicator for Y-axis
        
    Returns:
        List of tuples (evaluation_number, indicator_values_array)
    """
    # Check if indicators exist
    available_indicators = list(indicators_df.columns[2:])  # Skip Evaluation and SolutionId
    if x_indicator not in available_indicators:
        raise ValueError(f"X-indicator '{x_indicator}' not found. Available: {available_indicators}")
    if y_indicator not in available_indicators:
        raise ValueError(f"Y-indicator '{y_indicator}' not found. Available: {available_indicators}")
    
    plot_data = []
    
    # Group by evaluation and extract indicator values
    for evaluation in sorted(indicators_df['Evaluation'].unique()):
        eval_data = indicators_df[indicators_df['Evaluation'] == evaluation]
        
        # Extract x and y values for this evaluation
        x_values = eval_data[x_indicator].values
        y_values = eval_data[y_indicator].values
        
        # Combine into 2D array (n_solutions, 2)
        if len(x_values) > 0 and len(y_values) > 0:
            indicator_values = np.column_stack((x_values, y_values))
            plot_data.append((evaluation, indicator_values))
            print(f"Evaluation {evaluation}: {len(indicator_values)} solutions")
    
    return plot_data

def plot_fronts(fronts_data: List[Tuple[int, np.ndarray]], ax, x_label: str, y_label: str):
    """Plot all fronts with different colors and consistent point style."""
    if not fronts_data:
        print("No data to plot")
        return
        
    # Use a colormap for smooth color transitions
    colors = cm.rainbow(np.linspace(0, 1, len(fronts_data)))
    
    # Plot each front with a different color but same marker
    for i, (eval_num, indicators) in enumerate(fronts_data):
        if len(indicators) > 0:
            ax.scatter(indicators[:, 0], indicators[:, 1], 
                      color=colors[i], 
                      alpha=0.7,
                      s=40,
                      marker='o',  # Consistent circle marker
                      edgecolors='w',
                      linewidth=0.5,
                      label=f'Eval {eval_num}')

def plot_optimization_progress(data_dir: str, frequency: int, output_file: str, 
                             x_indicator: str, y_indicator: str):
    """
    Create a static plot of the optimization progress.
    
    Args:
        data_dir: Directory containing structured data files
        frequency: Plot every N evaluations
        output_file: Output file name for the plot
        x_indicator: Quality indicator for X-axis
        y_indicator: Quality indicator for Y-axis
    """
    try:
        # Load experiment data
        indicators_df, metadata = load_experiment_data(data_dir)
        
        # Filter data by frequency
        filtered_df = filter_data_by_frequency(indicators_df, frequency)
        
        if len(filtered_df) == 0:
            print("No data available after filtering.")
            return
        
        # Prepare plot data
        plot_data = prepare_plot_data(filtered_df, x_indicator, y_indicator)
        
        if not plot_data:
            print("No valid data points to plot.")
            return
        
        # Set up the figure with adjusted size and margins
        plt.figure(figsize=(14, 10))
        ax = plt.gca()
        
        # Adjust layout to make room for legend
        plt.subplots_adjust(right=0.75, left=0.1, top=0.95, bottom=0.1)
        
        # Plot the data
        plot_fronts(plot_data, ax, x_indicator, y_indicator)
        
        # Set axis labels and title
        ax.set_xlabel(x_indicator)
        ax.set_ylabel(y_indicator)
        
        algorithm = metadata.get('Algorithm', 'Unknown')
        training_set = metadata.get('Problem Family', 'Unknown')
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
        
    except Exception as e:
        print(f"Error creating plot: {e}")
        return

def main():
    args = parse_args()
    
    if not os.path.isdir(args.data_dir):
        print(f"Error: Directory '{args.data_dir}' does not exist.")
        return
    
    # Handle list indicators option
    if args.list_indicators:
        try:
            indicators_df, metadata = load_experiment_data(args.data_dir)
            available_indicators = list(indicators_df.columns[2:])  # Skip Evaluation and SolutionId
            print(f"\nAvailable indicators in {args.data_dir}:")
            for indicator in available_indicators:
                print(f"  - {indicator}")
            print(f"\nExample usage:")
            print(f"  python {os.path.basename(__file__)} {args.data_dir} 200 --x-indicator {available_indicators[0]} --y-indicator {available_indicators[-1]}")
        except Exception as e:
            print(f"Error reading data: {e}")
        return
    
    if args.frequency <= 0:
        print("Error: Frequency must be a positive integer.")
        return
    
    print(f"Plotting optimization progress from {args.data_dir}")
    print(f"Plotting every {args.frequency} evaluations")
    print(f"X-axis: {args.x_indicator}, Y-axis: {args.y_indicator}")
    
    # Ensure output file has a valid extension
    if not args.output.endswith(('.png', '.jpg', '.jpeg', '.pdf', '.svg')):
        args.output = f"{os.path.splitext(args.output)[0]}.png"
    
    plot_optimization_progress(args.data_dir, args.frequency, args.output, 
                             args.x_indicator, args.y_indicator)

if __name__ == "__main__":
    main()