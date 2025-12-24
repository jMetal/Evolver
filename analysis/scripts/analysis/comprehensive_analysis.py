#!/usr/bin/env python3
"""
Comprehensive Experiment Analysis Suite

This script provides a complete analysis of optimization experiments including:
- Parameter correlation analysis with conditional parameter handling
- Parameter sensitivity analysis
- Convergence pattern analysis
- Interactive visualizations and reports

Usage:
    python comprehensive_analysis.py results/nsgaii/ZDT --output-dir analysis_results
"""

import os
import sys
import argparse
from pathlib import Path
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns

# Add the analysis directory to Python path
sys.path.append(str(Path(__file__).parent))

from experiment_analyzer import ExperimentAnalyzer
from correlation_analyzer import CorrelationAnalyzer
from performance_analyzer import PerformanceAnalyzer

def create_comprehensive_report(data_dir: str, output_dir: str = "analysis_results"):
    """
    Create a comprehensive analysis report for optimization experiments.
    
    Args:
        data_dir: Directory containing experiment data
        output_dir: Directory to save analysis results
    """
    # Create output directory
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    
    print("="*80)
    print("COMPREHENSIVE EXPERIMENT ANALYSIS")
    print("="*80)
    print(f"Data directory: {data_dir}")
    print(f"Output directory: {output_dir}")
    
    # Initialize main analyzer
    print("\n1. Loading and preprocessing data...")
    exp_analyzer = ExperimentAnalyzer(data_dir)
    
    # Print parameter summary
    print("\n2. Parameter analysis...")
    exp_analyzer.print_parameter_summary()
    
    # Initialize specialized analyzers
    corr_analyzer = CorrelationAnalyzer(exp_analyzer)
    perf_analyzer = PerformanceAnalyzer(exp_analyzer)
    
    # Correlation analysis
    print("\n3. Computing parameter correlations...")
    correlations_spearman = corr_analyzer.compute_conditional_correlations(method='spearman')
    correlations_pearson = corr_analyzer.compute_conditional_correlations(method='pearson')
    
    # Print correlation summary
    corr_analyzer.print_correlation_summary(correlations_spearman)
    
    # Parameter sensitivity analysis
    print("\n4. Analyzing parameter sensitivity...")
    sensitivity_results = perf_analyzer.analyze_parameter_sensitivity()
    
    # Convergence analysis
    print("\n5. Analyzing convergence patterns...")
    convergence_results = perf_analyzer.analyze_convergence_patterns()
    
    # Generate visualizations
    print("\n6. Generating visualizations...")
    
    # Correlation heatmaps
    corr_analyzer.plot_correlation_heatmap(
        correlations_spearman, 
        output_file=output_path / "correlation_heatmap_spearman.png",
        figsize=(12, 15)
    )
    
    corr_analyzer.plot_correlation_heatmap(
        correlations_pearson, 
        output_file=output_path / "correlation_heatmap_pearson.png",
        figsize=(12, 15)
    )
    
    # Sensitivity analysis plot
    perf_analyzer.plot_parameter_sensitivity(
        sensitivity_results,
        output_file=output_path / "parameter_sensitivity.png",
        figsize=(15, 10)
    )
    
    # Generate separate optimization progress plots for each indicator
    print("  Generating optimization progress plots...")
    try:
        import subprocess
        import sys
        
        # Get the indicators for the plot
        indicators = list(correlations_spearman.keys())
        
        # Generate separate plots for each indicator
        for i, indicator in enumerate(indicators):
            # Use the same indicator for both axes to focus on single indicator
            # (the script will still show both but we'll focus on one)
            cmd = [
                sys.executable, 
                str(Path(__file__).parent.parent / "visualization" / "plot_optimization_progress.py"),
                args.data_dir if 'args' in locals() else data_dir,
                "50",  # frequency
                "--indicator1", indicator,
                "--indicator2", indicator,  # Same indicator for both axes
                "--aggregation", "median",
                "--show-range",
                "--range-type", "quartiles",
                "--output", f"analysis_results/optimization_progress_{indicator.lower()}.png"
            ]
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode == 0:
                print(f"    Optimization progress plot for {indicator} saved to optimization_progress_{indicator.lower()}.png")
            else:
                print(f"    Warning: Could not generate optimization progress plot for {indicator}: {result.stderr}")
                
        # Also generate a combined plot if there are multiple indicators
        if len(indicators) >= 2:
            cmd = [
                sys.executable, 
                str(Path(__file__).parent.parent / "visualization" / "plot_optimization_progress.py"),
                args.data_dir if 'args' in locals() else data_dir,
                "50",  # frequency
                "--indicator1", indicators[0],
                "--indicator2", indicators[1],
                "--aggregation", "median",
                "--show-range",
                "--range-type", "quartiles",
                "--output", "analysis_results/optimization_progress_combined.png"
            ]
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            if result.returncode == 0:
                print(f"    Combined optimization progress plot saved to optimization_progress_combined.png")
            else:
                print(f"    Warning: Could not generate combined optimization progress plot: {result.stderr}")
                
    except Exception as e:
        print(f"    Warning: Could not generate optimization progress plots: {e}")
    
    # Save detailed results to CSV files
    print("\n7. Saving detailed results...")
    
    # Save correlation results
    for method, correlations in [('spearman', correlations_spearman), ('pearson', correlations_pearson)]:
        for indicator, corr_df in correlations.items():
            filename = f"correlations_{method}_{indicator.lower()}.csv"
            corr_df.to_csv(output_path / filename, index=False)
    
    # Save sensitivity results
    for indicator, sens_df in sensitivity_results.items():
        filename = f"sensitivity_{indicator.lower()}.csv"
        sens_df.to_csv(output_path / filename, index=False)
    
    # Save convergence results
    for indicator, conv_results in convergence_results.items():
        filename = f"convergence_{indicator.lower()}.csv"
        conv_results['evaluation_stats'].to_csv(output_path / filename, index=False)
    
    # Generate summary report
    print("\n8. Generating summary report...")
    generate_summary_report(exp_analyzer, correlations_spearman, sensitivity_results, 
                          convergence_results, output_path)
    
    print(f"\n{'='*80}")
    print("ANALYSIS COMPLETE!")
    print(f"Results saved to: {output_path.absolute()}")
    print("Generated files:")
    for file in sorted(output_path.glob("*")):
        print(f"  - {file.name}")
    print(f"{'='*80}")

