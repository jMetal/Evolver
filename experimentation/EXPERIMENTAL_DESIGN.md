# Analysis of Meta-Optimization Experiments with Evolver

This document describes all the analyses to be performed on the results of
meta-optimization experiments. For each analysis, the description, the technical details
required to implement it from scratch (self-contained, no prior context assumed), and the
expected outcomes are provided.

**Goal.** The goal of these experiments is to automatically configure a base-level multi-objective optimization algorithm — NSGA-II — so that it performs well on a given class of problems. The configuration space is defined by the file `NSGAIIDouble.yaml`, which specifies 32 configurable parameters covering crossover operators, mutation operators, selection mechanisms, population sizing, and initialization strategies. The experimentation is divided into two phases: **training** and **validation**.

**Training phase.** In this phase, the goal is to find the best algorithmic configurations using a subset of representative problems. Two training sets of multi-objective problems are used: the RE benchmark suite with three objectives (RE3D, comprising 7 problems: RE31–RE37) and the Real-World Applications benchmark with three objectives (RWA3D, comprising 6 problems: RWA2–RWA7).

**Validation phase.** The best configurations derived from the training phase (the "representative" configurations, as extracted in Section E) are then validated on the full, original benchmark suites. This includes the full RE benchmark (RE 2D, RE 3D, and RE High-D) and the full RWA benchmark (RWA1–RWA10). This phase verifies whether configurations tuned exclusively on 3-objective instances generalize well to the entire problem family (as implemented in `RERWAStudy.java`).

**Meta-optimization approach.** To find good configurations during the **training** phase, a meta-optimization approach is used: an asynchronous parallel NSGA-II (AsyncNSGA-II) acts as the meta-optimizer, treating the 32 configuration parameters as decision variables. At the meta-level, each candidate configuration is evaluated by running the base-level NSGA-II on all problems in the chosen training set. For each problem, the algorithm is executed multiple independent times, and the median quality indicator value (HV, EP) across these runs is computed. The final performance assigned to the configuration at the meta-level is the arithmetic mean of these median values across all problems in the set. The meta-optimizer runs with a population size of 50, up to 3000 meta-level evaluations, and 16 parallel cores.

**Evaluation budget.** Running the base-level algorithm requires a maximum number of function evaluations, referred to as the *budget*. Experiments are repeated for four budget values: 1000, 3000, 5000, and 7000 base-level evaluations. A higher budget gives the base-level algorithm more time to converge but increases the cost of each meta-level evaluation.

**Quality indicators and reference fronts.** The primary indicator for assessing base-level performance is the hypervolume (HV), which measures both convergence and diversity of the resulting approximation front with respect to a *reference front* — an approximation to the true Pareto-optimal front of the problem. Because objective scales can vary significantly within and across problems, the objective values of both the approximation front and the reference front are normalized into the [0, 1] range using the minimum and maximum values of the reference front before computing any indicator. A reference point of [1.1, 1.1, 1.1] is used to compute the HV of the normalized fronts. The quality of the reference front directly affects this normalization and the resulting HV values. Since Evolver minimizes objectives, HV is negated to produce HVMinus (= −HV), which is one of the two meta-level objectives to minimize.

At the beginning of the meta-search, many configurations are poor and produce fronts that are dominated by the reference point used to compute HV, yielding HV = 0. This creates a *zero-indicator plateau*: all solutions are equally ranked by HVMinus, stalling the search. To break this plateau, the additive epsilon indicator (EP) is used as a helper objective. Since EP measures how much the approximation front must be shifted to dominate the reference front, a solution with HV = 0 but lower EP still dominates another solution with HV = 0 and higher EP, allowing the meta-search to progress. The final goal remains finding configurations that maximize HV; EP is only a navigational aid during optimization and is not a target metric.

