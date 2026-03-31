# RQ4 Validation Runbook

## Agent Quick Start

If you are delegating this task to an automated agent such as GPT-5.4, do not rely on "read the README" alone. Give the agent an explicit execution goal.

Recommended prompt:

```text
Read experiments/rq4_validation/README.md and execute the RQ4 validation campaign on this server.
Use the wrapper experiments/rq4_validation/run_rq4_validation.ps1.
Run the full validation for RE and RWA, include ablations, use all available CPU cores minus one, and save outputs under experiments/rq4_validation/results/.
Do not compile the paper on the server unless I explicitly ask for it.
When the run finishes, verify that the expected QualityIndicatorSummary.csv files exist and report which files I should copy back to my local machine.
```

Default agent behavior for this runbook:

- Read this file before executing anything.
- Prefer the wrapper `experiments/rq4_validation/run_rq4_validation.ps1`.
- Use `Suite=all`, `CompileProject=true`, `RunAblations=true`, and `Cores=-1` unless the user says otherwise.
- Treat manuscript compilation as optional and disabled by default on remote servers.
- Leave all raw results in `experiments/rq4_validation/results/` and all generated summaries in `experiments/rq4_validation/generated/`.
- Report the output locations and minimal post-run checks.

This document describes how to execute the compact `RQ4` validation study on an external server.

It is intended to be as self-contained as possible and focuses on:

1. Running the full-suite validation for `RE` and `RWA`.
2. Running the compact real ablations.
3. Regenerating the `RQ4` CSV/figure artifacts.
4. Optionally compiling the manuscript afterwards.

The commands below assume you start from the repository root.

## 1. What This Produces

The run creates the validation bundles expected by the manuscript extension:

- `experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRE`
- `experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRWA`
- `experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRE-Ablation-Complete_RE3D`
- `experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRE-Ablation-Extreme_RE3D`
- `experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRWA-Ablation-Complete_RWA3D`
- `experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRWA-Ablation-Extreme_RWA3D`

Each bundle contains:

- `QualityIndicatorSummary.csv`
- `metadata_algorithms.csv`
- `metadata_problem_splits.csv`
- `metadata_run_configuration.csv`
- jMetal indicator files and BEST/MEDIAN FUN/VAR outputs under `data/`

The Python post-processing scripts then generate:

- `experiments/rq4_validation/generated/validation_summary_rq4.csv`
- `experiments/rq4_validation/generated/validation_pairwise_rq4.csv`
- `experiments/rq4_validation/generated/validation_transfer_delta_rq4.csv`
- `experiments/rq4_validation/generated/validation_wtl_rq4.tex`
- `experiments/rq4_validation/generated/validation_wtl_rq4.png`
- `experiments/rq4_validation/generated/validation_cd_rq4.csv`
- `experiments/rq4_validation/generated/validation_ablation_rq4.csv`
- `experiments/rq4_validation/generated/validation_summary_rq4.png`
- `experiments/rq4_validation/generated/validation_cd_rq4.png`
- `experiments/rq4_validation/generated/validation_transfer_delta_rq4.png`
- `experiments/rq4_validation/generated/validation_ablation_rq4.png`

## 2. Prerequisites

Minimum requirements for the server:

- Java 21
- Maven 3.9+

If you also want to generate the `RQ4` figures/tables on the server:

- Python 3.10+
- Python packages: `numpy`, `pandas`, `matplotlib`, `scipy`

If you also want to compile the full manuscript on the server:

- a working LaTeX toolchain (`latexmk` recommended)
- the two canonical training bundles already available in the locations expected by the other paper scripts

If the server does **not** have the training bundles used by `RQ1` to `RQ3`, do **not** compile the full manuscript there. In that case, run only the `RQ4` experiments on the server, then copy `experiments/rq4_validation/results/representative-configs/` back to your local machine and regenerate the paper artifacts locally.

## 3. Recommended Execution Order

Recommended order for a full `RQ4` campaign:

1. Compile the project.
2. Build the runtime classpath file.
3. Run `RE` validation.
4. Run `RWA` validation.
5. Run the four compact ablation bundles.
6. Regenerate the `RQ4` summary/ablation figures and tables.
7. Optionally compile the full manuscript.

Running ablations for all four representative candidates is the safest option because the ablation figure later selects the in-suite winner automatically and then looks for the corresponding ablation bundle.

## 4. Recommended Server Workflow

For long jobs, use `tmux` or `screen`.

Example:

```bash
tmux new -s rq4_validation
```

Everything below can then be run inside that session.

## 5. Manual Bash Commands

This is the most portable route for a Linux/macOS server.

### 5.1 Compile the Project

```bash
mvn -DskipTests compile
```

### 5.2 Build the Runtime Classpath File

```bash
mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -Dmdep.outputFile=target/runtime-classpath.txt
```

### 5.3 Export the Java Classpath

```bash
export CP="target/classes:$(cat target/runtime-classpath.txt)"
```

### 5.4 Run RE Validation

```bash
java -cp "$CP" org.uma.evolver.example.validation.RepresentativeConfigurationValidationStudy \
  --suite re \
  --output-dir experiments/rq4_validation/results/representative-configs \
  --cores -1 \
  --runs 30 \
  --run-algorithms
```

### 5.5 Run RWA Validation

```bash
java -cp "$CP" org.uma.evolver.example.validation.RepresentativeConfigurationValidationStudy \
  --suite rwa \
  --output-dir experiments/rq4_validation/results/representative-configs \
  --cores -1 \
  --runs 30 \
  --run-algorithms
```

### 5.6 Run RE Ablations

