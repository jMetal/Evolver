"""K-means clustering of final-best configurations."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
from sklearn.cluster import KMeans
from sklearn.decomposition import PCA
from sklearn.metrics import silhouette_score

from config import FRONT_TYPE_LABELS, TABLES_DIR, save_figure, setup_style
from configuration_analysis import (
    active_configuration_columns,
    build_model_preprocessor,
    mode_value,
    report_table_path,
)
from data_loader import get_final_best_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "configuration_clustering"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("configuration_clustering")


def load_family_front_data(family: str, front_type: str) -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for budget in [1000, 3000, 5000, 7000]:
        final_best = get_final_best_configs(load_all_runs_with_config(family, front_type, budget)).copy()
        final_best["family"] = family
        final_best["front_type"] = front_type
        final_best["budget"] = budget
        final_best["HV"] = -final_best["HVMinus"]
        frames.append(final_best)
    return pd.concat(frames, ignore_index=True)


def cluster_slice(df: pd.DataFrame) -> tuple[pd.DataFrame, pd.DataFrame, int]:
    config_columns = active_configuration_columns(df)
    preprocessor, feature_names = build_model_preprocessor(df, config_columns)
    X = preprocessor.fit_transform(df[feature_names])

    silhouette_rows: list[dict[str, object]] = []
    best_score = -np.inf
    best_k = 2
    best_labels = None
    max_k = min(7, len(df) - 1)
    for k in range(2, max_k + 1):
        model = KMeans(n_clusters=k, random_state=42, n_init="auto")
        labels = model.fit_predict(X)
        score = silhouette_score(X, labels)
        silhouette_rows.append({"k": k, "silhouette_score": float(score)})
        if score > best_score:
            best_score = float(score)
            best_k = k
            best_labels = labels

    if best_labels is None:
        raise ValueError("Could not fit any KMeans model.")

    clustered = df.copy()
    clustered["cluster"] = best_labels.astype(int)
    silhouette_df = pd.DataFrame(silhouette_rows)
    return clustered, silhouette_df, best_k


def characterize_clusters(df: pd.DataFrame) -> pd.DataFrame:
    config_columns = active_configuration_columns(df)
    rows: list[dict[str, object]] = []
    for cluster_id, cluster_df in df.groupby("cluster", sort=True):
        row: dict[str, object] = {
            "cluster": int(cluster_id),
            "n_rows": int(len(cluster_df)),
            "hv_median": float(cluster_df["HV"].median()),
            "ep_median": float(cluster_df["EP"].median()),
        }
        for column in config_columns:
            if pd.api.types.is_numeric_dtype(cluster_df[column]):
                values = pd.to_numeric(cluster_df[column], errors="coerce").dropna()
                row[f"{column}_median"] = float(values.median()) if not values.empty else np.nan
            else:
                row[f"{column}_mode"] = mode_value(cluster_df[column])
        rows.append(row)
    return pd.DataFrame(rows).sort_values("cluster")


def plot_slice(
    family: str,
    front_type: str,
    clustered_df: pd.DataFrame,
    silhouette_df: pd.DataFrame,
) -> Path:
    config_columns = active_configuration_columns(clustered_df)
    preprocessor, feature_names = build_model_preprocessor(clustered_df, config_columns)
    X = preprocessor.fit_transform(clustered_df[feature_names])
    coords = PCA(n_components=2, random_state=42).fit_transform(X)

    fig, axes = plt.subplots(2, 2, figsize=(16, 12))
    fig.suptitle(
        f"K-means clustering | {family} | {FRONT_TYPE_LABELS[front_type]}",
        fontsize=13,
        fontweight="bold",
    )

    axes[0][0].plot(silhouette_df["k"], silhouette_df["silhouette_score"], marker="o", color="#1b9e77")
    axes[0][0].set_title("Silhouette by k")
    axes[0][0].set_xlabel("k")
    axes[0][0].set_ylabel("Silhouette score")

    cluster_scatter = axes[0][1].scatter(
        coords[:, 0],
        coords[:, 1],
        c=clustered_df["cluster"],
        cmap="tab10",
        alpha=0.7,
        s=24,
    )
    axes[0][1].set_title("PCA projection colored by cluster")
    axes[0][1].set_xlabel("PC1")
    axes[0][1].set_ylabel("PC2")
    fig.colorbar(cluster_scatter, ax=axes[0][1], fraction=0.045, pad=0.02)

    hv_scatter = axes[1][0].scatter(
        coords[:, 0],
        coords[:, 1],
        c=clustered_df["HV"],
        cmap="viridis",
        alpha=0.7,
        s=24,
    )
    axes[1][0].set_title("PCA projection colored by HV")
    axes[1][0].set_xlabel("PC1")
    axes[1][0].set_ylabel("PC2")
    fig.colorbar(hv_scatter, ax=axes[1][0], fraction=0.045, pad=0.02)

    cluster_ids = sorted(clustered_df["cluster"].unique())
    hv_by_cluster = [
        clustered_df.loc[clustered_df["cluster"] == cluster_id, "HV"].to_numpy(dtype=float)
        for cluster_id in cluster_ids
    ]
    axes[1][1].boxplot(hv_by_cluster, tick_labels=[str(cluster_id) for cluster_id in cluster_ids], showfliers=False)
    axes[1][1].set_title("HV by cluster")
    axes[1][1].set_xlabel("Cluster")
    axes[1][1].set_ylabel("HV")

    fig.tight_layout()
    return save_figure(fig, str(FIGURE_ROOT / f"configuration_clustering_{family}_{front_type}.png"))


def main() -> None:
    setup_style()

    silhouette_rows: list[pd.DataFrame] = []
    characterization_rows: list[pd.DataFrame] = []
    compact_rows: list[dict[str, object]] = []

    for family in ["RE3D", "RWA3D"]:
        for front_type in ["referenceFronts", "extremePointsFronts"]:
            slice_df = load_family_front_data(family, front_type)
            clustered_df, silhouette_df, best_k = cluster_slice(slice_df)
            characterization_df = characterize_clusters(clustered_df)

            silhouette_df["family"] = family
            silhouette_df["front_type"] = front_type
            silhouette_df["selected_k"] = best_k
            silhouette_rows.append(silhouette_df)

            characterization_df["family"] = family
            characterization_df["front_type"] = front_type
            characterization_rows.append(characterization_df)

            compact_rows.append(
                {
                    "Benchmark": family,
                    "FrontType": FRONT_TYPE_LABELS[front_type],
                    "SelectedK": best_k,
                    "BestSilhouette": float(silhouette_df["silhouette_score"].max()),
                    "Clusters": int(characterization_df["cluster"].nunique()),
                }
            )

            plot_slice(family, front_type, clustered_df, silhouette_df)

    silhouette_all_df = pd.concat(silhouette_rows, ignore_index=True)
    characterization_all_df = pd.concat(characterization_rows, ignore_index=True)
    silhouette_all_df.to_csv(TABLE_ROOT / "clustering_silhouette_all.csv", index=False)
    characterization_all_df.to_csv(TABLE_ROOT / "clustering_characterization_all.csv", index=False)
    pd.DataFrame(compact_rows).to_csv(report_table_path("configuration_clustering_summary.csv"), index=False)

    print(f"Saved: {TABLE_ROOT / 'clustering_silhouette_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'clustering_characterization_all.csv'}")
    print(f"Saved: {report_table_path('configuration_clustering_summary.csv')}")


if __name__ == "__main__":
    main()
