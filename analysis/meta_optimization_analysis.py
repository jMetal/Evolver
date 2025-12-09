#!/usr/bin/env python3
"""
Meta-optimization Analysis Tool for Evolver

Analyzes the output of meta-optimization experiments to identify:
- Parameter importance and correlations
- Principal components in the configuration space
- Convergence patterns over evaluations

Usage:
    python meta_optimization_analysis.py --input ../RESULTS/NSGAII/DTLZ3 --output ./output
"""

import argparse
import re
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from scipy import stats
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler


# Conditional parameter mappings
# Maps parent parameter -> {parent_value: [conditional_params]}
# Values are the encoded numeric values from the CSV files
CONDITIONAL_PARAMETERS = {
    "crossover": {
        0.0: ["sbxDistributionIndex"],  # SBX
        1.0: ["blxAlphaCrossoverAlpha"],  # BLX_ALPHA
        # 2.0: wholeArithmetic (no conditional params)
    },
    "mutation": {
        0.0: ["polynomialMutationDistributionIndex"],  # polynomial
        1.0: ["linkedPolynomialMutationDistributionIndex"],  # linkedPolynomial
        2.0: ["uniformMutationPerturbation"],  # uniform
        3.0: ["nonUniformMutationPerturbation"],  # nonUniform
    },
    "algorithmResult": {
        1.0: ["populationSizeWithArchive", "archiveType"],  # externalArchive
        # 0.0: population (no conditional params)
    },
    "selection": {
        0.0: ["selectionTournamentSize"],  # tournament
        # 1.0: random (no conditional params)
    },
}

# Reverse mapping: conditional param -> (parent_param, valid_parent_value)
PARAM_TO_PARENT = {}
for parent, value_map in CONDITIONAL_PARAMETERS.items():
    for value, params in value_map.items():
        for param in params:
            PARAM_TO_PARENT[param] = (parent, value)


def load_configurations(results_dir: Path) -> tuple[pd.DataFrame, pd.DataFrame]:
    """
    Load all configuration files and objective values from a results directory.
    
    Returns:
        configs_df: DataFrame with parameter values indexed by evaluation number
        objectives_df: DataFrame with objective values indexed by evaluation number
    """
    configs = []
    objectives = []
    
    # Find all DoubleValues configuration files
    config_pattern = re.compile(r"VAR\..*\.Conf\.DoubleValues\.(\d+)\.csv")
    fun_pattern = re.compile(r"FUN\..*\.(\d+)\.csv")
    
    results_path = Path(results_dir)
    
    # Load configurations
    for config_file in sorted(results_path.glob("VAR.*.Conf.DoubleValues.*.csv")):
        match = config_pattern.search(config_file.name)
        if match:
            eval_num = int(match.group(1))
            df = pd.read_csv(config_file)
            df["evaluation"] = eval_num
            configs.append(df)
    
    # Load objectives
    for fun_file in sorted(results_path.glob("FUN.*.csv")):
        match = fun_pattern.search(fun_file.name)
        if match:
            eval_num = int(match.group(1))
            obj_df = pd.read_csv(fun_file, header=None, names=["Epsilon", "NormHypervolume"])
            obj_df["evaluation"] = eval_num
            objectives.append(obj_df)
    
    if not configs:
        raise ValueError(f"No configuration files found in {results_dir}")
    
    configs_df = pd.concat(configs, ignore_index=True)
    objectives_df = pd.concat(objectives, ignore_index=True) if objectives else pd.DataFrame()
    
    return configs_df, objectives_df


