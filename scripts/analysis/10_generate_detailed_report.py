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
FRONT_TYPES = ["referenceFronts", "estimatedReferenceFronts"]
BUDGETS = [1000, 3000, 5000, 7000]


def front_label(front_type: str) -> str:
    return {
        "referenceFronts": "Real",
        "estimatedReferenceFronts": "Estimado",
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
                "Contexto": "Frente estimado",
                "Decision": "Budget final conservador",
                "Recomendacion": "7000",
                "Motivo": "Tambien es el unico budget globalmente suficiente cuando se exigen ambos benchmarks y ambas metricas.",
            },
            {
                "Contexto": "Frente estimado",
                "Decision": "Budget exploratorio util",
                "Recomendacion": "3000",
                "Motivo": "Ya capta parte de la calidad final y reduce coste; es razonable para barridos iniciales.",
            },
            {
                "Contexto": "Frente estimado",
                "Decision": "Descartar para conclusiones finales",
                "Recomendacion": "1000",
                "Motivo": "Es demasiado restrictivo para extraer conclusiones estables de calidad final.",
            },
            {
                "Contexto": "Frente estimado",
                "Decision": "No usar como unico default",
                "Recomendacion": "5000",
                "Motivo": "Aporta de forma asimetrica segun benchmark; no mejora lo suficiente como presupuesto estandar unico.",
            },
        ]
    )
    result.to_csv(REPORT_TABLES_DIR / "budget_recommendations.csv", index=False)
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

    pooled_hv_figure = subfigure_grid(
        [
            (FIGURES_DIR / "base_hv_pooled" / f"base_hv_normalized_convergence_pooled_budget_{budget}.png", f"Budget {budget}")
            for budget in BUDGETS
        ],
        "Curvas pooled descriptivas de HV normalizado del algoritmo base. Estas figuras se usan solo para describir patrones macro de convergencia; no se utilizan para atribucion causal de componentes.",
        "fig:pooled_hv",
    )
    re3d_hv_figure = subfigure_grid(
        [
            (FIGURES_DIR / "base_hv_normalized_individual" / f"base_hv_normalized_convergence_RE3D_budget_{budget}.png", f"Budget {budget}")
            for budget in BUDGETS
        ],
        "Convergencia normalizada del HV del algoritmo base para RE3D, separando frente real y estimado en cada figura.",
        "fig:re3d_hv",
    )
    rwa_hv_figure = subfigure_grid(
        [
            (FIGURES_DIR / "base_hv_normalized_individual" / f"base_hv_normalized_convergence_RWA3D_budget_{budget}.png", f"Budget {budget}")
            for budget in BUDGETS
        ],
        "Convergencia normalizada del HV del algoritmo base para RWA3D, separando frente real y estimado en cada figura.",
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
                FIGURES_DIR / "component_importance" / f"budget_{budget}" / f"component_importance_consensus_estimated_{budget}.png",
                f"Budget {budget}",
            )
            for budget in BUDGETS
        ],
        "Importancia consensus de componentes en frente estimado para los cuatro budgets. La variabilidad entre budgets es notablemente mayor que en frente real.",
        "fig:consensus_estimated",
    )
    individual_7000_figure = subfigure_grid(
        [
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RE3D_referenceFronts_7000.png", "RE3D, frente real"),
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RE3D_estimatedReferenceFronts_7000.png", "RE3D, frente estimado"),
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RWA3D_referenceFronts_7000.png", "RWA3D, frente real"),
            (FIGURES_DIR / "component_importance" / "budget_7000" / "component_importance_RWA3D_estimatedReferenceFronts_7000.png", "RWA3D, frente estimado"),
        ],
        "Importancia de componentes a budget 7000 en los analisis desagregados. Esta figura es la referencia principal para comparar la asimetria entre benchmarks cuando el frente es estimado.",
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
                / f"configuration_distance_distribution_estimatedReferenceFronts_{budget}.png",
                f"Budget {budget}",
            )
            for budget in BUDGETS
        ],
        "Distribuciones de distancia entre configuraciones finales para frente estimado. La ponderacion consensus amplifica la separacion cuando los benchmarks difieren en componentes relevantes.",
        "fig:distance_estimated",
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
        (consensus_leaders_df["Frente"] == "Estimado")
        & (consensus_leaders_df["Budget"] == 7000)
        & (consensus_leaders_df["Indicador"] == "EP")
    ].iloc[0]
    estimated_7000_hv = consensus_leaders_df.loc[
        (consensus_leaders_df["Frente"] == "Estimado")
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
        "{\\Large Convergencia, importancia de componentes y repeticion de configuraciones\\\\[1.6cm]}",
        "{\\large RE3D y RWA3D con frentes reales y estimados\\\\[2.2cm]}",
        "\\begin{tabular}{ll}",
        "Benchmarks analizados & RE3D, RWA3D\\\\",
        "Tipos de frente & real, estimado\\\\",
        "Budgets del algoritmo base & 1000, 3000, 5000, 7000\\\\",
        "Meta-optimizador & 3000 meta-evaluaciones fijas\\\\",
        "Fuentes de datos & \\texttt{INDICATORS.csv}, \\texttt{CONFIGURATIONS.csv}\\\\",
        "Nivel del informe & vistas agregadas, desagregadas y consensus\\\\",
        "\\end{tabular}",
        "\\vfill",
        "{\\large Generado automaticamente a partir de \\texttt{scripts/analysis}\\\\}",
        "{\\large 9 de marzo de 2026\\\\}",
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
        "\\item En frente real, los budgets altos del algoritmo base siguen aportando mejora util: RE3D necesita budget 5000 para EP y 7000 para HV, mientras que RWA3D necesita 7000 para ambos indicadores. En frente estimado la respuesta es mas heterogenea y dependiente del benchmark.",
        "\\item El analisis de componentes recomienda usar \\emph{consensus} para conclusiones conjuntas. A budget 7000 y frente real domina \\texttt{%s} tanto en EP como en HV; en frente estimado dominan \\texttt{%s} para EP y \\texttt{%s} para HV, con mayor asimetria entre benchmarks."
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
        "\\item Las configuraciones exactas repetidas no son una buena heuristica de calidad: la top repetida coincide 0 de %d veces con la mejor por EP y 0 de %d con la mejor por HV; ademas no aparece ninguna configuracion exacta compartida entre RE3D y RWA3D."
        % (stats["best_match_cases"], stats["best_match_cases"]),
        "\\end{itemize}",
        "\\clearpage",
        "\\tableofcontents",
        "\\clearpage",
        "\\pagenumbering{arabic}",
        "\\section{Objetivo y preguntas de trabajo}",
        "Este informe sintetiza todo el trabajo analitico realizado hasta ahora sobre los benchmarks RE3D y RWA3D en Evolver. El objetivo no es solo catalogar figuras o tablas, sino responder con detalle a tres preguntas: (i) como converge el meta-optimizador a lo largo de sus 3000 meta-evaluaciones y como cambia el rendimiento cuando varia el budget del algoritmo base; (ii) que componentes o familias de parametros son mas influyentes en cada benchmark, frente y budget; y (iii) hasta que punto se repiten configuraciones finales exactas, tanto por benchmark como en una lectura conjunta.",
        "El informe combina vistas desagregadas por benchmark (RE3D frente a RWA3D), por tipo de frente (real frente a estimado) y por budget del algoritmo base (1000, 3000, 5000 y 7000 evaluaciones). Cuando la agregacion es interpretable se usan vistas conjuntas; cuando no lo es, se explicita el motivo y se sustituye por una alternativa metodologicamente mas limpia.",
        "\\section{Contexto experimental y metodologia}",
        "Cada experimento fija el meta-optimizador en 3000 meta-evaluaciones y varia el budget del algoritmo base en \\{1000, 3000, 5000, 7000\\}. Las salidas utilizadas son \\texttt{INDICATORS.csv} y \\texttt{CONFIGURATIONS.csv} en los directorios de resultados. Los indicadores principales son EP y HVMinus; para la interpretacion de las curvas del algoritmo base se trabaja con \\(HV = -HVMinus\\), de modo que valores mayores de HV implican mejor calidad.",
        "Las curvas de convergencia presentadas en este informe son medianas con banda intercuartil entre runs y se analizan tanto en meta-evaluaciones como en budget del algoritmo base. La vista pooled de HV se mantiene unicamente como recurso descriptivo para ver patrones macro; no se usa para atribucion de componentes. Para esa pregunta se adopta exclusivamente la nocion de \\emph{consensus}, definida como la media de importancias normalizadas aprendidas por separado en RE3D y RWA3D.",
        "Las importancias de componentes se obtienen con \\texttt{RandomForestRegressor} tras codificar categoricos en one-hot y normalizar numericos mediante min-max. Las configuraciones repetidas se identifican mediante una firma estable construida sobre todos los parametros finales del algoritmo base, redondeando las variables numericas a seis decimales. El nivel de analisis es agregado por benchmark: no se descompone por instancia individual del training set, por lo que las conclusiones describen la respuesta conjunta de cada familia y no de un problema aislado.",
        "\\section{Convergencia del meta-optimizador y del algoritmo base}",
        "El primer hallazgo es que el limite de 3000 meta-evaluaciones es conservador. Considerando conjuntamente EP y HV, %d de %d hitos \\(T95\\) aparecen a 2000 meta-evaluaciones o antes; el maximo observado es %d meta-evaluaciones y la mediana es %.0f. En otras palabras, el meta-optimizador suele haber capturado ya la mayor parte de la mejora bastante antes de agotarse el presupuesto completo."
        % (stats["count_t95_le_2000"], stats["total_t95"], stats["max_t95"], stats["median_t95"]),
        "El segundo hallazgo es que el impacto del budget del algoritmo base depende mucho mas del tipo de frente que del propio limite de meta-evaluaciones. En frente real, los budgets altos siguen aportando mejora util: RE3D necesita budget 5000 para llegar al 95\\% de EP y 7000 para HV, mientras que RWA3D necesita 7000 para ambos indicadores. En frente estimado, la situacion es mas heterogenea: RE3D alcanza \\(T95\\) de HV ya con 3000, mientras que RWA3D alcanza \\(T95\\) de EP a 3000 pero sigue necesitando 7000 para HV.",
        pooled_hv_figure,
        re3d_hv_figure,
        rwa_hv_figure,
        "\\begin{landscape}",
        meta_table,
        "\\end{landscape}",
        budget_t95_table,
        "Las ganancias relativas refuerzan esa lectura. RE3D con frente real y budget 1000 es el slice con mayor mejora relativa en EP (40.9\\%), seguido de RWA3D con frente real y budget 1000 (5.1\\%). En contraste, cuando el frente es estimado las mejoras absolutas y relativas son mucho menores porque el proceso arranca ya muy cerca de su valor final; por ejemplo, RE3D estimado pasa de una mejora relativa en EP del 0.75\\% a budget 1000 a apenas 0.03\\% a budget 7000.",
        "\\section{Importancia de componentes}",
        "Las curvas de importancia muestran dos regimenes distintos. En frente real, la estructura es bastante estable y el componente dominante termina concentrandose en el tamano de poblacion con archivo. En frente estimado, en cambio, la jerarquia de componentes cambia con el budget y ademas difiere entre RE3D y RWA3D, por lo que cualquier lectura conjunta debe hacerse con cuidado.",
        "La figura consensus en frente real es especialmente estable. A budget 1000 el top-1 consensus para EP y HV es \\texttt{mutationProbabilityFactor}. A budget 3000 se abre paso \\texttt{populationSizeWithArchive} en EP y \\texttt{powerLawMutationDelta} en HV. A budgets 5000 y 7000 el tamano de poblacion domina claramente EP, y en 7000 tambien HV. Esta trayectoria coincide con el analisis desagregado: en budget 7000 tanto RE3D como RWA3D tienen \\texttt{populationSizeWithArchive} como principal predictor en frente real para EP y HV.",
        "En frente estimado la historia cambia. El top-1 consensus para EP pasa de \\texttt{mutation} (1000) a \\texttt{mutationProbabilityFactor} (3000), despues a \\texttt{blxAlphaBetaCrossoverAlpha} (5000) y finalmente a \\texttt{uniformMutationPerturbation} (7000). Para HV, el top-1 pasa de \\texttt{mutationProbabilityFactor} (1000) a \\texttt{crossoverProbability} (3000) y luego a \\texttt{archiveType} (5000 y 7000). Sin embargo, estos consensus no siempre representan un componente compartido de verdad: a budget 7000, por ejemplo, \\texttt{archiveType} es top-1 en HV sobre todo por RE3D, mientras que en RWA3D domina \\texttt{levyFlightMutationBeta}. Esta asimetria es justamente la razon por la que se abandono el pooled crudo de importancia.",
        reference_consensus_figure,
        estimated_consensus_figure,
        individual_7000_figure,
        "\\begin{landscape}",
        consensus_compact_table,
        "\\end{landscape}",
        "\\section{Repeticion de configuraciones finales}",
        "El analisis de firmas exactas de configuracion es muy claro: las configuraciones finales exactas apenas se repiten. La repeticion maxima habitual es 2, y el caso mas extremo es RE3D con frente estimado y budget 7000, donde la configuracion mas repetida aparece 5 veces. Incluso en ese caso, esas repeticiones siguen estando concentradas en un unico run: la cuota de runs de la configuracion top es siempre 3.33\\%, es decir, un solo run de 30.",
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
        "\\section{Implicaciones practicas para experimentos futuros}",
        "Si el objetivo es transformar los hallazgos anteriores en un protocolo de trabajo mas barato y mas enfocado, la conclusion principal es que no conviene tratar todos los budgets ni todos los parametros como igualmente relevantes. Los resultados apoyan una estrategia agresiva de simplificacion: reducir el techo del meta-optimizador, eliminar budgets claramente insuficientes para conclusiones finales y podar parte del espacio de parametros en una primera version reducida.",
        "En budgets, la lectura es contundente: si hace falta un unico protocolo final comparable entre RE3D y RWA3D, el unico budget base que resulta globalmente suficiente en ambos frentes es 7000. En frente real, 1000 y 3000 pueden descartarse para conclusiones finales y 5000 deberia quedar como punto intermedio de sensibilidad, no como estandar comun. En frente estimado, 3000 sigue siendo util para exploracion rapida, pero 1000 es demasiado corto y 5000 no destaca como default unico frente a la combinacion 3000/7000.",
        "En parametros, el mensaje depende del tipo de frente. En frente real, el espacio reducido deberia seguir dejando libertad a \\texttt{populationSizeWithArchive}, \\texttt{powerLawMutationDelta}, \\texttt{crossoverProbability} y \\texttt{mutationProbabilityFactor}. En frente estimado, esa lista cambia hacia \\texttt{archiveType}, \\texttt{mutationProbabilityFactor}, \\texttt{uniformMutationPerturbation} y \\texttt{crossoverProbability}. Los parametros de muy baja importancia no deben interpretarse como irrelevantes en sentido absoluto, pero si como buenos candidatos a fijar o podar en una primera ronda de experimentos mas estrecha.",
        budget_recommendations_table,
        parameter_recommendations_table,
        "\\section{Conclusiones y recomendaciones}",
        "Tomados en conjunto, los resultados apuntan a una estrategia clara. Primero, el cuello de botella no parece estar en las 3000 meta-evaluaciones: la mayor parte de la mejora aparece antes. Segundo, en frente real el budget base sigue importando y, ademas, la estructura de componentes es bastante interpretable: al final del recorrido, \\texttt{%s} se consolida como el componente comun mas importante tanto para EP como para HV a budget 7000."
        % latex_escape(str(real_7000_ep["Componente"])),
        "Tercero, en frente estimado la sensibilidad a los componentes es mas inestable y benchmark-especifica. A budget 7000 el consensus prioriza \\texttt{%s} para EP y \\texttt{%s} para HV, pero la tabla compacta del cuerpo deja claro que el reparto entre RE3D y RWA3D no es simetrico; por eso el consensus debe leerse como promedio de relevancias, no como prueba de una unica configuracion universal."
        % (
            latex_escape(str(estimated_7000_ep["Componente"])),
            latex_escape(str(estimated_7000_hv["Componente"])),
        ),
        "\\begin{itemize}",
        "\\item Si el objetivo es reducir coste de meta-optimizacion, la recomendacion operativa es ensayar limites cercanos a 2000--2500 meta-evaluaciones y verificar si la calidad final cambia materialmente respecto a 3000.",
        "\\item Si el objetivo es fijar un unico protocolo final comparable entre benchmarks, el budget base de referencia debe ser 7000; 1000 y 3000 pueden descartarse para frente real y 1000 tambien para frente estimado.",
        "\\item Si el objetivo es definir prioridades de modelado en frente real, conviene dedicar mas resolucion a \\texttt{%s}, \\texttt{mutationProbabilityFactor}, \\texttt{powerLawMutationDelta} y \\texttt{crossoverProbability}."
        % latex_escape(str(real_7000_hv["Componente"])),
        "\\item Si el objetivo es construir un espacio reducido de parametros, la primera poda razonable pasa por subparametros persistentemente debiles como \\texttt{undcCrossoverEta}, \\texttt{undcCrossoverZeta}, \\texttt{fuzzyRecombinationCrossoverAlpha} y varios indices de distribucion poco activos.",
        "\\item Si el objetivo es trasladar conocimiento entre benchmarks, no conviene transferir configuraciones exactas finales: no hay coincidencias literales shared, la configuracion mas repetida no es la mejor en ninguno de los 16 casos analizados y la distancia between supera sistematicamente a la within.",
        "\\item Para frente estimado, las decisiones sobre archivo y mutacion deben mantenerse condicionadas por benchmark y budget; una regla unica para RE3D y RWA3D seria excesivamente fuerte con la evidencia actual.",
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
        "\\item \\texttt{scripts/analysis/figures/base\\_hv\\_normalized\\_individual}: curvas desagregadas de HV normalizado.",
        "\\item \\texttt{scripts/analysis/figures/base\\_hv\\_pooled}: curvas pooled descriptivas de HV normalizado.",
        "\\item \\texttt{scripts/analysis/figures/component\\_importance}: figuras de importancia por benchmark y consensus, separadas por budget.",
        "\\item \\texttt{scripts/analysis/tables/component\\_importance}: tablas de importancia por benchmark y consensus.",
        "\\item \\texttt{scripts/analysis/figures/configuration\\_distance}: figuras de distancia Gower por frente y budget.",
        "\\item \\texttt{scripts/analysis/tables/configuration\\_distance}: resumentes within/between, mejores configuraciones, medoids y vecinos cruzados.",
        "\\item \\texttt{scripts/analysis/tables/repeated\\_configurations}: tablas de configuraciones repetidas y consensus exacto.",
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
    )
    print(f"Saved: {report_path}")


if __name__ == "__main__":
    main()
