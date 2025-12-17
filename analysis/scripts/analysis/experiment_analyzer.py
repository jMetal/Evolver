#!/usr/bin/env python3
"""
Comprehensive Experiment Analysis Suite

This module provides utilities for analyzing multi-objective optimization experiments
with focus on parameter correlations, conditional parameter handling, and performance analysis.

Features:
- Parameter correlation analysis with conditional parameter handling
- Performance vs parameter analysis
- Convergence analysis
- Statistical significance testing
- Interactive visualizations

Dependencies:
    - pandas, numpy, matplotlib, seaborn, scipy, plotly
"""

import os
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path
from typing import Dict, List, Tuple, Optional, Any
from scipy import stats
from scipy.stats import pearsonr, spearmanr
import warnings
warnings.filterwarnings('ignore')

class ExperimentAnalyzer:
    """Main class for analyzing optimization experiments."""
    
    def __init__(self, data_dir: str):
        """
        Initialize analyzer with experiment data.
        
        Args:
            data_dir: Directory containing INDICATORS.csv, CONFIGURATIONS.csv, METADATA.txt
        """
        self.data_dir = Path(data_dir)
        self.indicators_df = None
        self.configurations_df = None
        self.metadata = {}
        self.merged_df = None
        
        self._load_data()
        self._preprocess_data()
    
    def _load_data(self):
        """Load all experiment data files."""
        print(f"Loading experiment data from {self.data_dir}")
        
        # Load indicators
        indicators_file = self.data_dir / 'INDICATORS.csv'
        if indicators_file.exists():
            self.indicators_df = pd.read_csv(indicators_file)
            print(f"Loaded indicators: {len(self.indicators_df)} rows, {len(self.indicators_df.columns)} columns")
        else:
            raise FileNotFoundError(f"INDICATORS.csv not found in {self.data_dir}")
        
        # Load configurations
        configs_file = self.data_dir / 'CONFIGURATIONS.csv'
        if configs_file.exists():
            self.configurations_df = pd.read_csv(configs_file)
            print(f"Loaded configurations: {len(self.configurations_df)} rows, {len(self.configurations_df.columns)} columns")
        else:
            raise FileNotFoundError(f"CONFIGURATIONS.csv not found in {self.data_dir}")
        
        # Load metadata
        metadata_file = self.data_dir / 'METADATA.txt'
        if metadata_file.exists():
            with open(metadata_file, 'r') as f:
                content = f.read()
            
            for line in content.split('\n'):
                if ':' in line and not line.startswith('=') and not line.startswith('-'):
                    key, value = line.split(':', 1)
                    self.metadata[key.strip()] = value.strip()
        
        print(f"Experiment: {self.metadata.get('Algorithm', 'Unknown')} on {self.metadata.get('Problem Family', 'Unknown')}")
    
    def _preprocess_data(self):
        """Preprocess and merge data for analysis."""
        # Merge indicators and configurations
        self.merged_df = pd.merge(
            self.indicators_df, 
            self.configurations_df, 
            on=['Evaluation', 'SolutionId'], 
            how='inner'
        )
        
        print(f"Merged dataset: {len(self.merged_df)} rows")
        
        # Identify parameter columns (exclude metadata columns)
        metadata_cols = ['Evaluation', 'SolutionId']
        indicator_cols = [col for col in self.indicators_df.columns if col not in metadata_cols]
        self.parameter_cols = [col for col in self.configurations_df.columns if col not in metadata_cols]
        self.indicator_cols = indicator_cols
        
        print(f"Found {len(self.parameter_cols)} parameters and {len(self.indicator_cols)} indicators")
    
    def get_parameter_info(self) -> Dict[str, Dict]:
        """
        Analyze parameter types and conditional relationships.
        
        Returns:
            Dictionary with parameter information including type, unique values, and dependencies
        """
        param_info = {}
        
        for param in self.parameter_cols:
            series = self.merged_df[param]
            
            # Basic statistics
            info = {
                'type': 'unknown',
                'unique_values': series.nunique(),
                'missing_count': series.isna().sum(),
                'missing_percentage': (series.isna().sum() / len(series)) * 100
            }
            
            # Determine parameter type
            non_null_series = series.dropna()
            if len(non_null_series) > 0:
                # Check if parameter is constant (only one unique value)
                if non_null_series.nunique() == 1:
                    info['type'] = 'constant'
                    info['constant_value'] = non_null_series.iloc[0]
                elif non_null_series.dtype in ['int64', 'float64']:
                    if non_null_series.nunique() <= 10:
                        info['type'] = 'categorical_numeric'
                        info['categories'] = sorted(non_null_series.unique())
                    else:
                        info['type'] = 'continuous'
                        info['min'] = non_null_series.min()
                        info['max'] = non_null_series.max()
                        info['mean'] = non_null_series.mean()
                        info['std'] = non_null_series.std()
                else:
                    info['type'] = 'categorical'
                    info['categories'] = list(non_null_series.unique())
            
            # Check if parameter is conditional (has many NaN values)
            if info['missing_percentage'] > 50:
                info['conditional'] = True
                # Find potential conditioning parameters
                info['conditioning_params'] = self._find_conditioning_parameters(param)
            else:
                info['conditional'] = False
            
            param_info[param] = info
        
        return param_info
    
    def _find_conditioning_parameters(self, param: str) -> List[str]:
        """Find parameters that might condition the given parameter."""
        conditioning_params = []
        param_series = self.merged_df[param]
        
        for other_param in self.parameter_cols:
            if other_param == param:
                continue
            
            # Check if the presence of param values correlates with other_param values
            param_present = ~param_series.isna()
            other_series = self.merged_df[other_param]
            
            # Group by other_param and check param presence rate
            if not other_series.isna().all():
                grouped = self.merged_df.groupby(other_series, dropna=False)[param].apply(lambda x: (~x.isna()).mean())
                if len(grouped.unique()) > 1 and grouped.max() - grouped.min() > 0.5:
                    conditioning_params.append(other_param)
        
        return conditioning_params
    
    def print_parameter_summary(self):
        """Print a comprehensive summary of all parameters."""
        param_info = self.get_parameter_info()
        
        print("\n" + "="*80)
        print("PARAMETER ANALYSIS SUMMARY")
        print("="*80)
        
        continuous_params = []
        categorical_params = []
        conditional_params = []
        constant_params = []
        
        for param, info in param_info.items():
            print(f"\n{param}:")
            print(f"  Type: {info['type']}")
            print(f"  Unique values: {info['unique_values']}")
            print(f"  Missing: {info['missing_count']} ({info['missing_percentage']:.1f}%)")
            
            if info['conditional']:
                print(f"  CONDITIONAL parameter")
                if info['conditioning_params']:
                    print(f"  Likely conditioned by: {', '.join(info['conditioning_params'])}")
                conditional_params.append(param)
            
            if info['type'] == 'constant':
                print(f"  CONSTANT parameter (value: {info['constant_value']})")
                constant_params.append(param)
            elif info['type'] == 'continuous':
                print(f"  Range: [{info['min']:.4f}, {info['max']:.4f}]")
                print(f"  Mean ± Std: {info['mean']:.4f} ± {info['std']:.4f}")
                continuous_params.append(param)
            elif info['type'] in ['categorical', 'categorical_numeric']:
                print(f"  Categories: {info.get('categories', [])}")
                categorical_params.append(param)
        
        print(f"\n" + "-"*80)
        print(f"SUMMARY:")
        print(f"  Continuous parameters: {len(continuous_params)}")
        print(f"  Categorical parameters: {len(categorical_params)}")
        print(f"  Conditional parameters: {len(conditional_params)}")
        print(f"  Constant parameters: {len(constant_params)}")
        print(f"  Total parameters: {len(param_info)}")
        print(f"  Quality indicators: {len(self.indicator_cols)}")
        
        if constant_params:
            print(f"\n  Constant parameters (excluded from correlation analysis):")
            for param in constant_params:
                value = param_info[param]['constant_value']
                print(f"    {param}: {value}")
    
    def get_data_summary(self) -> Dict[str, Any]:
        """Get a summary of the loaded data."""
        return {
            'experiment_info': self.metadata,
            'data_shape': self.merged_df.shape,
            'evaluations': sorted(self.merged_df['Evaluation'].unique()),
            'num_evaluations': self.merged_df['Evaluation'].nunique(),
            'solutions_per_evaluation': self.merged_df.groupby('Evaluation').size().describe(),
            'indicators': self.indicator_cols,
            'parameters': self.parameter_cols,
            'parameter_info': self.get_parameter_info()
        }

def main():
    """Example usage of the ExperimentAnalyzer."""
    import argparse
    
    parser = argparse.ArgumentParser(description='Analyze optimization experiment data')
    parser.add_argument('data_dir', help='Directory containing experiment data')
    args = parser.parse_args()
    
    # Initialize analyzer
    analyzer = ExperimentAnalyzer(args.data_dir)
    
    # Print parameter summary
    analyzer.print_parameter_summary()
    
    # Get data summary
    summary = analyzer.get_data_summary()
    print(f"\nExperiment covers {summary['num_evaluations']} evaluation points")
    print(f"Solutions per evaluation: {summary['solutions_per_evaluation']['mean']:.1f} ± {summary['solutions_per_evaluation']['std']:.1f}")

if __name__ == "__main__":
    main()