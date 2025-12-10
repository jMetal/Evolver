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


# Known quality indicator abbreviations and their full names
INDICATOR_NAMES = {
    "EP": "Epsilon",
    "IGD": "IGD",
    "IGD+": "IGD+",
    "IGDP": "IGD+",  # Alternative notation
    "NHV": "NormHypervolume",
    "HV": "Hypervolume",
    "SPREAD": "Spread",
    "GD": "GenerationalDistance",
}


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


def detect_objective_names(results_dir: Path) -> list[str]:
    """
    Detect objective/indicator names from FUN file names.
    
    File naming convention: FUN.<algorithm>.<problem>.<indicator1>.<indicator2>[.<indicatorN>].<eval>.csv
    Example: FUN.NSGA-II.DTLZ3.EP.NHV.100.csv -> ["EP", "NHV"]
    """
    fun_files = list(results_dir.glob("FUN.*.csv"))
    if not fun_files:
        raise ValueError(f"No FUN.*.csv files found in {results_dir}")
    
    # Parse the first FUN file to extract indicator names
    sample_file = fun_files[0].name
    parts = sample_file.replace(".csv", "").split(".")
    
    # Format: FUN.<algo>.<problem>.<ind1>.<ind2>...<eval>
    # The last part is the evaluation number, indicators are in between
    if len(parts) < 5:
        raise ValueError(f"Unexpected FUN file format: {sample_file}")
    
    # Find indicator parts: after algorithm and problem, before evaluation number
    # Indicators are uppercase abbreviations, eval number is numeric
    indicators = []
    for part in parts[3:-1]:  # Skip FUN, algo, problem, and eval number
        if part.isnumeric():
            break
        # Check if it's a known indicator or looks like one (uppercase, short)
        if part in INDICATOR_NAMES or (part.isupper() and len(part) <= 6):
            indicators.append(part)
    
    if not indicators:
        raise ValueError(f"Could not detect indicators from filename: {sample_file}")
    
    # Count columns in the file to verify
    with open(fun_files[0], 'r') as f:
        first_line = f.readline().strip()
        n_cols = len(first_line.split(','))
    
    if n_cols != len(indicators):
        print(f"Warning: Detected {len(indicators)} indicators from filename but file has {n_cols} columns")
        # Fall back to generic names if mismatch
        indicators = [f"Objective{i+1}" for i in range(n_cols)]
    
    print(f"Detected quality indicators: {indicators}")
    return indicators


def load_configurations(results_dir: Path) -> tuple[pd.DataFrame, pd.DataFrame, list[str]]:
    """
    Load all configuration files and objective values from a results directory.
    
    Returns:
        configs_df: DataFrame with parameter values indexed by evaluation number
        objectives_df: DataFrame with objective values indexed by evaluation number
        objective_names: List of detected objective/indicator names
    """
    configs = []
    objectives = []
    
    # Detect objective names from FUN file naming convention
    objective_names = detect_objective_names(results_dir)
    
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
    
    # Load objectives with detected column names
    for fun_file in sorted(results_path.glob("FUN.*.csv")):
        match = fun_pattern.search(fun_file.name)
        if match:
            eval_num = int(match.group(1))
            obj_df = pd.read_csv(fun_file, header=None, names=objective_names)
            obj_df["evaluation"] = eval_num
            objectives.append(obj_df)
    
    if not configs:
        raise ValueError(f"No configuration files found in {results_dir}")
    
    configs_df = pd.concat(configs, ignore_index=True)
    objectives_df = pd.concat(objectives, ignore_index=True) if objectives else pd.DataFrame()
    
    return configs_df, objectives_df, objective_names