def generate_summary_report(analyzer: ExperimentAnalyzer, correlations: dict, 
                          sensitivity: dict, convergence: dict, output_path: Path):
    """Generate a comprehensive summary report in markdown format."""
    
    report_file = output_path / "analysis_summary.md"
    
    with open(report_file, 'w') as f:
        f.write("# Experiment Analysis Summary\n\n")
        
        # Experiment info
        f.write("## Experiment Information\n\n")
        for key, value in analyzer.metadata.items():
            f.write(f"- **{key}**: {value}\n")
        
        f.write(f"\n- **Total Evaluations**: {analyzer.merged_df['Evaluation'].nunique()}\n")
        f.write(f"- **Total Solutions**: {len(analyzer.merged_df)}\n")
        f.write(f"- **Parameters**: {len(analyzer.parameter_cols)}\n")
        f.write(f"- **Quality Indicators**: {len(analyzer.indicator_cols)}\n\n")
        
        # Parameter summary
        f.write("## Parameter Analysis\n\n")
        param_info = analyzer.get_parameter_info()
        
        continuous_params = [p for p, info in param_info.items() if info['type'] == 'continuous']
        categorical_params = [p for p, info in param_info.items() if info['type'] in ['categorical', 'categorical_numeric']]
        conditional_params = [p for p, info in param_info.items() if info['conditional']]
        
        f.write(f"- **Continuous Parameters**: {len(continuous_params)}\n")
        f.write(f"- **Categorical Parameters**: {len(categorical_params)}\n")
        f.write(f"- **Conditional Parameters**: {len(conditional_params)}\n\n")
        
        if conditional_params:
            f.write("### Conditional Parameters\n\n")
            for param in conditional_params:
                info = param_info[param]
                f.write(f"- **{param}**: {info['missing_percentage']:.1f}% missing")
                if info['conditioning_params']:
                    f.write(f" (conditioned by: {', '.join(info['conditioning_params'])})")
                f.write("\n")
            f.write("\n")
        
        # Correlation analysis
        f.write("## Correlation Analysis\n\n")
        for indicator, corr_df in correlations.items():
            significant_corrs = corr_df[corr_df['significant']]
            f.write(f"### {indicator}\n\n")
            f.write(f"- **Significant correlations**: {len(significant_corrs)}/{len(corr_df)}\n")
            
            if len(significant_corrs) > 0:
                f.write("- **Top correlations**:\n")
                for _, row in significant_corrs.head(5).iterrows():
                    cond_marker = " (conditional)" if row['conditional'] else ""
                    f.write(f"  - {row['parameter']}: r={row['correlation']:.3f}, p={row['p_value']:.4f}{cond_marker}\n")
            f.write("\n")
        
        # Sensitivity analysis
        f.write("## Parameter Sensitivity Analysis\n\n")
        for indicator, sens_df in sensitivity.items():
            significant_sens = sens_df[sens_df['significant']]
            f.write(f"### {indicator}\n\n")
            f.write(f"- **Significant sensitivities**: {len(significant_sens)}/{len(sens_df)}\n")
            
            if len(significant_sens) > 0:
                f.write("- **Most sensitive parameters**:\n")
                for _, row in significant_sens.head(5).iterrows():
                    cond_marker = " (conditional)" if row['conditional'] else ""
                    f.write(f"  - {row['parameter']}: effect={row['sensitivity']:.3f}, p={row['p_value']:.4f}{cond_marker}\n")
            f.write("\n")
        
        # Convergence analysis
        f.write("## Convergence Analysis\n\n")
        for indicator, conv_results in convergence.items():
            f.write(f"### {indicator}\n\n")
            f.write(f"- **Convergence rate**: {conv_results['convergence_rate']:.4f}\n")
            f.write(f"- **Stability index**: {conv_results['stability_index']:.4f}\n")
            f.write(f"- **Improvement trend**: {conv_results['improvement_trend']}\n")
            
            final_perf = conv_results['final_performance']
            f.write(f"- **Final performance**: {final_perf['median']:.4f} ± {final_perf['std']:.4f}\n\n")
        
        # Recommendations
        f.write("## Key Findings and Recommendations\n\n")
        
        # Find most important parameters across all indicators
        all_important_params = set()
        for indicator in correlations.keys():
            corr_df = correlations[indicator]
            sens_df = sensitivity[indicator]
            
            # Parameters with strong correlations or high sensitivity
            important_corr = corr_df[(corr_df['significant']) & (abs(corr_df['correlation']) > 0.5)]['parameter'].tolist()
            important_sens = sens_df[(sens_df['significant']) & (sens_df['sensitivity'] > 0.3)]['parameter'].tolist()
            
            all_important_params.update(important_corr + important_sens)
        
        if all_important_params:
            f.write("### Most Important Parameters\n\n")
            f.write("The following parameters show strong correlations or high sensitivity across indicators:\n\n")
            for param in sorted(all_important_params):
                param_type = param_info[param]['type']
                conditional = " (conditional)" if param_info[param]['conditional'] else ""
                f.write(f"- **{param}** ({param_type}){conditional}\n")
            f.write("\n")
        
        # Conditional parameter insights
        if conditional_params:
            f.write("### Conditional Parameter Insights\n\n")
            f.write("Several parameters are conditional (active only under certain conditions). ")
            f.write("This suggests hierarchical parameter dependencies that should be considered ")
            f.write("when configuring the algorithm.\n\n")
        
        f.write("### Visualization Files\n\n")
        f.write("All visualization files are stored in the `analysis_results/` directory:\n\n")
        f.write("- `correlation_heatmap_spearman.png`: Spearman correlation heatmap\n")
        f.write("- `correlation_heatmap_pearson.png`: Pearson correlation heatmap\n")
        f.write("- `parameter_sensitivity.png`: Parameter sensitivity analysis\n")
        f.write("- `optimization_progress_ep.png`: EP indicator convergence plot\n")
        f.write("- `optimization_progress_nhv.png`: NHV indicator convergence plot\n")
        f.write("- `optimization_progress_combined.png`: Combined dual-axis plot (if multiple indicators)\n")