def analyze_correlations(configs_df: pd.DataFrame, objectives_df: pd.DataFrame, 
                         output_dir: Path) -> pd.DataFrame:
    """
    Analyze correlations between parameters and objectives.
    
    Handles conditional parameters by only computing correlations on samples
    where the parent parameter has the appropriate value.
    """
    print("\n=== Correlation Analysis ===")
    
    # Get numeric columns (excluding evaluation)
    param_cols = [col for col in configs_df.columns if col != "evaluation"]
    
    # Merge with objectives (use mean objective per evaluation)
    obj_means = objectives_df.groupby("evaluation").mean().reset_index()
    merged = configs_df.merge(obj_means, on="evaluation", how="inner")
    
    # Calculate correlations with objectives
    correlations = {}
    for param in param_cols:
        if merged[param].std() > 0:  # Skip constant parameters
            # Check if this is a conditional parameter
            if param in PARAM_TO_PARENT:
                parent_param, valid_value = PARAM_TO_PARENT[param]
                if parent_param in merged.columns:
                    # Filter to only rows where parent has the valid value
                    mask = merged[parent_param] == valid_value
                    filtered = merged[mask]
                    n_samples = len(filtered)
                    
                    if n_samples < 3:  # Not enough samples for correlation
                        continue
                    
                    if filtered[param].std() > 0:
                        corr_eps, p_eps = stats.spearmanr(filtered[param], filtered["Epsilon"])
                        corr_nhv, p_nhv = stats.spearmanr(filtered[param], filtered["NormHypervolume"])
                        correlations[param] = {
                            "Epsilon_corr": corr_eps,
                            "Epsilon_pvalue": p_eps,
                            "NormHV_corr": corr_nhv,
                            "NormHV_pvalue": p_nhv,
                            "n_samples": n_samples,
                            "is_conditional": True,
                            "parent_param": parent_param,
                            "parent_value": valid_value,
                        }
                else:
                    # Parent not in data, skip
                    continue
            else:
                # Non-conditional parameter: use all samples
                corr_eps, p_eps = stats.spearmanr(merged[param], merged["Epsilon"])
                corr_nhv, p_nhv = stats.spearmanr(merged[param], merged["NormHypervolume"])
                correlations[param] = {
                    "Epsilon_corr": corr_eps,
                    "Epsilon_pvalue": p_eps,
                    "NormHV_corr": corr_nhv,
                    "NormHV_pvalue": p_nhv,
                    "n_samples": len(merged),
                    "is_conditional": False,
                    "parent_param": None,
                    "parent_value": None,
                }
    
    corr_df = pd.DataFrame(correlations).T
    corr_df["abs_mean_corr"] = (corr_df["Epsilon_corr"].abs() + corr_df["NormHV_corr"].abs()) / 2
    corr_df = corr_df.sort_values("abs_mean_corr", ascending=False)
    
    print("\nTop 10 parameters by correlation with objectives:")
    print("(n = number of valid samples for conditional parameters)")
    display_cols = ["Epsilon_corr", "NormHV_corr", "abs_mean_corr", "n_samples", "is_conditional"]
    print(corr_df.head(10)[display_cols])
    
    # Show conditional parameters separately
    conditional_params = corr_df[corr_df["is_conditional"] == True]
    if len(conditional_params) > 0:
        print("\n--- Conditional Parameters (filtered by parent value) ---")
        for param, row in conditional_params.iterrows():
            print(f"  {param}: n={int(row['n_samples'])} samples "
                  f"(when {row['parent_param']}={row['parent_value']})")
    
    # Save correlation results
    corr_df.to_csv(output_dir / "parameter_correlations.csv")
    
    # Plot correlation heatmap for top parameters
    top_params = corr_df.head(15).index.tolist()
    
    fig, ax = plt.subplots(figsize=(10, 8))
    corr_matrix = merged[top_params + ["Epsilon", "NormHypervolume"]].corr()
    sns.heatmap(corr_matrix, annot=True, fmt=".2f", cmap="RdBu_r", center=0,
                ax=ax, square=True, linewidths=0.5)
    plt.title("Parameter-Objective Correlation Matrix (Top 15 Parameters)")
    plt.tight_layout()
    plt.savefig(output_dir / "correlation_heatmap.png", dpi=150)
    plt.close()
    
    return corr_df


