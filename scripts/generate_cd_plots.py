#!/usr/bin/env python3
"""
Generate Critical Difference (CD) plots for TransferLearningStudy results.

Uses Friedman test + Nemenyi post-hoc test to identify significant differences
between algorithm rankings across problems.
"""

import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from scipy.stats import rankdata, f as f_dist
import seaborn as sns
from pathlib import Path

# Configuration
RESULTS_DIR = Path("/Users/ajnebro/Softw/jMetal/Evolver/experimentation/validation/TransferLearningStudy")
CSV_FILE = RESULTS_DIR / "QualityIndicatorSummary.csv"
OUTPUT_DIR = RESULTS_DIR / "cd_plots"
OUTPUT_DIR.mkdir(exist_ok=True)

# Quality indicators to visualize (abbreviated names as in CSV)
INDICATORS = [("EP", "Epsilon"), ("HV", "Hypervolume"), ("IGD", "Inverted Generational Distance"), ("IGD+", "Inverted Generational Distance Plus")]

def read_results(csv_path):
    """Read quality indicator results from CSV."""
    df = pd.read_csv(csv_path)
    return df

def compute_average_indicator_values(df, indicator):
    """
    Compute average indicator value per (algorithm, problem) pair.
    Returns a matrix with algorithms as rows, problems as columns.
    """
    subset = df[df['IndicatorName'] == indicator].copy()
    if subset.empty:
        return None

    # Group by algorithm and problem, compute mean
    summary = subset.groupby(['Algorithm', 'Problem'])['IndicatorValue'].mean().reset_index()

    # Pivot to get matrix format
    matrix = summary.pivot(index='Algorithm', columns='Problem', values='IndicatorValue')
    return matrix

def rank_algorithms_per_problem(matrix, higher_is_better=False):
    """
    Rank algorithms for each problem (column).
    For each column: lower indicator value = better rank (1 is best).
    """
    if higher_is_better:
        # For HV: higher is better, so negate before ranking
        ranked = matrix.apply(lambda col: rankdata(-col.values), axis=0)
    else:
        # For EP, IGD, IGD+: lower is better
        ranked = matrix.apply(lambda col: rankdata(col.values), axis=0)
    return ranked

def friedman_test(ranked_matrix):
    """
    Perform Friedman test on rankings.
    Returns: F statistic, p-value, and average ranks.
    """
    k = len(ranked_matrix)  # number of algorithms
    n = ranked_matrix.shape[1]  # number of problems

    avg_ranks = ranked_matrix.mean(axis=1)

    # Friedman statistic
    sum_r2 = (avg_ranks ** 2).sum()
    friedman_stat = (12 * n / (k * (k + 1))) * (sum_r2 - (k * (k + 1) ** 2 / 4))

    # p-value using chi-squared approximation
    from scipy.stats import chi2
    p_value = 1 - chi2.cdf(friedman_stat, k - 1)

    return friedman_stat, p_value, avg_ranks

def nemenyi_critical_difference(n_algorithms, n_problems, alpha=0.05):
    """
    Calculate critical difference for Nemenyi post-hoc test.
    n_algorithms: k (number of algorithms)
    n_problems: number of datasets
    """
    # Critical value from Nemenyi test (z-critical for given alpha)
    # For alpha=0.05 (two-tailed), z ~ 1.96 for pairwise comparison
    # Exact formula: CD = z * sqrt(k(k+1) / (6*n))

    # More precise: use student-t approximation
    # But simplified: CD ≈ q_alpha * sqrt(k(k+1) / (6*n))

    # For alpha=0.05: q_alpha ≈ sqrt(2) * 1.96 ≈ 2.77 (for many comparisons)
    # Simplified: just use 2.576 for alpha=0.05

    # Using the standard formula from literature
    q_alpha = {
        0.05: 2.576,  # for pairwise comparisons
        0.01: 2.807,
    }

    z_value = q_alpha.get(alpha, 2.576)
    cd = z_value * np.sqrt(n_algorithms * (n_algorithms + 1) / (6.0 * n_problems))
    return cd

