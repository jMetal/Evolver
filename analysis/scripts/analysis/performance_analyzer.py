#!/usr/bin/env python3
"""
Performance Analysis Utilities

This module provides comprehensive performance analysis for optimization experiments,
including parameter sensitivity analysis, convergence analysis, and statistical testing.

Features:
- Parameter sensitivity analysis
- Performance distribution analysis
- Convergence rate analysis
- Statistical significance testing
- Interactive performance visualizations
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats
from scipy.stats import mannwhitneyu, kruskal
from typing import Dict, List, Tuple, Optional, Any
import warnings
from experiment_analyzer import ExperimentAnalyzer

class PerformanceAnalyzer:
    """Analyze performance characteristics of optimization experiments."""
    
    def __init__(self, analyzer: ExperimentAnalyzer):
        """
        Initialize with an ExperimentAnalyzer instance.
        
        Args:
            analyzer: ExperimentAnalyzer instance with loaded data
        """
        self.analyzer = analyzer
        self.param_info = analyzer.get_parameter_info()
    
    def analyze_parameter_sensitivity(self, target_indicators: List[str] = None,
                                    top_n_params: int = 10) -> Dict[str, pd.DataFrame]:
        """
        Analyze sensitivity of indicators to parameter changes.
        
        Args:
            target_indicators: List of indicators to analyze (default: all)
            top_n_params: Number of top sensitive parameters to return
            
        Returns:
            Dictionary with sensitivity analysis results for each indicator
        """
        if target_indicators is None:
            target_indicators = self.analyzer.indicator_cols
        
        results = {}
        
        print("Analyzing parameter sensitivity...")
        
        for indicator in target_indicators:
            print(f"\nSensitivity analysis for {indicator}:")
            
            sensitivities = []
            
            for param in self.analyzer.parameter_cols:
                param_info = self.param_info[param]
                
                # Skip parameters with too much missing data or constant parameters
                if param_info['missing_percentage'] > 80:
                    continue
                
                if param_info['type'] == 'constant':
                    print(f"  {param}: Skipped (constant parameter)")
                    continue
                
                sensitivity = self._compute_parameter_sensitivity(param, indicator)
                sensitivities.append({
                    'parameter': param,
                    'sensitivity': sensitivity['effect_size'],
                    'p_value': sensitivity['p_value'],
                    'method': sensitivity['method'],
                    'sample_size': sensitivity['sample_size'],
                    'conditional': param_info['conditional']
                })
            
            # Create DataFrame and sort by sensitivity
            sensitivity_df = pd.DataFrame(sensitivities)
            sensitivity_df = sensitivity_df.sort_values('sensitivity', ascending=False, key=abs)
            
            # Add significance flag
            sensitivity_df['significant'] = sensitivity_df['p_value'] < 0.05
            
            results[indicator] = sensitivity_df.head(top_n_params)
            
            # Print top sensitive parameters
            print("  Top sensitive parameters:")
            for _, row in results[indicator].head(5).iterrows():
                sig_marker = "*" if row['significant'] else " "
                cond_marker = " (CONDITIONAL)" if row['conditional'] else ""
                print(f"    {sig_marker} {row['parameter']}: {row['sensitivity']:.3f} "
                      f"(p={row['p_value']:.4f}){cond_marker}")
        
        return results
    
    def _compute_parameter_sensitivity(self, param: str, indicator: str) -> Dict[str, Any]:
        """
        Compute sensitivity of an indicator to a parameter.
        
        Uses different methods based on parameter type:
        - Continuous: Correlation coefficient
        - Categorical: Effect size from ANOVA/Kruskal-Wallis
        """
        df = self.analyzer.merged_df
        param_info = self.param_info[param]
        
        # Get valid data
        valid_mask = ~(df[param].isna() | df[indicator].isna())
        valid_data = df[valid_mask]
        
        if len(valid_data) < 10:
            return {'effect_size': 0, 'p_value': 1, 'method': 'insufficient_data', 'sample_size': len(valid_data)}
        
        param_values = valid_data[param]
        indicator_values = valid_data[indicator]
        
        if param_info['type'] == 'continuous':
            # Use Spearman correlation for continuous parameters
            corr, p_val = stats.spearmanr(param_values, indicator_values)
            return {
                'effect_size': abs(corr) if not np.isnan(corr) else 0,
                'p_value': p_val if not np.isnan(p_val) else 1,
                'method': 'spearman_correlation',
                'sample_size': len(valid_data)
            }
        
        elif param_info['type'] in ['categorical', 'categorical_numeric']:
            # Use Kruskal-Wallis test for categorical parameters
            groups = [indicator_values[param_values == cat].values 
                     for cat in param_values.unique()]
            groups = [g for g in groups if len(g) > 0]
            
            if len(groups) < 2:
                return {'effect_size': 0, 'p_value': 1, 'method': 'insufficient_groups', 'sample_size': len(valid_data)}
            
            try:
                h_stat, p_val = stats.kruskal(*groups)
                
                # Convert H-statistic to effect size (eta-squared approximation)
                n_total = len(indicator_values)
                effect_size = (h_stat - len(groups) + 1) / (n_total - len(groups))
                effect_size = max(0, min(1, effect_size))  # Clamp to [0, 1]
                
                return {
                    'effect_size': effect_size,
                    'p_value': p_val,
                    'method': 'kruskal_wallis',
                    'sample_size': len(valid_data)
                }
            except Exception as e:
                return {'effect_size': 0, 'p_value': 1, 'method': f'error_{str(e)[:20]}', 'sample_size': len(valid_data)}
        
        return {'effect_size': 0, 'p_value': 1, 'method': 'unknown_type', 'sample_size': len(valid_data)}
    
    def analyze_convergence_patterns(self, target_indicators: List[str] = None) -> Dict[str, Dict]:
        """
        Analyze convergence patterns of indicators over evaluations.
        
        Args:
            target_indicators: List of indicators to analyze (default: all)
            
        Returns:
            Dictionary with convergence analysis results
        """
        if target_indicators is None:
            target_indicators = self.analyzer.indicator_cols
        
        results = {}
        
        print("Analyzing convergence patterns...")
        
        for indicator in target_indicators:
            print(f"\nConvergence analysis for {indicator}:")
            
            # Group by evaluation and compute statistics
            eval_stats = self.analyzer.merged_df.groupby('Evaluation')[indicator].agg([
                'count', 'mean', 'median', 'std', 'min', 'max'
            ]).reset_index()
            
            # Compute convergence metrics
            convergence_metrics = self._compute_convergence_metrics(eval_stats, indicator)
            
            results[indicator] = {
                'evaluation_stats': eval_stats,
                'convergence_rate': convergence_metrics['convergence_rate'],
                'stability_index': convergence_metrics['stability_index'],
                'improvement_trend': convergence_metrics['improvement_trend'],
                'final_performance': convergence_metrics['final_performance']
            }
            
            print(f"  Convergence rate: {convergence_metrics['convergence_rate']:.4f}")
            print(f"  Stability index: {convergence_metrics['stability_index']:.4f}")
            print(f"  Improvement trend: {convergence_metrics['improvement_trend']}")
        
        return results
    
    def _compute_convergence_metrics(self, eval_stats: pd.DataFrame, indicator: str) -> Dict[str, Any]:
        """Compute various convergence metrics."""
        evaluations = eval_stats['Evaluation'].values
        medians = eval_stats['median'].values
        stds = eval_stats['std'].values
        
        # Convergence rate (slope of improvement)
        if len(evaluations) > 1:
            slope, _, r_value, p_value, _ = stats.linregress(evaluations, medians)
            convergence_rate = abs(slope)
        else:
            convergence_rate = 0
        
        # Stability index (decreasing variance over time)
        if len(stds) > 1 and not np.isnan(stds).all():
            std_slope, _, _, _, _ = stats.linregress(evaluations, np.nan_to_num(stds))
            stability_index = -std_slope  # Negative slope means increasing stability
        else:
            stability_index = 0
        
        # Improvement trend
        if len(medians) > 1:
            first_half = medians[:len(medians)//2]
            second_half = medians[len(medians)//2:]
            
            if np.mean(second_half) < np.mean(first_half):
                improvement_trend = "improving"
            elif np.mean(second_half) > np.mean(first_half):
                improvement_trend = "degrading"
            else:
                improvement_trend = "stable"
        else:
            improvement_trend = "insufficient_data"
        
        # Final performance
        final_performance = {
            'median': medians[-1] if len(medians) > 0 else np.nan,
            'std': stds[-1] if len(stds) > 0 else np.nan,
            'evaluation': evaluations[-1] if len(evaluations) > 0 else np.nan
        }
        
        return {
            'convergence_rate': convergence_rate,
            'stability_index': stability_index,
            'improvement_trend': improvement_trend,
            'final_performance': final_performance
        }
    
    def plot_parameter_sensitivity(self, sensitivity_results: Dict[str, pd.DataFrame],
                                 output_file: str = None, figsize: Tuple[int, int] = (12, 8)):
        """
        Plot parameter sensitivity analysis results.
        
        Args:
            sensitivity_results: Results from analyze_parameter_sensitivity
            output_file: Output file path (optional)
            figsize: Figure size
        """
        n_indicators = len(sensitivity_results)
        
        if n_indicators == 1:
            fig, ax = plt.subplots(1, 1, figsize=figsize)
            axes = [ax]
        else:
            fig, axes = plt.subplots(1, n_indicators, figsize=figsize, sharey=True)
            if n_indicators == 1:
                axes = [axes]
        
        for i, (indicator, sens_df) in enumerate(sensitivity_results.items()):
            ax = axes[i] if n_indicators > 1 else axes[0]
            
            # Prepare data
            params = sens_df['parameter'].values
            sensitivities = sens_df['sensitivity'].values
            significant = sens_df['significant'].values
            conditional = sens_df['conditional'].values
            
            # Create colors based on significance and conditional status
            colors = []
            for sig, cond in zip(significant, conditional):
                if sig and cond:
                    colors.append('orange')  # Significant conditional
                elif sig:
                    colors.append('darkblue')  # Significant non-conditional
                elif cond:
                    colors.append('lightcoral')  # Non-significant conditional
                else:
                    colors.append('lightgray')  # Non-significant non-conditional
            
            # Create horizontal bar plot
            y_pos = np.arange(len(params))
            bars = ax.barh(y_pos, sensitivities, color=colors, alpha=0.7)
            
            # Customize plot
            ax.set_yticks(y_pos)
            ax.set_yticklabels(params)
            ax.set_xlabel('Sensitivity (Effect Size)')
            ax.set_title(f'Parameter Sensitivity\n{indicator}')
            ax.grid(True, alpha=0.3, axis='x')
            
            # Add significance markers
            for j, (bar, sig) in enumerate(zip(bars, significant)):
                if sig:
                    ax.text(bar.get_width() + 0.01, bar.get_y() + bar.get_height()/2, 
                           '*', ha='left', va='center', fontweight='bold', fontsize=12)
        
        # Add legend
        from matplotlib.patches import Patch
        legend_elements = [
            Patch(facecolor='darkblue', alpha=0.7, label='Significant'),
            Patch(facecolor='orange', alpha=0.7, label='Significant Conditional'),
            Patch(facecolor='lightcoral', alpha=0.7, label='Non-significant Conditional'),
            Patch(facecolor='lightgray', alpha=0.7, label='Non-significant')
        ]
        
        plt.figlegend(handles=legend_elements, loc='upper right', bbox_to_anchor=(0.98, 0.98))
        
        plt.suptitle(f'Parameter Sensitivity Analysis\n'
                    f'Algorithm: {self.analyzer.metadata.get("Algorithm", "Unknown")} | '
                    f'Training Set: {self.analyzer.metadata.get("Problem Family", "Unknown")}', 
                    fontsize=14, y=0.95)
        
        plt.tight_layout()
        
        if output_file:
            plt.savefig(output_file, dpi=300, bbox_inches='tight')
            print(f"Sensitivity plot saved to {output_file}")
        
        plt.show()
    
    def generate_optimization_progress_plot(self, output_file: str = None):
        """
        Generate optimization progress plot using the superior plot_optimization_progress.py script.
        
        Args:
            output_file: Output file path (optional)
        """
        try:
            import subprocess
            import sys
            from pathlib import Path
            
            # Get available indicators
            indicators = self.analyzer.indicator_cols
            
            if len(indicators) >= 2:
                # Use dual Y-axis plot with the two main indicators
                cmd = [
                    sys.executable, 
                    str(Path(__file__).parent.parent / "visualization" / "plot_optimization_progress.py"),
                    str(self.analyzer.data_dir),
                    "--indicator1", indicators[0],
                    "--indicator2", indicators[1],
                    "--aggregation", "median",
                    "--show-range",
                    "--range-type", "quartiles"
                ]
            else:
                # Single indicator plot
                cmd = [
                    sys.executable,
                    str(Path(__file__).parent.parent / "visualization" / "plot_optimization_progress.py"),
                    str(self.analyzer.data_dir),
                    "--indicator1", indicators[0] if indicators else "EP",
                    "--aggregation", "median",
                    "--show-range",
                    "--range-type", "quartiles"
                ]
            
            if output_file:
                cmd.extend(["--output", output_file])
            # If no output file specified, it will use the default analysis_results directory
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode == 0:
                print(f"Optimization progress plot generated successfully")
                if output_file:
                    print(f"Saved to: {output_file}")
            else:
                print(f"Warning: Could not generate optimization progress plot: {result.stderr}")
                
        except Exception as e:
            print(f"Warning: Could not generate optimization progress plot: {e}")

def main():
    """Example usage of the PerformanceAnalyzer."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Analyze performance characteristics of optimization experiments')
    parser.add_argument('data_dir', help='Directory containing experiment data')
    parser.add_argument('--sensitivity-output', help='Output file for sensitivity plot')
    parser.add_argument('--convergence-output', help='Output file for convergence plot')
    args = parser.parse_args()
    
    # Initialize analyzers
    exp_analyzer = ExperimentAnalyzer(args.data_dir)
    perf_analyzer = PerformanceAnalyzer(exp_analyzer)
    
    # Analyze parameter sensitivity
    sensitivity_results = perf_analyzer.analyze_parameter_sensitivity()
    
    # Analyze convergence patterns
    convergence_results = perf_analyzer.analyze_convergence_patterns()
    
    # Create plots
    if args.sensitivity_output:
        perf_analyzer.plot_parameter_sensitivity(sensitivity_results, args.sensitivity_output)
    
    if args.convergence_output:
        perf_analyzer.generate_optimization_progress_plot(args.convergence_output)
    
    print("Performance analysis completed!")

if __name__ == "__main__":
    main()