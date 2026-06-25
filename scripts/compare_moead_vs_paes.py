#!/usr/bin/env python3
"""
Compare meta-optimization results between two experiments (e.g. MOEA/D vs PAES).

Usage:
    python scripts/compare_moead_vs_paes.py <dir1> <dir2> [--output <out_dir>]

Each directory must contain INDICATORS.csv, CONFIGURATIONS.csv and METADATA.txt
as written by ConsolidatedOutputResults.
"""

import argparse
import sys
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

def read_metadata(directory: Path) -> dict:
    """Parse METADATA.txt into a flat dict. Falls back gracefully."""
    meta = {}
    path = directory / "METADATA.txt"
    if not path.exists():
        return meta
    for line in path.read_text().splitlines():
        if ":" in line:
            key, _, value = line.partition(":")
            meta[key.strip()] = value.strip()
    return meta


def load_experiment(directory: Path) -> pd.DataFrame:
    """Merge INDICATORS.csv and CONFIGURATIONS.csv on (Evaluation, SolutionId)."""
    ind = pd.read_csv(directory / "INDICATORS.csv")
    conf = pd.read_csv(directory / "CONFIGURATIONS.csv")
    return pd.merge(ind, conf, on=["Evaluation", "SolutionId"])


def algorithm_name(directory: Path) -> str:
    """Return the base-level algorithm name from METADATA.txt, else directory name."""
    meta = read_metadata(directory)
    return meta.get("Algorithm", directory.name)


# ---------------------------------------------------------------------------
# Analysis helpers
# ---------------------------------------------------------------------------

def final_population(df: pd.DataFrame, final_eval: int | None = None) -> pd.DataFrame:
    """Return rows at the last available evaluation checkpoint."""
    ev = final_eval if final_eval is not None else df["Evaluation"].max()
    return df[df["Evaluation"] == ev].copy()


def best_hv_evolution(df: pd.DataFrame) -> pd.DataFrame:
    """For each checkpoint, compute the best (minimum) HVMinus seen so far."""
    per_eval = df.groupby("Evaluation")["HVMinus"].min().reset_index()
    per_eval = per_eval.sort_values("Evaluation")
    per_eval["BestSoFar"] = per_eval["HVMinus"].cummin()
    return per_eval


def best_configuration(df: pd.DataFrame) -> pd.Series:
    """Row with the lowest HVMinus at the final evaluation."""
    pop = final_population(df)
    return pop.loc[pop["HVMinus"].idxmin()]


def parse_var_conf(directory: Path) -> list[dict]:
    """Parse VAR_CONF.txt into a list of dicts with keys evaluation, EP, HVMinus, config."""
    entries = []
    current_eval = None
    path = directory / "VAR_CONF.txt"
    if not path.exists():
        return entries
    for line in path.read_text().splitlines():
        line = line.strip()
        if line.startswith("# Evaluation:"):
            current_eval = int(line.split(":")[1].strip())
        elif "|" in line and current_eval is not None:
            indicators_part, _, config_part = line.partition("|")
            kv = dict(token.split("=") for token in indicators_part.split() if "=" in token)
            entries.append({
                "evaluation": current_eval,
                "EP": float(kv.get("EP", "nan")),
                "HVMinus": float(kv.get("HVMinus", "nan")),
                "config": config_part.strip(),
            })
    return entries


def best_config_string(directory: Path) -> tuple[dict, str]:
    """Return (indicator_dict, config_string) for the best HVMinus at the last checkpoint."""
    entries = parse_var_conf(directory)
    if not entries:
        return {}, ""
    max_eval = max(e["evaluation"] for e in entries)
    final = [e for e in entries if e["evaluation"] == max_eval]
    best = min(final, key=lambda e: e["HVMinus"])
    return best, best["config"]


def print_best_config(label: str, directory: Path) -> None:
    """Pretty-print the best configuration from VAR_CONF.txt."""
    entry, config = best_config_string(directory)
    print(f"\n{'='*60}")
    print(f"Best configuration for: {label}")
    if not entry:
        print("  (VAR_CONF.txt not found)")
        return
    print(f"  EP      = {entry['EP']:.6f}")
    print(f"  HVMinus = {entry['HVMinus']:.6f}  (HV ≈ {-entry['HVMinus']:.6f})")
    print(f"  Evaluation checkpoint: {entry['evaluation']}")
    print(f"  {config}")
    print(f"{'='*60}")


# ---------------------------------------------------------------------------
# Plotting
# ---------------------------------------------------------------------------

