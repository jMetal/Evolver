#!/usr/bin/env python3
"""
Analysis A: HV Evolution Analysis
=================================

Generates hypervolume evolution comparison figures for NSGA-II meta-optimization experiments.
Compares convergence between reference fronts and extreme points fronts.

Author: Evolver Analysis Team
Date: 2026-03-17
Purpose: Scientific publication figure generation
"""

import os
import sys
from pathlib import Path
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from typing import Dict, List, Tuple, Optional
import warnings
warnings.filterwarnings('ignore')

# Configuration
BASE_DIR = Path(__file__).parent.parent.parent
EXPERIMENTATION_DIR = BASE_DIR / "experimentation"
TRAINING_DIR = EXPERIMENTATION_DIR / "training"
FIGURES_DIR = Path(__file__).parent

# Dataset configuration
DATASETS = {
    "referenceFronts": {
        "path": TRAINING_DIR / "referenceFronts",
        "label": "Reference Fronts",
        "color": "#1f77b4",  # Blue
        "linestyle": "-"
    },
    "extremePointsFronts": {
        "path": TRAINING_DIR / "extremePoints", 
        "label": "Extreme Points Fronts",
        "color": "#ff7f0e",  # Orange
        "linestyle": "-"
    }
}

# Experimental configuration
PROBLEMS = ["RE3D", "RWA3D"]
BUDGETS = [1000, 3000, 5000, 7000]
N_RUNS = 30
N_CHECKPOINTS = 30

def parse_var_conf_file(filepath: Path) -> List[Tuple[int, float]]:
    """
    Parse VAR_CONF.txt file and extract (evaluation, HV) pairs.
    
    Args:
        filepath: Path to VAR_CONF.txt file
        
    Returns:
        List of (evaluation, HV) tuples with running maximum applied
    """
    evaluations = []
    hv_values = []
    
    if not filepath.exists():
        print(f"Warning: File not found: {filepath}")
        return []
    
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    current_block_eval = None
    block_hv_values = []
    
    for line in lines:
        line = line.strip()
        if line.startswith("# Evaluation:"):
            # Save previous block data
            if current_block_eval is not None and block_hv_values:
                max_hv = max(block_hv_values)
                evaluations.append(current_block_eval)
                hv_values.append(max_hv)
            
            # Start new block
            current_block_eval = int(line.split(":")[1].strip())
            block_hv_values = []
            
        elif line.startswith("EP=") and "HVMinus=" in line:
            # Extract HVMinus value and convert to HV
            try:
                parts = line.split("HVMinus=")[1].split()[0]
                hv_minus = float(parts)
                hv = -hv_minus  # Convert back to positive HV
                block_hv_values.append(hv)
            except (ValueError, IndexError):
                continue
    
    # Don't forget the last block
    if current_block_eval is not None and block_hv_values:
        max_hv = max(block_hv_values)
        evaluations.append(current_block_eval)
        hv_values.append(max_hv)
    
    # Apply running maximum (monotonically non-decreasing)
    if hv_values:
        running_max = []
        current_max = hv_values[0]
        for hv in hv_values:
            current_max = max(current_max, hv)
            running_max.append(current_max)
        hv_values = running_max
    
    return list(zip(evaluations, hv_values))

def forward_fill_interpolation(data: List[Tuple[int, float]], 
                              target_evaluations: List[int]) -> List[float]:
    """
    Apply forward-fill interpolation to align data points.
    
    Args:
        data: List of (evaluation, HV) tuples
        target_evaluations: Target evaluation points
        
    Returns:
        List of HV values at target evaluation points
    """
    if not data:
        return [0.0] * len(target_evaluations)
    
    result = []
    data_dict = dict(data)
    
    for target_eval in target_evaluations:
        # Find the largest evaluation <= target_eval
        available_evals = [e for e in data_dict.keys() if e <= target_eval]
        if available_evals:
            closest_eval = max(available_evals)
            result.append(data_dict[closest_eval])
        else:
            # No data available, use 0
            result.append(0.0)
    
    return result

