"""Compare reduced-space validation reruns against the current baseline results."""

from __future__ import annotations

from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

from config import BUDGETS, FAMILIES, FRONT_TYPE_LABELS, FRONT_TYPES, save_figure, setup_style
from data_loader import get_final_configs, load_all_runs_with_config

TABLE_ROOT = Path(__file__).resolve().parent / "tables" / "validation_reduced_space"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
FIGURE_ROOT = Path("validation_reduced_space")

HV_TOLERANCE_PCT = {
    "referenceFronts": 0.5,
    "extremePointsFronts": 1.0,
}
EP_TOLERANCE_PCT = 5.0
MIN_REDUCTION_PCT = 25.0


def load_final_metrics(
    family: str,
    front_type: str,
    budget: int,
    results_root: Path | None = None,
) -> dict[str, object]:
    """Load final EP/HV medians for one slice."""
    final_df = get_final_configs(load_all_runs_with_config(family, front_type, budget, results_root))
    hv = -final_df["HVMinus"]
    return {
        "n_rows": int(len(final_df)),
        "n_runs": int(final_df["run"].nunique()) if "run" in final_df.columns else np.nan,
        "ep_median": float(final_df["EP"].median()),
        "hv_median": float(hv.median()),
    }


def proposal_covers_slice(proposal_row: pd.Series, family: str, front_type: str) -> bool:
    """Return whether a proposal should be evaluated on a given slice."""
    if str(proposal_row["proposal_scope"]) == "global_front_type":
        return str(proposal_row["front_type"]) == front_type
    return (
        str(proposal_row["front_type"]) == front_type
        and str(proposal_row["family"]) == family
    )


def status_for_slice(proposal_row: pd.Series, hv_delta_pct: float, ep_delta_pct: float) -> str:
    """Evaluate one slice against the acceptance thresholds."""
    reduction_ok = (
        float(proposal_row["active_dimension_reduction_pct"]) >= MIN_REDUCTION_PCT
        or float(proposal_row["effective_complexity_reduction_pct"]) >= MIN_REDUCTION_PCT
    )
    hv_ok = hv_delta_pct >= -HV_TOLERANCE_PCT[str(proposal_row["front_type"])]
    ep_ok = ep_delta_pct >= -EP_TOLERANCE_PCT
    return "accepted" if reduction_ok and hv_ok and ep_ok else "rejected"


def plot_validation_overview(summary_df: pd.DataFrame) -> Path | None:
    """Create a compact completion/pass-rate figure when validation data exists."""
    completed = summary_df.loc[summary_df["completed_slices"] > 0].copy()
    if completed.empty:
        return None

    fig, axes = plt.subplots(1, 2, figsize=(12, 5))
    x = np.arange(len(completed))

    axes[0].bar(x, completed["completed_slices"], color="#a6bddb", label="Completados")
    axes[0].bar(x, completed["passed_slices"], color="#1b9e77", label="Aceptados")
    axes[0].set_xticks(x)
    axes[0].set_xticklabels(completed["proposal_name"], rotation=25, ha="right")
    axes[0].set_ylabel("Slices")
    axes[0].set_title("Cobertura y aceptación")
    axes[0].legend(frameon=False)

    axes[1].bar(x, completed["mean_hv_delta_pct"], color="#2b8cbe", label="ΔHV (%)")
    axes[1].bar(x, completed["mean_ep_delta_pct"], color="#de2d26", alpha=0.75, label="ΔEP (%)")
    axes[1].set_xticks(x)
    axes[1].set_xticklabels(completed["proposal_name"], rotation=25, ha="right")
    axes[1].set_ylabel("Cambio mediano (%)")
    axes[1].set_title("Cambio medio frente a baseline")
    axes[1].legend(frameon=False)

    fig.tight_layout()
    return save_figure(fig, str(FIGURE_ROOT / "validation_overview.png"))


