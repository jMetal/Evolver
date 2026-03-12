"""Synthesize reduced NSGA-II parameter spaces from elite recommendations."""

from __future__ import annotations

import copy
import math
from pathlib import Path

import numpy as np
import pandas as pd

from config import ANALYSIS_DIR, FAMILIES, TABLES_DIR
from configuration_analysis import JOINT_VIEW, active_configuration_columns, add_joint_scores, elite_subset
from data_loader import get_final_configs, load_runs_with_config_for_families

try:
    import yaml
except ImportError as exc:  # pragma: no cover - handled at runtime
    raise SystemExit("PyYAML is required to synthesize reduced parameter spaces.") from exc

TABLE_ROOT = TABLES_DIR / "space_reduction"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)
PARAMETER_SPACE_PATH = Path("src/main/resources/parameterSpaces/NSGAIIDouble.yaml")
REDUCED_PARAMETER_SPACE_DIR = Path("src/main/resources/parameterSpaces/reduced")
REDUCED_PARAMETER_SPACE_DIR.mkdir(parents=True, exist_ok=True)
VALIDATION_ROOT = ANALYSIS_DIR / "validation_results"
VALIDATION_ROOT.mkdir(parents=True, exist_ok=True)
SOURCE_BUDGET = 7000


def load_yaml_parameter_space() -> dict[str, object]:
    """Load the baseline NSGA-II parameter space."""
    return yaml.safe_load(PARAMETER_SPACE_PATH.read_text(encoding="utf-8"))


def visit_parameter_nodes(container: object, result: dict[str, dict[str, object]]) -> None:
    """Collect parameter nodes recursively by their unique parameter name."""
    if isinstance(container, dict):
        if "type" in container:
            return
        for key, value in container.items():
            if isinstance(value, dict) and "type" in value:
                result[key] = value
                for nested_key in ["conditionalParameters", "globalSubParameters"]:
                    if nested_key in value:
                        visit_parameter_nodes(value[nested_key], result)
                values = value.get("values")
                if isinstance(values, dict):
                    for option in values.values():
                        visit_parameter_nodes(option, result)
            else:
                visit_parameter_nodes(value, result)


def parameter_nodes(parameter_space: dict[str, object]) -> dict[str, dict[str, object]]:
    """Return a name-to-node map for all parameters in a parameter space."""
    result: dict[str, dict[str, object]] = {}
    visit_parameter_nodes(parameter_space, result)
    return result


def coerce_choice(existing_values: object, desired_value: object) -> object:
    """Map a string label back to the scalar stored in YAML."""
    if isinstance(existing_values, dict):
        for key in existing_values:
            if str(key) == str(desired_value):
                return key
    if isinstance(existing_values, list):
        for value in existing_values:
            if str(value) == str(desired_value):
                return value
    return desired_value


def apply_action_to_parameter_space(
    parameter_space: dict[str, object],
    action_row: pd.Series,
) -> None:
    """Apply one fix/narrow action to a parsed YAML parameter space."""
    nodes = parameter_nodes(parameter_space)
    parameter = str(action_row["parameter"])
    if parameter not in nodes:
        return

    node = nodes[parameter]
    action = str(action_row["action"])
    if action == "fix" and node.get("type") == "categorical":
        values = node.get("values")
        selected = coerce_choice(values, action_row["recommended_value"])
        if isinstance(values, dict) and selected in values:
            node["values"] = {selected: copy.deepcopy(values[selected])}
        elif isinstance(values, list):
            node["values"] = [selected]
    elif action == "narrow" and "range" in node:
        current_min, current_max = node["range"]
        proposed_min = float(action_row["recommended_min"])
        proposed_max = float(action_row["recommended_max"])
        proposed_min = max(float(current_min), proposed_min)
        proposed_max = min(float(current_max), proposed_max)
        if node.get("type") == "integer":
            proposed_min = int(math.floor(proposed_min))
            proposed_max = int(math.ceil(proposed_max))
        else:
            proposed_min = round(proposed_min, 6)
            proposed_max = round(proposed_max, 6)
        if proposed_min > proposed_max:
            proposed_min = proposed_max
        node["range"] = [proposed_min, proposed_max]