def load_experiment_data() -> Dict[str, Dict[str, Dict[int, List[float]]]]:
    """
    Load all experimental data organized by dataset, problem, and budget.
    
    Returns:
        Nested dictionary structure:
        dataset -> problem -> budget -> list of HV series (one per run)
    """
    print("Loading experimental data...")
    
    all_data = {}
    
    for dataset_name, dataset_config in DATASETS.items():
        dataset_path = dataset_config["path"]
        all_data[dataset_name] = {}
        
        for problem in PROBLEMS:
            all_data[dataset_name][problem] = {}
            
            for budget in BUDGETS:
                problem_budget_dir = dataset_path / f"{problem}.{dataset_name}.{budget}"
                
                if not problem_budget_dir.exists():
                    print(f"Warning: Directory not found: {problem_budget_dir}")
                    all_data[dataset_name][problem][budget] = []
                    continue
                
                # Load data from all runs
                run_hv_series = []
                
                for run_num in range(1, N_RUNS + 1):
                    var_conf_path = problem_budget_dir / f"run{run_num}" / "VAR_CONF.txt"
                    hv_data = parse_var_conf_file(var_conf_path)
                    
                    if hv_data:
                        # Determine max evaluation for this run
                        max_eval = max(e for e, _ in hv_data)
                        target_evals = np.linspace(100, max_eval, N_CHECKPOINTS, dtype=int)

                        # Interpolate to standard checkpoints
                        interpolated_hv = forward_fill_interpolation(hv_data, target_evals)
                        run_hv_series.append(interpolated_hv)
                
                all_data[dataset_name][problem][budget] = run_hv_series
    
    print("Data loading complete.")
    return all_data

def compute_statistics_curves(all_data: Dict) -> Dict[str, Dict[str, Dict[int, Tuple[List[int], List[float], List[float], List[float]]]]]:
    """
    Compute statistics curves (median, min, max) across runs for each configuration.
    
    Args:
        all_data: Raw experimental data
        
    Returns:
        Dictionary with statistics curves and corresponding evaluation points:
        dataset -> problem -> budget -> (eval_points, median_hv, min_hv, max_hv)
    """
    print("Computing statistics curves...")
    
    stats_data = {}
    
    for dataset_name in DATASETS.keys():
        stats_data[dataset_name] = {}
        
        for problem in PROBLEMS:
            stats_data[dataset_name][problem] = {}
            
            for budget in BUDGETS:
                run_series = all_data[dataset_name][problem][budget]
                
                if not run_series:
                    stats_data[dataset_name][problem][budget] = ([], [], [], [])
                    continue
                
                # Convert to numpy array for statistics computation
                hv_array = np.array(run_series)
                
                # Compute statistics across runs at each checkpoint
                median_hv = np.median(hv_array, axis=0)
                min_hv = np.min(hv_array, axis=0)
                max_hv = np.max(hv_array, axis=0)
                
                # Use evaluation points from first run (they should be consistent)
                if run_series[0]:
                    # Reconstruct evaluation points
                    first_run_data = parse_var_conf_file(
                        DATASETS[dataset_name]["path"] / 
                        f"{problem}.{dataset_name}.{budget}" / 
                        "run1" / "VAR_CONF.txt"
                    )
                    if first_run_data:
                        max_eval = max(e for e, _ in first_run_data)
                        eval_points = np.linspace(100, max_eval, N_CHECKPOINTS, dtype=int)
                    else:
                        eval_points = list(range(len(median_hv)))
                else:
                    eval_points = list(range(len(median_hv)))
                
                stats_data[dataset_name][problem][budget] = (eval_points, median_hv, min_hv, max_hv)
    
    print("Statistics computation complete.")
    return stats_data