**Reference front variants.** A critical research question in this study is whether the meta-optimizer requires dense, high-quality reference fronts—which are computationally prohibitive or impossible to obtain in real-world scenarios—to find good algorithmic configurations. To simulate a realistic environment with expensive fitness evaluations, two sets of reference fronts were used across the experiments:
1. **Estimated Pareto fronts (`referenceFronts`, directory `experimentation/training/referenceFronts`):** The standard approach, using dense reference fronts provided by the problem authors (created by aggregating the best solutions from multiple algorithms). While ideal, these are unrealistic to possess a priori for expensive real-world problems.
2. **Extreme-point-based fronts (`extremePointsFronts`, directory `experimentation/training/extremePoints`):** A pragmatic approach containing only the extreme points (the hypothetical single-objective optimum for each objective). In a real-world context, domain experts can often estimate or calculate these extremes, which is sufficient to establish the single reference point required by the HV indicator.

Comparing results across these two sets allows us to study if there are significant differences in the quality of the derived configurations when training with complete reference fronts versus using only extreme objective values.

**Independent runs.** Each (problem, budget, dataset) combination was executed 30 independent times to account for the stochasticity of the meta-optimizer. This yields 2 problems × 4 budgets × 2 datasets × 30 runs = 480 runs in total.

---

## Data Context

*Note: This data context describes exclusively the logs produced during the **Training Phase** (meta-optimization). The logs produced during the subsequent Validation Phase (e.g., using `RERWAStudy.java`) follow a different structure and are not the subject of these analyses.*

### Experiment structure

| Dataset key | Results directory | Reference front type |
|---|---|---|
| `referenceFronts` | `experimentation/training/referenceFronts` | Estimated Pareto fronts |
| `extremePointsFronts` | `experimentation/training/extremePoints` | Extreme-point-based fronts |

**Experiments:** 2 problems (RE3D, RWA3D) × 4 budgets (1000, 3000, 5000, 7000) × 2 datasets × 30 runs = **480 runs total**.

### Directory layout

Inside each results directory, the runs are grouped in folders named `<problem>.<dataset>.<budget>`. Inside these, each of the 30 independent runs has its own folder `run<N>`, containing the `VAR_CONF.txt` file:
```
<results_dir>/<problem>.<dataset>.<budget>/run<N>/VAR_CONF.txt
```
*(Example: `experimentation/training/referenceFronts/RE3D.referenceFronts.1000/run1/VAR_CONF.txt`)*

### File format (`VAR_CONF.txt`)

Each file contains a sequence of evaluation checkpoints. A checkpoint begins with a
header line:
```
# Evaluation: <N>
```
followed by one or more data lines, each with the format:
```
EP=<float_or_scientific> HVMinus=<negative_float> | --param1 val1 --param2 val2 ...
```
- `EP` is the Epsilon quality indicator value (can be float or scientific notation).
- `HVMinus` is the negated hypervolume (always ≤ 0); the actual HV is `-HVMinus`.
- The best HV at a checkpoint is `max(-HVMinus)` over all lines in that checkpoint block.
- Parameters after `|` are space-separated pairs of `--key value` forming the configuration of the solution. They are continuous on a single line.

### Python environment

- Interpreter: `/opt/anaconda3/envs/Evolver/bin/python3` (Python 3.11).
  The system `python3` has a numpy/sklearn binary incompatibility and must not be used.
- Required libraries: `pandas`, `numpy`, `matplotlib` (Agg backend for headless rendering),
  `scipy` (for statistical tests), `scikit-learn` ≥ 1.8.0 (for analyses D4 and D5 only).
- When generating plots without a display server, set `MPLBACKEND=Agg` or call
  `matplotlib.use('Agg')` before importing `pyplot`.

---

## Defined Experiments

### A. HV Evolution Analysis

#### Description
Track how the best hypervolume (HV) found by the meta-optimizer evolves across successive
evaluation checkpoints for each experiment. The goal is to visualize the convergence
behaviour and compare experiments by problem and evaluation budget.

#### Technical details

**Parsing:**
- For each `VAR_CONF.txt`, iterate over checkpoint blocks (lines starting with
  `# Evaluation: N`). For each block, compute `HV = max(-HVMinus)` over all data lines.
- Apply a running maximum so the per-run HV curve is monotonically non-decreasing.

**Aggregation:**
- Sample 30 evenly-spaced checkpoint values from eval 100 to the maximum checkpoint
  (e.g., 3000).