def pooled_numeric_action(front_type: str, parameter: str) -> dict[str, object]:
    """Compute a global numeric narrowing suggestion using pooled elite final rows."""
    pooled = load_runs_with_config_for_families(FAMILIES, front_type, SOURCE_BUDGET)
    pooled_final = add_joint_scores(get_final_configs(pooled))
    elite = elite_subset(pooled_final, JOINT_VIEW, 0.10)
    observed = pd.to_numeric(pooled_final[parameter], errors="coerce").dropna()
    elite_values = pd.to_numeric(elite[parameter], errors="coerce").dropna()
    if observed.empty or elite_values.empty:
        return {
            "action": "keep_open",
            "recommended_min": np.nan,
            "recommended_max": np.nan,
        }
    observed_min = float(observed.min())
    observed_max = float(observed.max())
    elite_median = float(elite_values.median())
    elite_iqr = float(elite_values.quantile(0.75) - elite_values.quantile(0.25))
    proposed_min = max(observed_min, elite_median - 1.5 * elite_iqr)
    proposed_max = min(observed_max, elite_median + 1.5 * elite_iqr)
    return {
        "action": "narrow",
        "recommended_min": proposed_min,
        "recommended_max": proposed_max,
    }


def build_global_actions(
    elite_recommendations: pd.DataFrame,
    front_type: str,
) -> pd.DataFrame:
    """Build one global proposal for a front type from benchmark agreement."""
    rows: list[dict[str, object]] = []
    scope_rows = elite_recommendations.loc[
        (elite_recommendations["front_type"] == front_type)
        & (elite_recommendations["budget"] == SOURCE_BUDGET)
    ].copy()

    for parameter, group in scope_rows.groupby("parameter"):
        family_rows = {
            str(row["family"]): row
            for _, row in group.iterrows()
        }
        if len(family_rows) < len(FAMILIES):
            continue

        first = next(iter(family_rows.values()))
        action = "keep_open"
        recommended_value = None
        recommended_min = np.nan
        recommended_max = np.nan

        if all(str(row["action"]) == "fix" for row in family_rows.values()):
            values = {str(row["recommended_value"]) for row in family_rows.values()}
            if len(values) == 1:
                action = "fix"
                recommended_value = next(iter(values))
        elif all(str(row["action"]) == "narrow" for row in family_rows.values()):
            pooled_action = pooled_numeric_action(front_type, parameter)
            action = str(pooled_action["action"])
            recommended_min = pooled_action["recommended_min"]
            recommended_max = pooled_action["recommended_max"]

        rows.append(
            {
                "proposal_name": f"global_{front_type}",
                "proposal_scope": "global_front_type",
                "family": "ALL",
                "front_type": front_type,
                "budget": SOURCE_BUDGET,
                "parameter": parameter,
                "parameter_kind": first["parameter_kind"],
                "action": action,
                "recommended_value": recommended_value,
                "recommended_min": recommended_min,
                "recommended_max": recommended_max,
            }
        )

    return pd.DataFrame(rows)


def build_family_specific_actions(elite_recommendations: pd.DataFrame) -> pd.DataFrame:
    """Keep the budget-7000 slice-level recommendations as family-specific proposals."""
    scoped = elite_recommendations.loc[elite_recommendations["budget"] == SOURCE_BUDGET].copy()
    scoped["proposal_name"] = scoped["family"] + "_" + scoped["front_type"]
    scoped["proposal_scope"] = "family_front_type"
    return scoped[
        [
            "proposal_name",
            "proposal_scope",
            "family",
            "front_type",
            "budget",
            "parameter",
            "parameter_kind",
            "action",
            "recommended_value",
            "recommended_min",
            "recommended_max",
        ]
    ].copy()


