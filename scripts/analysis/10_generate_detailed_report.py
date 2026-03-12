"""Generate a detailed LaTeX report for the analyses produced so far."""

from __future__ import annotations

from pathlib import Path

import pandas as pd

from config import RESULTS_ROOT
from data_loader import convergence_summary

ANALYSIS_DIR = Path(__file__).resolve().parent
FIGURES_DIR = ANALYSIS_DIR / "figures"
TABLES_DIR = ANALYSIS_DIR / "tables"
REPORT_DIR = ANALYSIS_DIR / "report"
REPORT_TABLES_DIR = TABLES_DIR / "report"
REPORT_DIR.mkdir(parents=True, exist_ok=True)
REPORT_TABLES_DIR.mkdir(parents=True, exist_ok=True)

FAMILIES = ["RE3D", "RWA3D"]
FRONT_TYPES = ["referenceFronts", "extremePointsFronts"]
BUDGETS = [1000, 3000, 5000, 7000]


def front_label(front_type: str) -> str:
    return {
        "referenceFronts": "Real",
        "extremePointsFronts": "Puntos extremos",
    }[front_type]


def indicator_label(indicator: str) -> str:
    return "HV" if indicator == "HVMinus" else indicator


def relative_to_report(path: Path) -> str:
    return f"../{path.relative_to(ANALYSIS_DIR).as_posix()}"


def latex_escape(text: object) -> str:
    value = str(text)
    replacements = {
        "\\": r"\textbackslash{}",
        "&": r"\&",
        "%": r"\%",
        "$": r"\$",
        "#": r"\#",
        "_": r"\_",
        "{": r"\{",
        "}": r"\}",
        "~": r"\textasciitilde{}",
        "^": r"\textasciicircum{}",
    }
    for source, target in replacements.items():
        value = value.replace(source, target)
    return value


def dataframe_to_latex(
    df: pd.DataFrame,
    caption: str,
    label: str,
    longtable: bool = False,
    col_space: str = "\\scriptsize",
) -> str:
    table = df.copy()
    for column in table.columns:
        if table[column].dtype == "bool":
            table[column] = table[column].map({True: "si", False: "no"})
    float_columns = table.select_dtypes(include=["float"]).columns
    for column in float_columns:
        table[column] = table[column].map(lambda value: f"{value:.3f}")
    latex_body = table.to_latex(
        index=False,
        escape=True,
        longtable=longtable,
        caption=caption,
        label=label,
    )
    if longtable:
        return "\n".join(
            [
                "{%s" % col_space,
                "\\setlength{\\LTpre}{0pt}",
                "\\setlength{\\LTpost}{0pt}",
                latex_body,
                "}",
            ]
        )

    latex_body = latex_body.replace("\\begin{table}\n", "\\begin{table}[p]\n\\centering\n", 1)
    return "\n".join(
        [
            "{%s" % col_space,
            latex_body,
            "}",
        ]
    )


def subfigure_grid(
    figure_paths: list[tuple[Path, str]],
    caption: str,
    label: str,
) -> str:
    lines = ["\\begin{figure}[p]", "\\centering"]
    for index, (path, subcaption) in enumerate(figure_paths, start=1):
        lines.extend(
            [
                "\\begin{subfigure}{0.48\\textwidth}",
                "\\centering",
                f"\\includegraphics[width=\\linewidth]{{{relative_to_report(path)}}}",
                f"\\caption{{{latex_escape(subcaption)}}}",
                "\\end{subfigure}",
            ]
        )
        if index % 2 == 1:
            lines.append("\\hfill")
        else:
            lines.append("\\vspace{0.6em}")
    lines.extend(
        [
            f"\\caption{{{latex_escape(caption)}}}",
            f"\\label{{{label}}}",
            "\\end{figure}",
        ]
    )
    return "\n".join(lines)


def single_figure(path: Path, caption: str, label: str, width: str = "0.88\\textwidth") -> str:
    return "\n".join(
        [
            "\\begin{figure}[p]",
            "\\centering",
            f"\\includegraphics[width={width}]{{{relative_to_report(path)}}}",
            f"\\caption{{{latex_escape(caption)}}}",
            f"\\label{{{label}}}",
            "\\end{figure}",
        ]
    )


def compute_meta_convergence_summary() -> pd.DataFrame:
    rows: list[dict[str, object]] = []
    for family in FAMILIES:
        for front_type in FRONT_TYPES:
            for budget in BUDGETS:
                stats = convergence_summary(family, front_type, budget, RESULTS_ROOT).copy()
                metric_rows: dict[str, dict[str, float]] = {}
                for metric, column, higher_better in [
                    ("EP", "ep_median", False),
                    ("HV", "hv_median", True),
                ]:
                    series = -stats[column] if metric == "HV" else stats[column]
                    start = float(series.iloc[0])
                    end = float(series.iloc[-1])
                    total = end - start if higher_better else start - end
                    progress = (
                        (series - start) / total
                        if higher_better
                        else (start - series) / total
                    )
                    t90 = int(stats.loc[progress >= 0.90, "Evaluation"].iloc[0])
                    t95 = int(stats.loc[progress >= 0.95, "Evaluation"].iloc[0])
                    gain_pct = (
                        0.0
                        if start == 0.0
                        else 100.0 * (end - start) / abs(start)
                        if higher_better
                        else 100.0 * (start - end) / abs(start)
                    )
                    metric_rows[metric] = {
                        "T90": t90,
                        "T95": t95,
                        "gain_pct": gain_pct,
                    }

                rows.append(
                    {
                        "Benchmark": family,
                        "Frente": front_label(front_type),
                        "Budget": budget,
                        "T90 EP": metric_rows["EP"]["T90"],
                        "T95 EP": metric_rows["EP"]["T95"],
                        "T90 HV": metric_rows["HV"]["T90"],
                        "T95 HV": metric_rows["HV"]["T95"],
                        "Ganancia EP (%)": metric_rows["EP"]["gain_pct"],
                        "Ganancia HV (%)": metric_rows["HV"]["gain_pct"],
                    }
                )
    result = pd.DataFrame(rows)
    result.to_csv(REPORT_TABLES_DIR / "meta_convergence_summary.csv", index=False)
    return result


def load_budget_t95_summary() -> pd.DataFrame:
    df = pd.read_csv(TABLES_DIR / "budget_convergence_t95.csv").copy()
    df["Frente"] = df["front_type"].map(front_label)
    df["Benchmark"] = df["family"]
    result = df[
        [
            "Benchmark",
            "Frente",
            "budget_T95_EP",
            "budget_T95_HVMinus",
            "final_best_budget_EP",
            "final_best_budget_HVMinus",
        ]
    ].rename(
        columns={
            "budget_T95_EP": "Budget T95 EP",
            "budget_T95_HVMinus": "Budget T95 HV",
            "final_best_budget_EP": "Budget final EP",
            "final_best_budget_HVMinus": "Budget final HV",
        }
    )
    result.to_csv(REPORT_TABLES_DIR / "budget_t95_summary.csv", index=False)
    return result


def load_individual_component_top3() -> pd.DataFrame:
    frames: list[pd.DataFrame] = []
    for budget in BUDGETS:
        path = TABLES_DIR / "component_importance" / f"budget_{budget}" / f"component_importance_top3_{budget}.csv"
        frame = pd.read_csv(path).copy()
        frame["Frente"] = frame["front_type"].map(front_label)
        frame["Indicador"] = frame["indicator"].map(indicator_label)
        frame["Benchmark"] = frame["scope"]
        frame = frame.rename(
            columns={
                "budget": "Budget",
                "rank": "Rank",
                "parameter_family": "Componente",
                "importance": "Importancia",
            }
        )
        frames.append(frame[["Benchmark", "Frente", "Budget", "Indicador", "Rank", "Componente", "Importancia"]])
    result = pd.concat(frames, ignore_index=True)
    result.to_csv(REPORT_TABLES_DIR / "individual_component_top3.csv", index=False)
    return result


def load_consensus_component_summary() -> tuple[pd.DataFrame, pd.DataFrame]:
    compact_rows: list[dict[str, object]] = []
    detail_rows: list[pd.DataFrame] = []
    for budget in BUDGETS:
        path = TABLES_DIR / "component_importance" / f"budget_{budget}" / f"component_importance_consensus_top3_{budget}.csv"
        frame = pd.read_csv(path).copy()
        frame["Frente"] = frame["front_type"].map(front_label)
        frame["Indicador"] = frame["indicator"].map(indicator_label)
        frame = frame.rename(
            columns={
                "budget": "Budget",
                "consensus_rank": "Rank",
                "parameter_family": "Componente",
                "consensus_importance": "Importancia",
                "RE3D_normalized_importance": "Importancia RE3D",
                "RWA3D_normalized_importance": "Importancia RWA3D",
            }
        )
        detail_rows.append(
            frame[
                [
                    "Frente",
                    "Budget",
                    "Indicador",
                    "Rank",
                    "Componente",
                    "Importancia",
                    "Importancia RE3D",
                    "Importancia RWA3D",
                ]
            ]
        )
        for (front, indicator), sub in frame.groupby(["Frente", "Indicador"]):
            ordered = sub.sort_values("Rank")
            top3 = "; ".join(
                f"{int(row['Rank'])}) {row['Componente']}"
                for _, row in ordered.iterrows()
            )
            compact_rows.append(
                {
                    "Frente": front,
                    "Budget": budget,
                    "Indicador": indicator,
                    "Top 3 consensus": top3,
                }
            )
    detail = pd.concat(detail_rows, ignore_index=True)
    compact = pd.DataFrame(compact_rows).sort_values(["Budget", "Frente", "Indicador"])
    detail.to_csv(REPORT_TABLES_DIR / "consensus_component_top3_detail.csv", index=False)
    compact.to_csv(REPORT_TABLES_DIR / "consensus_component_top3_compact.csv", index=False)
    return compact, detail


def build_individual_component_leaders(individual_components_df: pd.DataFrame) -> pd.DataFrame:
    result = (
        individual_components_df.loc[individual_components_df["Rank"] == 1]
        .copy()
        .sort_values(["Budget", "Benchmark", "Frente", "Indicador"])
        [["Benchmark", "Frente", "Budget", "Indicador", "Componente", "Importancia"]]
    )
    result.to_csv(REPORT_TABLES_DIR / "individual_component_leaders.csv", index=False)
    return result


def build_consensus_component_leaders(consensus_components_detail_df: pd.DataFrame) -> pd.DataFrame:
    result = (
        consensus_components_detail_df.loc[consensus_components_detail_df["Rank"] == 1]
        .copy()
        .sort_values(["Budget", "Frente", "Indicador"])
        [
            [
                "Frente",
                "Budget",
                "Indicador",
                "Componente",
                "Importancia",
                "Importancia RE3D",
                "Importancia RWA3D",
            ]
        ]
    )
    result.to_csv(REPORT_TABLES_DIR / "consensus_component_leaders.csv", index=False)
    return result


def load_repeated_configuration_summary() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    repeated_rows: list[dict[str, object]] = []
    shared_rows: list[dict[str, object]] = []
    total_cases = 0
    matches_ep = 0
    matches_hv = 0

    for budget in BUDGETS:
        repeated_path = (
            TABLES_DIR
            / "repeated_configurations"
            / f"budget_{budget}"
            / f"repeated_configs_summary_{budget}.csv"
        )
        repeated_df = pd.read_csv(repeated_path)
        for (scope, front), sub in repeated_df.groupby(["scope", "front_type"]):
            top = sub.iloc[0]
            total_cases += 1
            matches_ep += int(bool(top["is_best_EP"]))
            matches_hv += int(bool(top["is_best_HV"]))
            repeated_rows.append(
                {
                    "Benchmark": scope,
                    "Frente": front_label(front),
                    "Budget": budget,
                    "Max. repeticiones": int(top["rows"]),
                    "Runs distintos": int(top["runs"]),
                    "Cuota de filas (%)": float(top["row_share_pct"]),
                    "Cuota de runs (%)": float(top["run_share_pct"]),
                    "Configs. con repeticiones": int((sub["rows"] > 1).sum()),
                    "Configs. unicas": int(sub["config_signature"].nunique()),
                    "Top es mejor EP": bool(top["is_best_EP"]),
                    "Top es mejor HV": bool(top["is_best_HV"]),
                }
            )

        for front_type in FRONT_TYPES:
            shared_path = (
                TABLES_DIR
                / "repeated_configurations"
                / f"budget_{budget}"
                / f"repeated_configs_consensus_{front_type}_{budget}.csv"
            )
            shared_df = pd.read_csv(shared_path)
            shared_rows.append(
                {
                    "Frente": front_label(front_type),
                    "Budget": budget,
                    "Configuraciones exactas compartidas": len(shared_df),
                }
            )

    repeated_summary = pd.DataFrame(repeated_rows).sort_values(["Budget", "Benchmark", "Frente"])
    shared_summary = pd.DataFrame(shared_rows).sort_values(["Budget", "Frente"])
    best_match_summary = pd.DataFrame(
        [
            {
                "Casos analizados": total_cases,
                "Top repetida coincide con mejor EP": matches_ep,
                "Top repetida coincide con mejor HV": matches_hv,
            }
        ]
    )

    repeated_summary.to_csv(REPORT_TABLES_DIR / "repeated_configuration_summary.csv", index=False)
    shared_summary.to_csv(REPORT_TABLES_DIR / "shared_exact_configuration_summary.csv", index=False)
    best_match_summary.to_csv(REPORT_TABLES_DIR / "repeated_best_match_summary.csv", index=False)
    return repeated_summary, shared_summary, best_match_summary