def perform_pca(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                output_dir: Path) -> tuple[PCA, np.ndarray]:
    """
    Perform PCA on the configuration space.
    """
    print("\n=== PCA Analysis ===")
    
    # Get numeric parameter columns
    param_cols = [col for col in configs_df.columns if col != "evaluation"]
    
    # Remove constant columns
    non_constant = [col for col in param_cols if configs_df[col].std() > 0]
    X = configs_df[non_constant].values
    
    # Standardize
    scaler = StandardScaler()
    X_scaled = scaler.fit_transform(X)
    
    # PCA
    pca = PCA()
    X_pca = pca.fit_transform(X_scaled)
    
    # Explained variance
    cumsum = np.cumsum(pca.explained_variance_ratio_)
    n_90 = np.argmax(cumsum >= 0.90) + 1
    n_95 = np.argmax(cumsum >= 0.95) + 1
    
    print(f"Components for 90% variance: {n_90}")
    print(f"Components for 95% variance: {n_95}")
    print(f"Total parameters: {len(non_constant)}")
    
    # Scree plot
    fig, axes = plt.subplots(1, 2, figsize=(14, 5))
    
    # Individual variance
    axes[0].bar(range(1, len(pca.explained_variance_ratio_) + 1), 
                pca.explained_variance_ratio_, alpha=0.7)
    axes[0].set_xlabel("Principal Component")
    axes[0].set_ylabel("Explained Variance Ratio")
    axes[0].set_title("Scree Plot")
    
    # Cumulative variance
    axes[1].plot(range(1, len(cumsum) + 1), cumsum, 'b-o', markersize=4)
    axes[1].axhline(y=0.90, color='r', linestyle='--', label='90% variance')
    axes[1].axhline(y=0.95, color='g', linestyle='--', label='95% variance')
    axes[1].set_xlabel("Number of Components")
    axes[1].set_ylabel("Cumulative Explained Variance")
    axes[1].set_title("Cumulative Explained Variance")
    axes[1].legend()
    
    plt.tight_layout()
    plt.savefig(output_dir / "pca_variance.png", dpi=150)
    plt.close()
    
    # PC loadings for top components
    loadings = pd.DataFrame(
        pca.components_[:5].T,
        columns=[f"PC{i+1}" for i in range(5)],
        index=non_constant
    )
    loadings["PC1_abs"] = loadings["PC1"].abs()
    loadings = loadings.sort_values("PC1_abs", ascending=False)
    
    print("\nTop loadings for PC1:")
    print(loadings.head(10)[["PC1", "PC2", "PC3"]])
    
    loadings.to_csv(output_dir / "pca_loadings.csv")
    
    # 2D PCA scatter with objective coloring
    if not objectives_df.empty:
        obj_means = objectives_df.groupby("evaluation").mean().reset_index()
        merged = configs_df.merge(obj_means, on="evaluation", how="inner")
        
        fig, axes = plt.subplots(1, 2, figsize=(14, 6))
        
        # Match PCA indices with merged data
        merged_params = merged[non_constant].values
        merged_scaled = scaler.transform(merged_params)
        merged_pca = pca.transform(merged_scaled)
        
        sc1 = axes[0].scatter(merged_pca[:, 0], merged_pca[:, 1], 
                              c=merged["Epsilon"], cmap="viridis", alpha=0.7)
        axes[0].set_xlabel("PC1")
        axes[0].set_ylabel("PC2")
        axes[0].set_title("PCA - Colored by Epsilon")
        plt.colorbar(sc1, ax=axes[0], label="Epsilon")
        
        sc2 = axes[1].scatter(merged_pca[:, 0], merged_pca[:, 1], 
                              c=merged["NormHypervolume"], cmap="viridis", alpha=0.7)
        axes[1].set_xlabel("PC1")
        axes[1].set_ylabel("PC2")
        axes[1].set_title("PCA - Colored by NormHypervolume")
        plt.colorbar(sc2, ax=axes[1], label="NormHypervolume")
        
        plt.tight_layout()
        plt.savefig(output_dir / "pca_scatter.png", dpi=150)
        plt.close()
    
    return pca, X_pca