def summarize_parameter(
    proposal_name: str,
    proposal_scope: str,
    family: str,
    front_type: str,
    parameter: str,
    original_node: dict[str, object],
    reduced_node: dict[str, object] | None,
    action: str,
) -> dict[str, object]:
    """Create one parameter-level comparison row."""
    reduced_node = reduced_node or {}
    original_values = original_node.get("values")
    reduced_values = reduced_node.get("values")

    original_choice_count = (
        len(original_values)
        if isinstance(original_values, (dict, list))
        else np.nan
    )
    reduced_choice_count = (
        len(reduced_values)
        if isinstance(reduced_values, (dict, list))
        else np.nan
    )

    original_range = original_node.get("range", [np.nan, np.nan])
    reduced_range = reduced_node.get("range", [np.nan, np.nan])
    original_width = (
        float(original_range[1] - original_range[0])
        if len(original_range) == 2 and pd.notna(original_range[0]) and pd.notna(original_range[1])
        else np.nan
    )
    reduced_width = (
        float(reduced_range[1] - reduced_range[0])
        if len(reduced_range) == 2 and pd.notna(reduced_range[0]) and pd.notna(reduced_range[1])
        else np.nan
    )

    return {
        "proposal_name": proposal_name,
        "proposal_scope": proposal_scope,
        "family": family,
        "front_type": front_type,
        "parameter": parameter,
        "parameter_kind": original_node.get("type"),
        "action": action if reduced_node else "deactivated_by_parent",
        "original_choice_count": original_choice_count,
        "reduced_choice_count": reduced_choice_count,
        "original_range_min": original_range[0] if len(original_range) == 2 else np.nan,
        "original_range_max": original_range[1] if len(original_range) == 2 else np.nan,
        "reduced_range_min": reduced_range[0] if len(reduced_range) == 2 else np.nan,
        "reduced_range_max": reduced_range[1] if len(reduced_range) == 2 else np.nan,
        "original_width": original_width,
        "reduced_width": reduced_width,
    }


def complexity_contribution(node: dict[str, object], original_width: float | None = None) -> float:
    """Compute a simple effective-complexity contribution for one parameter."""
    values = node.get("values")
    if isinstance(values, dict):
        return float(np.log2(max(len(values), 1)))
    if isinstance(values, list):
        return float(np.log2(max(len(values), 1)))

    if "range" in node:
        width = float(node["range"][1] - node["range"][0])
        if original_width is None or original_width <= 0.0:
            return 0.0 if width <= 0.0 else 1.0
        return max(0.0, width / original_width)
    return 0.0


def write_parameter_space(parameter_space: dict[str, object], output_path: Path) -> None:
    """Write one YAML file preserving top-level order."""
    output_path.write_text(
        yaml.safe_dump(parameter_space, sort_keys=False, allow_unicode=False),
        encoding="utf-8",
    )


