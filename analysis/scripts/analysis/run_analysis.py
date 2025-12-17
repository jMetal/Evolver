#!/usr/bin/env python3
"""
Quick Analysis Runner

Simple script to run different types of analysis on optimization experiment data.

Usage Examples:
    # Full comprehensive analysis
    python run_analysis.py results/nsgaii/ZDT --full
    
    # Just correlations
    python run_analysis.py results/nsgaii/ZDT --correlations
    
    # Just sensitivity analysis
    python run_analysis.py results/nsgaii/ZDT --sensitivity
    
    # Custom output directory
    python run_analysis.py results/nsgaii/ZDT --full --output custom_results
"""

import sys
import argparse
from pathlib import Path

# Add the analysis directory to Python path
sys.path.append(str(Path(__file__).parent))

from experiment_analyzer import ExperimentAnalyzer
from correlation_analyzer import CorrelationAnalyzer
from performance_analyzer import PerformanceAnalyzer

def run_correlation_analysis(data_dir: str, output_dir: str = "results"):
    """Run correlation analysis only."""
    print("Running correlation analysis...")
    
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    
    analyzer = ExperimentAnalyzer(data_dir)
    corr_analyzer = CorrelationAnalyzer(analyzer)
    
    # Compute correlations
    correlations = corr_analyzer.compute_conditional_correlations(method='spearman')
    
    # Print summary
    corr_analyzer.print_correlation_summary(correlations)
    
    # Create heatmap
    corr_analyzer.plot_correlation_heatmap(
        correlations, 
        output_file=output_path / "correlation_heatmap.png"
    )
    
    # Save results
    for indicator, corr_df in correlations.items():
        filename = f"correlations_{indicator.lower()}.csv"
        corr_df.to_csv(output_path / filename, index=False)
    
    print(f"Correlation analysis complete. Results in {output_path}")

def run_sensitivity_analysis(data_dir: str, output_dir: str = "results"):
    """Run sensitivity analysis only."""
    print("Running sensitivity analysis...")
    
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    
    analyzer = ExperimentAnalyzer(data_dir)
    perf_analyzer = PerformanceAnalyzer(analyzer)
    
    # Analyze sensitivity
    sensitivity_results = perf_analyzer.analyze_parameter_sensitivity()
    
    # Create plot
    perf_analyzer.plot_parameter_sensitivity(
        sensitivity_results,
        output_file=output_path / "parameter_sensitivity.png"
    )
    
    # Save results
    for indicator, sens_df in sensitivity_results.items():
        filename = f"sensitivity_{indicator.lower()}.csv"
        sens_df.to_csv(output_path / filename, index=False)
    
    print(f"Sensitivity analysis complete. Results in {output_path}")

def run_convergence_analysis(data_dir: str, output_dir: str = "results"):
    """Run convergence analysis only."""
    print("Running convergence analysis...")
    
    output_path = Path(output_dir)
    output_path.mkdir(exist_ok=True)
    
    analyzer = ExperimentAnalyzer(data_dir)
    perf_analyzer = PerformanceAnalyzer(analyzer)
    
    # Analyze convergence patterns (statistical metrics)
    convergence_results = perf_analyzer.analyze_convergence_patterns()
    
    # Generate superior optimization progress plot (will use analysis_results by default)
    perf_analyzer.generate_optimization_progress_plot(
        output_file="analysis_results/optimization_progress.png"
    )
    
    # Save convergence statistics
    for indicator, conv_results in convergence_results.items():
        filename = f"convergence_{indicator.lower()}.csv"
        conv_results['evaluation_stats'].to_csv(output_path / filename, index=False)
    
    print(f"Convergence analysis complete. Results in {output_path}")
    print("Note: Using superior plot_optimization_progress.py for visualization")

def run_parameter_summary(data_dir: str):
    """Run parameter summary analysis only."""
    print("Running parameter summary...")
    
    analyzer = ExperimentAnalyzer(data_dir)
    analyzer.print_parameter_summary()
    
    print("Parameter summary complete.")

def main():
    parser = argparse.ArgumentParser(description='Quick analysis runner for optimization experiments')
    parser.add_argument('data_dir', help='Directory containing experiment data')
    parser.add_argument('--output', '-o', default='analysis_results', help='Output directory')
    
    # Analysis type options
    parser.add_argument('--full', action='store_true', help='Run comprehensive analysis (all types)')
    parser.add_argument('--correlations', action='store_true', help='Run correlation analysis')
    parser.add_argument('--sensitivity', action='store_true', help='Run sensitivity analysis')
    parser.add_argument('--convergence', action='store_true', help='Run convergence analysis')
    parser.add_argument('--summary', action='store_true', help='Show parameter summary')
    
    args = parser.parse_args()
    
    # Validate input
    data_path = Path(args.data_dir)
    if not data_path.exists():
        print(f"Error: Data directory {args.data_dir} does not exist")
        sys.exit(1)
    
    required_files = ['INDICATORS.csv', 'CONFIGURATIONS.csv']
    missing_files = [f for f in required_files if not (data_path / f).exists()]
    
    if missing_files:
        print(f"Error: Missing required files: {missing_files}")
        sys.exit(1)
    
    # Determine what to run
    if args.full:
        # Run comprehensive analysis
        from comprehensive_analysis import create_comprehensive_report
        create_comprehensive_report(args.data_dir, args.output)
    else:
        # Run specific analyses
        if args.summary:
            run_parameter_summary(args.data_dir)
        
        if args.correlations:
            run_correlation_analysis(args.data_dir, args.output)
        
        if args.sensitivity:
            run_sensitivity_analysis(args.data_dir, args.output)
        
        if args.convergence:
            run_convergence_analysis(args.data_dir, args.output)
        
        # If no specific analysis requested, show summary
        if not any([args.summary, args.correlations, args.sensitivity, args.convergence]):
            print("No specific analysis requested. Showing parameter summary:")
            run_parameter_summary(args.data_dir)

if __name__ == "__main__":
    main()