```bash
java -cp "$CP" org.uma.evolver.example.validation.RepresentativeConfigurationValidationStudy \
  --suite re \
  --output-dir experiments/rq4_validation/results/representative-configs \
  --cores -1 \
  --runs 30 \
  --run-algorithms \
  --ablation-base Complete-RE3D
```

```bash
java -cp "$CP" org.uma.evolver.example.validation.RepresentativeConfigurationValidationStudy \
  --suite re \
  --output-dir experiments/rq4_validation/results/representative-configs \
  --cores -1 \
  --runs 30 \
  --run-algorithms \
  --ablation-base Extreme-RE3D
```

### 5.7 Run RWA Ablations

```bash
java -cp "$CP" org.uma.evolver.example.validation.RepresentativeConfigurationValidationStudy \
  --suite rwa \
  --output-dir experiments/rq4_validation/results/representative-configs \
  --cores -1 \
  --runs 30 \
  --run-algorithms \
  --ablation-base Complete-RWA3D
```

```bash
java -cp "$CP" org.uma.evolver.example.validation.RepresentativeConfigurationValidationStudy \
  --suite rwa \
  --output-dir experiments/rq4_validation/results/representative-configs \
  --cores -1 \
  --runs 30 \
  --run-algorithms \
  --ablation-base Extreme-RWA3D
```

### 5.8 Regenerate the RQ4 Artifacts

These post-processing scripts only depend on the new validation bundles.

```bash
python experiments/rq4_validation/generate_validation_summary.py
python experiments/rq4_validation/generate_validation_cd_diagram.py
python experiments/rq4_validation/generate_validation_transfer_delta.py
python experiments/rq4_validation/generate_validation_ablation.py
```

### 5.9 Optional: Compile the Full Manuscript

Only do this if the server already has the two canonical training bundles required by the `RQ1` to `RQ3` scripts.

If you also want to refresh the local manuscript assets before compiling, run:

```bash
python paper/scripts/07_regenerate_manuscript_assets.py
```

```bash
python paper/scripts/08_compile_manuscript_pdf.py --no-sync
```

## 6. PowerShell Wrapper Alternative

If the server has PowerShell 7 (`pwsh`), you can use the wrapper script instead.

The wrapper:

- compiles the project if requested
- builds the runtime classpath if needed
- runs the Java validation main
- optionally runs all four ablations
- regenerates the `RQ4` figures
- optionally compiles the manuscript

### 6.1 Dry Run

Use this first to print the exact commands without executing them.

```bash
pwsh -File experiments/rq4_validation/run_rq4_validation.ps1 \
  -Suite all \
  -RunAblations \
  -CompilePaper \
  -DryRun
```

### 6.2 Full Validation + Ablations

```bash
pwsh -File experiments/rq4_validation/run_rq4_validation.ps1 \
  -Suite all \
  -CompileProject \
  -RunAblations
```

### 6.3 Full Validation + Ablations + Manuscript

Only use this if the server also has the training bundles for the earlier figures.

```bash
pwsh -File experiments/rq4_validation/run_rq4_validation.ps1 \
  -Suite all \
  -CompileProject \
  -RunAblations \
  -CompilePaper
```

## 7. Minimal Sanity Checks After the Run

Check that these files exist:

```bash
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRE/QualityIndicatorSummary.csv
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRWA/QualityIndicatorSummary.csv
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRE/metadata_problem_splits.csv
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRWA/metadata_problem_splits.csv
ls experiments/rq4_validation/generated/validation_summary_rq4.csv
ls experiments/rq4_validation/generated/validation_pairwise_rq4.csv
ls experiments/rq4_validation/generated/validation_transfer_delta_rq4.csv
ls experiments/rq4_validation/generated/validation_wtl_rq4.tex
ls experiments/rq4_validation/generated/validation_wtl_rq4.png
ls experiments/rq4_validation/generated/validation_cd_rq4.csv
ls experiments/rq4_validation/generated/validation_ablation_rq4.csv
ls experiments/rq4_validation/generated/validation_cd_rq4.png
```

If ablations were executed, also check:

```bash
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRE-Ablation-Complete_RE3D/QualityIndicatorSummary.csv
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRE-Ablation-Extreme_RE3D/QualityIndicatorSummary.csv
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRWA-Ablation-Complete_RWA3D/QualityIndicatorSummary.csv
ls experiments/rq4_validation/results/representative-configs/RepresentativeConfigsRWA-Ablation-Extreme_RWA3D/QualityIndicatorSummary.csv
```

## 8. Suggested Copy-Back to the Local Machine

If the experiments run on the server but the paper is compiled locally, copy back the validation bundles:

```bash
rsync -avz experiments/rq4_validation/results/representative-configs/ user@local-machine:/path/to/Evolver/experiments/rq4_validation/results/representative-configs/
```

Then, on the local machine:

```bash
python paper/scripts/07_regenerate_manuscript_assets.py
python paper/scripts/08_compile_manuscript_pdf.py --no-sync
```

## 9. Notes

- `--cores -1` means: use all available processors minus one.
- The validation study uses:
  - 10,000 base-level evaluations
  - population size 100
  - 30 runs per problem by default
- The validation uses complete benchmark reference fronts from `resources/referenceFronts`.
- All experiment-side files, raw bundles, and generated `RQ4` artifacts live under `experiments/rq4_validation/`, which is outside the gitignored `paper/` workspace.
- The transfer-asymmetry figure is derived only from the aggregated `validation_summary_rq4.csv`; it does not require rerunning the Java experiments.
- The representative configurations and ablation logic are implemented in:
  - `src/main/java/org/uma/evolver/example/validation/RepresentativeConfigurationCatalog.java`
  - `src/main/java/org/uma/evolver/example/validation/RepresentativeConfigurationValidationStudy.java`