def analyze_correlations(configs_df: pd.DataFrame, objectives_df: pd.DataFrame, 
                         output_dir: Path, objective_names: list[str]) -> pd.DataFrame:
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
    
    # Calculate correlations with all objectives
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
                        param_corr = {"n_samples": n_samples, "is_conditional": True,
                                      "parent_param": parent_param, "parent_value": valid_value}
                        for obj in objective_names:
                            corr, p_val = stats.spearmanr(filtered[param], filtered[obj])
                            param_corr[f"{obj}_corr"] = corr
                            param_corr[f"{obj}_pvalue"] = p_val
                        correlations[param] = param_corr
                else:
                    # Parent not in data, skip
                    continue
            else:
                # Non-conditional parameter: use all samples
                param_corr = {"n_samples": len(merged), "is_conditional": False,
                              "parent_param": None, "parent_value": None}
                for obj in objective_names:
                    corr, p_val = stats.spearmanr(merged[param], merged[obj])
                    param_corr[f"{obj}_corr"] = corr
                    param_corr[f"{obj}_pvalue"] = p_val
                correlations[param] = param_corr
    
    corr_df = pd.DataFrame(correlations).T
    
    # Calculate average absolute correlation across all objectives
    corr_cols = [f"{obj}_corr" for obj in objective_names]
    corr_df["abs_mean_corr"] = corr_df[corr_cols].abs().mean(axis=1)
    corr_df = corr_df.sort_values("abs_mean_corr", ascending=False)
    
    print("\nTop 10 parameters by correlation with objectives:")
    print("(n = number of valid samples for conditional parameters)")
    display_cols = corr_cols + ["abs_mean_corr", "n_samples", "is_conditional"]
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
    corr_matrix = merged[top_params + objective_names].corr()
    sns.heatmap(corr_matrix, annot=True, fmt=".2f", cmap="RdBu_r", center=0,
                ax=ax, square=True, linewidths=0.5)
    plt.title(f"Parameter-Objective Correlation Matrix (Top 15 Parameters)\nObjectives: {', '.join(objective_names)}")
    plt.tight_layout()
    plt.savefig(output_dir / "correlation_heatmap.png", dpi=150)
    plt.close()
    
    return corr_df


def perform_pca(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                output_dir: Path, objective_names: list[str]) -> tuple[PCA, np.ndarray]:
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
        
        # Create subplots for each objective
        n_obj = len(objective_names)
        n_cols = min(3, n_obj)
        n_rows = (n_obj + n_cols - 1) // n_cols
        fig, axes = plt.subplots(n_rows, n_cols, figsize=(6 * n_cols, 5 * n_rows))
        if n_obj == 1:
            axes = [axes]
        else:
            axes = axes.flatten() if n_rows > 1 else (axes if n_obj > 1 else [axes])
        
        # Match PCA indices with merged data
        merged_params = merged[non_constant].values
        merged_scaled = scaler.transform(merged_params)
        merged_pca = pca.transform(merged_scaled)
        
        for i, obj in enumerate(objective_names):
            ax = axes[i]
            sc = ax.scatter(merged_pca[:, 0], merged_pca[:, 1], 
                           c=merged[obj], cmap="viridis", alpha=0.7)
            ax.set_xlabel("PC1")
            ax.set_ylabel("PC2")
            ax.set_title(f"PCA - Colored by {obj}")
            plt.colorbar(sc, ax=ax, label=obj)
        
        # Hide unused axes
        for j in range(n_obj, len(axes)):
            axes[j].set_visible(False)
        
        plt.tight_layout()
        plt.savefig(output_dir / "pca_scatter.png", dpi=150)
        plt.close()
    
    return pca, X_pca


