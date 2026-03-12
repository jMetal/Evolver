# Convergence Analysis

This directory contains a lightweight convergence analysis pipeline for the
meta-optimization runs stored in `Results_Evolver` plus the extreme-point runs
stored in `2026.03.11.results`.

## Inputs

The scripts expect experiment folders like:

```text
C:\Users\nicor\Desktop\Results_Evolver\
  RE3D.referenceFronts.1000\
    run1\
      INDICATORS.csv

C:\Users\nicor\Desktop\2026.03.11.results\
  RE3D.extremePointsFronts.1000\
    run1\
      INDICATORS.csv
```

Each `INDICATORS.csv` must contain:

- `Evaluation`
- `SolutionId`
- `EP`
- `HVMinus`

## Outputs

Running `01_convergence.py` generates eight figures:

- `convergence_RE3D_budget_1000.png`
- `convergence_RE3D_budget_3000.png`
- `convergence_RE3D_budget_5000.png`
- `convergence_RE3D_budget_7000.png`
- `convergence_RWA3D_budget_1000.png`
- `convergence_RWA3D_budget_3000.png`
- `convergence_RWA3D_budget_5000.png`
- `convergence_RWA3D_budget_7000.png`

Each figure compares `referenceFronts` vs `extremePointsFronts` for one
family and one budget, with median curves and IQR bands for both `EP` and
`HVMinus`.

Running `02_budget_convergence_profiles.py` generates four separate budget
profiles:

- `budget_convergence_RE3D_referenceFronts.png`
- `budget_convergence_RE3D_extremePointsFronts.png`
- `budget_convergence_RWA3D_referenceFronts.png`
- `budget_convergence_RWA3D_extremePointsFronts.png`

Those figures use budget on the x-axis (`0..7000`) and mark the first budget
that reaches 95% of the total improvement observed between budget `1000` and
the best final budget result.

Running `03_base_hv_convergence.py` generates eight convergence figures using
the base-level hypervolume recovered as `HV = -HVMinus`:

- `base_hv_convergence_RE3D_budget_1000.png`
- `base_hv_convergence_RE3D_budget_3000.png`
- `base_hv_convergence_RE3D_budget_5000.png`
- `base_hv_convergence_RE3D_budget_7000.png`
- `base_hv_convergence_RWA3D_budget_1000.png`
- `base_hv_convergence_RWA3D_budget_3000.png`
- `base_hv_convergence_RWA3D_budget_5000.png`
- `base_hv_convergence_RWA3D_budget_7000.png`

These use meta-evaluations on the x-axis and compare `referenceFronts` vs
`extremePointsFronts` on the y-axis with positive `HV`.

Running `04_base_hv_normalized_convergence.py` generates an additional set of
eight figures with normalized improvement, without replacing the raw-HV ones:

- `base_hv_normalized_convergence_RE3D_budget_1000.png`
- `base_hv_normalized_convergence_RE3D_budget_3000.png`
- `base_hv_normalized_convergence_RE3D_budget_5000.png`
- `base_hv_normalized_convergence_RE3D_budget_7000.png`
- `base_hv_normalized_convergence_RWA3D_budget_1000.png`
- `base_hv_normalized_convergence_RWA3D_budget_3000.png`
- `base_hv_normalized_convergence_RWA3D_budget_5000.png`
- `base_hv_normalized_convergence_RWA3D_budget_7000.png`

They show relative gain from the first checkpoint to the final checkpoint on a
0-to-1 scale, which makes very small absolute changes visible.

Running `05_pooled_base_hv_convergence.py` generates pooled raw and normalized
HV figures, treating `RE3D` and `RWA3D` as one benchmark:

- `base_hv_convergence_pooled_budget_1000.png`
- `base_hv_convergence_pooled_budget_3000.png`
- `base_hv_convergence_pooled_budget_5000.png`
- `base_hv_convergence_pooled_budget_7000.png`
- `base_hv_normalized_convergence_pooled_budget_1000.png`
- `base_hv_normalized_convergence_pooled_budget_3000.png`
- `base_hv_normalized_convergence_pooled_budget_5000.png`
- `base_hv_normalized_convergence_pooled_budget_7000.png`

Running `06_pooled_base_hv_delta_convergence.py` generates a pooled `delta HV`
view for each budget:

