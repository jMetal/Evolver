"""Track how key parameter values evolve during the meta-optimization search."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import (
    BUDGETS,
    FIGURES_DIR,
    FRONT_TYPE_LABELS,
    FRONT_TYPES,
    TABLES_DIR,
    TOP_PARAMETERS,
    save_figure,
    setup_style,
)
from data_loader import load_all_runs_with_config

WINDOW_SIZE = 50
STABILITY_THRESHOLD = 0.01
SLICES = [
    (family, front_type)
    for front_type in FRONT_TYPES
    for family in ["RE3D", "RWA3D"]
]
FIGURE_OUTPUT_ROOT = FIGURES_DIR / "parameter_drift"
TABLE_OUTPUT_ROOT = TABLES_DIR / "parameter_drift"
FIGURE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)
TABLE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)


def compute_parameter_trajectory(df: pd.DataFrame, param: str) -> pd.DataFrame:
    """Compute median and IQR of a parameter at each evaluation step across runs."""
    grouped = (
        df.groupby("Evaluation")[param]
        .agg(["median", lambda v: v.quantile(0.25), lambda v: v.quantile(0.75)])
        .reset_index()
    )
    grouped.columns = ["Evaluation", "median", "q25", "q75"]
    return grouped.sort_values("Evaluation").reset_index(drop=True)


def detect_stabilization(
    trajectory: pd.DataFrame, window: int = WINDOW_SIZE, threshold: float = STABILITY_THRESHOLD
) -> int | None:
    """Find the evaluation where the parameter stabilizes.

    Stabilization = rolling std of the median (normalized by range) stays
    below *threshold* for all remaining evaluations.
    """
    medians = trajectory["median"].values
    value_range = medians.max() - medians.min()
    if value_range == 0 or len(medians) < window:
        return int(trajectory["Evaluation"].iloc[0])

    rolling_std = pd.Series(medians).rolling(window, min_periods=window).std().values
    normalized = rolling_std / value_range

    for i in range(window - 1, len(normalized)):
        if np.isnan(normalized[i]):
            continue
        if normalized[i] < threshold and all(
            (np.isnan(v) or v < threshold) for v in normalized[i:]
        ):
            return int(trajectory["Evaluation"].iloc[i])
    return None


def plot_drift(
    trajectories: dict[str, pd.DataFrame],
    params: list[str],
    stabilization: dict[str, int | None],
    scope: str,
    front_type: str,
    budget: int,
) -> Path:
    """Create 2x2 drift figure for key parameters."""
    n_params = len(params)
    nrows = (n_params + 1) // 2
    fig, axes = plt.subplots(nrows, 2, figsize=(14, 5 * nrows))
    if nrows == 1:
        axes = axes.reshape(1, -1)
    fig.suptitle(
        f"Parameter drift | {scope} | {FRONT_TYPE_LABELS[front_type]} | budget {budget}",
        fontsize=13,
        fontweight="bold",
    )

    colors = ["#1b9e77", "#d95f02", "#7570b3", "#e7298a"]

    for idx, param in enumerate(params):
        row, col = divmod(idx, 2)
        ax = axes[row, col]
        traj = trajectories[param]
        color = colors[idx % len(colors)]

        ax.plot(traj["Evaluation"], traj["median"], color=color, linewidth=1.5, label="Median")
        ax.fill_between(
            traj["Evaluation"],
            traj["q25"],
            traj["q75"],
            alpha=0.2,
            color=color,
            label="IQR",
        )

        stab = stabilization.get(param)
        if stab is not None:
            ax.axvline(stab, color="red", linestyle="--", alpha=0.7, linewidth=1.5, label=f"Stable @ {stab}")

        ax.set_title(param, fontsize=10)
        ax.set_xlabel("Evaluation")
        ax.set_ylabel("Value")
        ax.legend(fontsize=8)

    for idx in range(n_params, nrows * 2):
        row, col = divmod(idx, 2)
        axes[row, col].set_visible(False)

    fig.tight_layout()
    filename = f"drift_{scope}_{front_type}_{budget}.png"
    return save_figure(fig, str(Path("parameter_drift") / f"budget_{budget}" / filename))


def main() -> None:
    setup_style()
    all_stabilization_rows: list[dict[str, object]] = []

    for budget in BUDGETS:
        budget_rows: list[dict[str, object]] = []

        for scope, front_type in SLICES:
            print(f"Processing {scope} | {front_type} | budget {budget} ...")
            df = load_all_runs_with_config(scope, front_type, budget)
            total_evals = int(df["Evaluation"].max())

            available_params = [p for p in TOP_PARAMETERS if p in df.columns]
            if not available_params:
                print(f"  No key parameters found, skipping.")
                continue

            trajectories: dict[str, pd.DataFrame] = {}
            stabilization: dict[str, int | None] = {}

            for param in available_params:
                traj = compute_parameter_trajectory(df, param)
                trajectories[param] = traj
                stab = detect_stabilization(traj)
                stabilization[param] = stab

                row = {
                    "scope": scope,
                    "front_type": front_type,
                    "budget": budget,
                    "parameter": param,
                    "stabilization_eval": stab,
                    "total_evals": total_evals,
                    "pct_budget_at_stabilization": (
                        round(100.0 * stab / total_evals, 1) if stab is not None else None
                    ),
                }
                budget_rows.append(row)
                all_stabilization_rows.append(row)

            plot_drift(trajectories, available_params, stabilization, scope, front_type, budget)

            del df

        table_dir = TABLE_OUTPUT_ROOT / f"budget_{budget}"
        table_dir.mkdir(parents=True, exist_ok=True)
        budget_df = pd.DataFrame(budget_rows)
        if not budget_df.empty:
            path = table_dir / f"parameter_drift_stabilization_{budget}.csv"
            budget_df.to_csv(path, index=False)
            print(f"Saved: {path}")

    all_df = pd.DataFrame(all_stabilization_rows)
    if not all_df.empty:
        all_path = TABLE_OUTPUT_ROOT / "parameter_drift_stabilization_all.csv"
        all_df.to_csv(all_path, index=False)
        print(f"Saved: {all_path}")


if __name__ == "__main__":
    main()