def analyze_convergence(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                        output_dir: Path, top_params: list[str]):
    """
    Analyze how parameters converge over evaluations.
    """
    print("\n=== Convergence Analysis ===")
    
    # Plot parameter evolution for top correlated parameters
    n_params = min(6, len(top_params))
    fig, axes = plt.subplots(2, 3, figsize=(15, 10))
    axes = axes.flatten()
    
    for i, param in enumerate(top_params[:n_params]):
        ax = axes[i]
        grouped = configs_df.groupby("evaluation")[param].agg(["mean", "std", "min", "max"])
        grouped = grouped.sort_index()
        
        ax.fill_between(grouped.index, grouped["min"], grouped["max"], alpha=0.2)
        ax.plot(grouped.index, grouped["mean"], 'b-', linewidth=2, label="Mean")
        ax.set_xlabel("Evaluation")
        ax.set_ylabel(param)
        ax.set_title(f"Evolution of {param}")
    
    plt.tight_layout()
    plt.savefig(output_dir / "parameter_convergence.png", dpi=150)
    plt.close()
    
    # Objective convergence
    if not objectives_df.empty:
        fig, axes = plt.subplots(1, 2, figsize=(14, 5))
        
        for i, obj in enumerate(["Epsilon", "NormHypervolume"]):
            ax = axes[i]
            grouped = objectives_df.groupby("evaluation")[obj].agg(["mean", "min", "max"])
            grouped = grouped.sort_index()
            
            ax.fill_between(grouped.index, grouped["min"], grouped["max"], alpha=0.3)
            ax.plot(grouped.index, grouped["mean"], 'b-', linewidth=2, label="Mean")
            ax.plot(grouped.index, grouped["min"], 'g--', linewidth=1, label="Best")
            ax.set_xlabel("Evaluation")
            ax.set_ylabel(obj)
            ax.set_title(f"Convergence of {obj}")
            ax.legend()
        
        plt.tight_layout()
        plt.savefig(output_dir / "objective_convergence.png", dpi=150)
        plt.close()


def analyze_categorical_parameters(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                                   output_dir: Path):
    """
    Analyze categorical parameters (encoded as integers).
    """
    print("\n=== Categorical Parameter Analysis ===")
    
    categorical_params = [
        "algorithmResult", "archiveType", "createInitialSolutions",
        "variation", "crossover", "crossoverRepairStrategy",
        "mutation", "mutationRepairStrategy", "selection"
    ]
    
    # Filter existing categorical params
    existing_cats = [p for p in categorical_params if p in configs_df.columns]
    
    if not existing_cats or objectives_df.empty:
        print("No categorical parameters or objectives to analyze")
        return
    
    obj_means = objectives_df.groupby("evaluation").mean().reset_index()
    merged = configs_df.merge(obj_means, on="evaluation", how="inner")
    
    # Box plots for each categorical parameter
    n_cats = len(existing_cats)
    n_rows = (n_cats + 2) // 3
    
    fig, axes = plt.subplots(n_rows, 3, figsize=(15, 4 * n_rows))
    axes = axes.flatten() if n_rows > 1 else [axes] if n_cats == 1 else axes
    
    for i, param in enumerate(existing_cats):
        if i < len(axes):
            ax = axes[i]
            merged.boxplot(column="Epsilon", by=param, ax=ax)
            ax.set_title(f"Epsilon by {param}")
            ax.set_xlabel(param)
    
    # Hide unused axes
    for j in range(i + 1, len(axes)):
        axes[j].set_visible(False)
    
    plt.suptitle("Categorical Parameter Impact on Epsilon", y=1.02)
    plt.tight_layout()
    plt.savefig(output_dir / "categorical_analysis.png", dpi=150)
    plt.close()


