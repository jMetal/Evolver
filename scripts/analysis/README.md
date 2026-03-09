# Convergence Analysis

This directory contains a lightweight convergence analysis pipeline for the
meta-optimization runs stored in `Results_Evolver`.

## Inputs

The script expects experiment folders like:

```text
C:\Users\nicor\Desktop\Results_Evolver\
  RE3D.referenceFronts.1000\
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

Each figure compares `referenceFronts` vs `estimatedReferenceFronts` for one
family and one budget, with median curves and IQR bands for both `EP` and
`HVMinus`.

Running `02_budget_convergence_profiles.py` generates four separate budget
profiles:

- `budget_convergence_RE3D_referenceFronts.png`
- `budget_convergence_RE3D_estimatedReferenceFronts.png`
- `budget_convergence_RWA3D_referenceFronts.png`
- `budget_convergence_RWA3D_estimatedReferenceFronts.png`

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
`estimatedReferenceFronts` on the y-axis with positive `HV`.

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

## Run

```bash
python scripts/analysis/01_convergence.py
```

Optional:

```bash
python scripts/analysis/01_convergence.py --results-root C:\path\to\Results_Evolver
```