def build_repeated_configuration_compact(repeated_summary_df: pd.DataFrame) -> pd.DataFrame:
    result = repeated_summary_df[
        [
            "Benchmark",
            "Frente",
            "Budget",
            "Max. repeticiones",
            "Configs. con repeticiones",
            "Top es mejor EP",
            "Top es mejor HV",
        ]
    ].copy()
    result.to_csv(REPORT_TABLES_DIR / "repeated_configuration_compact.csv", index=False)
    return result


def load_configuration_distance_summaries() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    summary_df = pd.read_csv(
        TABLES_DIR / "configuration_distance" / "configuration_distance_summary_all.csv"
    ).copy()
    summary_df["Frente"] = summary_df["front_type"].map(front_label)
    summary_df["within_mean"] = 0.5 * (
        summary_df["within_RE3D_mean"] + summary_df["within_RWA3D_mean"]
    )
    summary_compact = (
        summary_df.pivot_table(
            index=["Frente", "budget"],
            columns="scheme",
            values=["within_mean", "between_mean", "between_minus_mean_within"],
        )
        .reset_index()
    )
    summary_compact.columns = [
        "Frente",
        "Budget",
        "Between mean weighted",
        "Between mean unweighted",
        "Separation weighted",
        "Separation unweighted",
        "Within mean weighted",
        "Within mean unweighted",
    ]
    summary_compact = summary_compact[
        [
            "Frente",
            "Budget",
            "Within mean unweighted",
            "Between mean unweighted",
            "Separation unweighted",
            "Within mean weighted",
            "Between mean weighted",
            "Separation weighted",
        ]
    ].sort_values(["Budget", "Frente"])
    summary_compact.to_csv(REPORT_TABLES_DIR / "configuration_distance_compact.csv", index=False)

    best_df = pd.read_csv(
        TABLES_DIR / "configuration_distance" / "configuration_distance_best_configs_all.csv"
    ).copy()
    best_df["Frente"] = best_df["front_type"].map(front_label)
    best_compact = (
        best_df.loc[best_df["pair"].isin(["EP_vs_EP", "HV_vs_HV"])]
        .pivot_table(index=["Frente", "budget"], columns=["scheme", "pair"], values="distance")
        .reset_index()
    )
    best_compact.columns = [
        "Frente",
        "Budget",
        "Best EP weighted",
        "Best HV weighted",
        "Best EP unweighted",
        "Best HV unweighted",
    ]
    best_compact = best_compact[
        [
            "Frente",
            "Budget",
            "Best EP unweighted",
            "Best HV unweighted",
            "Best EP weighted",
            "Best HV weighted",
        ]
    ].sort_values(["Budget", "Frente"])
    best_compact.to_csv(REPORT_TABLES_DIR / "configuration_distance_best_compact.csv", index=False)

    medoid_df = pd.read_csv(
        TABLES_DIR / "configuration_distance" / "configuration_distance_medoids_all.csv"
    ).copy()
    medoid_df["Frente"] = medoid_df["front_type"].map(front_label)
    medoid_compact = (
        medoid_df.loc[
            (medoid_df["scheme"] == "gower_consensus_weighted")
            & (medoid_df["cluster_rank"] == 1)
        ]
        .pivot_table(
            index=["Frente", "budget"],
            columns="family",
            values=["cluster_share_pct", "mean_distance_to_medoid"],
        )
        .reset_index()
    )
    medoid_compact.columns = [
        "Frente",
        "Budget",
        "Dist medoid RE3D",
        "Dist medoid RWA3D",
        "Archetype RE3D (%)",
        "Archetype RWA3D (%)",
    ]
    medoid_compact = medoid_compact[
        [
            "Frente",
            "Budget",
            "Archetype RE3D (%)",
            "Archetype RWA3D (%)",
            "Dist medoid RE3D",
            "Dist medoid RWA3D",
        ]
    ].sort_values(["Budget", "Frente"])
    medoid_compact.to_csv(REPORT_TABLES_DIR / "configuration_distance_medoids_compact.csv", index=False)

    return summary_compact, best_compact, medoid_compact


def build_parameter_space_recommendations() -> tuple[pd.DataFrame, pd.DataFrame]:
    frames: list[pd.DataFrame] = []
    for budget in BUDGETS:
        path = (
            TABLES_DIR
            / "component_importance"
            / f"budget_{budget}"
            / f"component_importance_consensus_summary_{budget}.csv"
        )
        frame = pd.read_csv(path).copy()
        frame["rank_within"] = frame.groupby(["front_type", "indicator", "budget"])[
            "consensus_importance"
        ].rank(ascending=False, method="first")
        frames.append(frame)

    all_df = pd.concat(frames, ignore_index=True)
    detail = (
        all_df.groupby(["front_type", "parameter_family"], as_index=False)
        .agg(
            mean_importance=("consensus_importance", "mean"),
            top3_hits=("rank_within", lambda values: int((values <= 3).sum())),
            top1_hits=("rank_within", lambda values: int((values == 1).sum())),
        )
        .sort_values(["front_type", "top3_hits", "mean_importance"], ascending=[True, False, False])
        .reset_index(drop=True)
    )

    def decide_action(row: pd.Series) -> str:
        if int(row["top3_hits"]) >= 3 or float(row["mean_importance"]) >= 0.08:
            return "Priorizar"
        if int(row["top3_hits"]) >= 1 or float(row["mean_importance"]) > 0.01:
            return "Mantener abiertos"
        return "Candidatos a fijar/podar"

    detail["Frente"] = detail["front_type"].map(front_label)
    detail["Decision"] = detail.apply(decide_action, axis=1)
    detail = detail.rename(
        columns={
            "parameter_family": "Componente",
            "mean_importance": "Importancia media",
            "top3_hits": "Apariciones top3",
            "top1_hits": "Apariciones top1",
        }
    )
    detail.to_csv(REPORT_TABLES_DIR / "parameter_space_recommendations_detail.csv", index=False)

    rows: list[dict[str, object]] = []
    for front_type in FRONT_TYPES:
        front_detail = detail.loc[detail["front_type"] == front_type].copy()
        for decision, limit in [
            ("Priorizar", 4),
            ("Mantener abiertos", 4),
            ("Candidatos a fijar/podar", 4),
        ]:
            subset = front_detail.loc[front_detail["Decision"] == decision].head(limit)
            components = ", ".join(subset["Componente"].tolist())
            rows.append(
                {
                    "Frente": front_label(front_type),
                    "Decision": decision,
                    "Componentes": components,
                }
            )
    compact = pd.DataFrame(rows)
    compact.to_csv(REPORT_TABLES_DIR / "parameter_space_recommendations_compact.csv", index=False)
    return compact, detail


def build_budget_recommendations() -> pd.DataFrame:
    result = pd.DataFrame(
        [
            {
                "Contexto": "Meta-optimizador",
                "Decision": "Default recomendado",
                "Recomendacion": "2000-2500 meta-evaluaciones",
                "Motivo": "27/32 hitos T95 aparecen a 2000 o antes; el maximo observado es 2500.",
            },
            {
                "Contexto": "Frente real",
                "Decision": "Budget final comun",
                "Recomendacion": "7000",
                "Motivo": "Es el unico budget que alcanza T95 en EP y HV para RE3D y RWA3D.",
            },
            {
                "Contexto": "Frente real",
                "Decision": "Descartar para conclusiones finales",
                "Recomendacion": "1000 y 3000",
                "Motivo": "No alcanzan el nivel final requerido en ninguna lectura conjunta real.",
            },
            {
                "Contexto": "Frente real",
                "Decision": "Mantener solo como intermedio",
                "Recomendacion": "5000",
                "Motivo": "Puede servir para sensibilidad o RE3D/EP, pero no como protocolo comun.",
            },
            {
                "Contexto": "Frente de puntos extremos",
                "Decision": "Budget final conservador",
                "Recomendacion": "7000",
                "Motivo": "Tambien es el unico budget globalmente suficiente cuando se exigen ambos benchmarks y ambas metricas.",
            },
            {
                "Contexto": "Frente de puntos extremos",
                "Decision": "Budget exploratorio util",
                "Recomendacion": "3000",
                "Motivo": "Ya capta parte de la calidad final y reduce coste; es razonable para barridos iniciales.",
            },
            {
                "Contexto": "Frente de puntos extremos",
                "Decision": "Descartar para conclusiones finales",
                "Recomendacion": "1000",
                "Motivo": "Es demasiado restrictivo para extraer conclusiones estables de calidad final.",
            },
            {
                "Contexto": "Frente de puntos extremos",
                "Decision": "No usar como unico default",
                "Recomendacion": "5000",
                "Motivo": "Aporta de forma asimetrica segun benchmark; no mejora lo suficiente como presupuesto estandar unico.",
            },
        ]
    )
    result.to_csv(REPORT_TABLES_DIR / "budget_recommendations.csv", index=False)
    return result


def proposal_label(proposal_name: str) -> str:
    return {
        "global_referenceFronts": "Global / frente real",
        "global_extremePointsFronts": "Global / puntos extremos",
        "RE3D_referenceFronts": "RE3D / frente real",
        "RE3D_extremePointsFronts": "RE3D / puntos extremos",
        "RWA3D_referenceFronts": "RWA3D / frente real",
        "RWA3D_extremePointsFronts": "RWA3D / puntos extremos",
    }.get(proposal_name, proposal_name)


def load_elite_configuration_summary() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    detail = pd.read_csv(TABLES_DIR / "elite_subspace" / "elite_recommendations_all.csv").copy()
    checks = pd.read_csv(TABLES_DIR / "elite_subspace" / "elite_consistency_checks.csv").copy()
    detail["Frente"] = detail["front_type"].map(front_label)
    detail["Benchmark"] = detail["family"]
    detail["Budget"] = detail["budget"]
    detail["Accion"] = detail["action"].map(
        {
            "fix": "Fijar",
            "narrow": "Acotar",
            "keep_open": "Mantener",
        }
    )
    detail["Parametro"] = detail["parameter"]

    compact_rows: list[dict[str, object]] = []
    for (benchmark, front, budget), group in detail.groupby(["Benchmark", "Frente", "Budget"]):
        fixed_parameters = sorted(group.loc[group["action"] == "fix", "Parametro"].tolist())
        narrowed_parameters = sorted(group.loc[group["action"] == "narrow", "Parametro"].tolist())
        compact_rows.append(
            {
                "Benchmark": benchmark,
                "Frente": front,
                "Budget": budget,
                "Fijos": len(fixed_parameters),
                "Acotados": len(narrowed_parameters),
                "Parametros fijados": ", ".join(fixed_parameters[:6]),
                "Parametros acotados": ", ".join(narrowed_parameters[:6]),
            }
        )

    compact = pd.DataFrame(compact_rows).sort_values(["Budget", "Frente", "Benchmark"])
    compact.to_csv(REPORT_TABLES_DIR / "elite_subspace_actions_compact.csv", index=False)
    detail[
        [
            "Benchmark",
            "Frente",
            "Budget",
            "Parametro",
            "Accion",
            "supported_views",
            "recommended_value",
            "recommended_min",
            "recommended_max",
        ]
    ].to_csv(REPORT_TABLES_DIR / "elite_subspace_actions_detail.csv", index=False)
    checks.to_csv(REPORT_TABLES_DIR / "elite_subspace_consistency_checks.csv", index=False)
    return compact, detail, checks


