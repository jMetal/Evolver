"""Categorical frequency analysis over final-best configurations."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import BUDGETS, FRONT_TYPE_COLORS, FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import REPRESENTATIVE_CATEGORICALS, report_table_path
from data_loader import get_final_best_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "categorical_frequencies"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("categorical_frequencies")


def load_family_data(family: str) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for front_type in ["referenceFronts", "extremePointsFronts"]:
        for budget in BUDGETS:
            final_best = get_final_best_configs(load_all_runs_with_config(family, front_type, budget)).copy()
            final_best["family"] = family
            final_best["front_type"] = front_type
            final_best["budget"] = budget
            frames.append(final_best)
    return pd.concat(frames, ignore_index=True)


def compute_frequency_rows(df: pd.DataFrame) -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for (family, front_type, budget), slice_df in df.groupby(["family", "front_type", "budget"], sort=True):
        for parameter in REPRESENTATIVE_CATEGORICALS:
            if parameter not in slice_df.columns:
                continue
            counts = slice_df[parameter].fillna("<NA>").astype(str).value_counts(normalize=True)
            for value, share in counts.items():
                rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "budget": budget,
                        "parameter": parameter,
                        "value": value,
                        "frequency_pct": float(100.0 * share),
                    }
                )
    return pd.DataFrame(rows).sort_values(["family", "parameter", "budget", "front_type", "frequency_pct"], ascending=[True, True, True, True, False])


def plot_family(df: pd.DataFrame, family: str) -> Path:
    family_df = df.loc[df["family"] == family].copy()
    fig, axes = plt.subplots(2, 2, figsize=(16, 10))
    fig.suptitle(f"Categorical parameter frequencies | {family}", fontsize=13, fontweight="bold")

    for ax, parameter in zip(axes.flat, REPRESENTATIVE_CATEGORICALS, strict=False):
        param_df = family_df.loc[family_df["parameter"] == parameter].copy()
        if param_df.empty:
            ax.set_visible(False)
            continue

        values = (
            param_df.groupby("value")["frequency_pct"]
            .max()
            .sort_values(ascending=False)
            .index.tolist()
        )
        y = np.arange(len(values))
        base_offsets = np.linspace(-0.35, 0.35, len(BUDGETS))
        width = 0.16

        for budget_offset, budget in zip(base_offsets, BUDGETS, strict=False):
            for front_idx, front_type in enumerate(["referenceFronts", "extremePointsFronts"]):
                sub = param_df.loc[
                    (param_df["budget"] == budget) & (param_df["front_type"] == front_type)
                ].set_index("value")
                freqs = [float(sub.loc[value, "frequency_pct"]) if value in sub.index else 0.0 for value in values]
                ax.barh(
                    y + budget_offset + (front_idx - 0.5) * width,
                    freqs,
                    height=width,
                    color=FRONT_TYPE_COLORS[front_type],
                    alpha=0.35 + 0.15 * (budget / max(BUDGETS)),
                    label=f"{budget} | {FRONT_TYPE_LABELS[front_type]}",
                )

        ax.set_yticks(y)
        ax.set_yticklabels(values, fontsize=9)
        ax.invert_yaxis()
        ax.set_xlabel("Frequency (%)")
        ax.set_title(parameter)

    handles, labels = axes.flat[0].get_legend_handles_labels()
    by_label = dict(zip(labels, handles, strict=False))
    fig.legend(by_label.values(), by_label.keys(), loc="lower center", ncol=4, fontsize=8, frameon=False)
    fig.tight_layout(rect=(0, 0.05, 1, 1))
    return save_figure(fig, str(FIGURE_ROOT / f"categorical_frequencies_{family}.png"))


def main() -> None:
    setup_style()

    all_rows: list[pd.DataFrame] = []
    mode_rows: list[dict[str, object]] = []

    for family in ["RE3D", "RWA3D"]:
        family_df = load_family_data(family)
        frequency_df = compute_frequency_rows(family_df)
        all_rows.append(frequency_df)
        plot_family(frequency_df, family)

        mode_df = (
            frequency_df.sort_values(["family", "parameter", "budget", "front_type", "frequency_pct"], ascending=[True, True, True, True, False])
            .groupby(["family", "front_type", "budget", "parameter"], as_index=False)
            .head(1)
            .reset_index(drop=True)
        )
        mode_rows.extend(mode_df.to_dict("records"))

    all_df = pd.concat(all_rows, ignore_index=True)
    mode_df = pd.DataFrame(mode_rows).sort_values(["family", "front_type", "budget", "parameter"])
    all_df.to_csv(TABLE_ROOT / "categorical_parameter_frequencies_all.csv", index=False)
    mode_df.to_csv(TABLE_ROOT / "categorical_parameter_modes_all.csv", index=False)

    compact_df = mode_df.rename(
        columns={
            "family": "Benchmark",
            "front_type": "FrontType",
            "budget": "Budget",
            "parameter": "Parameter",
            "value": "Mode",
            "frequency_pct": "FrequencyPct",
        }
    )
    compact_df["FrontType"] = compact_df["FrontType"].map(FRONT_TYPE_LABELS)
    compact_df.to_csv(report_table_path("categorical_parameter_modes_all.csv"), index=False)

    print(f"Saved: {TABLE_ROOT / 'categorical_parameter_frequencies_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'categorical_parameter_modes_all.csv'}")
    print(f"Saved: {report_table_path('categorical_parameter_modes_all.csv')}")


if __name__ == "__main__":
    main()