def plot_cd(avg_ranks, algorithm_names, cd_value, indicator_name, output_path):
    """
    Plot Critical Difference diagram.
    """
    fig, ax = plt.subplots(figsize=(12, 6))

    # Sort algorithms by average rank (best on left)
    sorted_indices = np.argsort(avg_ranks.values)
    sorted_ranks = avg_ranks.iloc[sorted_indices]
    sorted_names = algorithm_names[sorted_indices]

    # Draw x-axis
    max_rank = len(avg_ranks)
    ax.set_xlim(0, max_rank + 1)
    ax.set_ylim(-1, len(sorted_names))

    # Plot algorithms as points on x-axis
    y_positions = np.arange(len(sorted_names))
    ax.scatter(sorted_ranks.values, y_positions, s=200, color='red', zorder=3)

    # Add algorithm names on y-axis
    ax.set_yticks(y_positions)
    ax.set_yticklabels(sorted_names, fontsize=11)

    # X-axis: average rank (lower is better)
    ax.set_xlabel('Average Rank', fontsize=12, fontweight='bold')
    ax.set_title(f'Critical Difference Plot\n{indicator_name} (lower rank = better)',
                 fontsize=13, fontweight='bold')

    # Draw horizontal lines connecting algorithms within CD range
    # Algorithms within CD are NOT significantly different
    for i in range(len(sorted_names)):
        for j in range(i + 1, len(sorted_names)):
            diff = abs(sorted_ranks.values[i] - sorted_ranks.values[j])
            if diff <= cd_value:
                # Draw connecting line
                y1, y2 = y_positions[i], y_positions[j]
                x_min = min(sorted_ranks.values[i], sorted_ranks.values[j]) - 0.1
                x_max = max(sorted_ranks.values[i], sorted_ranks.values[j]) + 0.1
                ax.plot([x_min, x_max], [y1, y2], 'b-', linewidth=2, alpha=0.4, zorder=2)

    # Draw vertical line showing CD range at the best rank
    best_rank = sorted_ranks.values[0]
    ax.axvline(best_rank + cd_value, color='green', linestyle='--', linewidth=2,
               label=f'CD = {cd_value:.3f}')

    # Grid
    ax.grid(axis='x', alpha=0.3)
    ax.legend(fontsize=11, loc='upper right')

    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()

    print(f"Saved: {output_path}")

def main():
    print("Reading results from:", CSV_FILE)
    df = read_results(CSV_FILE)

    print(f"Loaded {len(df)} records")
    print(f"Indicators found: {df['IndicatorName'].unique()}")
    print(f"Algorithms: {sorted(df['Algorithm'].unique())}")
    print(f"Problems: {sorted(df['Problem'].unique())}")

    # Process each indicator
    for short_name, long_name in INDICATORS:
        print(f"\n{'='*60}")
        print(f"Processing: {long_name} ({short_name})")
        print('='*60)

        matrix = compute_average_indicator_values(df, short_name)
        if matrix is None:
            print(f"Skipping {long_name} - not found in data")
            continue

        print(f"Matrix shape: {matrix.shape} (algorithms × problems)")
        print(f"Algorithms: {list(matrix.index)}")

        # Determine if higher is better
        higher_is_better = (short_name == "HV")

        # Rank algorithms per problem
        ranked = rank_algorithms_per_problem(matrix, higher_is_better=higher_is_better)

        # Run Friedman test
        friedman_stat, p_value, avg_ranks = friedman_test(ranked)
        print(f"\nFriedman Test:")
        print(f"  Statistic: {friedman_stat:.4f}")
        print(f"  p-value: {p_value:.6f}")
        print(f"  Significant (α=0.05): {'Yes' if p_value < 0.05 else 'No'}")

        print(f"\nAverage Ranks (lower is better):")
        for algo, rank in sorted(avg_ranks.items(), key=lambda x: x[1]):
            print(f"  {algo:20s}: {rank:.3f}")

        # Calculate critical difference
        n_alg = len(matrix)
        n_prob = matrix.shape[1]
        cd = nemenyi_critical_difference(n_alg, n_prob, alpha=0.05)
        print(f"\nCritical Difference (α=0.05): {cd:.4f}")
        print(f"  Algorithms within CD of best: ", end="")
        best_rank = avg_ranks.min()
        within_cd = [name for name, rank in avg_ranks.items() if rank - best_rank <= cd]
        print(", ".join(within_cd))

        # Generate plot
        algo_names = np.array(matrix.index)
        output_file = OUTPUT_DIR / f"cd_plot_{short_name}.png"
        plot_cd(avg_ranks, algo_names, cd, long_name, output_file)

    print(f"\n{'='*60}")
    print(f"All CD plots saved to: {OUTPUT_DIR}")
    print('='*60)

if __name__ == "__main__":
    main()