def load_conditional_rule_report() -> pd.DataFrame:
    rules = pd.read_csv(TABLES_DIR / "conditional_rules" / "conditional_rules_all.csv").copy()
    if rules.empty:
        result = pd.DataFrame(
            columns=["Benchmark", "Frente", "Budget", "Regla", "Soporte (%)", "Precision (%)"]
        )
    else:
        result = (
            rules.assign(
                Frente=rules["front_type"].map(front_label),
                Benchmark=rules["family"],
                Budget=rules["budget"],
                Regla=rules["rule_text"],
                **{
                    "Soporte (%)": 100.0 * rules["support"],
                    "Precision (%)": 100.0 * rules["precision"],
                },
            )[
                ["Benchmark", "Frente", "Budget", "Regla", "Soporte (%)", "Precision (%)"]
            ]
            .sort_values(["Precision (%)", "Soporte (%)"], ascending=[False, False])
            .head(8)
        )
    result.to_csv(REPORT_TABLES_DIR / "conditional_rules_compact.csv", index=False)
    return result


def load_trajectory_configuration_summary() -> pd.DataFrame:
    entry = pd.read_csv(
        TABLES_DIR / "configuration_trajectory" / "trajectory_medoid_entry_summary_all.csv"
    ).copy()
    stability = pd.read_csv(
        TABLES_DIR / "configuration_trajectory" / "trajectory_parameter_stability_summary_all.csv"
    ).copy()
    stability = stability.rename(columns={"budget": "Budget"})

    top3_rows: list[dict[str, object]] = []
    for (family, front_type, budget), group in stability.groupby(["family", "front_type", "Budget"]):
        ordered = group.sort_values(["stabilization_pct_median", "stable_by_50_pct", "parameter"])
        top3 = "; ".join(
            f"{row['parameter']} ({row['stabilization_pct_median']:.1f}%)"
            for _, row in ordered.head(3).iterrows()
        )
        top3_rows.append(
            {
                "family": family,
                "front_type": front_type,
                "Budget": budget,
                "Top 3 tempranos": top3,
            }
        )
    top3_df = pd.DataFrame(top3_rows)

    result = (
        entry.assign(
            Frente=entry["front_type"].map(front_label),
            Benchmark=entry["family"],
            Budget=entry["budget"],
        )
        .merge(top3_df, on=["family", "front_type", "Budget"], how="left")
        .rename(
            columns={
                "entry_eval_median": "Entrada mediana al medoid",
                "entry_pct_median": "Entrada mediana (%)",
                "median_final_distance_to_medoid": "Distancia final mediana",
            }
        )[
            [
                "Benchmark",
                "Frente",
                "Budget",
                "Entrada mediana al medoid",
                "Entrada mediana (%)",
                "Distancia final mediana",
                "Top 3 tempranos",
            ]
        ]
        .sort_values(["Budget", "Frente", "Benchmark"])
    )
    result.to_csv(REPORT_TABLES_DIR / "configuration_trajectory_compact.csv", index=False)
    return result


def load_reduced_space_report() -> pd.DataFrame:
    result = pd.read_csv(TABLES_DIR / "space_reduction" / "reduced_space_summary.csv").copy()
    result["Propuesta"] = result["proposal_name"].map(proposal_label)
    result = result.rename(
        columns={
            "fixed_categorical_count": "Categoricos fijados",
            "narrowed_numeric_count": "Numericos acotados",
            "active_dimension_reduction_pct": "Reduccion dimensiones (%)",
            "effective_complexity_reduction_pct": "Reduccion complejidad (%)",
            "mean_numeric_width_reduction_pct": "Reduccion media ancho numerico (%)",
        }
    )[
        [
            "Propuesta",
            "Categoricos fijados",
            "Numericos acotados",
            "Reduccion dimensiones (%)",
            "Reduccion complejidad (%)",
            "Reduccion media ancho numerico (%)",
        ]
    ].sort_values("Propuesta")
    result.to_csv(REPORT_TABLES_DIR / "reduced_space_summary_compact.csv", index=False)
    return result


def load_validation_report() -> tuple[pd.DataFrame, pd.DataFrame, bool]:
    summary = pd.read_csv(
        TABLES_DIR / "validation_reduced_space" / "validation_acceptance_summary.csv"
    ).copy()
    recommendations = pd.read_csv(
        TABLES_DIR / "validation_reduced_space" / "validation_recommendations.csv"
    ).copy()
    has_completed_runs = bool(summary["completed_slices"].sum() > 0)
    summary["Propuesta"] = summary["proposal_name"].map(proposal_label)
    summary["Frente"] = summary["front_type"].map(front_label)
    summary = summary.rename(
        columns={
            "completed_slices": "Slices completados",
            "passed_slices": "Slices aceptados",
            "mean_hv_delta_pct": "Delta HV medio (%)",
            "mean_ep_delta_pct": "Delta EP medio (%)",
        }
    )[
        [
            "Propuesta",
            "Frente",
            "Slices completados",
            "Slices aceptados",
            "Delta HV medio (%)",
            "Delta EP medio (%)",
        ]
    ].sort_values("Propuesta")
    recommendations["Frente"] = recommendations["front_type"].map(front_label)
    recommendations["Benchmark"] = recommendations["family"]
    recommendations = recommendations.rename(
        columns={
            "budget": "Budget",
            "recommended_proposal": "Propuesta recomendada",
            "recommendation_status": "Estado",
        }
    )[
        ["Benchmark", "Frente", "Budget", "Propuesta recomendada", "Estado"]
    ].sort_values(["Budget", "Frente", "Benchmark"])
    summary.to_csv(REPORT_TABLES_DIR / "validation_acceptance_compact.csv", index=False)
    recommendations.to_csv(REPORT_TABLES_DIR / "validation_recommendations_compact.csv", index=False)
    return summary, recommendations, has_completed_runs


def load_front_type_statistical_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "front_type_statistical_comparison.csv").copy()
    result = result.rename(
        columns={
            "reference_hv_median": "HV real mediana",
            "extreme_hv_median": "HV extremos mediana",
            "median_delta_pct": "Delta HV mediana (%)",
            "p_value": "p",
            "Effect": "Efecto",
            "Significant": "Significativo",
        }
    )[
        [
            "Benchmark",
            "Budget",
            "HV real mediana",
            "HV extremos mediana",
            "Delta HV mediana (%)",
            "p",
            "A",
            "Efecto",
            "Significativo",
        ]
    ].sort_values(["Budget", "Benchmark"])
    result.to_csv(REPORT_TABLES_DIR / "front_type_statistical_comparison_compact.csv", index=False)
    return result


def load_categorical_frequency_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "categorical_parameter_modes_all.csv").copy()
    result = result.rename(
        columns={
            "FrontType": "Frente",
            "Parameter": "Parametro",
            "Mode": "Moda",
            "FrequencyPct": "Frecuencia modal (%)",
        }
    )[["Benchmark", "Frente", "Budget", "Parametro", "Moda", "Frecuencia modal (%)"]]
    result.to_csv(REPORT_TABLES_DIR / "categorical_parameter_modes_compact.csv", index=False)
    return result


def load_numerical_distribution_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "numerical_parameter_distribution_summary_all.csv").copy()
    result = result.rename(
        columns={
            "FrontType": "Frente",
            "Parameter": "Parametro",
        }
    )[["Benchmark", "Frente", "Budget", "Parametro", "n", "Median", "Q25", "Q75"]]
    result.to_csv(REPORT_TABLES_DIR / "numerical_parameter_distribution_summary_compact.csv", index=False)
    return result


def load_parameter_association_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "parameter_association_tests_all.csv").copy()
    result = result.rename(
        columns={
            "FrontType": "Frente",
            "Test": "Prueba",
            "Parameter": "Parametro",
            "Statistic": "Estadistico",
            "p_value": "p",
            "Significant": "Significativo",
        }
    )[["Benchmark", "Frente", "Prueba", "Parametro", "Estadistico", "p", "Significativo"]]
    result.to_csv(REPORT_TABLES_DIR / "parameter_association_tests_compact.csv", index=False)
    return result


def load_rf_predictive_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "rf_predictive_r2_all.csv").copy()
    result = result.rename(
        columns={
            "FrontType": "Frente",
            "Samples": "Muestras",
            "TestR2": "R2 test",
            "CVR2Mean": "R2 CV media",
            "CVR2Std": "R2 CV desv",
        }
    )[["Benchmark", "Frente", "Muestras", "R2 test", "R2 CV media", "R2 CV desv"]]
    result.to_csv(REPORT_TABLES_DIR / "rf_predictive_r2_compact.csv", index=False)
    return result


def load_clustering_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "configuration_clustering_summary.csv").copy()
    result = result.rename(
        columns={
            "FrontType": "Frente",
            "SelectedK": "k seleccionado",
            "BestSilhouette": "Silhouette",
            "Clusters": "Clusters",
        }
    )[["Benchmark", "Frente", "k seleccionado", "Silhouette", "Clusters"]]
    result.to_csv(REPORT_TABLES_DIR / "configuration_clustering_summary_compact.csv", index=False)
    return result


def load_representative_configuration_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "representative_configurations_summary.csv").copy()
    result = result.rename(
        columns={
            "MatchingRows": "Filas coincidentes",
            "CoveragePct": "Cobertura (%)",
            "HVMedian": "HV mediana",
            "EPMedian": "EP mediana",
        }
    )[
        [
            "Benchmark",
            "Filas coincidentes",
            "Cobertura (%)",
            "HV mediana",
            "EP mediana",
            "crossover",
            "mutation",
            "selection",
            "createInitialSolutions",
        ]
    ].sort_values("Benchmark")
    result.to_csv(REPORT_TABLES_DIR / "representative_configurations_summary_compact.csv", index=False)
    return result


def load_hv90_report() -> tuple[pd.DataFrame, pd.DataFrame]:
    percentiles = pd.read_csv(REPORT_TABLES_DIR / "hv90_percentiles.csv").copy().sort_values("Benchmark")
    parameter_comparison = pd.read_csv(REPORT_TABLES_DIR / "hv90_parameter_comparison.csv").copy()
    parameter_comparison = parameter_comparison.rename(
        columns={
            "family": "Benchmark",
            "parameter": "Parametro",
            "parameter_kind": "Tipo",
            "early_value": "Valor temprano",
            "late_value": "Valor final",
            "abs_pct_diff": "Dif. abs. (%)",
            "consistent": "Consistente",
        }
    )[["Benchmark", "Parametro", "Tipo", "Valor temprano", "Valor final", "Dif. abs. (%)", "Consistente"]]
    percentiles.to_csv(REPORT_TABLES_DIR / "hv90_percentiles_compact.csv", index=False)
    parameter_comparison.to_csv(REPORT_TABLES_DIR / "hv90_parameter_comparison_compact.csv", index=False)
    return percentiles, parameter_comparison


def load_cost_quality_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "cost_quality_thresholds_pooled.csv").copy()
    result = result.rename(
        columns={
            "family": "Benchmark",
            "target_pct_reference_hv": "Objetivo HV ref. (%)",
            "front_type": "Frente",
            "budget": "Budget",
            "evaluation": "Meta-evaluacion",
            "total_cost": "Coste total",
            "pct_reference_hv": "HV alcanzado (%)",
        }
    )[
        [
            "Benchmark",
            "Objetivo HV ref. (%)",
            "Frente",
            "Budget",
            "Meta-evaluacion",
            "Coste total",
            "HV alcanzado (%)",
        ]
    ].sort_values(["Benchmark", "Objetivo HV ref. (%)"])
    result.to_csv(REPORT_TABLES_DIR / "cost_quality_thresholds_compact.csv", index=False)
    return result