def main() -> None:
    setup_style()

    manifest_df = pd.read_csv(
        Path(__file__).resolve().parent / "tables" / "space_reduction" / "reduced_space_manifest.csv"
    )
    proposal_summary_df = pd.read_csv(
        Path(__file__).resolve().parent / "tables" / "space_reduction" / "reduced_space_summary.csv"
    )
    proposal_df = manifest_df.merge(
        proposal_summary_df,
        on=["proposal_name", "proposal_scope", "family", "front_type"],
        how="left",
    )

    baseline_cache: dict[tuple[str, str, int], dict[str, object]] = {}
    slice_rows: list[dict[str, object]] = []

    for _, proposal_row in proposal_df.iterrows():
        proposal_root = Path(str(proposal_row["validation_results_root"]))
        for family in FAMILIES:
            for front_type in FRONT_TYPES:
                if not proposal_covers_slice(proposal_row, family, front_type):
                    continue
                for budget in BUDGETS:
                    slice_key = (family, front_type, budget)
                    if slice_key not in baseline_cache:
                        baseline_cache[slice_key] = load_final_metrics(family, front_type, budget)
                    baseline = baseline_cache[slice_key]

                    row = {
                        "proposal_name": proposal_row["proposal_name"],
                        "proposal_scope": proposal_row["proposal_scope"],
                        "family": family,
                        "front_type": front_type,
                        "budget": budget,
                        "baseline_ep_median": baseline["ep_median"],
                        "baseline_hv_median": baseline["hv_median"],
                        "active_dimension_reduction_pct": proposal_row["active_dimension_reduction_pct"],
                        "effective_complexity_reduction_pct": proposal_row["effective_complexity_reduction_pct"],
                        "validation_results_root": str(proposal_root),
                    }

                    try:
                        validation = load_final_metrics(
                            family,
                            front_type,
                            budget,
                            results_root=proposal_root,
                        )
                    except FileNotFoundError:
                        row.update(
                            {
                                "status": "not_run",
                                "validation_ep_median": np.nan,
                                "validation_hv_median": np.nan,
                                "ep_delta_pct": np.nan,
                                "hv_delta_pct": np.nan,
                                "validation_runs": 0,
                            }
                        )
                        slice_rows.append(row)
                        continue

                    ep_delta_pct = 100.0 * (
                        (validation["ep_median"] - baseline["ep_median"]) / abs(baseline["ep_median"])
                    ) if baseline["ep_median"] != 0 else 0.0
                    hv_delta_pct = 100.0 * (
                        (validation["hv_median"] - baseline["hv_median"]) / abs(baseline["hv_median"])
                    ) if baseline["hv_median"] != 0 else 0.0

                    row.update(
                        {
                            "status": status_for_slice(proposal_row, hv_delta_pct, ep_delta_pct),
                            "validation_ep_median": validation["ep_median"],
                            "validation_hv_median": validation["hv_median"],
                            "ep_delta_pct": ep_delta_pct,
                            "hv_delta_pct": hv_delta_pct,
                            "validation_runs": validation["n_runs"],
                        }
                    )
                    slice_rows.append(row)

    comparison_df = pd.DataFrame(slice_rows).sort_values(
        ["proposal_name", "front_type", "family", "budget"]
    )
    proposal_rollup_df = (
        comparison_df.groupby(["proposal_name", "proposal_scope", "front_type"], as_index=False)
        .agg(
            completed_slices=("status", lambda values: int((values != "not_run").sum())),
            passed_slices=("status", lambda values: int((values == "accepted").sum())),
            mean_hv_delta_pct=("hv_delta_pct", "mean"),
            mean_ep_delta_pct=("ep_delta_pct", "mean"),
        )
        .sort_values(["proposal_scope", "proposal_name"])
    )

    recommendation_rows: list[dict[str, object]] = []
    for family in FAMILIES:
        for front_type in FRONT_TYPES:
            for budget in BUDGETS:
                sub = comparison_df.loc[
                    (comparison_df["family"] == family)
                    & (comparison_df["front_type"] == front_type)
                    & (comparison_df["budget"] == budget)
                    & (comparison_df["status"] != "not_run")
                ].copy()
                if sub.empty:
                    recommendation_rows.append(
                        {
                            "family": family,
                            "front_type": front_type,
                            "budget": budget,
                            "recommended_proposal": "not_run",
                            "recommendation_status": "not_run",
                        }
                    )
                    continue

                accepted_global = sub.loc[
                    (sub["proposal_scope"] == "global_front_type")
                    & (sub["status"] == "accepted")
                ]
                accepted_specific = sub.loc[
                    (sub["proposal_scope"] == "family_front_type")
                    & (sub["status"] == "accepted")
                ]
                if not accepted_global.empty:
                    chosen = accepted_global.sort_values(["hv_delta_pct", "ep_delta_pct"], ascending=[False, True]).iloc[0]
                elif not accepted_specific.empty:
                    chosen = accepted_specific.sort_values(["hv_delta_pct", "ep_delta_pct"], ascending=[False, True]).iloc[0]
                else:
                    chosen = sub.sort_values(["hv_delta_pct", "ep_delta_pct"], ascending=[False, True]).iloc[0]

                recommendation_rows.append(
                    {
                        "family": family,
                        "front_type": front_type,
                        "budget": budget,
                        "recommended_proposal": chosen["proposal_name"],
                        "recommendation_status": chosen["status"],
                    }
                )

    recommendation_df = pd.DataFrame(recommendation_rows).sort_values(
        ["front_type", "family", "budget"]
    )

    comparison_df.to_csv(TABLE_ROOT / "validation_slice_comparison.csv", index=False)
    proposal_rollup_df.to_csv(TABLE_ROOT / "validation_acceptance_summary.csv", index=False)
    recommendation_df.to_csv(TABLE_ROOT / "validation_recommendations.csv", index=False)

    figure_path = plot_validation_overview(proposal_rollup_df)
    if figure_path is not None:
        print(f"Saved: {figure_path}")
    print(f"Saved: {TABLE_ROOT / 'validation_slice_comparison.csv'}")
    print(f"Saved: {TABLE_ROOT / 'validation_acceptance_summary.csv'}")
    print(f"Saved: {TABLE_ROOT / 'validation_recommendations.csv'}")


if __name__ == "__main__":
    main()
