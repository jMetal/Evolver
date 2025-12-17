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
        default='analysis_results/optimization_progress.png',
        help='Output filename (supports .png, .jpg, .pdf, .svg) - default: analysis_results/optimization_progress.png'
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
    parser.add_argument(
        '--aggregation',
        type=str,
        choices=['min', 'max', 'mean', 'median', 'std'],
        default='median',
        help='Central tendency method for multiple values per evaluation (default: median)'
    )
    parser.add_argument(
        '--show-range',
        action='store_true',
        help='Show confidence intervals/error bars representing the range of values'
    )
    parser.add_argument(
        '--range-type',
        type=str,
        choices=['minmax', 'quartiles', 'std'],
        default='quartiles',
        help='Type of range to show: minmax (full range), quartiles (25th-75th percentile), std (±1 standard deviation)'
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

def prepare_time_series_data(indicators_df: pd.DataFrame, indicator1: str, indicator2: str, 
                           aggregation: str = 'median', show_range: bool = False, 
                           range_type: str = 'quartiles') -> Tuple[np.ndarray, np.ndarray, np.ndarray, Optional[Dict]]:
    """
    Prepare data for time series plotting with evaluation numbers on X-axis.
    
    Args:
        indicators_df: DataFrame with indicators data
        indicator1: Name of first indicator (left Y-axis)
        indicator2: Name of second indicator (right Y-axis)
        aggregation: Central tendency method ('min', 'max', 'mean', 'median', 'std')
        show_range: Whether to calculate confidence intervals/ranges
        range_type: Type of range ('minmax', 'quartiles', 'std')
        
    Returns:
        Tuple of (evaluations, indicator1_values, indicator2_values, range_data)
        range_data is None if show_range=False, otherwise dict with confidence intervals
    """
    # Check if indicators exist
    available_indicators = list(indicators_df.columns[2:])  # Skip Evaluation and SolutionId
    if indicator1 not in available_indicators:
        raise ValueError(f"Indicator '{indicator1}' not found. Available: {available_indicators}")
    if indicator2 not in available_indicators:
        raise ValueError(f"Indicator '{indicator2}' not found. Available: {available_indicators}")
    
    # Define aggregation functions
    agg_functions = {
        'min': lambda x: x.min(),
        'max': lambda x: x.max(),
        'mean': lambda x: x.mean(),
        'median': lambda x: x.median(),
        'std': lambda x: x.std()
    }
    
    if aggregation not in agg_functions:
        raise ValueError(f"Unknown aggregation method '{aggregation}'. Available: {list(agg_functions.keys())}")
    
    agg_func = agg_functions[aggregation]
    
    # Group by evaluation and calculate statistics
    evaluations = []
    indicator1_values = []
    indicator2_values = []
    
    # Range data for confidence intervals
    range_data = None
    if show_range:
        range_data = {
            'indicator1_lower': [],
            'indicator1_upper': [],
            'indicator2_lower': [],
            'indicator2_upper': []
        }
    
    print(f"\nUsing {aggregation} aggregation for multiple solutions per evaluation:")
    if show_range:
        print(f"Showing {range_type} confidence intervals")
    
    for evaluation in sorted(indicators_df['Evaluation'].unique()):
        eval_data = indicators_df[indicators_df['Evaluation'] == evaluation]
        
        # Apply aggregation function to each indicator
        ind1_agg = agg_func(eval_data[indicator1])
        ind2_agg = agg_func(eval_data[indicator2])
        
        evaluations.append(evaluation)
        indicator1_values.append(ind1_agg)
        indicator2_values.append(ind2_agg)
        
        # Calculate ranges if requested
        if show_range and len(eval_data) > 1:
            ind1_series = eval_data[indicator1]
            ind2_series = eval_data[indicator2]
            
            if range_type == 'minmax':
                ind1_lower, ind1_upper = ind1_series.min(), ind1_series.max()
                ind2_lower, ind2_upper = ind2_series.min(), ind2_series.max()
            elif range_type == 'quartiles':
                ind1_lower, ind1_upper = ind1_series.quantile(0.25), ind1_series.quantile(0.75)
                ind2_lower, ind2_upper = ind2_series.quantile(0.25), ind2_series.quantile(0.75)
            elif range_type == 'std':
                ind1_mean, ind1_std = ind1_series.mean(), ind1_series.std()
                ind2_mean, ind2_std = ind2_series.mean(), ind2_series.std()
                ind1_lower, ind1_upper = ind1_mean - ind1_std, ind1_mean + ind1_std
                ind2_lower, ind2_upper = ind2_mean - ind2_std, ind2_mean + ind2_std
            
            range_data['indicator1_lower'].append(ind1_lower)
            range_data['indicator1_upper'].append(ind1_upper)
            range_data['indicator2_lower'].append(ind2_lower)
            range_data['indicator2_upper'].append(ind2_upper)
        elif show_range:
            # Single solution - no range
            range_data['indicator1_lower'].append(ind1_agg)
            range_data['indicator1_upper'].append(ind1_agg)
            range_data['indicator2_lower'].append(ind2_agg)
            range_data['indicator2_upper'].append(ind2_agg)
        
        # Show individual values if there are multiple solutions
        if len(eval_data) > 1:
            ind1_values_arr = eval_data[indicator1].values
            ind2_values_arr = eval_data[indicator2].values
            print(f"Evaluation {evaluation}: {len(eval_data)} solutions")
            print(f"  {indicator1}: min={ind1_values_arr.min():.4f}, median={np.median(ind1_values_arr):.4f}, max={ind1_values_arr.max():.4f}")
            print(f"  {indicator2}: min={ind2_values_arr.min():.4f}, median={np.median(ind2_values_arr):.4f}, max={ind2_values_arr.max():.4f}")
        else:
            print(f"Evaluation {evaluation}: {indicator1}={ind1_agg:.6f}, {indicator2}={ind2_agg:.6f} (1 solution)")
    
    # Convert range data to numpy arrays
    if show_range:
        for key in range_data:
            range_data[key] = np.array(range_data[key])
    
    return np.array(evaluations), np.array(indicator1_values), np.array(indicator2_values), range_data

def plot_dual_axis_time_series(evaluations: np.ndarray, indicator1_values: np.ndarray, 
                              indicator2_values: np.ndarray, indicator1_name: str, 
                              indicator2_name: str, ax1, range_data: Optional[Dict] = None,
                              range_type: str = 'quartiles'):
    """Plot time series with dual Y-axes and optional confidence intervals."""
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
    
    # Add confidence intervals for first indicator
    if range_data is not None:
        ax1.fill_between(evaluations, 
                        range_data['indicator1_lower'], 
                        range_data['indicator1_upper'],
                        color=color1, alpha=0.2, 
                        label=f'{indicator1_name} {range_type}')
    
    # Create second Y-axis for second indicator
    ax2 = ax1.twinx()
    color2 = 'tab:red'
    ax2.set_ylabel(indicator2_name, color=color2)
    line2 = ax2.plot(evaluations, indicator2_values, color=color2, marker='s', 
                     linewidth=2, markersize=4, label=indicator2_name)
    ax2.tick_params(axis='y', labelcolor=color2)
    
    # Add confidence intervals for second indicator
    if range_data is not None:
        ax2.fill_between(evaluations, 
                        range_data['indicator2_lower'], 
                        range_data['indicator2_upper'],
                        color=color2, alpha=0.2,
                        label=f'{indicator2_name} {range_type}')
    
    return line1, line2

def plot_separate_subplots(evaluations: np.ndarray, indicator1_values: np.ndarray, 
                          indicator2_values: np.ndarray, indicator1_name: str, 
                          indicator2_name: str, range_data: Optional[Dict] = None,
                          range_type: str = 'quartiles', metadata: Dict = None):
    """Plot time series with separate subplots for each indicator (like convergence_analysis.png)."""
    
    # Determine if we need separate plots or if both indicators are the same
    if indicator1_name == indicator2_name:
        # Single indicator plot
        fig, ax = plt.subplots(1, 1, figsize=(12, 6))
        axes = [ax]
        indicators = [indicator1_name]
        values = [indicator1_values]
        range_keys = [('indicator1_lower', 'indicator1_upper')]
    else:
        # Two separate subplots
        fig, axes = plt.subplots(2, 1, figsize=(12, 10), sharex=True)
        indicators = [indicator1_name, indicator2_name]
        values = [indicator1_values, indicator2_values]
        range_keys = [('indicator1_lower', 'indicator1_upper'), 
                     ('indicator2_lower', 'indicator2_upper')]
    
    colors = ['tab:blue', 'tab:red']
    
    for i, (ax, indicator, vals, range_key, color) in enumerate(zip(axes, indicators, values, range_keys, colors)):
        # Plot the main line
        ax.plot(evaluations, vals, color=color, marker='o', 
               linewidth=2, markersize=4, label='Median')
        
        # Add confidence intervals if available
        if range_data is not None and range_key[0] in range_data:
            ax.fill_between(evaluations, 
                           range_data[range_key[0]], 
                           range_data[range_key[1]],
                           color=color, alpha=0.3, 
                           label=f'{range_type.title()} Range')
        
        ax.set_ylabel(f'{indicator} Value')
        ax.set_title(f'{indicator} Convergence')
        ax.legend()
        ax.grid(True, alpha=0.3)
        
        # Only set xlabel on bottom plot
        if i == len(axes) - 1:
            ax.set_xlabel('Evaluation Number')
    
    # Add overall title
    algorithm = metadata.get('Algorithm', 'Unknown') if metadata else 'Unknown'
    training_set = metadata.get('Training Set', 'Unknown') if metadata else 'Unknown'
    
    if len(indicators) == 1:
        plt.suptitle(f'{indicators[0]} Convergence Analysis\n'
                    f'Algorithm: {algorithm} | Training Set: {training_set}', 
                    fontsize=14, y=0.95)
    else:
        plt.suptitle(f'Convergence Analysis\n'
                    f'Algorithm: {algorithm} | Training Set: {training_set}', 
                    fontsize=14, y=0.95)
    
    plt.tight_layout()
    return fig

def plot_optimization_progress(data_dir: str, frequency: int, output_file: str, 
                             indicator1: str, indicator2: str, aggregation: str = 'median',
                             show_range: bool = False, range_type: str = 'quartiles'):
    """
    Create a time series plot of the optimization progress with separate subplots.
    
    Args:
        data_dir: Directory containing structured data files
        frequency: Plot every N evaluations
        output_file: Output file name for the plot
        indicator1: Quality indicator for first subplot
        indicator2: Quality indicator for second subplot (if different from indicator1)
        aggregation: Central tendency method for multiple values per evaluation
        show_range: Whether to show confidence intervals
        range_type: Type of confidence intervals to show
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
        evaluations, indicator1_values, indicator2_values, range_data = prepare_time_series_data(
            filtered_df, indicator1, indicator2, aggregation, show_range, range_type)
        
        if len(evaluations) == 0:
            print("No valid data points to plot.")
            return
        
        # Create separate subplots (like convergence_analysis.png)
        fig = plot_separate_subplots(
            evaluations, indicator1_values, indicator2_values, 
            indicator1, indicator2, range_data, range_type, metadata)
        
        # Save the figure
        try:
            # Ensure output directory exists
            output_path = Path(output_file)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            
            plt.savefig(output_file, dpi=300, bbox_inches='tight')
            print(f"Plot saved to {output_file}")
            
            if indicator1 == indicator2:
                print(f"Plot shows convergence of {indicator1} over {len(evaluations)} evaluation points")
            else:
                print(f"Plot shows convergence of {indicator1} and {indicator2} in separate subplots over {len(evaluations)} evaluation points")
            
            if show_range:
                print(f"Central tendency: {aggregation} (used when multiple solutions exist per evaluation)")
                print(f"Confidence intervals: {range_type} (shaded areas show variability)")
        except Exception as e:
            print(f"Error saving plot: {e}")
        finally:
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
    print(f"Central tendency: {args.aggregation}")
    if args.show_range:
        print(f"Confidence intervals: {args.range_type}")
    
    # Ensure output file has a valid extension
    if not args.output.endswith(('.png', '.jpg', '.jpeg', '.pdf', '.svg')):
        args.output = f"{os.path.splitext(args.output)[0]}.png"
    
    plot_optimization_progress(args.data_dir, args.frequency, args.output, 
                             args.indicator1, args.indicator2, args.aggregation,
                             args.show_range, args.range_type)

if __name__ == "__main__":
    main()