def generate_report(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                    corr_df: pd.DataFrame, output_dir: Path):
    """
    Generate a summary report.
    """
    report = []
    report.append("# Meta-optimization Analysis Report\n")
    report.append(f"**Generated:** {pd.Timestamp.now()}\n")
    
    report.append("\n## Dataset Summary\n")
    report.append(f"- Total configurations: {len(configs_df)}")
    report.append(f"- Evaluation range: {configs_df['evaluation'].min()} - {configs_df['evaluation'].max()}")
    report.append(f"- Number of parameters: {len(configs_df.columns) - 1}")
    
    if not objectives_df.empty:
        report.append("\n## Objective Statistics\n")
        report.append(f"- Best Epsilon: {objectives_df['Epsilon'].min():.4f}")
        report.append(f"- Best NormHypervolume: {objectives_df['NormHypervolume'].min():.4f}")
        report.append(f"- Final Pareto front size: {len(objectives_df[objectives_df['evaluation'] == objectives_df['evaluation'].max()])}")
    
    report.append("\n## Top 10 Most Important Parameters (by correlation)\n")
    report.append("*Note: Conditional parameters are only correlated using samples where the parent parameter has the appropriate value.*\n")
    report.append("| Parameter | Epsilon Corr | NormHV Corr | Avg |Abs| Corr | n | Conditional |")
    report.append("|-----------|--------------|-------------|-----------------|---|-------------|")
    for param, row in corr_df.head(10).iterrows():
        is_cond = "Yes" if row.get("is_conditional", False) else "No"
        n_samples = int(row.get("n_samples", 0))
        report.append(f"| {param} | {row['Epsilon_corr']:.3f} | {row['NormHV_corr']:.3f} | {row['abs_mean_corr']:.3f} | {n_samples} | {is_cond} |")
    
    # Add conditional parameters section
    conditional_params = corr_df[corr_df.get("is_conditional", False) == True]
    if len(conditional_params) > 0:
        report.append("\n## Conditional Parameters Detail\n")
        report.append("| Parameter | Parent | Valid When | Samples |")
        report.append("|-----------|--------|------------|---------|")
        for param, row in conditional_params.iterrows():
            report.append(f"| {param} | {row['parent_param']} | {row['parent_value']} | {int(row['n_samples'])} |")
    
    report.append("\n## Generated Files\n")
    report.append("- `parameter_correlations.csv`: Full correlation analysis (with conditional info)")
    report.append("- `pca_loadings.csv`: PCA component loadings")
    report.append("- `correlation_heatmap.png`: Parameter-objective correlation matrix")
    report.append("- `pca_variance.png`: PCA explained variance plots")
    report.append("- `pca_scatter.png`: Configuration space in PC1-PC2")
    report.append("- `parameter_convergence.png`: Evolution of top parameters")
    report.append("- `objective_convergence.png`: Objective convergence over evaluations")
    report.append("- `categorical_analysis.png`: Impact of categorical parameters")
    
    report_text = "\n".join(report)
    (output_dir / "analysis_report.md").write_text(report_text)
    print(f"\n{report_text}")


def main():
    parser = argparse.ArgumentParser(description="Analyze meta-optimization results")
    parser.add_argument("--input", "-i", type=str, required=True,
                        help="Path to results directory (e.g., ../RESULTS/NSGAII/DTLZ3)")
    parser.add_argument("--output", "-o", type=str, default="./output",
                        help="Output directory for analysis results")
    args = parser.parse_args()
    
    input_dir = Path(args.input)
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"Loading data from: {input_dir}")
    print(f"Output directory: {output_dir}")
    
    # Load data
    configs_df, objectives_df = load_configurations(input_dir)
    print(f"Loaded {len(configs_df)} configurations across {configs_df['evaluation'].nunique()} evaluations")
    
    # Run analyses
    corr_df = analyze_correlations(configs_df, objectives_df, output_dir)
    pca, X_pca = perform_pca(configs_df, objectives_df, output_dir)
    
    top_params = corr_df.head(10).index.tolist()
    analyze_convergence(configs_df, objectives_df, output_dir, top_params)
    analyze_categorical_parameters(configs_df, objectives_df, output_dir)
    
    # Generate report
    generate_report(configs_df, objectives_df, corr_df, output_dir)
    
    print(f"\nAnalysis complete! Results saved to {output_dir}")


if __name__ == "__main__":
    main()
