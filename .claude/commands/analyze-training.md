Analyze the results of a meta-optimization (training) run in the given directory.

Directory to analyze: $ARGUMENTS

If no directory is specified, use the most recent training output under `experimentation/`.

**Not this skill:** to compare several algorithms on a benchmark (a validation *study* producing
`QualityIndicatorSummary.csv`), use `analyze-study`. This skill is for the output of a meta-optimizer
that searches for base-algorithm *configurations*.

## The meta-optimization output format

A training run (written by `ConsolidatedOutputResults`) produces, in the output directory:

- `METADATA.txt` — human-readable run context: meta-optimizer (algorithm, max evals, pop size,
  cores), base-level algorithm (name, pop/swarm size, max evals, evaluation strategy, parameter
  space YAML, number of optimizable parameters), training set (problem family + problems), and the
  quality indicators.
- `INDICATORS.csv` — header `Evaluation,SolutionId,<Indicator1>,<Indicator2>,…`. One row per
  non-dominated configuration in the snapshot taken at meta-evaluation `Evaluation`; the indicator
  columns are that configuration's values on the training set.
- `CONFIGURATIONS.csv` — header `Evaluation,SolutionId,<param1>,<param2>,…`. Same rows as
  `INDICATORS.csv`, with decoded parameter values (`NaN` for parameters inactive under the
  conditional structure).
- `VAR_CONF.txt` — per meta-evaluation a `# Evaluation: N` block, then one line per non-dominated
  configuration: `<IND>=<value> … | --param value --param value …` (indicator values and the
  ready-to-run CLI configuration string).

Rows are keyed by `(Evaluation, SolutionId)`, so the three result files can be joined on that pair.
Indicators are **minimized** by the meta-optimizer (e.g. normalized-hypervolume-minus, epsilon),
so *lower is better*.

## Steps

### 1. Read the run context
Read `METADATA.txt` and state up front the meta-optimizer, base algorithm, training problems,
indicators, and budgets.

### 2. Pick the target meta-evaluation
Quality is usually compared at a specific meta-evaluation (often the last/maximum, e.g. 2000).
Default to the maximum `Evaluation` present in `INDICATORS.csv`; mention which one you used.

### 3. Find the best configuration(s)
For the target meta-evaluation, find the configuration with the best (minimum) primary-indicator
value. Read its ready-to-run CLI string from `VAR_CONF.txt` (same `(Evaluation, SolutionId)`), and
show the most relevant parameters (operators, population/offspring sizes, probabilities). With
multiple indicators, report the non-dominated trade-offs.

### 4. Convergence across meta-evaluations
Show how the best primary-indicator value evolves across `Evaluation` values (a small table or the
trend), to judge whether the meta-search had converged.

### 5. Parameter patterns
Across the best configurations (top-k at the target evaluation, or across evaluations), note
recurring choices (a dominant crossover/mutation operator, typical ranges) using `CONFIGURATIONS.csv`.

### 6. Detect anomalies
Flag empty/incomplete files, unexpected `NaN` patterns, missing meta-evaluations, or outliers.

### 7. Executive summary
Close with 3–5 lines: the best configuration and its indicator value, whether the search converged,
clear parameter patterns, and what to investigate next.

Use Markdown tables, e.g.:

| Evaluation | Best `<indicator>` | Configuration (key params) |
|---|---|---|
