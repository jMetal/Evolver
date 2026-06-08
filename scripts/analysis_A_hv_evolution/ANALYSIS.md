# ANALYSIS: Detailed plan for HV-evolution figures

This document provides a step-by-step plan to analyze and document the HV-evolution plots (main figure: `hv_evolution_grid_2x2.png`). It is intended to produce publication-ready results and supplementary materials for a technical article.

---

## 1. High-level objectives

- Quantify and compare convergence speed (95% threshold) across budgets and front types (reference vs extreme points) for RE3D and RWA3D.
- Measure stability/variability (min–max envelope, std, confidence intervals) of HV trajectories across independent runs.
- Compute aggregated performance metrics that summarize quality over time (AUC, initial slope, plateau point).
- Perform statistical tests and report effect sizes for the main comparisons.
- Assess robustness of conclusions against methodological choices (number of checkpoints, interpolation method, convergence threshold).

## 2. Preparation: data extraction

1. Expected input: `VAR_CONF.txt` files stored under `{PROBLEM}.{dataset_name}.{budget}/run{run}/VAR_CONF.txt`.
2. Parser: reuse `parse_var_conf_file()` from the analysis script; it returns `(evaluation, hv)` pairs for each `# Evaluation:` block.
3. Per-run alignment: reconstruct `target_evals` for each run as `np.linspace(100, max_eval, N_CHECKPOINTS)` and apply forward-fill interpolation (as the current script does).
4. Recommended data frame layout (pandas): columns `problem, dataset, budget, run, checkpoint_idx, eval, hv`.

Suggested command (from `scripts/analysis_A_hv_evolution`):

```bash
python3 analysis_A_hv_evolution.py  # loader exists; extend to dump CSV if desired
```

Suggestion: add a helper `dump_all_series_to_csv(output_path)` to produce `hv_series.csv` for downstream processing.

## 3. Analysis 1 — guided visual description

- Goal: document qualitative patterns visible in each subplot (which budget converges faster, whether front types differ, any anomalous runs).
- Outputs: (1) annotated main figure highlighting key features; (2) one short descriptive paragraph per subplot.
- Tools: matplotlib annotations (`ax.annotate`, `bbox`) and high-resolution export.

## 4. Analysis 2 — 95% convergence times

- Definition: for a given time series, the convergence time is the first checkpoint `t` such that
  `HV(t) >= HV_min + 0.95*(HV_max - HV_min)`.
- Compute this on the median series for a configuration to report a representative value, and on individual runs to estimate variability.
- Outputs: `convergence_times.csv` with columns `problem,dataset,budget,run,convergence_eval`, and a summary table per configuration with median, IQR, min, max.
- Visuals: boxplots (or violin plots) of convergence times grouped by budget (separate plots for each problem/dataset).

Statistical comparison of convergence times can follow (see Section 5).

## 5. Analysis 3 — statistical comparisons

- Tests to consider:
  - Check normality (Shapiro–Wilk) on differences or inspect distributions.
  - Paired comparisons: Wilcoxon signed‑rank test (nonparametric) or paired t‑test (parametric) when appropriate.
  - Multiple budgets: use Friedman test for repeated measures across budgets, followed by post‑hoc (Nemenyi or pairwise Wilcoxon with correction).
- Effect sizes: Cohen's d (parametric) or Cliff's delta (nonparametric).
- Report: raw p‑values, adjusted p‑values (Benjamini–Hochberg or Bonferroni), and effect sizes with interpretation.

## 6. Analysis 4 — temporal variability and stability

- For each checkpoint and configuration compute: min, 25th percentile, median, 75th percentile, max, mean, std.
- Compute coefficient of variation (CV = std/mean) for checkpoints of interest (start, mid, end) and as an integrated CV (AUC‑CV).
- Visualizations: median curves with shaded bands (min–max and bootstrap 95% CI); heatmap of CV across checkpoints (x) × budgets (y).

## 7. Analysis 5 — aggregated performance metrics