def compute_95_percent_convergence(eval_points: List[int], hv_values: List[float]) -> Tuple[int, float]:
    """
    Compute the evaluation point where 95% of total HV improvement is achieved.
    
    Args:
        eval_points: List of evaluation points
        hv_values: List of HV values (running maximum)
        
    Returns:
        Tuple of (convergence_evaluation, convergence_hv)
    """
    if len(hv_values) < 2:
        return eval_points[0] if eval_points else 0, hv_values[0] if hv_values else 0.0
    
    hv_min = hv_values[0]
    hv_max = hv_values[-1]
    hv_threshold = hv_min + 0.95 * (hv_max - hv_min)
    
    # Find first evaluation where HV >= threshold
    for i, (eval_point, hv) in enumerate(zip(eval_points, hv_values)):
        if hv >= hv_threshold:
            return eval_point, hv
    
    # If threshold not reached, return last point
    return eval_points[-1], hv_values[-1]

def create_figures(stats_data: Dict) -> List[plt.Figure]:
    """
    Create a single 2x2 grid figure with all 4 configurations.
    Layout: RE3D (first column), RWA3D (second column)
    
    Args:
        stats_data: Statistics curves for all configurations
        
    Returns:
        List with single matplotlib figure object
    """
    print("Creating figures...")
    
    # Color scheme for different budgets
    budget_colors = {
        1000: '#1f77b4',    # Blue
        3000: '#ff7f0e',    # Orange  
        5000: '#2ca02c',    # Green
        7000: '#d62728'     # Red
    }
    
    # Create single 2x2 figure
    fig, axes = plt.subplots(2, 2, figsize=(16, 12))
    fig.suptitle('HV Evolution: RE3D vs RWA3D Problems\nVertical lines: 95% convergence of total HV improvement', 
                 fontsize=18, fontweight='bold')
    
    # Define subplot positions - group by problem in columns
    subplot_configs = [
        (0, 0, "RE3D", "referenceFronts", "Reference Fronts"),      # Top-left
        (0, 1, "RWA3D", "referenceFronts", "Reference Fronts"),     # Top-right
        (1, 0, "RE3D", "extremePointsFronts", "Extreme Points"),     # Bottom-left
        (1, 1, "RWA3D", "extremePointsFronts", "Extreme Points")    # Bottom-right
    ]
    
    # Track y-axis limits for problem-specific scaling
    problem_limits = {
        "RE3D": {"min": float('inf'), "max": float('-inf')},
        "RWA3D": {"min": float('inf'), "max": float('-inf')}
    }
    
    # First pass: collect data to determine y-axis limits per problem
    for row, col, problem, dataset_name, dataset_label in subplot_configs:
        for budget in BUDGETS:
            eval_points, median_hv, min_hv, max_hv = stats_data[dataset_name][problem][budget]
            if len(median_hv) > 0:
                problem_limits[problem]["min"] = min(problem_limits[problem]["min"], min(min_hv))
                problem_limits[problem]["max"] = max(problem_limits[problem]["max"], max(max_hv))
    
    # Add padding to limits for each problem (guard against missing data)
    for problem in problem_limits:
        if problem_limits[problem]["min"] == float('inf') or problem_limits[problem]["max"] == float('-inf'):
            # No data found for this problem: set sensible defaults
            problem_limits[problem]["min"] = 0.0
            problem_limits[problem]["max"] = 1.0
            problem_limits[problem]["padding"] = 0.1
            continue

        y_range = problem_limits[problem]["max"] - problem_limits[problem]["min"]
        if y_range <= 0:
            # Degenerate range: add small absolute padding
            y_padding = abs(problem_limits[problem]["max"]) * 0.05 + 1e-6
        else:
            y_padding = y_range * 0.05

        problem_limits[problem]["min"] = max(0, problem_limits[problem]["min"] - y_padding)
        problem_limits[problem]["max"] = problem_limits[problem]["max"] + y_padding
        problem_limits[problem]["padding"] = y_padding
    
    # Second pass: create the plots
    for row, col, problem, dataset_name, dataset_label in subplot_configs:
        ax = axes[row, col]
        
        # Plot each budget as separate line with shaded area
        for budget in BUDGETS:
            eval_points, median_hv, min_hv, max_hv = stats_data[dataset_name][problem][budget]
            
            if len(eval_points) > 0 and len(median_hv) > 0:
                # Plot shaded area (min-max range)
                ax.fill_between(eval_points, min_hv, max_hv, 
                              alpha=0.2, color=budget_colors[budget])
                
                # Plot median line
                ax.plot(eval_points, median_hv, 
                       color=budget_colors[budget],
                       linewidth=2.5,
                       label=f'{budget} evals')
                
                # Add vertical line for 95% convergence
                conv_eval, conv_hv = compute_95_percent_convergence(eval_points, median_hv)
                ax.axvline(x=conv_eval, color=budget_colors[budget], 
                      linestyle='--', alpha=0.7, linewidth=1.5)

                # Add convergence value label at the bottom of each line, just above X-axis (vertical orientation)
                y_text = problem_limits[problem]["min"] + problem_limits[problem].get("padding", 1e-6) * 0.2
                ax.text(conv_eval, y_text, 
                    f'{conv_eval}', 
                    color=budget_colors[budget], 
                    fontsize=8, 
                    ha='center', 
                    va='bottom',
                    rotation=90)
        
        # Formatting
        ax.set_xlabel('Meta-evaluations')
        ax.set_ylabel('Best Hypervolume')
        ax.set_title(f'{problem} - {dataset_label}')
        ax.grid(True, alpha=0.3)
        
        # Add legend to all subplots with Budget title (force all budgets present)
        legend_handles = [mpatches.Patch(color=budget_colors[b], label=f'{b} evals') for b in BUDGETS]
        if row == 1 and col == 1:
            ax.legend(handles=legend_handles, title='Budget', loc='lower right')
        else:
            ax.legend(handles=legend_handles, title='Budget', loc='best')
        
        # Set problem-specific y-axis limits and fixed X-axis to README spec
        ax.set_ylim(problem_limits[problem]["min"], problem_limits[problem]["max"])
        ax.set_xlim(0, 3000)
    
    # Adjust layout
    plt.tight_layout()
    plt.subplots_adjust(top=0.88)  # Increased spacing to prevent title overlap
    
    print("Figure creation complete.")
    return [fig]