- Because AsyncNSGA-II is asynchronous, checkpoints do not occur at the exact same evaluation counts across all 30 runs. To align them, use a **forward-fill interpolation** (last observation carried forward): for each run and each sampled checkpoint $N$, use the `HV` of the largest evaluation count $\le N$.
- For each (problem, budget) combination, collect the aligned HV at each sampled checkpoint
  across all 30 runs and compute the **median**.

**Visualization:**
- Produce one figure per dataset (`referenceFronts`, `extremePointsFronts`).
- Each figure is a 2-row × 4-column grid: rows = problem (RE3D, RWA3D), columns = budget
  (1000, 3000, 5000, 7000).
- Each subplot shows median HV vs. evaluation number as a line plot.
- Use consistent colors/styles across subplots.

#### Expected outcomes
- A set of two figures (one per dataset), each showing 8 HV evolution curves.
- The curves should reveal how quickly the meta-optimizer converges and whether higher
  budgets yield better final HV values.
- A tabular summary of median HV per checkpoint per (problem, budget, dataset) is also
  useful for further analyses.

---

### B. Statistical Comparison: referenceFronts vs. extremePointsFronts

#### Description
Test whether using `extremePointsFronts` as reference fronts produces statistically
different final HV values compared to `referenceFronts`, for each (problem × budget)
combination. This determines whether the choice of reference front type has a practical
impact on the optimization outcome.

#### Technical details