- AUC: numerical integral of `HV_median(t)` over time, optionally normalized by total time and by maximum attainable HV.
- Initial slope: fit a linear model on the first X% of checkpoints (e.g., first 10%) and extract slope.
- Plateau detection: identify the first checkpoint after which the incremental gain remains below a small relative threshold for N consecutive checkpoints.
- Outputs: `metrics_summary.csv` with `problem,dataset,budget,AUC,slope_initial,plateau_eval`.
- Visuals: bar charts for AUC with bootstrap CI; scatter plot of slope vs AUC.

## 8. Analysis 6 — methodological sensitivity

- Vary `N_CHECKPOINTS` (e.g., 15, 30, 60) and compare AUC and convergence times.
- Compare interpolation methods: forward‑fill vs linear interpolation and measure metric differences.
- Vary convergence threshold (e.g., 90%, 95%, 99%) to test robustness of conclusions.
- Output: sensitivity matrix and summary heatmap showing whether main conclusions change under these alternatives.

## 9. Analysis 7 — robustness and validation

- Bootstrap runs (resample runs with replacement) to compute 95% confidence intervals for AUC, convergence times, and slopes.
- Detect and report outliers (e.g., runs with unusually low HV); quantify impact by comparing metrics with and without outliers.
- Report missing runs or absent directories and their potential effect on results.

## 10. Final figures and tables (recommended for the manuscript)

1. Main figure: 2×2 grid with median curves, shaded min–max bands, and dashed 95% convergence lines (high-resolution, 300 DPI).
2. Supplementary figure A: boxplots of 95% convergence times (grouped by budget) for each problem/dataset.
3. Supplementary figure B: AUC bars with bootstrap CIs (problem × dataset).
4. Table 1: numeric summary per configuration (median conv time ± IQR, AUC ± CI, slope ± CI, key p‑values and effect sizes).
5. Supplementary Table: full matrix of p‑values and corrected significance tests.

Example CSV schema:

```
problem,dataset,budget,conv_median,conv_IQR,AUC,AUC_CI_low,AUC_CI_high,slope_initial
```

## 11. Writing: results and discussion

- For each principal finding: (1) state the quantitative result (e.g., "median conv time 300 evals vs 450 evals, p<0.01, Cliff's delta=0.6"), (2) provide practical interpretation, (3) state limitations.
- Include a concise robustness paragraph summarizing sensitivity analyses (e.g., how conclusions change with different thresholds).

## 12. Practical implementation: scripts and commands

- Provide a notebook `analysis_A_postproc.ipynb` that:
  1. loads `hv_series.csv` (or directly parses `VAR_CONF.txt`),
  2. computes `convergence_times.csv` and `metrics_summary.csv`,
  3. runs statistical tests (using `scipy`, `statsmodels` or `pingouin`),
  4. generates publication-ready figures and tables.

Suggested workflow:

```bash
# from scripts/analysis_A_hv_evolution
python3 analysis_A_hv_evolution.py   # generate/collect HV series and main figure
python3 postprocess_analysis.py      # new script: compute CSVs, statistics, and supplementary figures
```

Key functions for `postprocess_analysis.py`:

- `compute_convergence_times(df_series) -> DataFrame`
- `compute_metrics(df_series) -> DataFrame`
- `bootstrap_ci(series, metric, n=1000)`
- `run_statistical_tests(summary_df)`

## 13. Compute requirements and time estimates

- Basic processing is light (seconds); bootstrap (e.g., 1000 replicates) may take minutes depending on hardware.
- Recommendation: parallelize bootstrap and per-configuration computations with `joblib` or Python multiprocessing on machines with >=4 cores.

## 14. Deliverables checklist for the manuscript

- [ ] Main figure (2×2) at 300 DPI
- [ ] Supplementary figures (boxplots, AUC bars, heatmaps)
- [ ] Summary table with metrics and statistical tests
- [ ] Postprocessing code (`postprocess_analysis.py` or notebook)
- [ ] Short methodological note (interpolation method, convergence definition, `N_RUNS`)

---

If you prefer, I can implement `postprocess_analysis.py` and produce the first outputs (e.g., `convergence_times.csv` and `metrics_summary.csv`), or I can prepare the Jupyter notebook. Tell me which option you prefer and I will start with that task (I suggest starting with extracting all convergence times and producing `convergence_times.csv`).