def save_figures(figures: List[plt.Figure]) -> List[str]:
    """
    Save the single figure to file with publication settings.
    
    Args:
        figures: List with single matplotlib figure
        
    Returns:
        List of saved file paths
    """
    # Ensure figures directory exists
    FIGURES_DIR.mkdir(exist_ok=True)
    
    saved_files = []
    
    # Save the single figure
    fig = figures[0]
    filename = "hv_evolution_grid_2x2.png"
    output_path = FIGURES_DIR / filename
    
    # Save with publication settings
    fig.savefig(output_path, 
               dpi=300, 
               bbox_inches='tight',
               facecolor='white',
               edgecolor='none')
    
    saved_files.append(str(output_path))
    print(f"Figure saved: {output_path}")
    
    return saved_files

def main():
    """Main execution function."""
    print("=" * 60)
    print("HV Evolution Analysis - Experiment A")
    print("=" * 60)
    
    # Load and process data
    all_data = load_experiment_data()
    stats_data = compute_statistics_curves(all_data)
    
    # Create and save figures
    figures = create_figures(stats_data)
    saved_files = save_figures(figures)
    
    print("=" * 60)
    print("Analysis complete!")
    print(f"Generated {len(saved_files)} figures:")
    for file_path in saved_files:
        print(f"  - {file_path}")
    print("=" * 60)

if __name__ == "__main__":
    main()