**Sample definition:**
- For each run, extract the best HV at the last available checkpoint (the run's final HV).
- This yields two independent groups of 30 values each (one per dataset) for every
  (problem, budget) pair — 8 comparisons in total.

**Statistical test:**
- Apply a two-sided Mann-Whitney U test (non-parametric, unpaired) at significance level
  α = 0.05.
- Report the U statistic and p-value for each comparison.

**Effect size:**
- Compute the Vargha-Delaney A statistic: $A = \frac{U}{n_1 \cdot n_2}$ where $n_1 = n_2 = 30$.
- Interpretation thresholds: A > 0.56 → small effect, A > 0.64 → medium, A > 0.71 → large.
  Values near 0.5 indicate no practical difference.

**Output:**
- A table with columns: problem, budget, U, p-value, A, significant (yes/no).

#### Expected outcomes
- A table of 8 rows (2 problems × 4 budgets) with p-values and effect sizes.
- Identifies which (problem, budget) combinations show statistically and practically
  significant differences between the two datasets.

---

### C. Convergence Analysis

#### Description
Define a per-run convergence point as the first evaluation checkpoint at which the run
has recovered 95% of its total HV improvement. Visualize this threshold on the HV
evolution curves and report its distribution across experiments.

#### Technical details

**Convergence threshold per run:**
- For each run, let `HV_min` and `HV_max` be the minimum and maximum HV across all
  checkpoints.
- The convergence threshold is: `HV_threshold = HV_min + 0.95 × (HV_max − HV_min)`.
- The convergence evaluation `eval_conv` is the smallest checkpoint N such that
  `HV(N) ≥ HV_threshold`.

**Aggregation:**
- Compute `eval_conv` for each run, then take the **median** across the 30 runs per
  (problem, budget, dataset) combination.

**Visualization:**
- Produce a 2×2 figure grid: rows = dataset, columns = problem.
- Each subplot overlays one median-HV curve per budget (4 curves), with a vertical
  dashed line at the median `eval_conv` for each budget.
- Use a consistent color-per-budget scheme.

#### Expected outcomes
- A figure showing where convergence typically occurs for each experiment.
- A table of median `eval_conv` per (problem, budget, dataset).
- Insight into whether a lower evaluation budget than the maximum is sufficient for
  convergence.

---

### D. Configuration Analysis

All sub-analyses in this section work on a common data structure: the best configuration
of each run at the **last available checkpoint**. This is the configuration on the data
line with maximum `-HVMinus` in the last checkpoint block of each `VAR_CONF.txt`.

**Parsing the configuration string:**
- Split the part after `|` by whitespace into `--key value` pairs.
- Store each configuration as a flat dict with keys = parameter names (without `--`) and
  values = strings.
- Add metadata fields: `HV` (float), `run` (int), `budget` (int), `problem` (str),
  `dataset` (str).
- Build a `pandas.DataFrame` from the list of dicts.
- Cast numeric columns with `pd.to_numeric(df[col], errors='coerce')` before any
  numerical operation.

**Categorical parameters of interest:**
`crossover`, `mutation`, `selection`, `createInitialSolutions`

**Numerical parameters of interest:**
`crossoverProbability`, `mutationProbabilityFactor`, `populationSizeWithArchive`,
`offspringPopulationSize`, `levyFlightMutationBeta`, `levyFlightMutationStepSize`,
`blxAlphaCrossoverAlpha`, `blxAlphaBetaCrossoverAlpha`, `blxAlphaBetaCrossoverBeta`,
`powerLawMutationDelta`, `boltzmannTemperature`

---

#### D1. Categorical Parameter Frequencies

#### Description
For each categorical parameter, compute the frequency (as a percentage) of each possible
value across all runs, grouped by budget and dataset. This reveals which operators the
meta-optimizer consistently selects.

#### Technical details

- Group the DataFrame by `(problem, dataset, budget)`.
- For each group and each categorical parameter, compute `value_counts(normalize=True) × 100`.
- Produce one figure per problem (RE3D, RWA3D).
- Each figure contains a grid of bar charts: one subplot per categorical parameter,
  with one bar group per budget, bars colored by dataset.
- Sort bars by frequency (descending) for readability.

#### Expected outcomes
- Two figures (one per problem), each with 4 subplots (one per categorical parameter).
- Each subplot shows which operator values are most frequently selected and whether the
  frequency distribution changes with budget or dataset.
- Identification of the modal (most frequent) operator value per parameter per problem.

---

#### D2. Numerical Parameter Distributions

#### Description
Compare the distribution of numerical parameter values between the two datasets
(`referenceFronts` and `extremePointsFronts`) for each budget, to assess whether the
choice of reference front influences the numerical configuration returned.

#### Technical details

- For each problem and numerical parameter, produce a panel of boxplots.
- Group by `(dataset, budget)`: each budget gets two boxes side-by-side (one per dataset),
  using different colors.
- For parameters whose presence depends on the selected categorical operator (e.g.,
  `levyFlightMutationBeta` only appears when `mutation=levyFlight`), include only the rows
  where that parameter is not NaN.
- Produce one figure per problem.

#### Expected outcomes
- Two figures (one per problem), each with one subplot per numerical parameter.
- Each subplot shows the spread and central tendency of that parameter across budgets and
  datasets.
- Allows assessing whether distributions shift with budget and whether the two datasets
  lead to similar numerical configurations.

---

#### D3. Spearman Correlation and Kruskal-Wallis Tests

#### Description
Quantify the statistical relationship between each parameter (numerical or categorical)
and the final HV, to identify which parameters matter most for performance.

#### Technical details

**Spearman correlation (numerical parameters):**
- For each (problem, dataset) combination, compute Spearman's ρ between each numerical
  parameter and HV using `scipy.stats.spearmanr`.
- Report ρ and its p-value; mark significant correlations (p < 0.05).
- Visualize as a horizontal bar chart of ρ values, colored by significance.

**Kruskal-Wallis H-test (categorical parameters):**
- For each categorical parameter and each (problem, dataset) combination, group HV values
  by the parameter's value and apply `scipy.stats.kruskal`.
- Report the H statistic and p-value.
- Visualize as a bar chart of H values with significance markers.

**Output:** one figure per problem with two panels (Spearman ρ for numericals,
Kruskal-Wallis H for categoricals).

#### Expected outcomes
- Per-problem figures showing which parameters have a statistically significant
  relationship with HV.
- Identification of the most influential parameters (high |ρ| or high H) and
  uninformative parameters (p not significant).

---

#### D4. Random Forest Feature Importance

#### Description
Train a Random Forest regressor to predict HV from all configuration parameters.
Use two importance measures — MDI and permutation importance — to rank parameters by
their contribution to predictive accuracy.

#### Technical details

**Preprocessing:**
- Encode categorical parameters with `sklearn.preprocessing.OrdinalEncoder`.
- Scale numerical parameters with `sklearn.preprocessing.StandardScaler`.
- Handle missing values (parameters absent for a given operator): impute with a sentinel
  value (e.g., -1) or use only rows where all features are present for a given operator
  combination.

**Model:**
- Train `sklearn.ensemble.RandomForestRegressor` (e.g., 200 trees) per
  (problem, dataset) combination with an 80/20 train/test split.
- Compute MDI importance from `rf.feature_importances_`.
- Compute permutation importance on the test set with
  `sklearn.inspection.permutation_importance`.
- Compute cross-validated R² (5-fold) with `sklearn.model_selection.cross_val_score`
  to assess overall model quality.

**Visualization:**
- Horizontal bar charts of MDI and permutation importance side by side, sorted by MDI.
- One figure per problem.

#### Expected outcomes
- Per-problem figures ranking all parameters by two importance metrics.
- A table of R² values (cross-validated) indicating how well the RF model predicts HV
  from configuration parameters.
- Note: a low or negative R² indicates that many equally-good configurations exist
  (flat fitness landscape), but importance rankings can still be informative.

---

#### D5. K-means Clustering

#### Description
Group configurations into clusters to discover whether the meta-optimizer discovers
structurally different but equally performant configurations, and to characterize
each cluster by its dominant parameter values and HV distribution.

#### Technical details

**Preprocessing:**
- Encode categorical parameters with `OrdinalEncoder` and scale numerical parameters
  with `StandardScaler` (same as D4).

**Cluster selection:**
- Run K-means for k ∈ {2, …, 7}.
- Select the k that maximises the average silhouette score
  (`sklearn.metrics.silhouette_score`).
- Plot silhouette score vs. k to justify the choice.

**Visualization (three-panel figure per problem):**
1. PCA projection (2 components) of configurations, colored by cluster label.
2. Same PCA projection, colored by HV value (continuous scale).
3. Boxplot of HV distribution per cluster.

**Cluster characterization:**
- For each cluster, report the mode of each categorical parameter and the median of each
  numerical parameter.

#### Expected outcomes
- Per-problem figures with silhouette curve and three-panel cluster visualization.
- A table characterizing each cluster by dominant operators and numerical parameter medians.
- Insight into whether clusters differ meaningfully in HV or represent equally-good
  but structurally distinct configurations.

---

### E. Representative Configurations

#### Description
Derive a single "representative" configuration per problem that summarizes the most
common configuration returned by the meta-optimizer across all budgets and datasets.

#### Technical details

**Procedure:**
1. Compute the **mode** of each categorical parameter (`crossover`, `mutation`,
   `selection`, `createInitialSolutions`) over all runs (combining both datasets and all
   budgets).
2. Filter the DataFrame to rows that match the complete modal categorical combination.
3. Compute the **median** of each numerical parameter over those filtered rows.
4. Report the number of runs matching the modal combination (as a coverage percentage).

**Output:**
- A configuration string in the `--key value` format (one argument pair per line) for
  each problem, ready to be passed to the base-level algorithm.
- The median HV of the matching runs.

#### Expected outcomes
- One representative configuration per problem (RE3D, RWA3D), with all parameters
  specified.
- Coverage percentage indicating how representative the modal combination is across runs.
- This configuration can be used as a starting point or default setting for the
  base-level algorithm on that problem class.

---

### F. Convergence to 90% of HV

#### Description
Analyse how quickly the meta-optimizer reaches 90% of each run's HV improvement, and
compare the configuration selected at that early convergence point against the
configuration found at the end of the run (budget=7000).

#### Technical details

**Convergence point definition:**
- For each run, compute `HV_threshold = HV_min + 0.90 × (HV_max − HV_min)`.
- `eval_90` = first checkpoint where `HV(N) ≥ HV_threshold`.

**Distribution analysis:**
- Pool `eval_90` values across all budgets and datasets for each problem.
- Report percentiles: P10, P25, median, P75, P90.
- Report the cumulative distribution: at which evaluation threshold have X% of runs
  converged (e.g., X = 50%, 90%, 99%)?

**Configuration comparison (eval_90 vs. budget=7000):**
- For each run, extract the best configuration at the checkpoint closest to `eval_90`
  and the configuration at the last checkpoint with budget=7000.
- Compare the modal categorical parameters and median numerical parameters between
  the two groups.
- Report which parameters are consistent between early convergence and end-of-run.

#### Expected outcomes
- A table of `eval_90` percentiles per problem.
- A cumulative distribution table/plot showing the fraction of runs converged by each
  evaluation milestone.
- A parameter-by-parameter comparison table (early vs. late configurations) highlighting
  which parameters stabilize early and which continue to shift.

---

### G. Cost-Quality Analysis

#### Description
Express the quality of each (problem, budget, checkpoint) combination as a fraction of
a reference HV, and relate it to total computational cost. This answers: *"What is the
cheapest (budget, meta_eval) combination that achieves at least X% of the reference HV?"*

#### Technical details

**Total computational cost:**
- Defined as `budget × meta_eval` = total number of base-level solution evaluations consumed.

**Reference HV:**
- Median HV across all runs at budget=7000, last checkpoint (pooling both datasets),
  computed separately per problem.

**Per-combination HV:**
- For each (problem, dataset, budget) and each sampled checkpoint `meta_eval`:
  - Compute the best HV seen so far in each run (monotonically non-decreasing).
  - Aggregate as median over 30 runs.
  - Express as `100 × median_HV / reference_HV` (percentage of reference).

**Visualization:**
- Heatmap or table: rows = budget, columns = meta_eval checkpoint, cell = % of reference HV.
- One table per problem (or combine both if values are similar).

**Cost-to-quality table:**
- For a set of representative combinations, report: total cost, % absolute HV, cost as
  fraction of the reference combination cost.

#### Expected outcomes
- A per-problem table showing % of reference HV for each (budget, meta_eval) combination.
- Identification of the minimum-cost combination that reaches a given quality threshold
  (e.g., 99%, 99.9%).
- A cost-quality Pareto front across combinations.

---

### H. Cost Scenario × Configuration Comparison

#### Description
Compare the representative configurations (modal categoricals + median numericals)
extracted under three computational scenarios against a reference configuration (obtained
with the highest budget and all evaluations). This assesses how stable the configuration
recommendation is across different cost budgets.

#### Technical details

**Scenarios:**

| Scenario | Budgets used | meta_eval cap | Total cost |
|---|---|---|---|
| A | 1000 | ≤ 1000 | 1,000,000 |
| B | 3000 | ≤ 1000 | 3,000,000 |
| C (reference) | 7000 | all | up to 21,000,000 |

For each scenario, apply the same representative-configuration procedure as in §E
(mode of categoricals, median of numericals over matching runs).

**Comparison:**
- Report whether each categorical parameter matches between each scenario and the
  reference (scenario C), with a match count (e.g., 3/4 categorical parameters agree).
- For numerical parameters, report the % difference between each scenario and scenario C.
- Identify which parameters are stable across scenarios (invariant to budget) and which
  are sensitive.

#### Expected outcomes
- A table per problem: rows = scenarios (A, B, C), columns = categorical parameters,
  cells = modal value and whether it matches scenario C.
- A table per problem: rows = scenarios, columns = numerical parameters, cells = median
  value and % difference vs. scenario C.
- Identification of the parameters that converge quickly (stable from low budget) vs.
  those that require high budget to stabilize.

---

## Environment

- **Interpreter:** `/opt/anaconda3/envs/Evolver/bin/python3` (Python 3.11).
- **Libraries:** `pandas`, `numpy`, `scipy`, `matplotlib` (all analyses);
  `scikit-learn` ≥ 1.8.0 (analyses D4 and D5 only).
- **Headless rendering:** set `MPLBACKEND=Agg` or call `matplotlib.use('Agg')` before
  importing `pyplot` when running without a display server.
