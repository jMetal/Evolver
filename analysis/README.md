# Analysis Tools

This directory contains comprehensive tools and scripts for analyzing multi-objective optimization experiment results, with special focus on parameter correlation analysis and conditional parameter handling.

## Structure

```
analysis/
├── scripts/
│   ├── visualization/     # Visualization utilities
│   │   ├── plot_optimization_progress.py      # Time series optimization progress
│   │   ├── visualize_training_set_results.py  # Pareto front visualization
│   │   └── README.md                          # Visualization documentation
│   ├── analysis/          # Comprehensive experiment analysis suite
│   │   ├── experiment_analyzer.py             # Base data loading and preprocessing
│   │   ├── correlation_analyzer.py            # Parameter correlation analysis
│   │   ├── performance_analyzer.py            # Sensitivity and convergence analysis
│   │   ├── comprehensive_analysis.py          # Complete analysis pipeline
│   │   └── run_analysis.py                    # Quick analysis runner
├── notebooks/             # Jupyter notebooks for interactive analysis
│   ├── Plot2D.ipynb       # 2D front visualization notebook
│   └── Plot3D.ipynb       # 3D front visualization notebook
├── sample_data/           # Sample data files for testing scripts
├── output/                # Generated plots and analysis results
└── requirements.txt       # Python dependencies
```

## Getting Started

1. Set up the environment:
```bash
# Activate the Conda environment
conda activate evolver

# Or install dependencies manually
cd analysis
pip install -r requirements.txt
```

2. Run analysis on your experiment data (see examples below)

## 🎯 Quick Analysis Runner

The easiest way to analyze your experiments:

```bash
# Full comprehensive analysis (recommended)
python scripts/analysis/run_analysis.py results/nsgaii/ZDT --full

# Just correlation analysis
python scripts/analysis/run_analysis.py results/nsgaii/ZDT --correlations

# Just parameter sensitivity
python scripts/analysis/run_analysis.py results/nsgaii/ZDT --sensitivity

# Parameter summary only
python scripts/analysis/run_analysis.py results/nsgaii/ZDT --summary
```

## 📊 Comprehensive Analysis Suite

Complete analysis including correlations, sensitivity, and convergence:

```bash
python scripts/analysis/comprehensive_analysis.py results/nsgaii/ZDT \
    --output-dir analysis_results --include-interactions
```

**Features:**
- **Parameter correlation analysis** with conditional parameter handling
- **Parameter sensitivity analysis** using effect sizes and statistical tests
- **Convergence pattern analysis** with stability metrics
- **Interactive visualizations** with correlation heatmaps
- **Conditional parameter detection** for hierarchical dependencies
- **Statistical significance testing** for all analyses
- **Comprehensive reports** in markdown format

## 🔍 Individual Analysis Components

### 1. Experiment Analyzer (Base)
```bash
python scripts/analysis/experiment_analyzer.py results/nsgaii/ZDT
```
- Loads and preprocesses experiment data
- Identifies parameter types (continuous, categorical, conditional)
- Detects conditional parameter dependencies
- Provides data summary and parameter statistics

### 2. Correlation Analyzer
```bash
python scripts/analysis/correlation_analyzer.py results/nsgaii/ZDT \
    --method spearman --output correlation_heatmap.png
```
- **Conditional correlation analysis** handling parameters with many NaN values
- **Multiple correlation methods** (Pearson, Spearman, Kendall)
- **Categorical-continuous correlations** using ANOVA-based methods
- **Statistical significance testing** with p-values
- **Interactive heatmaps** highlighting conditional parameters

### 3. Performance Analyzer
```bash
python scripts/analysis/performance_analyzer.py results/nsgaii/ZDT \
    --sensitivity-output sensitivity.png --convergence-output convergence.png
```
- **Parameter sensitivity analysis** using effect sizes
- **Convergence pattern analysis** with stability metrics
- **Performance distribution analysis** across parameter values
- **Statistical testing** (Kruskal-Wallis, Mann-Whitney U)

## 📈 Visualization Tools

### Optimization Progress Visualization
```bash
# Plot optimization progress with dual indicators and confidence intervals
python scripts/visualization/plot_optimization_progress.py results/nsgaii/ZDT \
    --indicator1 EP --indicator2 NHV \
    --aggregation median --show-range --range-type quartiles \
    --output optimization_progress.png

# List available indicators
python scripts/visualization/plot_optimization_progress.py results/nsgaii/ZDT --list-indicators
```