def load_cost_scenario_report() -> pd.DataFrame:
    result = pd.read_csv(REPORT_TABLES_DIR / "cost_scenario_summary.csv").copy()
    result = result.rename(
        columns={
            "budget": "Budget",
            "meta_eval_cap": "Meta-eval max",
            "matching_rows": "Filas coincidentes",
            "CoveragePct": "Cobertura (%)",
            "HVMedian": "HV mediana",
            "EPMedian": "EP mediana",
            "CategoricalMatchCountVsC": "Coincidencias categ. vs C",
        }
    )[
        [
            "Benchmark",
            "Scenario",
            "Label",
            "Budget",
            "Meta-eval max",
            "Filas coincidentes",
            "Cobertura (%)",
            "Coincidencias categ. vs C",
            "HV mediana",
            "EP mediana",
            "crossover",
            "mutation",
            "selection",
            "createInitialSolutions",
        ]
    ].sort_values(["Benchmark", "Scenario"])
    result.to_csv(REPORT_TABLES_DIR / "cost_scenario_summary_compact.csv", index=False)
    return result


def report_statistics(
    meta_df: pd.DataFrame,
    shared_df: pd.DataFrame,
    best_match_df: pd.DataFrame,
    distance_summary_df: pd.DataFrame,
) -> dict[str, object]:
    meta_t95_values = pd.concat([meta_df["T95 EP"], meta_df["T95 HV"]], ignore_index=True)
    return {
        "num_slices": int(len(meta_df)),
        "count_t95_le_2000": int((meta_t95_values <= 2000).sum()),
        "total_t95": int(len(meta_t95_values)),
        "max_t95": int(meta_t95_values.max()),
        "median_t95": float(meta_t95_values.median()),
        "shared_exact_total": int(shared_df["Configuraciones exactas compartidas"].sum()),
        "best_match_ep": int(best_match_df.iloc[0]["Top repetida coincide con mejor EP"]),
        "best_match_hv": int(best_match_df.iloc[0]["Top repetida coincide con mejor HV"]),
        "best_match_cases": int(best_match_df.iloc[0]["Casos analizados"]),
        "distance_separation_weighted_mean": float(distance_summary_df["Separation weighted"].mean()),
        "distance_separation_unweighted_mean": float(distance_summary_df["Separation unweighted"].mean()),
    }