def analyze_convergence(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                        output_dir: Path, top_params: list[str], objective_names: list[str]):
    """
    Analyze how parameters converge over evaluations.
    Excludes categorical parameters with few unique values (<=5) as their 
    mean evolution is not meaningful.
    """
    print("\n=== Convergence Analysis ===")
    
    # Filter out categorical parameters with few values (<=5 unique values)
    # These should be analyzed with frequency distributions, not mean evolution
    LOW_CARDINALITY_THRESHOLD = 5
    continuous_params = []
    excluded_params = []
    
    for param in top_params:
        n_unique = configs_df[param].nunique()
        if n_unique > LOW_CARDINALITY_THRESHOLD:
            continuous_params.append(param)
        else:
            excluded_params.append((param, n_unique))
    
    if excluded_params:
        print(f"  Excluded from convergence plot (categorical with ≤{LOW_CARDINALITY_THRESHOLD} values):")
        for param, n in excluded_params:
            print(f"    - {param}: {n} unique values")
    
    # Plot parameter evolution for continuous/high-cardinality parameters
    params_to_plot = continuous_params[:6]  # Max 6 plots
    
    if len(params_to_plot) == 0:
        print("  No continuous parameters to plot for convergence analysis.")
    else:
        n_plots = len(params_to_plot)
        n_cols = min(3, n_plots)
        n_rows = (n_plots + n_cols - 1) // n_cols
        
        fig, axes = plt.subplots(n_rows, n_cols, figsize=(5 * n_cols, 5 * n_rows))
        if n_plots == 1:
            axes = [axes]
        else:
            axes = axes.flatten() if n_plots > 1 else [axes]
        
        for i, param in enumerate(params_to_plot):
            ax = axes[i]
            # Use percentiles for more robust visualization
            grouped = configs_df.groupby("evaluation")[param].agg(
                ["mean", "median", "count",
                 lambda x: x.quantile(0.25),  # Q1
                 lambda x: x.quantile(0.75)]  # Q3
            )
            grouped.columns = ["mean", "median", "count", "q25", "q75"]
            grouped = grouped.sort_index()
            
            # IQR band (25th-75th percentile) - more robust than min-max
            ax.fill_between(grouped.index, grouped["q25"], grouped["q75"], 
                           alpha=0.3, color='blue', label="IQR (25%-75%)")
            ax.plot(grouped.index, grouped["median"], 'b-', linewidth=2, label="Median")
            ax.plot(grouped.index, grouped["mean"], 'r--', linewidth=1, alpha=0.7, label="Mean")
            
            ax.set_xlabel("Evaluation")
            ax.set_ylabel(param)
            ax.set_title(f"Evolution of {param}")
            ax.legend(fontsize=8, loc='best')
            
            # Add sample count info in subtitle
            avg_n = grouped["count"].mean()
            ax.text(0.02, 0.98, f"avg n={avg_n:.1f}/eval", transform=ax.transAxes,
                   fontsize=8, va='top', ha='left', 
                   bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
        
        # Hide unused axes
        for j in range(len(params_to_plot), len(axes)):
            axes[j].set_visible(False)
        
        plt.tight_layout()
        plt.savefig(output_dir / "parameter_convergence.png", dpi=150)
        plt.close()
    
    # Objective convergence
    if not objectives_df.empty:
        n_obj = len(objective_names)
        n_cols = min(3, n_obj)
        n_rows = (n_obj + n_cols - 1) // n_cols
        fig, axes = plt.subplots(n_rows, n_cols, figsize=(6 * n_cols, 4 * n_rows))
        if n_obj == 1:
            axes = [axes]
        else:
            axes = axes.flatten() if n_rows > 1 else (axes if n_obj > 1 else [axes])
        
        for i, obj in enumerate(objective_names):
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
        
        # Hide unused axes
        for j in range(n_obj, len(axes)):
            axes[j].set_visible(False)
        
        plt.tight_layout()
        plt.savefig(output_dir / "objective_convergence.png", dpi=150)
        plt.close()


# Mapping of categorical parameter encoded values to their labels
CATEGORICAL_VALUE_LABELS = {
    "algorithmResult": {0.0: "population", 1.0: "externalArchive"},
    "archiveType": {0.0: "crowdingDistanceArchive", 1.0: "unboundedArchive"},
    "createInitialSolutions": {0.0: "random", 1.0: "latinHypercubeSampling", 2.0: "scatterSearch"},
    "variation": {0.0: "crossoverAndMutationVariation"},
    "crossover": {0.0: "SBX", 1.0: "BLX_ALPHA", 2.0: "wholeArithmetic"},
    "crossoverRepairStrategy": {0.0: "bounds", 1.0: "random", 2.0: "round"},
    "mutation": {0.0: "polynomial", 1.0: "linkedPolynomial", 2.0: "uniform", 3.0: "nonUniform"},
    "mutationRepairStrategy": {0.0: "bounds", 1.0: "random", 2.0: "round"},
    "selection": {0.0: "tournament", 1.0: "random"},
}


def analyze_categorical_parameters(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                                   output_dir: Path, objective_names: list[str]) -> pd.DataFrame:
    """
    Analyze categorical parameters using Kruskal-Wallis test and boxplots.
    
    Kruskal-Wallis is a non-parametric test that determines if there are 
    statistically significant differences between groups (categories).
    """
    print("\n=== Categorical Parameter Analysis (Kruskal-Wallis) ===")
    
    categorical_params = [
        "algorithmResult", "archiveType", "createInitialSolutions",
        "variation", "crossover", "crossoverRepairStrategy",
        "mutation", "mutationRepairStrategy", "selection"
    ]
    
    # Filter existing categorical params
    existing_cats = [p for p in categorical_params if p in configs_df.columns]
    
    if not existing_cats or objectives_df.empty:
        print("No categorical parameters or objectives to analyze")
        return pd.DataFrame()
    
    obj_means = objectives_df.groupby("evaluation").mean().reset_index()
    merged = configs_df.merge(obj_means, on="evaluation", how="inner")
    
    # Use first objective for primary analysis (typically the most important)
    primary_objective = objective_names[0]
    
    # Perform Kruskal-Wallis test for each categorical parameter and each objective
    kruskal_results = []
    
    for param in existing_cats:
        unique_values = merged[param].unique()
        
        if len(unique_values) < 2:
            # Skip if only one category present
            continue
        
        # Get category labels
        labels = CATEGORICAL_VALUE_LABELS.get(param, {})
        categories_str = ", ".join([labels.get(v, str(v)) for v in sorted(unique_values)])
        
        result_row = {
            "parameter": param,
            "n_categories": len(unique_values),
            "categories": categories_str,
        }
        
        # Perform Kruskal-Wallis test for each objective
        for obj in objective_names:
            groups = [merged[merged[param] == val][obj].values for val in unique_values]
            groups = [g for g in groups if len(g) > 0]
            
            if len(groups) < 2:
                continue
            
            stat, p_val = stats.kruskal(*groups)
            n_total = len(merged)
            eta_sq = max(0, (stat - len(groups) + 1) / (n_total - len(groups)))
            
            result_row[f"H_stat_{obj}"] = stat
            result_row[f"p_value_{obj}"] = p_val
            result_row[f"significant_{obj}"] = p_val < 0.05
            result_row[f"eta_sq_{obj}"] = eta_sq
        
        kruskal_results.append(result_row)
    
    kruskal_df = pd.DataFrame(kruskal_results)
    
    if len(kruskal_df) > 0:
        # Sort by p-value of primary objective
        p_col = f"p_value_{primary_objective}"
        if p_col in kruskal_df.columns:
            kruskal_df = kruskal_df.sort_values(p_col)
        
        print(f"\nKruskal-Wallis Test Results (H0: no difference between categories):")
        print(f"Objectives: {', '.join(objective_names)}")
        print("-" * 80)
        for _, row in kruskal_df.iterrows():
            print(f"\n{row['parameter']} ({row['n_categories']} categories: {row['categories']})")
            for obj in objective_names:
                h_col = f"H_stat_{obj}"
                p_col = f"p_value_{obj}"
                eta_col = f"eta_sq_{obj}"
                if h_col in row and pd.notna(row[h_col]):
                    p_val = row[p_col]
                    sig = "***" if p_val < 0.001 else "**" if p_val < 0.01 else "*" if p_val < 0.05 else ""
                    print(f"  {obj}: H={row[h_col]:.2f}, p={p_val:.4f} {sig}, η²={row[eta_col]:.3f}")
        
        print("\n(* p<0.05, ** p<0.01, *** p<0.001)")
        print("η² (eta-squared): effect size - small≈0.01, medium≈0.06, large≈0.14")
        
        # Save results
        kruskal_df.to_csv(output_dir / "categorical_kruskal_wallis.csv", index=False)
    
    # Create improved boxplots with category labels (using primary objective)
    params_to_plot = [p for p in existing_cats if merged[p].nunique() > 1]
    n_params = len(params_to_plot)
    
    if n_params > 0:
        n_cols = min(3, n_params)
        n_rows = (n_params + n_cols - 1) // n_cols
        
        fig, axes = plt.subplots(n_rows, n_cols, figsize=(5 * n_cols, 4 * n_rows))
        if n_params == 1:
            axes = [axes]
        else:
            axes = axes.flatten() if n_rows > 1 else axes
        
        for i, param in enumerate(params_to_plot):
            ax = axes[i]
            
            # Create labeled data using primary objective
            labels_map = CATEGORICAL_VALUE_LABELS.get(param, {})
            plot_data = []
            plot_labels = []
            
            for val in sorted(merged[param].unique()):
                data = merged[merged[param] == val][primary_objective].values
                if len(data) > 0:
                    plot_data.append(data)
                    label = labels_map.get(val, f"{val}")
                    plot_labels.append(f"{label}\n(n={len(data)})")
            
            bp = ax.boxplot(plot_data, tick_labels=plot_labels, patch_artist=True)
            
            # Color boxes using tab10 colormap
            cmap = plt.colormaps.get_cmap('tab10')
            colors = [cmap(i / max(len(plot_data) - 1, 1)) for i in range(len(plot_data))]
            for patch, color in zip(bp['boxes'], colors):
                patch.set_facecolor(color)
            
            ax.set_ylabel(primary_objective)
            ax.set_title(f"{param}")
            ax.tick_params(axis='x', rotation=45)
            
            # Add significance annotation if available
            p_col = f"p_value_{primary_objective}"
            if len(kruskal_df) > 0 and p_col in kruskal_df.columns:
                row = kruskal_df[kruskal_df["parameter"] == param]
                if len(row) > 0:
                    p_val = row.iloc[0][p_col]
                    sig = "***" if p_val < 0.001 else "**" if p_val < 0.01 else "*" if p_val < 0.05 else "ns"
                    ax.annotate(f"p={p_val:.3f} {sig}", xy=(0.95, 0.95), xycoords='axes fraction',
                               ha='right', va='top', fontsize=9,
                               bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.5))
        
        # Hide unused axes
        for j in range(i + 1, len(axes)):
            axes[j].set_visible(False)
        
        plt.suptitle(f"Categorical Parameter Impact on {primary_objective} (Kruskal-Wallis)", y=1.02, fontsize=12)
        plt.tight_layout()
        plt.savefig(output_dir / "categorical_analysis.png", dpi=150, bbox_inches='tight')
        plt.close()
    
    return kruskal_df


