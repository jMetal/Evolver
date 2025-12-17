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
    over time with evaluation numbers on X-axis and dual Y-axes for two indicators.

Example Usage:
    # Basic usage with default output name
    python plot_optimization_progress.py results/nsgaii/ZDT 200
    
    # Specify custom output file and indicators
    python plot_optimization_progress.py results/nsgaii/ZDT 200 --output optimization_progress.png --indicator1 EP --indicator2 NHV

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
        '--indicator1',
        type=str,
        default='EP',
        help='Quality indicator for left Y-axis (default: EP)'
    )
    parser.add_argument(
        '--indicator2',
        type=str,
        default='NHV',
        help='Quality indicator for right Y-axis (default: NHV)'
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

def prepare_time_series_data(indicators_df: pd.DataFrame, indicator1: str, indicator2: str) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
    """
    Prepare data for time series plotting with evaluation numbers on X-axis.
    
    Args:
        indicators_df: DataFrame with indicators data
        indicator1: Name of first indicator (left Y-axis)
        indicator2: Name of second indicator (right Y-axis)
        
    Returns:
        Tuple of (evaluations, indicator1_values, indicator2_values)
    """
    # Check if indicators exist
    available_indicators = list(indicators_df.columns[2:])  # Skip Evaluation and SolutionId
    if indicator1 not in available_indicators:
        raise ValueError(f"Indicator '{indicator1}' not found. Available: {available_indicators}")
    if indicator2 not in available_indicators:
        raise ValueError(f"Indicator '{indicator2}' not found. Available: {available_indicators}")
    
    # Group by evaluation and calculate statistics (mean, best, etc.)
    evaluations = []
    indicator1_values = []
    indicator2_values = []
    
    for evaluation in sorted(indicators_df['Evaluation'].unique()):
        eval_data = indicators_df[indicators_df['Evaluation'] == evaluation]
        
        # Calculate best (minimum) values for each indicator at this evaluation
        # For most quality indicators, lower is better
        ind1_best = eval_data[indicator1].min()
        ind2_best = eval_data[indicator2].min()
        
        evaluations.append(evaluation)
        indicator1_values.append(ind1_best)
        indicator2_values.append(ind2_best)
        
        print(f"Evaluation {evaluation}: {indicator1}={ind1_best:.6f}, {indicator2}={ind2_best:.6f} (from {len(eval_data)} solutions)")
    
    return np.array(evaluations), np.array(indicator1_values), np.array(indicator2_values)

def plot_dual_axis_time_series(evaluations: np.ndarray, indicator1_values: np.ndarray, 
                              indicator2_values: np.ndarray, indicator1_name: str, 
                              indicator2_name: str, ax1):
    """Plot time series with dual Y-axes."""
    if len(evaluations) == 0:
        print("No data to plot")
        return None, None
    
    # Plot first indicator on left Y-axis
    color1 = 'tab:blue'
    ax1.set_xlabel('Evaluation Number')
    ax1.set_ylabel(indicator1_name, color=color1)
    line1 = ax1.plot(evaluations, indicator1_values, color=color1, marker='o', 
                     linewidth=2, markersize=4, label=indicator1_name)
    ax1.tick_params(axis='y', labelcolor=color1)
    ax1.grid(True, alpha=0.3)
    
    # Create second Y-axis for second indicator
    ax2 = ax1.twinx()
    color2 = 'tab:red'
    ax2.set_ylabel(indicator2_name, color=color2)
    line2 = ax2.plot(evaluations, indicator2_values, color=color2, marker='s', 
                     linewidth=2, markersize=4, label=indicator2_name)
    ax2.tick_params(axis='y', labelcolor=color2)
    
    return line1, line2

def plot_optimization_progress(data_dir: str, frequency: int, output_file: str, 
                             indicator1: str, indicator2: str):
    """
    Create a time series plot of the optimization progress with dual Y-axes.
    
    Args:
        data_dir: Directory containing structured data files
        frequency: Plot every N evaluations
        output_file: Output file name for the plot
        indicator1: Quality indicator for left Y-axis
        indicator2: Quality indicator for right Y-axis
    """
    try:
        # Load experiment data
        indicators_df, metadata = load_experiment_data(data_dir)
        
        # Filter data by frequency
        filtered_df = filter_data_by_frequency(indicators_df, frequency)
        
        if len(filtered_df) == 0:
            print("No data available after filtering.")
            return
        
        # Prepare time series data
        evaluations, indicator1_values, indicator2_values = prepare_time_series_data(
            filtered_df, indicator1, indicator2)
        
        if len(evaluations) == 0:
            print("No valid data points to plot.")
            return
        
        # Set up the figure
        plt.figure(figsize=(12, 8))
        ax1 = plt.gca()
        
        # Plot dual-axis time series
        line1, line2 = plot_dual_axis_time_series(
            evaluations, indicator1_values, indicator2_values, 
            indicator1, indicator2, ax1)
        
        # Set title
        algorithm = metadata.get('Algorithm', 'Unknown')
        training_set = metadata.get('Problem Family', 'Unknown')
        ax1.set_title(f'Optimization Progress Over Time\nAlgorithm: {algorithm} | Training Set: {training_set}', 
                     fontsize=14, pad=20)
        
        # Add legend
        if line1 and line2:
            lines = line1 + line2
            labels = [l.get_label() for l in lines]
            ax1.legend(lines, labels, loc='upper right', frameon=True, framealpha=0.9)
        
        # Improve layout
        plt.tight_layout()
        
        # Save the figure
        try:
            plt.savefig(output_file, dpi=300, bbox_inches='tight')
            print(f"Plot saved to {output_file}")
            print(f"Plot shows progression of {indicator1} (blue, left axis) and {indicator2} (red, right axis) over {len(evaluations)} evaluation points")
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
            print(f"  python {os.path.basename(__file__)} {args.data_dir} 200 --indicator1 {available_indicators[0]} --indicator2 {available_indicators[-1]}")
        except Exception as e:
            print(f"Error reading data: {e}")
        return
    
    if args.frequency <= 0:
        print("Error: Frequency must be a positive integer.")
        return
    
    print(f"Plotting optimization progress from {args.data_dir}")
    print(f"Plotting every {args.frequency} evaluations")
    print(f"Left Y-axis: {args.indicator1}, Right Y-axis: {args.indicator2}")
    
    # Ensure output file has a valid extension
    if not args.output.endswith(('.png', '.jpg', '.jpeg', '.pdf', '.svg')):
        args.output = f"{os.path.splitext(args.output)[0]}.png"
    
    plot_optimization_progress(args.data_dir, args.frequency, args.output, 
                             args.indicator1, args.indicator2)

if __name__ == "__main__":
    main()