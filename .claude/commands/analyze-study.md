Analyze the results of an algorithm-comparison study (a jMetal `ExperimentBuilder` study, as
produced by the `*Study` classes in `example/validation`).

Directory to analyze: $ARGUMENTS

The directory should contain `QualityIndicatorSummary.csv` (typically under
`<StudyBaseDir>/<StudyName>/data/`).

**Not this skill:** to analyze a meta-optimizer searching for *configurations* (training output with
`INDICATORS.csv` / `CONFIGURATIONS.csv` / `VAR_CONF.txt`), use `analyze-training`.

## The study output format

A `*Study` runs `ExecuteAlgorithms` + `ComputeQualityIndicators` and produces:

- `data/QualityIndicatorSummary.csv` — the canonical summary, tidy/long format with header
  `Algorithm,Problem,IndicatorName,ExecutionId,IndicatorValue`. One row per
  (algorithm, problem, indicator, independent run).
- `data/<Algorithm>/<Problem>/` — `FUNx.csv`/`VARx.csv` per run, per-indicator files (`HV`, `EP`, …)
  with one value per line, and `BEST_<IND>_FUN`/`MEDIAN_<IND>_FUN` representative fronts.
- `latex/` — mean/median ± deviation tables and a Friedman ranking (if
  `GenerateLatexTablesWithStatistics` ran).
- `R/` — boxplots and Wilcoxon rank-sum tables (if the R generators ran).

## Steps

### 1. Locate and load the summary
Find `QualityIndicatorSummary.csv` (search the given directory recursively). Load it — a short
pandas snippet via Bash is ideal:
```python
import pandas as pd
df = pd.read_csv(".../QualityIndicatorSummary.csv")
(df.groupby(["Problem", "IndicatorName", "Algorithm"])["IndicatorValue"]
   .agg(["median", "std", "min", "max", "count"]))
```

### 2. Per-(problem, indicator) comparison
For each problem and indicator, build a table of algorithms with median ± IQR (or std) and mark the
winner. **Respect indicator direction:** `HV`/`NHV` → higher is better; `EP`, `IGD`, `IGD+` → lower
is better.

### 3. Per-problem winners and overall ranking
Summarize which algorithm wins each problem (per primary indicator) and give an overall ranking. If
`latex/` contains a Friedman table, report its ranking; otherwise compute a mean rank across problems.

### 4. Surface companion artifacts
Point to the `BEST_<IND>_FUN`/`MEDIAN_<IND>_FUN` fronts (for plotting each algorithm's representative
front — `plot_front.py` can render them) and to the `latex/` (statistical tables, Friedman) and `R/`
(boxplots, Wilcoxon) directories when present.

### 5. Detect anomalies
Flag inconsistent run counts across algorithms/problems, missing combinations, `NaN`/`inf` indicator
values, or one algorithm dominating suspiciously.

### 6. Executive summary
Close with 3–5 lines: which algorithm(s) are best overall and per indicator, where differences are
clear vs marginal, and whether a statistical test confirms them.

References: jMetal docs `experimentation.rst` §Quality Indicator Computing; a `*Study` example such
as `REMOEADStudy.java` in `example/validation`.