def create_parameter_interaction_analysis(analyzer: ExperimentAnalyzer, output_path: Path):
    """Create additional analysis for parameter interactions."""
    
    print("Creating parameter interaction analysis...")
    
    # Get continuous parameters for interaction analysis
    param_info = analyzer.get_parameter_info()
    continuous_params = [p for p, info in param_info.items() 
                        if info['type'] == 'continuous' and info['missing_percentage'] < 50]
    
    if len(continuous_params) < 2:
        print("Insufficient continuous parameters for interaction analysis")
        return
    
    # Create pairwise correlation matrix for continuous parameters
    df_continuous = analyzer.merged_df[continuous_params].dropna()
    
    if len(df_continuous) < 10:
        print("Insufficient data for parameter interaction analysis")
        return
    
    # Compute parameter-parameter correlations
    param_corr_matrix = df_continuous.corr(method='spearman')
    
    # Plot parameter interaction heatmap
    plt.figure(figsize=(12, 10))
    mask = np.triu(np.ones_like(param_corr_matrix, dtype=bool))
    
    sns.heatmap(param_corr_matrix, 
                mask=mask,
                annot=True, 
                fmt='.3f',
                cmap='RdBu_r',
                center=0,
                vmin=-1, vmax=1,
                square=True,
                cbar_kws={"shrink": .8})
    
    plt.title('Parameter-Parameter Correlations\n(Spearman Correlation)', fontsize=14)
    plt.tight_layout()
    plt.savefig(output_path / "parameter_interactions.png", dpi=300, bbox_inches='tight')
    plt.close()
    
    # Save correlation matrix
    param_corr_matrix.to_csv(output_path / "parameter_correlations.csv")
    
    print("Parameter interaction analysis saved")

