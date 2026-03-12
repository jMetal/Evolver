"""Extract interpretable conditional rules for elite final configurations."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.tree import DecisionTreeClassifier

from config import TABLES_DIR
from configuration_analysis import (
    JOINT_VIEW,
    active_configuration_columns,
    add_joint_scores,
    elite_subset,
    iter_slices,
    parameter_kind,
    prioritized_branching_parameters,
    safe_category_label,
)
from data_loader import get_final_configs, load_all_runs_with_config

TABLE_ROOT = TABLES_DIR / "conditional_rules"
TABLE_ROOT.mkdir(parents=True, exist_ok=True)

MAX_DEPTH = 3
MIN_SUPPORT = 0.10
MIN_PRECISION = 0.70


@dataclass(frozen=True)
class FeatureInfo:
    """Metadata used to translate model features back to parameter space."""

    parameter: str
    kind: str


def load_slice(family: str, front_type: str, budget: int) -> pd.DataFrame:
    """Load scored final configurations for one slice."""
    final_df = get_final_configs(load_all_runs_with_config(family, front_type, budget)).copy()
    return add_joint_scores(final_df)


def build_feature_matrix(
    df: pd.DataFrame,
    categorical_parameters: list[str],
    numerical_parameters: list[str],
) -> tuple[pd.DataFrame, dict[str, FeatureInfo]]:
    """Build a compact interpretable feature matrix."""
    parts: list[pd.DataFrame] = []
    feature_info: dict[str, FeatureInfo] = {}

    for parameter in categorical_parameters:
        encoded = pd.get_dummies(
            df[parameter].map(safe_category_label),
            prefix=parameter,
            dtype=float,
        )
        if encoded.empty:
            continue
        parts.append(encoded)
        for column in encoded.columns:
            feature_info[column] = FeatureInfo(parameter=parameter, kind="categorical")

    for parameter in numerical_parameters:
        values = pd.to_numeric(df[parameter], errors="coerce")
        if values.notna().sum() == 0:
            continue
        fill_value = float(values.median()) if values.notna().any() else 0.0
        filled = values.fillna(fill_value).astype(float)
        parts.append(pd.DataFrame({parameter: filled}, index=df.index))
        feature_info[parameter] = FeatureInfo(parameter=parameter, kind="numerical")

        if values.isna().any():
            missing_column = f"{parameter}__missing"
            parts.append(pd.DataFrame({missing_column: values.isna().astype(float)}, index=df.index))
            feature_info[missing_column] = FeatureInfo(parameter=parameter, kind="missing")

    if not parts:
        return pd.DataFrame(index=df.index), {}
    return pd.concat(parts, axis=1).astype(float), feature_info


def format_condition(
    feature_name: str, threshold: float, is_left: bool, feature_info: dict[str, FeatureInfo]
) -> str:
    """Translate a tree split into a readable condition."""
    metadata = feature_info[feature_name]
    operator = "<=" if is_left else ">"

    if metadata.kind == "categorical":
        category = feature_name.removeprefix(metadata.parameter + "_")
        if threshold <= 0.5:
            return (
                f"{metadata.parameter} != {category}"
                if is_left
                else f"{metadata.parameter} = {category}"
            )
        return f"{metadata.parameter} {operator} {threshold:.3f}"

    if metadata.kind == "missing":
        if threshold <= 0.5:
            return (
                f"{metadata.parameter} presente"
                if is_left
                else f"{metadata.parameter} ausente"
            )
        return f"{feature_name} {operator} {threshold:.3f}"

    return f"{metadata.parameter} {operator} {threshold:.3f}"


def collect_leaf_rules(
    model: DecisionTreeClassifier,
    feature_names: list[str],
    feature_info: dict[str, FeatureInfo],
) -> dict[int, list[str]]:
    """Collect the path conditions that lead to each leaf."""
    tree = model.tree_
    leaf_rules: dict[int, list[str]] = {}

    def walk(node_id: int, conditions: list[str]) -> None:
        left_id = tree.children_left[node_id]
        right_id = tree.children_right[node_id]
        if left_id == right_id:
            leaf_rules[node_id] = conditions
            return

        feature_name = feature_names[tree.feature[node_id]]
        threshold = float(tree.threshold[node_id])
        walk(
            left_id,
            [*conditions, format_condition(feature_name, threshold, True, feature_info)],
        )
        walk(
            right_id,
            [*conditions, format_condition(feature_name, threshold, False, feature_info)],
        )

    walk(0, [])
    return leaf_rules


def extract_rules(
    df: pd.DataFrame,
    features: pd.DataFrame,
    y: pd.Series,
    feature_info: dict[str, FeatureInfo],
    family: str,
    front_type: str,
    budget: int,
    categorical_parameters: list[str],
    numerical_parameters: list[str],
) -> tuple[pd.DataFrame, pd.DataFrame]:
    """Fit a shallow tree and export accepted rules plus conditional windows."""
    if features.empty or y.nunique() < 2:
        return pd.DataFrame(), pd.DataFrame()

    min_samples_leaf = max(2, int(np.ceil(len(df) * MIN_SUPPORT)))
    model = DecisionTreeClassifier(
        max_depth=MAX_DEPTH,
        min_samples_leaf=min_samples_leaf,
        random_state=42,
    )
    model.fit(features.values, y.values)

    leaf_rules = collect_leaf_rules(model, features.columns.tolist(), feature_info)
    leaf_assignments = model.apply(features.values)
    tree = model.tree_

    rule_rows: list[dict[str, object]] = []
    window_rows: list[dict[str, object]] = []

    for leaf_id, conditions in leaf_rules.items():
        mask = leaf_assignments == leaf_id
        if not mask.any():
            continue

        support = float(mask.mean())
        subset = df.loc[mask]
        precision = float(y.loc[mask].mean())
        elite_count = int(y.loc[mask].sum())
        non_elite_count = int(mask.sum() - elite_count)
        predicted_class = int(np.argmax(tree.value[leaf_id][0]))
        if predicted_class != 1 or support < MIN_SUPPORT or precision < MIN_PRECISION:
            continue

        rule_text = " AND ".join(conditions) if conditions else "ALL"
        rule_rows.append(
            {
                "family": family,
                "front_type": front_type,
                "budget": budget,
                "rule_text": rule_text,
                "rule_signature": rule_text,
                "n_rows": int(mask.sum()),
                "support": support,
                "precision": precision,
                "elite_rows": elite_count,
                "non_elite_rows": non_elite_count,
                "categorical_parameters": "|".join(categorical_parameters),
                "numerical_parameters": "|".join(numerical_parameters),
            }
        )

        elite_subset_df = subset.loc[y.loc[mask] == 1]
        for parameter in numerical_parameters:
            values = pd.to_numeric(elite_subset_df[parameter], errors="coerce").dropna()
            if len(values) < 5:
                continue
            window_rows.append(
                {
                    "family": family,
                    "front_type": front_type,
                    "budget": budget,
                    "rule_text": rule_text,
                    "parameter": parameter,
                    "elite_n": int(len(values)),
                    "recommended_min": float(values.quantile(0.25)),
                    "recommended_median": float(values.median()),
                    "recommended_max": float(values.quantile(0.75)),
                }
            )

    rule_df = pd.DataFrame(rule_rows)
    if not rule_df.empty:
        rule_df = rule_df.sort_values(["precision", "support", "rule_text"], ascending=[False, False, True])
        rule_df["rule_rank"] = np.arange(1, len(rule_df) + 1)
    window_df = pd.DataFrame(window_rows)
    return rule_df, window_df


def main() -> None:
    all_rule_rows: list[pd.DataFrame] = []
    all_window_rows: list[pd.DataFrame] = []

    for family, front_type, budget in iter_slices():
        print(f"Processing {family} | {front_type} | budget {budget} ...")
        slice_df = load_slice(family, front_type, budget)
        available_columns = active_configuration_columns(slice_df)
        categorical_parameters, numerical_parameters = prioritized_branching_parameters(
            front_type,
            budget,
            available_columns,
        )
        categorical_parameters = [
            parameter
            for parameter in categorical_parameters
            if parameter in available_columns and parameter_kind(parameter) == "categorical"
        ]
        numerical_parameters = [
            parameter
            for parameter in numerical_parameters
            if parameter in available_columns and parameter_kind(parameter) == "numerical"
        ]

        features, feature_info = build_feature_matrix(
            slice_df,
            categorical_parameters,
            numerical_parameters,
        )
        elite_df = elite_subset(slice_df, JOINT_VIEW, 0.10)
        elite_row_ids = set(elite_df.index.tolist())
        y = slice_df.index.to_series().map(lambda idx: 1 if idx in elite_row_ids else 0)

        rule_df, window_df = extract_rules(
            slice_df,
            features,
            y,
            feature_info,
            family,
            front_type,
            budget,
            categorical_parameters,
            numerical_parameters,
        )
        if not rule_df.empty:
            all_rule_rows.append(rule_df)
        if not window_df.empty:
            all_window_rows.append(window_df)

    if all_rule_rows:
        rules_df = pd.concat(all_rule_rows, ignore_index=True).sort_values(
            ["budget", "front_type", "family", "rule_rank"]
        )
    else:
        rules_df = pd.DataFrame(
            columns=[
                "family",
                "front_type",
                "budget",
                "rule_text",
                "rule_signature",
                "n_rows",
                "support",
                "precision",
                "elite_rows",
                "non_elite_rows",
                "categorical_parameters",
                "numerical_parameters",
                "rule_rank",
            ]
        )

    if all_window_rows:
        windows_df = pd.concat(all_window_rows, ignore_index=True).sort_values(
            ["budget", "front_type", "family", "rule_text", "parameter"]
        )
    else:
        windows_df = pd.DataFrame(
            columns=[
                "family",
                "front_type",
                "budget",
                "rule_text",
                "parameter",
                "elite_n",
                "recommended_min",
                "recommended_median",
                "recommended_max",
            ]
        )

    front_summary_df = (
        rules_df.groupby(["front_type", "rule_signature"], as_index=False)
        .agg(
            slices=("rule_signature", "size"),
            mean_support=("support", "mean"),
            mean_precision=("precision", "mean"),
            examples=("rule_text", lambda values: " || ".join(list(dict.fromkeys(values))[:2])),
        )
        .sort_values(["front_type", "slices", "mean_precision"], ascending=[True, False, False])
    )

    rules_df.to_csv(TABLE_ROOT / "conditional_rules_all.csv", index=False)
    windows_df.to_csv(TABLE_ROOT / "conditional_numeric_windows_all.csv", index=False)
    front_summary_df.to_csv(TABLE_ROOT / "conditional_rules_front_summary.csv", index=False)

    for budget in sorted(rules_df["budget"].dropna().unique()):
        budget_dir = TABLE_ROOT / f"budget_{int(budget)}"
        budget_dir.mkdir(parents=True, exist_ok=True)
        rules_df.loc[rules_df["budget"] == budget].to_csv(
            budget_dir / f"conditional_rules_{int(budget)}.csv",
            index=False,
        )
        windows_df.loc[windows_df["budget"] == budget].to_csv(
            budget_dir / f"conditional_numeric_windows_{int(budget)}.csv",
            index=False,
        )

    print(f"Saved: {TABLE_ROOT / 'conditional_rules_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'conditional_numeric_windows_all.csv'}")
    print(f"Saved: {TABLE_ROOT / 'conditional_rules_front_summary.csv'}")


if __name__ == "__main__":
    main()
