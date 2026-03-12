"""t-SNE/UMAP projections and parallel coordinates of configuration space."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from matplotlib.collections import LineCollection
from sklearn.manifold import TSNE
from umap import UMAP

from config import (
    BUDGETS,
    CATEGORICAL_DECODINGS,
    FAMILIES,
    FIGURES_DIR,
    FRONT_TYPE_LABELS,
    FRONT_TYPES,
    NUMERICAL_COLUMNS,
    TABLES_DIR,
    TOP_PARAMETERS,
    save_figure,
    setup_style,
)
from data_loader import (
    encode_config_vector,
    get_final_configs,
    load_runs_with_config_for_families,
)

TSNE_KWARGS = {"n_components": 2, "perplexity": 30, "random_state": 42, "max_iter": 1000}
UMAP_KWARGS = {"n_components": 2, "n_neighbors": 15, "min_dist": 0.1, "random_state": 42}
PARALLEL_TOP_K = 8
FIGURE_OUTPUT_ROOT = FIGURES_DIR / "projections"
TABLE_OUTPUT_ROOT = TABLES_DIR / "projections"
FIGURE_OUTPUT_ROOT.mkdir(parents=True, exist_ok=True)


def get_top_params(budget: int, front_type: str) -> list[str]:
    """Get top parameters from consensus importance table if available, else fallback."""
    consensus_path = (
        TABLES_DIR
        / "component_importance"
        / f"budget_{budget}"
        / f"component_importance_consensus_summary_{budget}.csv"
    )
    if consensus_path.exists():
        consensus = pd.read_csv(consensus_path)
        ft_data = consensus[consensus["front_type"] == front_type]
        if not ft_data.empty:
            top = (
                ft_data.groupby("parameter_family")["consensus_importance"]
                .mean()
                .sort_values(ascending=False)
                .head(PARALLEL_TOP_K)
                .index.tolist()
            )
            if len(top) >= 4:
                return top

    extras = [c for c in NUMERICAL_COLUMNS if c not in TOP_PARAMETERS][:4]
    return TOP_PARAMETERS + extras


def plot_projection(
    coords: np.ndarray,
    color_values: np.ndarray | pd.Series,
    color_label: str,
    method: str,
    front_type: str,
    budget: int,
    categorical: bool = False,
    family_labels: pd.Series | None = None,
) -> Path:
    """Scatter plot of 2D embedding."""
    fig, ax = plt.subplots(figsize=(9, 7))

    if categorical and family_labels is not None:
        families = family_labels.unique()
        palette = {"RE3D": "#1b9e77", "RWA3D": "#d95f02"}
        for fam in families:
            mask = family_labels == fam
            ax.scatter(
                coords[mask, 0],
                coords[mask, 1],
                c=palette.get(fam, "grey"),
                label=fam,
                alpha=0.5,
                s=15,
                edgecolors="none",
            )
        ax.legend(fontsize=9)
    else:
        sc = ax.scatter(
            coords[:, 0],
            coords[:, 1],
            c=color_values,
            cmap="viridis_r",
            alpha=0.5,
            s=15,
            edgecolors="none",
        )
        cbar = plt.colorbar(sc, ax=ax)
        cbar.set_label(color_label, fontsize=10)

    ax.set_title(
        f"{method.upper()} | {FRONT_TYPE_LABELS[front_type]} | budget {budget} | color: {color_label}",
        fontsize=11,
    )
    ax.set_xlabel(f"{method.upper()} 1")
    ax.set_ylabel(f"{method.upper()} 2")
    fig.tight_layout()

    filename = f"{method}_{color_label}_{front_type}_{budget}.png"
    return save_figure(fig, str(Path("projections") / f"budget_{budget}" / filename))


def plot_parallel_coordinates(
    df: pd.DataFrame,
    params: list[str],
    color_values: np.ndarray,
    color_label: str,
    front_type: str,
    budget: int,
) -> Path:
    """Parallel coordinates plot colored by performance quartile."""
    available = [p for p in params if p in df.columns]
    if not available:
        return Path()

    normalized = pd.DataFrame(index=df.index)
    for p in available:
        vals = df[p]
        if pd.api.types.is_numeric_dtype(vals):
            vmin, vmax = vals.min(), vals.max()
            if vmax > vmin:
                normalized[p] = (vals - vmin) / (vmax - vmin)
            else:
                normalized[p] = 0.5
        else:
            codes = vals.astype("category").cat.codes.astype(float)
            cmin, cmax = codes.min(), codes.max()
            if cmax > cmin:
                normalized[p] = (codes - cmin) / (cmax - cmin)
            else:
                normalized[p] = 0.5

    quartiles = pd.qcut(color_values, 4, labels=False, duplicates="drop")
    cmap = plt.cm.RdYlGn_r
    quartile_colors = [cmap(q / 3.0) for q in range(4)]
    quartile_labels = ["Q1 (best)", "Q2", "Q3", "Q4 (worst)"]

    fig, ax = plt.subplots(figsize=(max(14, 2 * len(available)), 7))

    x = np.arange(len(available))
    for q in reversed(range(4)):
        mask = quartiles == q
        subset = normalized.loc[mask]
        for _, row in subset.iterrows():
            ax.plot(x, row[available].values, color=quartile_colors[q], alpha=0.15, linewidth=0.5)

    for q in range(4):
        ax.plot([], [], color=quartile_colors[q], linewidth=2, label=quartile_labels[q])
    ax.legend(fontsize=9, loc="upper right")

    ax.set_xticks(x)
    ax.set_xticklabels(available, rotation=45, ha="right", fontsize=9)
    ax.set_ylabel("Normalized value")
    ax.set_title(
        f"Parallel coords | {FRONT_TYPE_LABELS[front_type]} | budget {budget} | color: {color_label}",
        fontsize=11,
    )
    ax.set_xlim(-0.5, len(available) - 0.5)
    ax.set_ylim(-0.05, 1.05)
    fig.tight_layout()

    filename = f"parallel_coords_{color_label}_{front_type}_{budget}.png"
    return save_figure(fig, str(Path("projections") / f"budget_{budget}" / filename))


def main() -> None:
    setup_style()

    for budget in BUDGETS:
        for front_type in FRONT_TYPES:
            print(f"Processing {front_type} | budget {budget} ...")

            pooled = load_runs_with_config_for_families(FAMILIES, front_type, budget)
            final = get_final_configs(pooled)

            feature_df, feature_names = encode_config_vector(final)

            # t-SNE
            print("  Computing t-SNE ...")
            tsne = TSNE(**TSNE_KWARGS)
            tsne_coords = tsne.fit_transform(feature_df.values)

            # UMAP
            print("  Computing UMAP ...")
            reducer = UMAP(**UMAP_KWARGS)
            umap_coords = reducer.fit_transform(feature_df.values)

            # Projection plots: family, EP, HVMinus
            for method, coords in [("tsne", tsne_coords), ("umap", umap_coords)]:
                plot_projection(
                    coords,
                    final["family"].values,
                    "family",
                    method,
                    front_type,
                    budget,
                    categorical=True,
                    family_labels=final["family"],
                )
                for indicator in ["EP", "HVMinus"]:
                    plot_projection(
                        coords,
                        final[indicator].values,
                        indicator,
                        method,
                        front_type,
                        budget,
                    )

            # Parallel coordinates
            top_params = get_top_params(budget, front_type)
            for indicator in ["EP", "HVMinus"]:
                plot_parallel_coordinates(
                    final,
                    top_params,
                    final[indicator].values,
                    indicator,
                    front_type,
                    budget,
                )

            del pooled, final, feature_df
            print(f"  Done.")


if __name__ == "__main__":
    main()