def generate_report(configs_df: pd.DataFrame, objectives_df: pd.DataFrame,
                    corr_df: pd.DataFrame, kruskal_df: pd.DataFrame, 
                    output_dir: Path, objective_names: list[str]):
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
    report.append(f"- Quality indicators: {', '.join(objective_names)}")
    
    if not objectives_df.empty:
        report.append("\n## Objective Statistics\n")
        for obj in objective_names:
            report.append(f"- Best {obj}: {objectives_df[obj].min():.4f}")
        report.append(f"- Final Pareto front size: {len(objectives_df[objectives_df['evaluation'] == objectives_df['evaluation'].max()])}")
    
    report.append("\n## Top 10 Most Important Parameters (by correlation)\n")
    report.append("*Note: Conditional parameters are only correlated using samples where the parent parameter has the appropriate value.*\n")
    
    # Build dynamic header
    header_parts = ["Parameter"]
    header_parts.extend([f"{obj} Corr" for obj in objective_names])
    header_parts.extend(["Avg |Abs| Corr", "n", "Conditional"])
    report.append("| " + " | ".join(header_parts) + " |")
    report.append("|" + "|".join(["---" for _ in header_parts]) + "|")
    
    for param, row in corr_df.head(10).iterrows():
        is_cond = "Yes" if row.get("is_conditional", False) else "No"
        n_samples = int(row.get("n_samples", 0))
        row_parts: list[str] = [str(param)]
        for obj in objective_names:
            corr_col = f"{obj}_corr"
            if corr_col in row:
                row_parts.append(f"{row[corr_col]:.3f}")
            else:
                row_parts.append("N/A")
        row_parts.extend([f"{row['abs_mean_corr']:.3f}", str(n_samples), is_cond])
        report.append("| " + " | ".join(row_parts) + " |")
    
    # Add conditional parameters section
    conditional_params = corr_df[corr_df.get("is_conditional", False) == True]
    if len(conditional_params) > 0:
        report.append("\n## Conditional Parameters Detail\n")
        report.append("| Parameter | Parent | Valid When | Samples |")
        report.append("|-----------|--------|------------|---------|")
        for param, row in conditional_params.iterrows():
            report.append(f"| {param} | {row['parent_param']} | {row['parent_value']} | {int(row['n_samples'])} |")
    
    # Add Kruskal-Wallis results for categorical parameters
    if len(kruskal_df) > 0:
        report.append("\n## Categorical Parameters (Kruskal-Wallis Test)\n")
        report.append("*For categorical parameters, Spearman correlation is not appropriate. Kruskal-Wallis H-test is used to determine if there are statistically significant differences between groups.*\n")
        
        # Build dynamic header for Kruskal-Wallis
        kw_header = ["Parameter"]
        for obj in objective_names:
            kw_header.extend([f"H ({obj})", f"p ({obj})"])
        kw_header.extend(["Groups", "Significant?"])
        report.append("| " + " | ".join(kw_header) + " |")
        report.append("|" + "|".join(["---" for _ in kw_header]) + "|")
        
        for _, row in kruskal_df.iterrows():
            row_parts = [row['parameter']]
            any_significant = False
            for obj in objective_names:
                h_col = f"H_stat_{obj}"
                p_col = f"p_value_{obj}"
                if h_col in row and pd.notna(row[h_col]):
                    row_parts.append(f"{row[h_col]:.2f}")
                    row_parts.append(f"{row[p_col]:.4f}")
                    if row[p_col] < 0.05:
                        any_significant = True
                else:
                    row_parts.extend(["N/A", "N/A"])
            row_parts.append(str(row['n_categories']))
            row_parts.append("Yes" if any_significant else "No")
            report.append("| " + " | ".join(row_parts) + " |")
        
        # Interpretation using primary objective
        primary = objective_names[0]
        p_col = f"p_value_{primary}"
        h_col = f"H_stat_{primary}"
        report.append("\n### Interpretation\n")
        if p_col in kruskal_df.columns:
            significant = kruskal_df[kruskal_df[p_col] < 0.05]
            if len(significant) > 0:
                report.append(f"The following categorical parameters show **statistically significant** impact on {primary} (p < 0.05):\n")
                for _, row in significant.iterrows():
                    effect = "large" if row[h_col] > 50 else "medium" if row[h_col] > 20 else "small"
                    report.append(f"- **{row['parameter']}**: H={row[h_col]:.2f}, p={row[p_col]:.4f} ({effect} effect)")
            else:
                report.append(f"No categorical parameters show statistically significant impact on {primary} at α=0.05 level.")
    
    report.append("\n## Generated Files\n")
    report.append("- `parameter_correlations.csv`: Full correlation analysis (with conditional info)")
    report.append("- `pca_loadings.csv`: PCA component loadings")
    report.append("- `kruskal_wallis_results.csv`: Kruskal-Wallis test results for categorical parameters")
    report.append("- `correlation_heatmap.png`: Parameter-objective correlation matrix")
    report.append("- `pca_variance.png`: PCA explained variance plots")
    report.append("- `pca_scatter.png`: Configuration space in PC1-PC2")
    report.append("- `parameter_convergence.png`: Evolution of top parameters")
    report.append("- `objective_convergence.png`: Objective convergence over evaluations")
    report.append("- `categorical_analysis.png`: Impact of categorical parameters (boxplots)")
    
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
    
    # Load data (now also returns detected objective names)
    configs_df, objectives_df, objective_names = load_configurations(input_dir)
    print(f"Loaded {len(configs_df)} configurations across {configs_df['evaluation'].nunique()} evaluations")
    print(f"Quality indicators: {', '.join(objective_names)}")
    
    # Run analyses (passing objective_names to all functions)
    corr_df = analyze_correlations(configs_df, objectives_df, output_dir, objective_names)
    pca, X_pca = perform_pca(configs_df, objectives_df, output_dir, objective_names)
    
    top_params = corr_df.head(10).index.tolist()
    analyze_convergence(configs_df, objectives_df, output_dir, top_params, objective_names)
    kruskal_df = analyze_categorical_parameters(configs_df, objectives_df, output_dir, objective_names)
    
    # Save Kruskal-Wallis results
    if len(kruskal_df) > 0:
        kruskal_df.to_csv(output_dir / "kruskal_wallis_results.csv", index=False)
    
    # Generate report
    generate_report(configs_df, objectives_df, corr_df, kruskal_df, output_dir, objective_names)
    
    print(f"\nAnalysis complete! Results saved to {output_dir}")


if __name__ == "__main__":
    main()