def main():
    """Main function for comprehensive analysis."""
    parser = argparse.ArgumentParser(description='Comprehensive optimization experiment analysis')
    parser.add_argument('data_dir', help='Directory containing experiment data (INDICATORS.csv, CONFIGURATIONS.csv, METADATA.txt)')
    parser.add_argument('--output-dir', default='analysis_results', help='Output directory for results')
    parser.add_argument('--include-interactions', action='store_true', help='Include parameter interaction analysis')
    
    args = parser.parse_args()
    
    # Validate input directory
    data_path = Path(args.data_dir)
    if not data_path.exists():
        print(f"Error: Data directory {args.data_dir} does not exist")
        sys.exit(1)
    
    required_files = ['INDICATORS.csv', 'CONFIGURATIONS.csv', 'METADATA.txt']
    missing_files = [f for f in required_files if not (data_path / f).exists()]
    
    if missing_files:
        print(f"Error: Missing required files: {missing_files}")
        sys.exit(1)
    
    try:
        # Run comprehensive analysis
        create_comprehensive_report(args.data_dir, args.output_dir)
        
        # Optional parameter interaction analysis
        if args.include_interactions:
            output_path = Path(args.output_dir)
            analyzer = ExperimentAnalyzer(args.data_dir)
            create_parameter_interaction_analysis(analyzer, output_path)
        
    except Exception as e:
        print(f"Error during analysis: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()