def write_report_tex(
    meta_df: pd.DataFrame,
    budget_t95_df: pd.DataFrame,
    individual_components_df: pd.DataFrame,
    consensus_components_compact_df: pd.DataFrame,
    consensus_components_detail_df: pd.DataFrame,
    individual_leaders_df: pd.DataFrame,
    consensus_leaders_df: pd.DataFrame,
    repeated_summary_df: pd.DataFrame,
    repeated_compact_df: pd.DataFrame,
    shared_summary_df: pd.DataFrame,
    best_match_df: pd.DataFrame,
    distance_summary_df: pd.DataFrame,
    distance_best_df: pd.DataFrame,
    distance_medoids_df: pd.DataFrame,
    budget_recommendations_df: pd.DataFrame,
    parameter_recommendations_df: pd.DataFrame,
    elite_actions_df: pd.DataFrame,
    conditional_rules_df: pd.DataFrame,
    trajectory_df: pd.DataFrame,
    reduced_space_df: pd.DataFrame,
    validation_summary_df: pd.DataFrame,
    validation_recommendations_df: pd.DataFrame,
    validation_has_completed_runs: bool,
    front_type_stats_df: pd.DataFrame,
    categorical_modes_df: pd.DataFrame,
    numerical_distribution_df: pd.DataFrame,
    association_df: pd.DataFrame,
    rf_predictive_df: pd.DataFrame,
    clustering_df: pd.DataFrame,
    representative_df: pd.DataFrame,
    hv90_percentiles_df: pd.DataFrame,
    hv90_parameter_comparison_df: pd.DataFrame,
    cost_quality_df: pd.DataFrame,
    cost_scenario_df: pd.DataFrame,
) -> Path:
    stats = report_statistics(meta_df, shared_summary_df, best_match_df, distance_summary_df)

    meta_table = dataframe_to_latex(
        meta_df,
        "Tiempos de convergencia sobre meta-evaluaciones (T90/T95) y ganancia total relativa.",
        "tab:meta_convergence",
        longtable=True,
    )
    budget_t95_table = dataframe_to_latex(
        budget_t95_df,
        "Budget del algoritmo base necesario para alcanzar el 95\\% del valor final por slice.",
        "tab:budget_t95",
    )
    consensus_compact_table = dataframe_to_latex(
        consensus_components_compact_df,
        "Top-3 de componentes consensus por frente, budget e indicador.",
        "tab:consensus_top3",
        longtable=True,
    )
    repeated_table = dataframe_to_latex(
        repeated_compact_df,
        "Resumen compacto de configuraciones finales exactas repetidas por benchmark y slice.",
        "tab:repeated_summary",
    )
    shared_table = dataframe_to_latex(
        shared_summary_df,
        "Numero de configuraciones exactas compartidas entre RE3D y RWA3D.",
        "tab:shared_exact",
    )
    best_match_table = dataframe_to_latex(
        best_match_df,
        "Coincidencia entre la configuracion exacta mas repetida y la mejor configuracion por indicador.",
        "tab:repeated_best_match",
    )
    appendix_individual_leaders_table = dataframe_to_latex(
        individual_leaders_df,
        "Componente dominante (rank 1) por benchmark, frente, budget e indicador.",
        "tab:individual_leaders",
        longtable=True,
        col_space="\\scriptsize",
    )
    appendix_consensus_leaders_table = dataframe_to_latex(
        consensus_leaders_df,
        "Componente dominante consensus (rank 1) y su peso relativo por benchmark.",
        "tab:consensus_leaders",
        longtable=True,
        col_space="\\scriptsize",
    )
    distance_summary_table = dataframe_to_latex(
        distance_summary_df,
        "Resumen compacto de distancias Gower dentro de benchmark y entre benchmarks.",
        "tab:configuration_distance_summary",
    )
    distance_best_table = dataframe_to_latex(
        distance_best_df,
        "Distancia entre las mejores configuraciones RE3D y RWA3D por EP y HV.",
        "tab:configuration_distance_best",
    )
    distance_medoids_table = dataframe_to_latex(
        distance_medoids_df,
        "Arquetipo dominante (medoid rank 1) por frente y budget bajo Gower ponderada.",
        "tab:configuration_distance_medoids",
    )
    budget_recommendations_table = dataframe_to_latex(
        budget_recommendations_df,
        "Recomendaciones operativas de presupuesto para experimentos futuros.",
        "tab:budget_recommendations",
    )
    parameter_recommendations_table = dataframe_to_latex(
        parameter_recommendations_df,
        "Recomendaciones compactas para reducir o priorizar el espacio de parametros.",
        "tab:parameter_recommendations",
    )
    elite_actions_table = dataframe_to_latex(
        elite_actions_df.loc[elite_actions_df["Budget"] == 7000].copy(),
        "Parametros candidatos a fijar o acotar en el subespacio elite conjunto (budget 7000).",
        "tab:elite_actions",
    )
    conditional_rules_table = dataframe_to_latex(
        conditional_rules_df,
        "Reglas interpretables que superan los umbrales de soporte y precision fijados.",
        "tab:conditional_rules",
        longtable=True,
    )
    trajectory_table = dataframe_to_latex(
        trajectory_df,
        "Entrada mediana al medoid elite final y parametros que se estabilizan antes.",
        "tab:configuration_trajectory",
        longtable=True,
    )
    reduced_space_table = dataframe_to_latex(
        reduced_space_df,
        "Resumen de propuestas de espacio reducido a partir del budget 7000.",
        "tab:reduced_space_summary",
    )
    validation_table = (
        dataframe_to_latex(
            validation_summary_df,
            "Estado agregado de validacion de espacios reducidos frente al baseline actual.",
            "tab:validation_summary",
        )
        if validation_has_completed_runs
        else ""
    )
    front_type_stats_table = dataframe_to_latex(
        front_type_stats_df,
        "Comparacion estadistica de HV final entre frente real y frente de puntos extremos por benchmark y budget.",
        "tab:front_type_stats",
    )
    categorical_modes_7000_table = dataframe_to_latex(
        categorical_modes_df.loc[categorical_modes_df["Budget"] == 7000].copy(),
        "Modas categoricas y su frecuencia a budget 7000.",
        "tab:categorical_modes_7000",
        longtable=True,
    )
    numerical_focus_df = numerical_distribution_df.loc[
        (numerical_distribution_df["Budget"] == 7000)
        & numerical_distribution_df["Parametro"].isin(
            [
                "populationSizeWithArchive",
                "crossoverProbability",
                "mutationProbabilityFactor",
                "powerLawMutationDelta",
            ]
        )
    ].copy()
    numerical_focus_table = dataframe_to_latex(
        numerical_focus_df,
        "Resumen numerico (mediana e IQR) para parametros clave a budget 7000.",
        "tab:numerical_focus_7000",
        longtable=True,
    )
    significant_association_df = association_df.loc[association_df["Significativo"]].copy()
    if not significant_association_df.empty:
        significant_association_df["Abs estadistico"] = significant_association_df["Estadistico"].abs()
        significant_association_df = significant_association_df.sort_values(
            ["Prueba", "Abs estadistico"], ascending=[True, False]
        ).head(14)
    association_table = dataframe_to_latex(
        significant_association_df.drop(columns="Abs estadistico", errors="ignore"),
        "Asociaciones significativas entre parametros y HV final.",
        "tab:parameter_association",
        longtable=True,
    )
    rf_table = dataframe_to_latex(
        rf_predictive_df,
        "Capacidad predictiva de random forest sobre HV final por benchmark y tipo de frente.",
        "tab:rf_predictive",
    )
    clustering_table = dataframe_to_latex(
        clustering_df,
        "Numero de clusters sugerido por silhouette en cada benchmark y tipo de frente.",
        "tab:configuration_clustering",
    )
    representative_table = dataframe_to_latex(
        representative_df,
        "Configuraciones representativas agregadas por benchmark, construidas a partir de modas categoricas y medianas numericas condicionadas.",
        "tab:representative_configs",
    )
    hv90_percentiles_table = dataframe_to_latex(
        hv90_percentiles_df,
        "Percentiles del momento en que se alcanza el 90\\% del HV final.",
        "tab:hv90_percentiles",
    )
    hv90_inconsistent_table = dataframe_to_latex(
        hv90_parameter_comparison_df.loc[~hv90_parameter_comparison_df["Consistente"]].copy(),
        "Parametros cuya configuracion temprana al 90\\% de HV difiere materialmente de la configuracion final.",
        "tab:hv90_inconsistent",
        longtable=True,
    )
    cost_quality_table = dataframe_to_latex(
        cost_quality_df,
        "Menor coste observado para alcanzar distintos porcentajes del HV de referencia pooled.",
        "tab:cost_quality",
    )
    cost_scenario_table = dataframe_to_latex(
        cost_scenario_df,
        "Comparacion de configuraciones representativas bajo tres escenarios de coste.",
        "tab:cost_scenarios",
        longtable=True,
    )

    pooled_hv_figure = subfigure_grid(
        [
            (FIGURES_DIR / f"base_hv_normalized_convergence_pooled_budget_{budget}.png", f"Budget {budget}")
            for budget in BUDGETS
        ],
        "Curvas pooled descriptivas de HV normalizado del algoritmo base. Estas figuras se usan solo para describir patrones macro de convergencia; no se utilizan para atribucion causal de componentes.",
        "fig:pooled_hv",
    )
    re3d_hv_figure = subfigure_grid(
        [
            (FIGURES_DIR / f"base_hv_normalized_convergence_RE3D_budget_{budget}.png", f"Budget {budget}")
            for budget in BUDGETS
        ],
        "Convergencia normalizada del HV del algoritmo base para RE3D, separando frente real y de puntos extremos en cada figura.",
        "fig:re3d_hv",
    )
    rwa_hv_figure = subfigure_grid(
        [
            (FIGURES_DIR / f"base_hv_normalized_convergence_RWA3D_budget_{budget}.png", f"Budget {budget}")
            for budget in BUDGETS
        ],
        "Convergencia normalizada del HV del algoritmo base para RWA3D, separando frente real y de puntos extremos en cada figura.",
        "fig:rwa_hv",
    )
    reference_consensus_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "component_importance" / f"budget_{budget}" / f"component_importance_consensus_reference_{budget}.png",
                f"Budget {budget}",
            )
            for budget in BUDGETS
        ],
        "Importancia consensus de componentes en frente real para los cuatro budgets. Cada subfigura contiene dos paneles: EP y HV.",
        "fig:consensus_reference",
    )
    estimated_consensus_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "component_importance" / f"budget_{budget}" / f"component_importance_consensus_extreme_points_{budget}.png",
                f"Budget {budget}",
            )
            for budget in BUDGETS
        ],
        "Importancia consensus de componentes en el frente de puntos extremos para los cuatro budgets. Cada subfigura contiene dos paneles: EP y HV.",
        "fig:consensus_estimated",
    )
    individual_7000_figure = subfigure_grid(
        [
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RE3D_referenceFronts_7000.png", "RE3D, frente real"),
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RE3D_extremePointsFronts_7000.png", "RE3D, frente de puntos extremos"),
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RWA3D_referenceFronts_7000.png", "RWA3D, frente real"),
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RWA3D_extremePointsFronts_7000.png", "RWA3D, frente de puntos extremos"),
        ],
        "Importancia de componentes a budget 7000 en los analisis desagregados. Esta figura es la referencia principal para comparar la asimetria entre benchmarks cuando se usan puntos extremos.",
        "fig:individual_7000_components",
    )
    reference_distance_figure = subfigure_grid(
        [
            (
                FIGURES_DIR
                / "configuration_distance"
                / f"budget_{budget}"
                / f"configuration_distance_distribution_referenceFronts_{budget}.png",
                f"Budget {budget}",
            )
            for budget in BUDGETS
        ],
        "Distribuciones de distancia entre configuraciones finales para frente real. Cada figura compara within RE3D, within RWA3D y between, con y sin ponderacion consensus.",
        "fig:distance_reference",
    )
    estimated_distance_figure = subfigure_grid(
        [
            (
                FIGURES_DIR
                / "configuration_distance"
                / f"budget_{budget}"
                / f"configuration_distance_distribution_extremePointsFronts_{budget}.png",
                f"Budget {budget}",
            )
            for budget in BUDGETS
        ],
        "Distribuciones de distancia entre configuraciones finales para el frente de puntos extremos. La ponderacion consensus amplifica la separacion cuando los benchmarks difieren en componentes relevantes.",
        "fig:distance_estimated",
    )
    elite_7000_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "elite_subspace" / "budget_7000" / "elite_profile_RE3D_referenceFronts_7000.png",
                "RE3D, frente real",
            ),
            (
                FIGURES_DIR / "elite_subspace" / "budget_7000" / "elite_profile_RE3D_extremePointsFronts_7000.png",
                "RE3D, puntos extremos",
            ),
            (
                FIGURES_DIR / "elite_subspace" / "budget_7000" / "elite_profile_RWA3D_referenceFronts_7000.png",
                "RWA3D, frente real",
            ),
            (
                FIGURES_DIR / "elite_subspace" / "budget_7000" / "elite_profile_RWA3D_extremePointsFronts_7000.png",
                "RWA3D, puntos extremos",
            ),
        ],
        "Comparacion entre la distribucion total y el elite conjunto del 10\\% a budget 7000. Estas figuras ayudan a ver que parametros se concentran de verdad en la region casi optima.",
        "fig:elite_7000",
    )
    trajectory_7000_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "configuration_trajectory" / "budget_7000" / "trajectory_RE3D_referenceFronts_7000.png",
                "RE3D, frente real",
            ),
            (
                FIGURES_DIR / "configuration_trajectory" / "budget_7000" / "trajectory_RE3D_extremePointsFronts_7000.png",
                "RE3D, puntos extremos",
            ),
            (
                FIGURES_DIR / "configuration_trajectory" / "budget_7000" / "trajectory_RWA3D_referenceFronts_7000.png",
                "RWA3D, frente real",
            ),
            (
                FIGURES_DIR / "configuration_trajectory" / "budget_7000" / "trajectory_RWA3D_extremePointsFronts_7000.png",
                "RWA3D, puntos extremos",
            ),
        ],
        "Trayectorias estructurales del incumbente a budget 7000. La parte superior muestra la distancia al medoid elite final y la inferior la fraccion de runs que ya estabilizaron cada parametro.",
        "fig:trajectory_7000",
    )
    front_type_stats_figure = single_figure(
        FIGURES_DIR / "statistical_comparison" / "final_hv_front_type_comparison.png",
        "Comparacion descriptiva del HV final entre frente real y frente de puntos extremos para todos los budgets.",
        "fig:front_type_stats",
        width="0.96\\textwidth",
    )
    categorical_frequency_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "categorical_frequencies" / "categorical_frequencies_RE3D.png",
                "RE3D",
            ),
            (
                FIGURES_DIR / "categorical_frequencies" / "categorical_frequencies_RWA3D.png",
                "RWA3D",
            ),
        ],
        "Frecuencias relativas de parametros categoricos por benchmark, frente y budget.",
        "fig:categorical_frequencies",
    )
    numerical_distribution_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "numerical_distributions" / "numerical_distributions_RE3D.png",
                "RE3D",
            ),
            (
                FIGURES_DIR / "numerical_distributions" / "numerical_distributions_RWA3D.png",
                "RWA3D",
            ),
        ],
        "Distribuciones numericas resumidas por benchmark, frente y budget.",
        "fig:numerical_distributions",
    )
    association_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "parameter_association" / "parameter_association_RE3D.png",
                "RE3D",
            ),
            (
                FIGURES_DIR / "parameter_association" / "parameter_association_RWA3D.png",
                "RWA3D",
            ),
        ],
        "Correlaciones y contrastes de asociacion entre parametros y HV final.",
        "fig:parameter_association",
    )
    rf_predictive_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "rf_predictive_importance" / "rf_predictive_importance_RE3D.png",
                "RE3D",
            ),
            (
                FIGURES_DIR / "rf_predictive_importance" / "rf_predictive_importance_RWA3D.png",
                "RWA3D",
            ),
        ],
        "Importancia predictiva segun random forest para cada benchmark.",
        "fig:rf_predictive",
    )
    clustering_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "configuration_clustering" / "configuration_clustering_RE3D_referenceFronts.png",
                "RE3D, frente real",
            ),
            (
                FIGURES_DIR / "configuration_clustering" / "configuration_clustering_RE3D_extremePointsFronts.png",
                "RE3D, puntos extremos",
            ),
            (
                FIGURES_DIR / "configuration_clustering" / "configuration_clustering_RWA3D_referenceFronts.png",
                "RWA3D, frente real",
            ),
            (
                FIGURES_DIR / "configuration_clustering" / "configuration_clustering_RWA3D_extremePointsFronts.png",
                "RWA3D, puntos extremos",
            ),
        ],
        "Estructura de clusters en el espacio de configuraciones finales para cada benchmark y tipo de frente.",
        "fig:configuration_clustering",
    )
    hv90_figure = single_figure(
        FIGURES_DIR / "hv90_convergence" / "hv90_cdf.png",
        "Distribucion acumulada del momento en que se alcanza el 90\\% del HV final.",
        "fig:hv90_convergence",
    )
    cost_quality_figure = subfigure_grid(
        [
            (
                FIGURES_DIR / "cost_quality" / "cost_quality_RE3D.png",
                "RE3D",
            ),
            (
                FIGURES_DIR / "cost_quality" / "cost_quality_RWA3D.png",
                "RWA3D",
            ),
        ],
        "Relaciones coste-calidad frente al HV de referencia pooled para ambos benchmarks.",
        "fig:cost_quality",
    )

    family_specific_reduction = reduced_space_df.loc[
        reduced_space_df["Propuesta"].str.contains("RE3D|RWA3D")
    ]["Reduccion complejidad (%)"]
    global_reduction = reduced_space_df.loc[
        reduced_space_df["Propuesta"].str.contains("Global")
    ]["Reduccion complejidad (%)"]
    conditional_rule_count = len(conditional_rules_df)
    top_rule_text = conditional_rules_df.iloc[0]["Regla"] if conditional_rule_count else ""
    significant_front_diffs = front_type_stats_df.loc[front_type_stats_df["Significativo"]].copy()
    front_gap_order = front_type_stats_df.copy()
    front_gap_order["A distancia a 0.5"] = (front_gap_order["A"] - 0.5).abs()
    strongest_front_gap = front_gap_order.sort_values(
        ["Significativo", "A distancia a 0.5", "p"], ascending=[False, False, True]
    ).iloc[0]
    strongest_association = significant_association_df.sort_values(
        "Abs estadistico", ascending=False
    ).iloc[0]
    significant_kruskal_count = int(
        association_df.loc[
            (association_df["Prueba"] != "Spearman") & association_df["Significativo"]
        ].shape[0]
    )
    best_rf_row = rf_predictive_df.sort_values("R2 CV media", ascending=False).iloc[0]
    worst_rf_row = rf_predictive_df.sort_values("R2 CV media", ascending=True).iloc[0]
    representative_re3d = representative_df.loc[representative_df["Benchmark"] == "RE3D"].iloc[0]
    representative_rwa3d = representative_df.loc[representative_df["Benchmark"] == "RWA3D"].iloc[0]
    hv90_overall_median = float(hv90_percentiles_df["Median"].median())
    hv90_overall_p90 = float(hv90_percentiles_df["P90"].max())
    hv90_inconsistent_count = int((~hv90_parameter_comparison_df["Consistente"]).sum())
    re3d_cost_99 = cost_quality_df.loc[
        (cost_quality_df["Benchmark"] == "RE3D")
        & (cost_quality_df["Objetivo HV ref. (%)"].round(1) == 99.0)
    ].iloc[0]
    rwa3d_cost_99 = cost_quality_df.loc[
        (cost_quality_df["Benchmark"] == "RWA3D")
        & (cost_quality_df["Objetivo HV ref. (%)"].round(1) == 99.0)
    ].iloc[0]
    re3d_cost_999 = cost_quality_df.loc[
        (cost_quality_df["Benchmark"] == "RE3D")
        & (cost_quality_df["Objetivo HV ref. (%)"].round(1) == 99.9)
    ].iloc[0]
    rwa3d_cost_999 = cost_quality_df.loc[
        (cost_quality_df["Benchmark"] == "RWA3D")
        & (cost_quality_df["Objetivo HV ref. (%)"].round(1) == 99.9)
    ].iloc[0]
    best_cost_scenario = cost_scenario_df.sort_values("Cobertura (%)", ascending=False).iloc[0]
    elite_nested_ok = True
    if (REPORT_TABLES_DIR / "elite_subspace_consistency_checks.csv").exists():
        elite_checks = pd.read_csv(REPORT_TABLES_DIR / "elite_subspace_consistency_checks.csv")
        elite_nested_ok = bool(
            elite_checks["top5_within_top10"].all()
            and elite_checks["top10_within_top20"].all()
        )

    real_7000_ep = consensus_leaders_df.loc[
        (consensus_leaders_df["Frente"] == "Real")
        & (consensus_leaders_df["Budget"] == 7000)
        & (consensus_leaders_df["Indicador"] == "EP")
    ].iloc[0]
    real_7000_hv = consensus_leaders_df.loc[
        (consensus_leaders_df["Frente"] == "Real")
        & (consensus_leaders_df["Budget"] == 7000)
        & (consensus_leaders_df["Indicador"] == "HV")
    ].iloc[0]
    estimated_7000_ep = consensus_leaders_df.loc[
        (consensus_leaders_df["Frente"] == "Puntos extremos")
        & (consensus_leaders_df["Budget"] == 7000)
        & (consensus_leaders_df["Indicador"] == "EP")
    ].iloc[0]
    estimated_7000_hv = consensus_leaders_df.loc[
        (consensus_leaders_df["Frente"] == "Puntos extremos")
        & (consensus_leaders_df["Budget"] == 7000)
        & (consensus_leaders_df["Indicador"] == "HV")
    ].iloc[0]

    tex_lines = [
        "\\documentclass[11pt,a4paper]{article}",
        "\\usepackage[utf8]{inputenc}",
        "\\usepackage[T1]{fontenc}",
        "\\usepackage[margin=2.2cm]{geometry}",
        "\\usepackage{graphicx}",
        "\\usepackage{subcaption}",
        "\\usepackage{booktabs}",
        "\\usepackage{longtable}",
        "\\usepackage{array}",
        "\\usepackage{pdflscape}",
        "\\usepackage{float}",
        "\\usepackage[hidelinks]{hyperref}",
        "\\hypersetup{pdftitle={Informe analitico detallado de Evolver},pdfauthor={Codex},pdfsubject={Convergencia, componentes y configuraciones}}",
        "\\setlength{\\parskip}{0.5em}",
        "\\setlength{\\parindent}{0pt}",
        "\\begin{document}",
        "\\hypersetup{pageanchor=false}",
        "\\begin{titlepage}",
        "\\centering",
        "\\vspace*{2.3cm}",
        "{\\Large Evolver\\\\[0.4cm]}",
        "{\\Huge\\bfseries Informe analitico detallado\\\\[0.35cm]}",
        "{\\Large Convergencia, configuraciones, tipos de frente y coste-calidad\\\\[1.6cm]}",
        "{\\large RE3D y RWA3D con frentes reales y de puntos extremos\\\\[2.2cm]}",
        "\\begin{tabular}{ll}",
        "Benchmarks analizados & RE3D, RWA3D\\\\",
        "Tipos de frente & real, puntos extremos\\\\",
        "Budgets del algoritmo base & 1000, 3000, 5000, 7000\\\\",
        "Meta-optimizador & 3000 meta-evaluaciones fijas\\\\",
        "Fuentes de datos & \\texttt{INDICATORS.csv}, \\texttt{CONFIGURATIONS.csv}\\\\",
        "Nivel del informe & vistas agregadas, desagregadas, consensus y coste\\\\",
        "\\end{tabular}",
        "\\vfill",
        "{\\large Generado automaticamente a partir de \\texttt{scripts/analysis}\\\\}",
        "{\\large 12 de marzo de 2026\\\\}",
        "\\end{titlepage}",
        "\\clearpage",
        "\\hypersetup{pageanchor=true}",
        "\\pagenumbering{roman}",
        "\\section*{Resumen ejecutivo}",
        "\\addcontentsline{toc}{section}{Resumen ejecutivo}",
        "\\begin{itemize}",
        "\\item El estudio cubre %d slices analiticos (2 benchmarks $\\times$ 2 frentes $\\times$ 4 budgets), todos con 3000 meta-evaluaciones; sobre EP y HV se obtienen %d hitos \\(T95\\) comparables."
        % (stats["num_slices"], stats["total_t95"]),
        "\\item El meta-optimizador converge pronto: %d de %d hitos \\(T95\\) aparecen a 2000 meta-evaluaciones o antes, con mediana %.0f y maximo %d."
        % (stats["count_t95_le_2000"], stats["total_t95"], stats["median_t95"], stats["max_t95"]),
        "\\item En frente real, los budgets altos del algoritmo base siguen aportando mejora util: RE3D necesita budget 5000 para EP y 7000 para HV, mientras que RWA3D necesita 7000 para ambos indicadores. En el frente de puntos extremos la respuesta es mas heterogenea y dependiente del benchmark.",
        "\\item El analisis de componentes recomienda usar \\emph{consensus} para conclusiones conjuntas. A budget 7000 y frente real domina \\texttt{%s} tanto en EP como en HV; en el frente de puntos extremos dominan \\texttt{%s} para EP y \\texttt{%s} para HV, con mayor asimetria entre benchmarks."
        % (
            latex_escape(str(real_7000_ep["Componente"])),
            latex_escape(str(estimated_7000_ep["Componente"])),
            latex_escape(str(estimated_7000_hv["Componente"])),
        ),
        "\\item Las configuraciones finales de RE3D y RWA3D permanecen separadas incluso cuando se comparan con una metrica mixta. La separacion media between-minus-within es %.3f con Gower no ponderada y %.3f con Gower ponderada por consensus, por lo que las diferencias mas relevantes para rendimiento son todavia mayores."
        % (
            stats["distance_separation_unweighted_mean"],
            stats["distance_separation_weighted_mean"],
        ),
        "\\item El subespacio elite es consistente en todos los slices (%s para las inclusiones top-5/top-10/top-20). Las propuestas globales por tipo de frente solo recortan %.1f\\%% de complejidad efectiva, mientras que las especificas por benchmark recortan entre %.1f\\%% y %.1f\\%%."
        % (
            "si" if elite_nested_ok else "no",
            float(global_reduction.mean()) if not global_reduction.empty else 0.0,
            float(family_specific_reduction.min()) if not family_specific_reduction.empty else 0.0,
            float(family_specific_reduction.max()) if not family_specific_reduction.empty else 0.0,
        ),
        "\\item La comparacion estadistica explicita entre frente real y de puntos extremos solo detecta diferencias significativas en %d de 8 combinaciones benchmark-budget; cuando aparecen, el mayor contraste se observa en %s con budget %d (A = %.3f, efecto %s)."
        % (
            int(significant_front_diffs.shape[0]),
            latex_escape(str(strongest_front_gap["Benchmark"])),
            int(strongest_front_gap["Budget"]),
            float(strongest_front_gap["A"]),
            latex_escape(str(strongest_front_gap["Efecto"])),
        ),
        "\\item El modelado predictivo por random forest es mucho mas estable en %s (%s, $R^2$ CV medio %.3f) que en %s (%s, $R^2$ CV medio %.3f), lo que sugiere una estructura configuracional mas aprendible en el primer caso."
        % (
            latex_escape(str(best_rf_row["Benchmark"])),
            latex_escape(str(best_rf_row["Frente"])),
            float(best_rf_row["R2 CV media"]),
            latex_escape(str(worst_rf_row["Benchmark"])),
            latex_escape(str(worst_rf_row["Frente"])),
            float(worst_rf_row["R2 CV media"]),
        ),
        "\\item El 90\\%% del HV final se alcanza pronto: la mediana global es %.0f meta-evaluaciones y el percentil 90 agregado no supera %.0f. Llegar al 99\\%% del HV de referencia requiere costes moderados, pero subir al 99.9\\%% mueve el coste a 1.4--1.5 millones de evaluaciones base."
        % (
            hv90_overall_median,
            hv90_overall_p90,
        ),
        "\\item Las configuraciones exactas repetidas no son una buena heuristica de calidad: la top repetida coincide %d de %d veces con la mejor por EP y %d de %d con la mejor por HV; ademas no aparece ninguna configuracion exacta compartida entre RE3D y RWA3D."
        % (
            stats["best_match_ep"],
            stats["best_match_cases"],
            stats["best_match_hv"],
            stats["best_match_cases"],
        ),
        "\\end{itemize}",
        "\\clearpage",
        "\\tableofcontents",
        "\\clearpage",
        "\\pagenumbering{arabic}",
        "\\section{Objetivo y preguntas de trabajo}",
        "Este informe sintetiza todo el trabajo analitico realizado hasta ahora sobre los benchmarks RE3D y RWA3D en Evolver. El objetivo no es solo catalogar figuras o tablas, sino responder con detalle a tres preguntas: (i) como converge el meta-optimizador a lo largo de sus 3000 meta-evaluaciones y como cambia el rendimiento cuando varia el budget del algoritmo base; (ii) que componentes o familias de parametros son mas influyentes en cada benchmark, frente y budget; y (iii) hasta que punto se repiten configuraciones finales exactas, tanto por benchmark como en una lectura conjunta.",
        "El informe combina vistas desagregadas por benchmark (RE3D frente a RWA3D), por tipo de frente (real frente a puntos extremos) y por budget del algoritmo base (1000, 3000, 5000 y 7000 evaluaciones). Cuando la agregacion es interpretable se usan vistas conjuntas; cuando no lo es, se explicita el motivo y se sustituye por una alternativa metodologicamente mas limpia.",
        "\\section{Contexto experimental y metodologia}",
        "Cada experimento fija el meta-optimizador en 3000 meta-evaluaciones y varia el budget del algoritmo base en \\{1000, 3000, 5000, 7000\\}. Las salidas utilizadas son \\texttt{INDICATORS.csv} y \\texttt{CONFIGURATIONS.csv} en los directorios de resultados. Los indicadores principales son EP y HVMinus; para la interpretacion de las curvas del algoritmo base se trabaja con \\(HV = -HVMinus\\), de modo que valores mayores de HV implican mejor calidad.",
        "Las curvas de convergencia presentadas en este informe son medianas con banda intercuartil entre runs y se analizan tanto en meta-evaluaciones como en budget del algoritmo base. La vista pooled de HV se mantiene unicamente como recurso descriptivo para ver patrones macro; no se usa para atribucion de componentes. Para esa pregunta se adopta exclusivamente la nocion de \\emph{consensus}, definida como la media de importancias normalizadas aprendidas por separado en RE3D y RWA3D.",
        "Las importancias de componentes se obtienen con \\texttt{RandomForestRegressor} tras codificar categoricos en one-hot y normalizar numericos mediante min-max. Las configuraciones repetidas se identifican mediante una firma estable construida sobre todos los parametros finales del algoritmo base, redondeando las variables numericas a seis decimales. El nivel de analisis es agregado por benchmark: no se descompone por instancia individual del training set, por lo que las conclusiones describen la respuesta conjunta de cada familia y no de un problema aislado.",
        "\\section{Convergencia del meta-optimizador y del algoritmo base}",
        "El primer hallazgo es que el limite de 3000 meta-evaluaciones es conservador. Considerando conjuntamente EP y HV, %d de %d hitos \\(T95\\) aparecen a 2000 meta-evaluaciones o antes; el maximo observado es %d meta-evaluaciones y la mediana es %.0f. En otras palabras, el meta-optimizador suele haber capturado ya la mayor parte de la mejora bastante antes de agotarse el presupuesto completo."
        % (stats["count_t95_le_2000"], stats["total_t95"], stats["max_t95"], stats["median_t95"]),
        "El segundo hallazgo es que el impacto del budget del algoritmo base depende mucho mas del tipo de frente que del propio limite de meta-evaluaciones. En frente real, los budgets altos siguen aportando mejora util: RE3D necesita budget 5000 para llegar al 95\\% de EP y 7000 para HV, mientras que RWA3D necesita 7000 para ambos indicadores. En el frente de puntos extremos, la situacion es mas heterogenea y dependiente del benchmark.",
        pooled_hv_figure,
        re3d_hv_figure,
        rwa_hv_figure,
        "\\begin{landscape}",
        meta_table,
        "\\end{landscape}",
        budget_t95_table,
        "Las ganancias relativas refuerzan esa lectura. RE3D con frente real y budget 1000 es el slice con mayor mejora relativa en EP (40.9\\%), seguido de RWA3D con frente real y budget 1000 (5.1\\%). En contraste, el frente de puntos extremos presenta un patron distinto y mas dependiente del benchmark, por lo que conviene leer las tablas de resumen junto con las figuras.",
        "\\section{Importancia de componentes}",
        "Las curvas de importancia muestran dos regimenes distintos. En frente real, la estructura es bastante estable y el componente dominante termina concentrandose en el tamano de poblacion con archivo. En el frente de puntos extremos, en cambio, la jerarquia de componentes cambia con el budget y ademas difiere entre RE3D y RWA3D, por lo que cualquier lectura conjunta debe hacerse con cuidado.",
        "La figura consensus en frente real es especialmente estable. A budget 1000 el top-1 consensus para EP y HV es \\texttt{mutationProbabilityFactor}. A budget 3000 se abre paso \\texttt{populationSizeWithArchive} en EP y \\texttt{powerLawMutationDelta} en HV. A budgets 5000 y 7000 el tamano de poblacion domina claramente EP, y en 7000 tambien HV. Esta trayectoria coincide con el analisis desagregado: en budget 7000 tanto RE3D como RWA3D tienen \\texttt{populationSizeWithArchive} como principal predictor en frente real para EP y HV.",
        "En el frente de puntos extremos la historia cambia. Los leaders consensus a budget 7000 son \\texttt{%s} para EP y \\texttt{%s} para HV. La lectura operativa sigue siendo la misma: los consensus deben leerse como promedio de relevancias, no como prueba de una configuracion universal compartida por RE3D y RWA3D."
        % (
            latex_escape(str(estimated_7000_ep["Componente"])),
            latex_escape(str(estimated_7000_hv["Componente"])),
        ),
        reference_consensus_figure,
        estimated_consensus_figure,
        individual_7000_figure,
        "\\begin{landscape}",
        consensus_compact_table,
        "\\end{landscape}",
        "\\section{Repeticion de configuraciones finales}",
        "El analisis de firmas exactas de configuracion es muy claro: las configuraciones finales exactas apenas se repiten. Incluso en los casos con mayor repeticion, esas coincidencias siguen estando concentradas en muy pocos runs.",
        "La conclusion mas fuerte es que la configuracion exacta mas repetida no coincide con la mejor configuracion ni por EP ni por HV. En %d de %d casos coincide con la mejor por EP, y en %d de %d coincide con la mejor por HV. Por tanto, repetir una configuracion exacta no es una senal de optimo global, sino simplemente de duplicacion local dentro de la nube final de soluciones."
        % (stats["best_match_ep"], stats["best_match_cases"], stats["best_match_hv"], stats["best_match_cases"]),
        "Tampoco aparece ninguna configuracion exacta compartida entre RE3D y RWA3D: el numero total de coincidencias exactas shared es %d. Este resultado se mantiene para los cuatro budgets y para ambos tipos de frente. La lectura operativa es importante: si se busca transferencia entre benchmarks, no conviene transferir configuraciones exactas; conviene transferir familias de parametros y patrones de componentes."
        % stats["shared_exact_total"],
        repeated_table,
        shared_table,
        best_match_table,
        "\\section{Distancia entre configuraciones y arquetipos finales}",
        "La igualdad exacta resulta demasiado estricta para responder si dos configuraciones son parecidas. Por eso se ha anadido una distancia Gower mixta sobre las configuraciones finales, con dos variantes: una no ponderada y otra ponderada por las importancias consensus de componentes. En los 16 casos analizados, la distancia media between supera a la distancia media within, lo que indica que RE3D y RWA3D no acaban ocupando la misma region del espacio de configuraciones. Ademas, la separacion media between-minus-within sube de %.3f a %.3f cuando se pondera por consensus."
        % (
            stats["distance_separation_unweighted_mean"],
            stats["distance_separation_weighted_mean"],
        ),
        "La lectura cualitativa es clara. La version no ponderada ya detecta diferencias estructurales entre benchmarks, pero la version ponderada las amplifica porque da mas peso a los componentes que realmente explican rendimiento. Eso significa que RE3D y RWA3D no solo difieren en parametros secundarios: difieren sobre todo en los parametros que mas importan para la calidad final. En paralelo, el analisis de medoids muestra que cada benchmark conserva un arquetipo dominante que suele capturar alrededor del 50--65\\% de las soluciones finales, con variaciones segun frente y budget.",
        reference_distance_figure,
        estimated_distance_figure,
        distance_summary_table,
        distance_best_table,
        distance_medoids_table,
        "\\section{Estructura de configuraciones y espacios reducidos}",
        "El analisis del subespacio elite deja una conclusion clara: las regiones casi optimas son mucho mas informativas que la mejor configuracion puntual. Ademas, las inclusiones nested top-5 dentro de top-10 y top-10 dentro de top-20 se cumplen en todos los slices (%s), lo que indica que la definicion de elite usada para estas tablas es estable y no una fluctuacion arbitraria."
        % ("si" if elite_nested_ok else "no"),
        "A budget 7000, las propuestas especificas por benchmark fijan entre %.0f y %.0f categoricos y acotan tres numericos, con reducciones de complejidad efectiva entre %.1f\\%% y %.1f\\%%. En cambio, las propuestas globales por tipo de frente apenas reducen %.1f\\%%, por lo que la evidencia favorece una poda especifica por benchmark antes que una poda unica para RE3D y RWA3D."
        % (
            float(reduced_space_df.loc[reduced_space_df["Propuesta"].str.contains("RE3D|RWA3D"), "Categoricos fijados"].min()),
            float(reduced_space_df.loc[reduced_space_df["Propuesta"].str.contains("RE3D|RWA3D"), "Categoricos fijados"].max()),
            float(family_specific_reduction.min()) if not family_specific_reduction.empty else 0.0,
            float(family_specific_reduction.max()) if not family_specific_reduction.empty else 0.0,
            float(global_reduction.mean()) if not global_reduction.empty else 0.0,
        ),
        elite_7000_figure,
        elite_actions_table,
        "Las reglas interpretables son mucho mas escasas que las concentraciones marginales: con los umbrales exigidos de soporte y precision solo sobreviven %d reglas. En los datos actuales la principal es \\texttt{%s}, lo que sugiere que la estructura buena existe pero no se deja resumir por muchas reglas simples y globales."
        % (
            conditional_rule_count,
            latex_escape(str(top_rule_text)) if conditional_rule_count else "sin regla robusta",
        ),
        conditional_rules_table,
        "Las trayectorias del incumbente muestran dos comportamientos. Por un lado, varios categoricos se estabilizan muy pronto; por otro, la entrada en la region elite final puede retrasarse mucho en algunos slices de RE3D, mientras RWA3D suele entrar antes. Esa combinacion refuerza la idea de que existe convergencia estructural, pero no una configuracion universal exacta compartida entre benchmarks.",
        trajectory_7000_figure,
        trajectory_table,
        reduced_space_table,
        (
            validation_table
            if validation_has_completed_runs
            else "Los reruns completos de validacion todavia no se han ejecutado, por lo que esta version del informe incluye la sintesis del espacio reducido, los YAML derivados y el runner Java compatible, pero no una comparacion cuantitativa final frente al baseline."
        ),
        "\\section{Contrastes estadisticos entre tipos de frente}",
        "La comparacion directa entre frente real y frente de puntos extremos sobre el HV final muestra un resultado mas matizado de lo que sugerian algunas lecturas visuales: solo %d de 8 combinaciones benchmark-budget presentan diferencias estadisticamente significativas. Eso significa que el cambio de frente altera la jerarquia de componentes y endurece EP, pero no desplaza de forma sistematica y grande el HV final en todos los slices."
        % int(significant_front_diffs.shape[0]),
        "Cuando la diferencia si aparece, el mayor contraste agregado se observa en %s con budget %d, donde la magnitud de Vargha-Delaney es %.3f y el efecto cae en la categoria %s. En paralelo, los deltas medianos de HV siguen siendo pequenos en valor absoluto, lo que refuerza la idea de que HV es bastante robusto al tipo de frente incluso cuando las pruebas detectan cambio estadistico."
        % (
            latex_escape(str(strongest_front_gap["Benchmark"])),
            int(strongest_front_gap["Budget"]),
            float(strongest_front_gap["A"]),
            latex_escape(str(strongest_front_gap["Efecto"])),
        ),
        front_type_stats_figure,
        front_type_stats_table,
        "\\section{Frecuencias, distribuciones y asociaciones de parametros}",
        "Las frecuencias categoricas y las distribuciones numericas confirman que la estructura de las configuraciones buenas no se reduce a una sola receta. Incluso a budget 7000, muchas modas categoricas quedan lejos de una dominancia total y varios parametros numericos conservan dispersion apreciable entre frente real y puntos extremos. Por eso las podas globales agresivas son menos defendibles que las reducciones especificas por benchmark.",
        "Las asociaciones con el HV final concentran senal util en pocos parametros. La asociacion significativa mas fuerte observada en este bloque es %s en %s bajo %s, con estadistico %.3f. Ademas, el numero de contrastes no parametricos significativos distintos de Spearman asciende a %d, lo que indica que parte de la senal es monotona y parte responde a cambios de distribucion entre ramas parametrizadas."
        % (
            latex_escape(str(strongest_association["Parametro"])),
            latex_escape(str(strongest_association["Benchmark"])),
            latex_escape(str(strongest_association["Frente"])),
            float(strongest_association["Estadistico"]),
            significant_kruskal_count,
        ),
        categorical_frequency_figure,
        categorical_modes_7000_table,
        numerical_distribution_figure,
        numerical_focus_table,
        association_figure,
        association_table,
        "\\section{Modelado predictivo, clustering y configuraciones representativas}",
        "Los modelos predictivos reafirman que no todos los benchmarks tienen la misma estructura aprendible. El mejor caso es %s con %s, donde el random forest alcanza un $R^2$ medio en validacion cruzada de %.3f; el peor es %s con %s, que baja a %.3f. Esa diferencia encaja con la intuicion visual: RWA3D presenta una estructura algo mas compacta y regular, mientras que RE3D parece admitir varias familias competitivas y mas heterogeneas."
        % (
            latex_escape(str(best_rf_row["Benchmark"])),
            latex_escape(str(best_rf_row["Frente"])),
            float(best_rf_row["R2 CV media"]),
            latex_escape(str(worst_rf_row["Benchmark"])),
            latex_escape(str(worst_rf_row["Frente"])),
            float(worst_rf_row["R2 CV media"]),
        ),
        "El clustering por silhouette apunta en la misma direccion. RE3D con puntos extremos se resume bien con dos clusters, mientras que RE3D con frente real necesita siete; RWA3D queda en valores intermedios. El mensaje no es solo cuantitativo: el tipo de frente cambia la geometria del espacio final, y ese cambio es mas visible en RE3D que en RWA3D.",
        "La configuracion representativa estricta, construida por modas categoricas y medianas numericas condicionadas, es util como descripcion sintetica pero no como sustituto del subespacio elite. En RE3D solo cubre %.2f\\%% de las filas finales, mientras que en RWA3D sube a %.2f\\%%. Esa asimetria confirma que RE3D requiere una descripcion por familias o arquetipos, no por un unico punto representativo."
        % (
            float(representative_re3d["Cobertura (%)"]),
            float(representative_rwa3d["Cobertura (%)"]),
        ),
        rf_predictive_figure,
        rf_table,
        clustering_figure,
        clustering_table,
        representative_table,
        "\\section{Convergencia al 90\\% de HV y estabilidad temprana}",
        "La convergencia temprana medida por HV90 es bastante rapida. La mediana global para alcanzar el 90\\%% del HV final es %.0f meta-evaluaciones y el percentil 90 agregado no pasa de %.0f. Eso respalda la idea de que buena parte de la estructura competitiva aparece pronto, incluso cuando el optimo final siga refinandose despues."
        % (
            hv90_overall_median,
            hv90_overall_p90,
        ),
        "Sin embargo, llegar pronto al 90\\%% del HV no implica que la configuracion ya sea la final. En total hay %d parametros benchmark-especificos cuya version temprana difiere materialmente de la final segun el criterio fijado. En RE3D cambian piezas como \\texttt{crossover}, \\texttt{populationSizeWithArchive} o \\texttt{powerLawMutationDelta}; en RWA3D tambien aparecen discrepancias en \\texttt{selection}, \\texttt{crossover} y \\texttt{populationSizeWithArchive}. La lectura metodologica es clara: HV converge antes que la configuracion."
        % hv90_inconsistent_count,
        hv90_figure,
        hv90_percentiles_table,
        hv90_inconsistent_table,
        "\\section{Coste-calidad y escenarios operativos}",
        "El bloque de coste-calidad permite traducir la calidad relativa a decisiones operativas. Para alcanzar el 99\\%% del HV de referencia pooled, RE3D puede hacerlo con %s a budget %d y meta-evaluacion %d, para un coste total de %d. RWA3D es incluso mas barato en ese umbral: %s, budget %d, meta-evaluacion %d y coste %d."
        % (
            latex_escape(str(re3d_cost_99["Frente"])),
            int(re3d_cost_99["Budget"]),
            int(re3d_cost_99["Meta-evaluacion"]),
            int(re3d_cost_99["Coste total"]),
            latex_escape(str(rwa3d_cost_99["Frente"])),
            int(rwa3d_cost_99["Budget"]),
            int(rwa3d_cost_99["Meta-evaluacion"]),
            int(rwa3d_cost_99["Coste total"]),
        ),
        "El salto al 99.9\\%% cambia el panorama: RE3D necesita %s con budget %d y coste %d, mientras que RWA3D necesita %s con budget %d y coste %d. El coste marginal de rascar esa ultima decima porcentual es, por tanto, muy alto frente al objetivo del 99\\%%."
        % (
            latex_escape(str(re3d_cost_999["Frente"])),
            int(re3d_cost_999["Budget"]),
            int(re3d_cost_999["Coste total"]),
            latex_escape(str(rwa3d_cost_999["Frente"])),
            int(rwa3d_cost_999["Budget"]),
            int(rwa3d_cost_999["Coste total"]),
        ),
        "La comparacion por escenarios de coste confirma que las configuraciones representativas dependen del presupuesto operativo. El escenario con mayor cobertura observada es %s en %s, con %.2f\\%% de las filas y %d coincidencias categoriales respecto al escenario final C. En conjunto, estos resultados sugieren usar los escenarios baratos para cribado y reservar los caros para consolidacion final, no para exploracion amplia."
        % (
            latex_escape(str(best_cost_scenario["Scenario"])),
            latex_escape(str(best_cost_scenario["Benchmark"])),
            float(best_cost_scenario["Cobertura (%)"]),
            int(best_cost_scenario["Coincidencias categ. vs C"]),
        ),
        cost_quality_figure,
        cost_quality_table,
        cost_scenario_table,
        "\\section{Implicaciones practicas para experimentos futuros}",
        "Si el objetivo es transformar los hallazgos anteriores en un protocolo de trabajo mas barato y mas enfocado, la conclusion principal es que no conviene tratar todos los budgets ni todos los parametros como igualmente relevantes. Los resultados apoyan una estrategia agresiva de simplificacion: reducir el techo del meta-optimizador, eliminar budgets claramente insuficientes para conclusiones finales y podar parte del espacio de parametros en una primera version reducida.",
        "En budgets, la lectura es contundente: si hace falta un unico protocolo final comparable entre RE3D y RWA3D, el unico budget base que resulta globalmente suficiente en ambos frentes es 7000. En frente real, 1000 y 3000 pueden descartarse para conclusiones finales y 5000 deberia quedar como punto intermedio de sensibilidad, no como estandar comun. En el frente de puntos extremos, la utilidad relativa de 3000, 5000 y 7000 debe leerse a la luz de las tablas de T95 y las curvas por benchmark.",
        "En parametros, el mensaje depende del tipo de frente. En frente real, el espacio reducido deberia seguir dejando libertad a \\texttt{populationSizeWithArchive}, \\texttt{powerLawMutationDelta}, \\texttt{crossoverProbability} y \\texttt{mutationProbabilityFactor}. En el frente de puntos extremos, esa lista cambia hacia los leaders consensus observados en los CSV y figuras del bloque de importancia. Los parametros de muy baja importancia no deben interpretarse como irrelevantes en sentido absoluto, pero si como buenos candidatos a fijar o podar en una primera ronda de experimentos mas estrecha.",
        budget_recommendations_table,
        parameter_recommendations_table,
        "\\section{Conclusiones y recomendaciones}",
        "Tomados en conjunto, los resultados apuntan a una estrategia clara. Primero, el cuello de botella no parece estar en las 3000 meta-evaluaciones: la mayor parte de la mejora aparece antes. Segundo, en frente real el budget base sigue importando y, ademas, la estructura de componentes es bastante interpretable: al final del recorrido, \\texttt{%s} se consolida como el componente comun mas importante tanto para EP como para HV a budget 7000."
        % latex_escape(str(real_7000_ep["Componente"])),
        "Tercero, en el frente de puntos extremos la sensibilidad a los componentes es mas inestable y benchmark-especifica. A budget 7000 el consensus prioriza \\texttt{%s} para EP y \\texttt{%s} para HV, pero la tabla compacta del cuerpo deja claro que el reparto entre RE3D y RWA3D no es necesariamente simetrico; por eso el consensus debe leerse como promedio de relevancias, no como prueba de una unica configuracion universal."
        % (
            latex_escape(str(estimated_7000_ep["Componente"])),
            latex_escape(str(estimated_7000_hv["Componente"])),
        ),
        "\\begin{itemize}",
        "\\item Si el objetivo es reducir coste de meta-optimizacion, la recomendacion operativa es ensayar limites cercanos a 2000--2500 meta-evaluaciones y verificar si la calidad final cambia materialmente respecto a 3000.",
        "\\item Si el objetivo es fijar un unico protocolo final comparable entre benchmarks, el budget base de referencia debe ser 7000; 1000 y 3000 pueden descartarse para frente real y la decision en puntos extremos debe apoyarse en los T95 observados por benchmark.",
        "\\item Si el objetivo es definir prioridades de modelado en frente real, conviene dedicar mas resolucion a \\texttt{%s}, \\texttt{mutationProbabilityFactor}, \\texttt{powerLawMutationDelta} y \\texttt{crossoverProbability}."
        % latex_escape(str(real_7000_hv["Componente"])),
        "\\item Si el objetivo es construir un espacio reducido de parametros, la primera poda razonable pasa por subparametros persistentemente debiles como \\texttt{undcCrossoverEta}, \\texttt{undcCrossoverZeta}, \\texttt{fuzzyRecombinationCrossoverAlpha} y varios indices de distribucion poco activos.",
        "\\item Si el objetivo es trasladar conocimiento entre benchmarks, no conviene transferir configuraciones exactas finales: no hay coincidencias literales shared, la configuracion mas repetida no es la mejor en ninguno de los 16 casos analizados y la distancia between supera sistematicamente a la within.",
        "\\item Para el frente de puntos extremos, las decisiones sobre archivo y mutacion deben mantenerse condicionadas por benchmark y budget; una regla unica para RE3D y RWA3D seria excesivamente fuerte con la evidencia actual.",
        "\\end{itemize}",
        "\\section{Limitaciones}",
        "Este informe trabaja con los indicadores agregados que usa el propio meta-optimizador, no con desgloses por instancia individual de cada training set. Por tanto, cuando se habla de convergencia o importancia se esta hablando de la respuesta agregada de cada familia (RE3D o RWA3D), no de una instancia concreta del benchmark. Ademas, las importancias de random forest son descriptivas, no causales. Finalmente, la ausencia de configuraciones exactas shared entre benchmarks no implica ausencia de soluciones casi equivalentes; simplemente indica que la coincidencia literal de todos los parametros finales es extremadamente rara.",
        "\\appendix",
        "\\section{Tablas suplementarias compactas}",
        "El detalle completo de top-3 y de importancias consensus por componente se conserva en los CSV de \\texttt{scripts/analysis/tables/report}. En el PDF se muestran solo tablas compactas para mantener legibilidad.",
        "\\begin{landscape}",
        appendix_individual_leaders_table,
        "\\end{landscape}",
        "\\begin{landscape}",
        appendix_consensus_leaders_table,
        "\\end{landscape}",
        "\\section{Archivos clave generados}",
        "\\begin{itemize}",
        "\\item \\texttt{scripts/analysis/figures}: curvas desagregadas y pooled de HV normalizado.",
        "\\item \\texttt{scripts/analysis/figures/component\\_importance}: figuras de importancia por benchmark y consensus, separadas por budget.",
        "\\item \\texttt{scripts/analysis/tables/component\\_importance}: tablas de importancia por benchmark y consensus.",
        "\\item \\texttt{scripts/analysis/figures/configuration\\_distance}: figuras de distancia Gower por frente y budget.",
        "\\item \\texttt{scripts/analysis/tables/configuration\\_distance}: resumentes within/between, mejores configuraciones, medoids y vecinos cruzados.",
        "\\item \\texttt{scripts/analysis/tables/repeated\\_configurations}: tablas de configuraciones repetidas y consensus exacto.",
        "\\item \\texttt{scripts/analysis/figures/elite\\_subspace}: perfiles de distribucion total frente a elite conjunto del 10\\%.",
        "\\item \\texttt{scripts/analysis/figures/configuration\\_trajectory}: distancia al medoid elite final y estabilizacion temporal de parametros.",
        "\\item \\texttt{scripts/analysis/tables/elite\\_subspace}, \\texttt{conditional\\_rules}, \\texttt{configuration\\_trajectory}: salidas especificas del nuevo bloque de analisis de configuraciones.",
        "\\item \\texttt{scripts/analysis/figures/statistical\\_comparison}, \\texttt{categorical\\_frequencies}, \\texttt{numerical\\_distributions}: comparaciones entre tipos de frente y estructura marginal de parametros.",
        "\\item \\texttt{scripts/analysis/figures/parameter\\_association}, \\texttt{rf\\_predictive\\_importance}, \\texttt{configuration\\_clustering}: asociaciones, modelos predictivos y arquetipos de configuracion.",
        "\\item \\texttt{scripts/analysis/figures/hv90\\_convergence} y \\texttt{cost\\_quality}: convergencia temprana y curvas coste-calidad.",
        "\\item \\texttt{scripts/analysis/tables/statistical\\_comparison}, \\texttt{categorical\\_frequencies}, \\texttt{numerical\\_distributions}, \\texttt{parameter\\_association}, \\texttt{rf\\_predictive\\_importance}, \\texttt{configuration\\_clustering}, \\texttt{representative\\_configurations}, \\texttt{hv90\\_convergence}, \\texttt{cost\\_quality} y \\texttt{cost\\_scenarios}: salidas completas de los analisis adicionales del dise\\~no experimental.",
        "\\item \\texttt{src/main/resources/parameterSpaces/reduced}: YAML derivados para validar espacios reducidos.",
        "\\item \\texttt{scripts/analysis/tables/space\\_reduction} y \\texttt{validation\\_reduced\\_space}: resumenes de reduccion y estado de validacion frente al baseline.",
        "\\item \\texttt{scripts/analysis/tables/report}: tablas compactas y tablas auxiliares generadas especificamente para este informe.",
        "\\end{itemize}",
        "\\end{document}",
    ]

    output_path = REPORT_DIR / "detailed_analysis_report.tex"
    output_path.write_text("\n\n".join(tex_lines), encoding="utf-8")
    return output_path