def plot_pareto_fronts(
    dfs: list[pd.DataFrame],
    labels: list[str],
    output_path: Path | None = None,
) -> None:
    """Figure 1 — Scatter of (EP, HVMinus) at the final evaluation."""
    colors = ["#1f77b4", "#d62728"]
    markers = ["o", "s"]

    fig, ax = plt.subplots(figsize=(7, 5))
    for df, label, color, marker in zip(dfs, labels, colors, markers):
        pop = final_population(df)
        ax.scatter(
            pop["EP"],
            pop["HVMinus"],
            label=f"{label} (n={len(pop)})",
            color=color,
            marker=marker,
            alpha=0.75,
            edgecolors="white",
            linewidths=0.4,
            s=60,
        )

    ax.set_xlabel("EP (Epsilon indicator, lower = better)", fontsize=11)
    ax.set_ylabel("HVMinus (−HV, lower = better)", fontsize=11)
    ax.set_title("Meta-optimization Pareto front — final population", fontsize=12)
    ax.legend(fontsize=10)
    ax.grid(True, linestyle="--", alpha=0.4)
    fig.tight_layout()

    if output_path:
        fig.savefig(output_path, dpi=150)
        print(f"Saved: {output_path}")
    else:
        plt.show()
    plt.close(fig)


def plot_hv_evolution(
    dfs: list[pd.DataFrame],
    labels: list[str],
    output_path: Path | None = None,
) -> None:
    """Figure 2 — Best-so-far HVMinus over meta-evaluation checkpoints."""
    colors = ["#1f77b4", "#d62728"]
    linestyles = ["-", "--"]

    fig, ax = plt.subplots(figsize=(8, 5))
    for df, label, color, ls in zip(dfs, labels, colors, linestyles):
        evo = best_hv_evolution(df)
        ax.plot(
            evo["Evaluation"],
            evo["BestSoFar"],
            label=label,
            color=color,
            linestyle=ls,
            linewidth=2,
        )

    ax.set_xlabel("Meta-optimizer evaluations", fontsize=11)
    ax.set_ylabel("Best HVMinus seen so far (lower = better)", fontsize=11)
    ax.set_title("Convergence of best HVMinus during meta-optimization", fontsize=12)
    ax.legend(fontsize=10)
    ax.grid(True, linestyle="--", alpha=0.4)
    fig.tight_layout()

    if output_path:
        fig.savefig(output_path, dpi=150)
        print(f"Saved: {output_path}")
    else:
        plt.show()
    plt.close(fig)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(
        description="Compare two Evolver meta-optimization experiments."
    )
    p.add_argument("dir1", type=Path, help="First experiment directory (e.g. MOEAD_EXT_PTS)")
    p.add_argument("dir2", type=Path, help="Second experiment directory (e.g. PAES_EXT)")
    p.add_argument(
        "--output",
        type=Path,
        default=None,
        help="Directory to save figures (default: display interactively)",
    )
    return p.parse_args()


def main() -> None:
    args = parse_args()

    dirs = [args.dir1, args.dir2]
    for d in dirs:
        if not d.is_dir():
            print(f"ERROR: directory not found: {d}", file=sys.stderr)
            sys.exit(1)

    labels = [algorithm_name(d) for d in dirs]
    dfs = [load_experiment(d) for d in dirs]

    # Summary
    print("\nExperiment summary")
    print("-" * 50)
    for label, d, df in zip(labels, dirs, dfs):
        max_eval = df["Evaluation"].max()
        n_solutions = len(final_population(df))
        best_hv = -final_population(df)["HVMinus"].min()
        best_ep = final_population(df)["EP"].min()
        print(
            f"  {label:20s}  dir={d.name}  evals={max_eval}"
            f"  final_n={n_solutions}  best_HV={best_hv:.4f}  best_EP={best_ep:.4f}"
        )

    # Best configurations (using VAR_CONF.txt for human-readable parameter names)
    for label, d in zip(labels, dirs):
        print_best_config(label, d)

    # Figures
    if args.output:
        args.output.mkdir(parents=True, exist_ok=True)
        pareto_path = args.output / "pareto_final.png"
        evolution_path = args.output / "hv_evolution.png"
    else:
        pareto_path = None
        evolution_path = None

    plot_pareto_fronts(dfs, labels, output_path=pareto_path)
    plot_hv_evolution(dfs, labels, output_path=evolution_path)


if __name__ == "__main__":
    main()