- `base_hv_delta_convergence_pooled_budget_1000.png`
- `base_hv_delta_convergence_pooled_budget_3000.png`
- `base_hv_delta_convergence_pooled_budget_5000.png`
- `base_hv_delta_convergence_pooled_budget_7000.png`

These figures show `HV(t) - HV(start)` so tiny raw changes become visible while
keeping the pooled benchmark view.

Running `18_elite_subspace.py` adds an elite-subspace view over final
configurations:

- `tables/elite_subspace/elite_recommendations_all.csv`
- `tables/elite_subspace/elite_numeric_detail_all.csv`
- `tables/elite_subspace/elite_categorical_summary_all.csv`
- `figures/elite_subspace/budget_*/elite_profile_*.png`

These outputs compare the total final configuration cloud against elite sets
(`top 5%`, `10%`, `20%`) for `EP`, `HVMinus`, and the joint score.

Running `19_conditional_rules.py` extracts shallow decision-tree rules with
minimum support/precision thresholds:

- `tables/conditional_rules/conditional_rules_all.csv`
- `tables/conditional_rules/conditional_numeric_windows_all.csv`

Running `20_configuration_trajectory.py` tracks incumbent stabilization during
the meta-optimization:

- `tables/configuration_trajectory/trajectory_parameter_stability_summary_all.csv`
- `tables/configuration_trajectory/trajectory_medoid_entry_summary_all.csv`
- `figures/configuration_trajectory/budget_*/trajectory_*.png`

Running `21_reduced_parameter_spaces.py` synthesizes reduced NSGA-II parameter
spaces and writes derived YAML files under
`src/main/resources/parameterSpaces/reduced/`.

Running `22_compare_reduced_validation.py` compares any validation reruns found
under `scripts/analysis/validation_results/<proposal>/` against the current
baseline results and writes summary tables under
`tables/validation_reduced_space/`.

Running `23_front_type_statistical_comparison.py` adds the explicit
Mann-Whitney/Vargha-Delaney comparison of final HV between
`referenceFronts` and `extremePointsFronts`.

Running `24_categorical_parameter_frequencies.py` summarizes the frequency of
the main categorical choices (`crossover`, `mutation`, `selection`,
`createInitialSolutions`) over final-best configurations.

Running `25_numerical_parameter_distributions.py` compares the distribution of
the main numerical parameters across budgets and front types.

Running `26_parameter_association_tests.py` computes Spearman and
Kruskal-Wallis tests linking final HV with numerical and categorical
configuration parameters.

Running `27_random_forest_predictive_importance.py` trains a predictive random
forest per `family x frontType` and exports MDI, permutation importance, and
cross-validated `R^2`.

Running `28_configuration_clustering.py` fits K-means models to the final-best
configuration clouds, selects `k` by silhouette score, and exports cluster
characterizations.

Running `29_representative_configurations.py` derives one representative
configuration per family and writes it both as tables and as CLI-style
`--key value` files.

Running `30_hv90_convergence.py` measures convergence to 90% of the final HV
improvement and compares early-vs-late configurations.

Running `31_cost_quality_analysis.py` turns the running-best HV into
cost-quality tables and Pareto fronts.

Running `32_cost_scenario_configuration_comparison.py` compares representative
configurations under low-cost scenarios against the high-cost reference
scenario.

The reusable Java runner for those reruns is:

- `org.uma.evolver.analysis.validation.ReducedSpaceValidationRunner`

Useful optional argument for quick smoke tests:

- `--max-training-problems <int>` limits the training set to the first `N`
  problems while keeping the same output layout.

Expected output layout per proposal:

```text
scripts/analysis/validation_results/
  global_referenceFronts/
    RE3D.referenceFronts.1000/
      run1/
        INDICATORS.csv
        CONFIGURATIONS.csv
```

## Run

```bash
python scripts/analysis/01_convergence.py
```

Optional override:

```bash
python scripts/analysis/01_convergence.py --results-root C:\path\to\Results_Evolver
```

By default, the pipeline resolves `referenceFronts` from
`C:\Users\nicor\Desktop\Results_Evolver` and `extremePointsFronts` from
`C:\Users\nicor\Desktop\2026.03.11.results`.
