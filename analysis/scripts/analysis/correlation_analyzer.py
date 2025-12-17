#!/usr/bin/env python3
"""
Parameter Correlation Analysis with Conditional Parameter Handling

This module provides sophisticated correlation analysis for optimization experiments,
specifically handling conditional parameters that are only active under certain conditions.

Features:
- Conditional correlation analysis
- Hierarchical parameter grouping
- Multiple correlation methods (Pearson, Spearman, Kendall)
- Statistical significance testing
- Interactive heatmaps with conditional parameter handling
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from scipy.stats import pearsonr, spearmanr, kendalltau
from typing import Dict, List, Tuple, Optional, Any
import warnings
from experiment_analyzer import ExperimentAnalyzer

class CorrelationAnalyzer:
    """Analyze correlations between parameters and indicators with conditional parameter support."""
    
    def __init__(self, analyzer: ExperimentAnalyzer):
        """
        Initialize with an ExperimentAnalyzer instance.
        
        Args:
            analyzer: ExperimentAnalyzer instance with loaded data
        """
        self.analyzer = analyzer
        self.param_info = analyzer.get_parameter_info()
        
    def compute_conditional_correlations(self, target_indicators: List[str] = None, 
                                       method: str = 'pearson',
                                       min_samples: int = 10) -> Dict[str, pd.DataFrame]:
        """
        Compute correlations between parameters and indicators, handling conditional parameters.
        
        Args:
            target_indicators: List of indicators to analyze (default: all)
            method: Correlation method ('pearson', 'spearman', 'kendall')
            min_samples: Minimum number of samples required for correlation calculation
            
        Returns:
            Dictionary with correlation matrices for each indicator
        """
        if target_indicators is None:
            target_indicators = self.analyzer.indicator_cols
        
        correlation_methods = {
            'pearson': pearsonr,
            'spearman': spearmanr,
            'kendall': kendalltau
        }
        
        if method not in correlation_methods:
            raise ValueError(f"Method must be one of {list(correlation_methods.keys())}")
        
        corr_func = correlation_methods[method]
        results = {}
        
        print(f"Computing {method} correlations with conditional parameter handling...")
        
        for indicator in target_indicators:
            print(f"\nAnalyzing correlations with {indicator}:")
            
            correlations = []
            p_values = []
            sample_sizes = []
            param_names = []
            
            for param in self.analyzer.parameter_cols:
                param_info = self.param_info[param]
                
                # Get valid data for this parameter-indicator pair
                valid_data = self._get_valid_data_for_pair(param, indicator)
                
                if len(valid_data) < min_samples:
                    print(f"  {param}: Insufficient data ({len(valid_data)} samples)")
                    correlations.append(np.nan)
                    p_values.append(np.nan)
                    sample_sizes.append(len(valid_data))
                    param_names.append(param)
                    continue
                
                # Check if parameter has only one unique value (constant parameter)
                unique_param_values = valid_data[param].nunique()
                if unique_param_values <= 1:
                    print(f"  {param}: Constant parameter (only {unique_param_values} unique value)")
                    correlations.append(np.nan)
                    p_values.append(np.nan)
                    sample_sizes.append(len(valid_data))
                    param_names.append(param)
                    continue
                
                # Handle different parameter types
                if param_info['type'] == 'continuous':
                    # Check for zero variance in continuous parameters
                    if valid_data[param].std() == 0:
                        print(f"  {param}: Zero variance (constant continuous parameter)")
                        corr, p_val = np.nan, np.nan
                    else:
                        # Direct correlation for continuous parameters
                        corr, p_val = corr_func(valid_data[param], valid_data[indicator])
                elif param_info['type'] in ['categorical', 'categorical_numeric']:
                    # Convert categorical to numeric for correlation
                    corr, p_val = self._categorical_correlation(
                        valid_data[param], valid_data[indicator], method)
                else:
                    corr, p_val = np.nan, np.nan
                
                correlations.append(corr)
                p_values.append(p_val)
                sample_sizes.append(len(valid_data))
                param_names.append(param)
                
                # Print detailed info for significant correlations
                if not np.isnan(corr) and abs(corr) > 0.3 and p_val < 0.05:
                    print(f"  {param}: r={corr:.3f}, p={p_val:.4f}, n={len(valid_data)} {'(CONDITIONAL)' if param_info['conditional'] else ''}")
            
            # Create results DataFrame
            results_df = pd.DataFrame({
                'parameter': param_names,
                'correlation': correlations,
                'p_value': p_values,
                'sample_size': sample_sizes,
                'significant': [p < 0.05 and not np.isnan(p) for p in p_values],
                'conditional': [self.param_info[p]['conditional'] for p in param_names]
            })
            
            results[indicator] = results_df
        
        return results
    
    def _get_valid_data_for_pair(self, param: str, indicator: str) -> pd.DataFrame:
        """Get valid (non-NaN) data for a parameter-indicator pair."""
        df = self.analyzer.merged_df
        
        # Remove rows where either parameter or indicator is NaN
        valid_mask = ~(df[param].isna() | df[indicator].isna())
        valid_data = df[valid_mask]
        
        return valid_data[[param, indicator, 'Evaluation', 'SolutionId']]
    
    def _categorical_correlation(self, categorical_series: pd.Series, 
                               continuous_series: pd.Series, method: str) -> Tuple[float, float]:
        """
        Compute correlation between categorical and continuous variables.
        
        Uses ANOVA F-statistic converted to correlation-like measure.
        """
        try:
            from scipy.stats import f_oneway
            
            # Group continuous values by categorical values
            groups = [continuous_series[categorical_series == cat].values 
                     for cat in categorical_series.unique()]
            
            # Remove empty groups
            groups = [g for g in groups if len(g) > 0]
            
            if len(groups) < 2:
                return np.nan, np.nan
            
            # Perform ANOVA
            f_stat, p_val = f_oneway(*groups)
            
            # Convert F-statistic to correlation-like measure (eta-squared)
            # eta^2 = SS_between / SS_total
            n_total = len(continuous_series)
            n_groups = len(groups)
            
            if n_total <= n_groups:
                return np.nan, np.nan
            
            eta_squared = f_stat * (n_groups - 1) / (f_stat * (n_groups - 1) + n_total - n_groups)
            correlation = np.sqrt(eta_squared)
            
            return correlation, p_val
            
        except Exception as e:
            print(f"Error in categorical correlation: {e}")
            return np.nan, np.nan
    
    def plot_correlation_heatmap(self, correlations: Dict[str, pd.DataFrame], 
                               output_file: str = None, figsize: Tuple[int, int] = (15, 10)):
        """
        Create correlation heatmap with conditional parameter highlighting.
        
        Args:
            correlations: Results from compute_conditional_correlations
            output_file: Output file path (optional)
            figsize: Figure size
        """
        n_indicators = len(correlations)
        
        if n_indicators == 1:
            fig, ax = plt.subplots(1, 1, figsize=figsize)
            axes = [ax]
        else:
            fig, axes = plt.subplots(1, n_indicators, figsize=figsize, sharey=True)
            if n_indicators == 1:
                axes = [axes]
        
        for i, (indicator, corr_df) in enumerate(correlations.items()):
            ax = axes[i] if n_indicators > 1 else axes[0]
            
            # Prepare data for heatmap
            params = corr_df['parameter'].values
            corrs = corr_df['correlation'].values
            p_vals = corr_df['p_value'].values
            conditional = corr_df['conditional'].values
            
            # Create correlation matrix (single column)
            corr_matrix = corrs.reshape(-1, 1)
            
            # Create mask for non-significant correlations
            mask = np.isnan(corrs) | (p_vals >= 0.05)
            mask = mask.reshape(-1, 1)
            
            # Create heatmap
            sns.heatmap(corr_matrix, 
                       annot=True, 
                       fmt='.3f',
                       cmap='RdBu_r',
                       center=0,
                       vmin=-1, vmax=1,
                       yticklabels=params,
                       xticklabels=[indicator],
                       mask=mask,
                       ax=ax,
                       cbar=i == n_indicators - 1)  # Only show colorbar on last plot
            
            # Highlight conditional parameters
            for j, (param, is_conditional) in enumerate(zip(params, conditional)):
                if is_conditional:
                    # Add border around conditional parameters
                    ax.add_patch(plt.Rectangle((0, j), 1, 1, fill=False, 
                                             edgecolor='orange', linewidth=3))
            
            ax.set_title(f'Correlations with {indicator}')
            ax.set_xlabel('')
            
            # Rotate y-axis labels for better readability
            if i == 0:
                ax.set_ylabel('Parameters')
                plt.setp(ax.get_yticklabels(), rotation=0, ha='right')
            else:
                ax.set_ylabel('')
        
        plt.suptitle(f'Parameter-Indicator Correlations\n'
                    f'Algorithm: {self.analyzer.metadata.get("Algorithm", "Unknown")} | '
                    f'Training Set: {self.analyzer.metadata.get("Problem Family", "Unknown")}\n'
                    f'Orange borders indicate conditional parameters', 
                    fontsize=14, y=0.98)
        
        plt.tight_layout()
        
        if output_file:
            plt.savefig(output_file, dpi=300, bbox_inches='tight')
            print(f"Correlation heatmap saved to {output_file}")
        
        plt.show()
    
    def analyze_parameter_groups(self, correlations: Dict[str, pd.DataFrame], 
                               correlation_threshold: float = 0.7) -> Dict[str, List[List[str]]]:
        """
        Group parameters based on their correlation patterns with indicators.
        
        Args:
            correlations: Results from compute_conditional_correlations
            correlation_threshold: Threshold for grouping parameters
            
        Returns:
            Dictionary with parameter groups for each indicator
        """
        groups = {}
        
        for indicator, corr_df in correlations.items():
            # Get significant correlations
            significant_corrs = corr_df[corr_df['significant'] & ~corr_df['correlation'].isna()]
            
            if len(significant_corrs) == 0:
                groups[indicator] = []
                continue
            
            # Group by correlation strength
            strong_positive = significant_corrs[significant_corrs['correlation'] > correlation_threshold]['parameter'].tolist()
            moderate_positive = significant_corrs[(significant_corrs['correlation'] > 0.3) & 
                                                (significant_corrs['correlation'] <= correlation_threshold)]['parameter'].tolist()
            moderate_negative = significant_corrs[(significant_corrs['correlation'] < -0.3) & 
                                                (significant_corrs['correlation'] >= -correlation_threshold)]['parameter'].tolist()
            strong_negative = significant_corrs[significant_corrs['correlation'] < -correlation_threshold]['parameter'].tolist()
            
            indicator_groups = []
            if strong_positive:
                indicator_groups.append(('Strong Positive', strong_positive))
            if moderate_positive:
                indicator_groups.append(('Moderate Positive', moderate_positive))
            if moderate_negative:
                indicator_groups.append(('Moderate Negative', moderate_negative))
            if strong_negative:
                indicator_groups.append(('Strong Negative', strong_negative))
            
            groups[indicator] = indicator_groups
        
        return groups
    
    def print_correlation_summary(self, correlations: Dict[str, pd.DataFrame]):
        """Print a summary of correlation analysis results."""
        print("\n" + "="*80)
        print("CORRELATION ANALYSIS SUMMARY")
        print("="*80)
        
        for indicator, corr_df in correlations.items():
            print(f"\n{indicator.upper()}:")
            print("-" * 40)
            
            # Overall statistics
            valid_corrs = corr_df[~corr_df['correlation'].isna()]
            significant_corrs = corr_df[corr_df['significant']]
            conditional_corrs = corr_df[corr_df['conditional'] & corr_df['significant']]
            
            print(f"  Total parameters analyzed: {len(corr_df)}")
            print(f"  Valid correlations: {len(valid_corrs)}")
            print(f"  Significant correlations (p<0.05): {len(significant_corrs)}")
            print(f"  Significant conditional parameters: {len(conditional_corrs)}")
            
            if len(significant_corrs) > 0:
                print(f"\n  Top correlations:")
                top_corrs = significant_corrs.nlargest(5, 'correlation', keep='all')
                for _, row in top_corrs.iterrows():
                    cond_marker = " (CONDITIONAL)" if row['conditional'] else ""
                    print(f"    {row['parameter']}: r={row['correlation']:.3f}, p={row['p_value']:.4f}{cond_marker}")
                
                if len(conditional_corrs) > 0:
                    print(f"\n  Conditional parameters with significant correlations:")
                    for _, row in conditional_corrs.iterrows():
                        print(f"    {row['parameter']}: r={row['correlation']:.3f}, p={row['p_value']:.4f}")

def main():
    """Example usage of the CorrelationAnalyzer."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Analyze parameter correlations in optimization experiments')
    parser.add_argument('data_dir', help='Directory containing experiment data')
    parser.add_argument('--method', choices=['pearson', 'spearman', 'kendall'], 
                       default='spearman', help='Correlation method')
    parser.add_argument('--output', help='Output file for heatmap')
    args = parser.parse_args()
    
    # Initialize analyzers
    exp_analyzer = ExperimentAnalyzer(args.data_dir)
    corr_analyzer = CorrelationAnalyzer(exp_analyzer)
    
    # Compute correlations
    correlations = corr_analyzer.compute_conditional_correlations(method=args.method)
    
    # Print summary
    corr_analyzer.print_correlation_summary(correlations)
    
    # Create heatmap
    output_file = args.output or f"correlation_heatmap_{args.method}.png"
    corr_analyzer.plot_correlation_heatmap(correlations, output_file)
    
    # Analyze parameter groups
    groups = corr_analyzer.analyze_parameter_groups(correlations)
    
    print(f"\nParameter groups saved and heatmap generated: {output_file}")

if __name__ == "__main__":
    main()