def main() -> None:
    elite_recommendations = pd.read_csv(
        TABLES_DIR / "elite_subspace" / "elite_recommendations_all.csv"
    )
    original_space = load_yaml_parameter_space()
    original_nodes = parameter_nodes(original_space)

    proposal_actions = pd.concat(
        [
            build_global_actions(elite_recommendations, "referenceFronts"),
            build_global_actions(elite_recommendations, "extremePointsFronts"),
            build_family_specific_actions(elite_recommendations),
        ],
        ignore_index=True,
    ).sort_values(["proposal_name", "parameter"])

    detail_rows: list[dict[str, object]] = []
    summary_rows: list[dict[str, object]] = []
    manifest_rows: list[dict[str, object]] = []

    for proposal_name, proposal_df in proposal_actions.groupby("proposal_name", sort=True):
        proposal_space = copy.deepcopy(original_space)
        proposal_scope = str(proposal_df["proposal_scope"].iloc[0])
        family = str(proposal_df["family"].iloc[0])
        front_type = str(proposal_df["front_type"].iloc[0])

        for _, action_row in proposal_df.iterrows():
            if str(action_row["action"]) in {"fix", "narrow"}:
                apply_action_to_parameter_space(proposal_space, action_row)

        proposal_nodes = parameter_nodes(proposal_space)
        for parameter, original_node in original_nodes.items():
            reduced_node = proposal_nodes.get(parameter)
            matching_action = proposal_df.loc[proposal_df["parameter"] == parameter, "action"]
            action = str(matching_action.iloc[0]) if not matching_action.empty else "keep_open"
            detail_rows.append(
                summarize_parameter(
                    proposal_name,
                    proposal_scope,
                    family,
                    front_type,
                    parameter,
                    original_node,
                    reduced_node,
                    action,
                )
            )

        proposal_detail_df = pd.DataFrame(detail_rows)
        proposal_detail_df = proposal_detail_df.loc[
            proposal_detail_df["proposal_name"] == proposal_name
        ].copy()

        original_active_dimensions = (
            proposal_detail_df["original_choice_count"].fillna(0).gt(1).sum()
            + proposal_detail_df["original_width"].fillna(0).gt(0).sum()
        )
        reduced_active_dimensions = (
            proposal_detail_df["reduced_choice_count"].fillna(0).gt(1).sum()
            + proposal_detail_df["reduced_width"].fillna(0).gt(0).sum()
        )
        active_dimension_reduction_pct = (
            100.0 * (original_active_dimensions - reduced_active_dimensions) / original_active_dimensions
            if original_active_dimensions > 0
            else 0.0
        )

        original_complexity = 0.0
        reduced_complexity = 0.0
        numeric_width_reductions: list[float] = []
        fixed_categorical_count = 0
        narrowed_numeric_count = 0

        for parameter, original_node in original_nodes.items():
            reduced_node = proposal_nodes.get(parameter, {})
            original_width = proposal_detail_df.loc[
                proposal_detail_df["parameter"] == parameter, "original_width"
            ].iloc[0]
            reduced_width = proposal_detail_df.loc[
                proposal_detail_df["parameter"] == parameter, "reduced_width"
            ].iloc[0]
            original_complexity += complexity_contribution(original_node, original_width)
            reduced_complexity += complexity_contribution(reduced_node, original_width)

            if pd.notna(original_width) and original_width > 0.0 and pd.notna(reduced_width):
                numeric_width_reductions.append(100.0 * (1.0 - float(reduced_width) / float(original_width)))

        fixed_categorical_count = int((proposal_df["action"] == "fix").sum())
        narrowed_numeric_count = int((proposal_df["action"] == "narrow").sum())
        effective_complexity_reduction_pct = (
            100.0 * (1.0 - reduced_complexity / original_complexity)
            if original_complexity > 0.0
            else 0.0
        )

        yaml_path = REDUCED_PARAMETER_SPACE_DIR / f"NSGAIIDouble_{proposal_name}.yaml"
        write_parameter_space(proposal_space, yaml_path)

        summary_rows.append(
            {
                "proposal_name": proposal_name,
                "proposal_scope": proposal_scope,
                "family": family,
                "front_type": front_type,
                "source_budget": SOURCE_BUDGET,
                "fixed_categorical_count": fixed_categorical_count,
                "narrowed_numeric_count": narrowed_numeric_count,
                "active_dimension_reduction_pct": active_dimension_reduction_pct,
                "effective_complexity_reduction_pct": effective_complexity_reduction_pct,
                "mean_numeric_width_reduction_pct": float(np.mean(numeric_width_reductions)) if numeric_width_reductions else 0.0,
                "yaml_path": str(yaml_path),
            }
        )
        manifest_rows.append(
            {
                "proposal_name": proposal_name,
                "proposal_scope": proposal_scope,
                "family": family,
                "front_type": front_type,
                "yaml_path": str(yaml_path),
                "validation_results_root": str(VALIDATION_ROOT / proposal_name),
            }
        )
        print(f"Saved: {yaml_path}")

    detail_df = pd.DataFrame(detail_rows).sort_values(
        ["proposal_name", "parameter"]
    )
    summary_df = pd.DataFrame(summary_rows).sort_values(["proposal_scope", "proposal_name"])
    manifest_df = pd.DataFrame(manifest_rows).sort_values(["proposal_scope", "proposal_name"])

    detail_df.to_csv(TABLE_ROOT / "reduced_space_parameter_actions.csv", index=False)
    summary_df.to_csv(TABLE_ROOT / "reduced_space_summary.csv", index=False)
    manifest_df.to_csv(TABLE_ROOT / "reduced_space_manifest.csv", index=False)

    print(f"Saved: {TABLE_ROOT / 'reduced_space_parameter_actions.csv'}")
    print(f"Saved: {TABLE_ROOT / 'reduced_space_summary.csv'}")
    print(f"Saved: {TABLE_ROOT / 'reduced_space_manifest.csv'}")


if __name__ == "__main__":
    main()
