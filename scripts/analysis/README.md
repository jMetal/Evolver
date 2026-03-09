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

## Run

```bash
python scripts/analysis/01_convergence.py
```

Optional:

```bash
python scripts/analysis/01_convergence.py --results-root C:\path\to\Results_Evolver
```