**Features:**
- **Time series visualization** with evaluation numbers on X-axis
- **Dual Y-axes** for comparing two indicators simultaneously
- **Confidence intervals** (quartiles, min-max, standard deviation)
- **Multiple aggregation methods** (min, max, mean, median, std)
- **Statistical summaries** showing min/median/max for each evaluation

### Pareto Front Visualization
```bash
# Basic usage (individual plots)
python scripts/visualization/visualize_training_set_results.py results/fronts/ZDT

# Grid view of all fronts
python scripts/visualization/visualize_training_set_results.py results/fronts/ZDT --grid

# Interactive Plotly visualization (useful for 3D)
python scripts/visualization/visualize_training_set_results.py results/fronts/DTLZ3D --interactive
```

## 🔧 Key Features for Conditional Parameters

The analysis suite is specifically designed to handle **conditional parameters** - parameters that are only active under certain conditions (many NaN values):

### Conditional Parameter Detection
- Automatically identifies parameters with >50% missing values
- Finds potential conditioning parameters through correlation analysis
- Handles hierarchical parameter dependencies

### Specialized Correlation Analysis
- **Valid data extraction** for each parameter-indicator pair
- **Conditional correlation computation** using only relevant samples
- **Statistical significance testing** with appropriate sample sizes
- **Visual highlighting** of conditional parameters in heatmaps

### Parameter Sensitivity with Conditions
- **Effect size computation** for different parameter types
- **Conditional sensitivity analysis** using valid data subsets
- **Statistical testing** appropriate for each parameter type
- **Significance flagging** with multiple testing considerations

## 📋 Expected Data Format

Your experiment directory should contain:
- `INDICATORS.csv` - Quality indicators (EP, NHV, IGD+, etc.)
- `CONFIGURATIONS.csv` - Algorithm parameter configurations
- `METADATA.txt` - Experiment metadata (optional)

### Example Data Structure
```
results/nsgaii/ZDT/
├── INDICATORS.csv      # Evaluation,SolutionId,EP,NHV,...
├── CONFIGURATIONS.csv  # Evaluation,SolutionId,param1,param2,...
└── METADATA.txt        # Algorithm: NSGA-II, Problems: ZDT1,ZDT2,...
```

## 📊 Generated Outputs

### Comprehensive Analysis Results
- **Correlation heatmaps** (Spearman and Pearson)
- **Parameter sensitivity plots** with significance markers
- **Optimization progress plots** with confidence intervals and dual Y-axes
- **Parameter interaction matrices** for continuous parameters
- **Detailed CSV files** with all numerical results
- **Markdown summary report** with key findings and recommendations

### Key Insights Provided
- **Most important parameters** across all indicators
- **Conditional parameter relationships** and dependencies
- **Parameter sensitivity rankings** with statistical significance
- **Convergence characteristics** and stability metrics
- **Correlation patterns** between parameters and performance
- **Recommendations** for algorithm configuration

## 🚀 Quick Start Example

```bash
# 1. Run comprehensive analysis
python scripts/analysis/run_analysis.py results/nsgaii/ZDT --full

# 2. Check the generated summary
cat analysis_results/analysis_summary.md

# 3. View visualizations (all stored in analysis_results/ by default)
open analysis_results/correlation_heatmap_spearman.png
open analysis_results/parameter_sensitivity.png
open analysis_results/optimization_progress.png
```

This will generate a complete analysis with all visualizations, statistical tests, and a comprehensive report highlighting the most important parameters and their relationships with performance indicators.

## 📚 Notebooks

- **Plot2D.ipynb**: Interactive notebook for visualizing 2D Pareto fronts
- **Plot3D.ipynb**: Interactive notebook for visualizing 3D Pareto fronts

## 🔬 Analysis Examples

### Real Data Analysis Results

Based on the ZDT experiment analysis, the suite can identify:

**Key Findings:**
- **Most Important Parameters**: `populationSizeWithArchive` (r=0.956 with NHV), `mutation` (r=0.993 with EP)
- **Conditional Parameters**: `blxAlphaCrossoverAlpha`, `uniformMutationPerturbation` (97.9% missing)
- **Strong Correlations**: Population size strongly correlates with both quality indicators
- **Parameter Dependencies**: Crossover and mutation parameters show hierarchical relationships

**Generated Visualizations:**
- Correlation heatmaps highlighting conditional parameters with orange borders
- Sensitivity plots showing effect sizes with significance markers
- Convergence plots showing optimization trends and stability over time
- Parameter interaction matrices for continuous parameter relationships

The analysis automatically handles the complexity of conditional parameters and provides actionable insights for algorithm configuration and parameter tuning.