def main() -> None:
    meta_df = compute_meta_convergence_summary()
    budget_t95_df = load_budget_t95_summary()
    individual_components_df = load_individual_component_top3()
    consensus_compact_df, consensus_detail_df = load_consensus_component_summary()
    individual_leaders_df = build_individual_component_leaders(individual_components_df)
    consensus_leaders_df = build_consensus_component_leaders(consensus_detail_df)
    repeated_summary_df, shared_summary_df, best_match_df = load_repeated_configuration_summary()
    repeated_compact_df = build_repeated_configuration_compact(repeated_summary_df)
    distance_summary_df, distance_best_df, distance_medoids_df = load_configuration_distance_summaries()
    budget_recommendations_df = build_budget_recommendations()
    parameter_recommendations_df, _ = build_parameter_space_recommendations()
    elite_actions_df, _, _ = load_elite_configuration_summary()
    conditional_rules_df = load_conditional_rule_report()
    trajectory_df = load_trajectory_configuration_summary()
    reduced_space_df = load_reduced_space_report()
    validation_summary_df, validation_recommendations_df, validation_has_completed_runs = load_validation_report()
    front_type_stats_df = load_front_type_statistical_report()
    categorical_modes_df = load_categorical_frequency_report()
    numerical_distribution_df = load_numerical_distribution_report()
    association_df = load_parameter_association_report()
    rf_predictive_df = load_rf_predictive_report()
    clustering_df = load_clustering_report()
    representative_df = load_representative_configuration_report()
    hv90_percentiles_df, hv90_parameter_comparison_df = load_hv90_report()
    cost_quality_df = load_cost_quality_report()
    cost_scenario_df = load_cost_scenario_report()
    report_path = write_report_tex(
        meta_df,
        budget_t95_df,
        individual_components_df,
        consensus_compact_df,
        consensus_detail_df,
        individual_leaders_df,
        consensus_leaders_df,
        repeated_summary_df,
        repeated_compact_df,
        shared_summary_df,
        best_match_df,
        distance_summary_df,
        distance_best_df,
        distance_medoids_df,
        budget_recommendations_df,
        parameter_recommendations_df,
        elite_actions_df,
        conditional_rules_df,
        trajectory_df,
        reduced_space_df,
        validation_summary_df,
        validation_recommendations_df,
        validation_has_completed_runs,
        front_type_stats_df,
        categorical_modes_df,
        numerical_distribution_df,
        association_df,
        rf_predictive_df,
        clustering_df,
        representative_df,
        hv90_percentiles_df,
        hv90_parameter_comparison_df,
        cost_quality_df,
        cost_scenario_df,
    )
    print(f"Saved: {report_path}")


if __name__ == "__main__":
    main()
