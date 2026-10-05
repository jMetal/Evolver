# Scripts for Evolver

Python scripts that turn Evolver's results into figures. The core Evolver framework (Java + Maven)
needs no Python: these scripts are optional. Only active, reusable scripts live here; one-off
analyses of a particular experiment belong with that experiment's data, not in this directory.

## Environment setup

Run the setup **from the repository root**:

```bash
# Option A — conda (creates the 'evolver' environment)
conda env create -f environment.yml
conda activate evolver

# Option B — virtualenv
python -m venv .venv
source .venv/bin/activate
pip install -r scripts/requirements.txt
```

## Scripts

| Script | What it does |
|---|---|
| `plot_front.py` | Plots one front (a `FUN.csv`) against its reference front: 2D, 3D, or parallel coordinates for more objectives. Static PNG (matplotlib). |
| `plot_front_interactive.py` | Same as `plot_front.py`, as an interactive Plotly figure (in the browser, or a self-contained HTML file). |
| `plot_fronts.py` | Plots several labelled bi-objective fronts against a reference front, e.g. the fronts of different configurations of an algorithm on the same problem: one panel per front with shared axes (default), or all of them on a single panel (`--mode overlay`). |
| `plot_training_convergence.py` | Plots how each meta-objective of a training run converges over the meta-evaluations: median and best–worst band of the configurations on the meta-optimizer's front at each checkpoint, pooled over replications, and the meta-evaluation at which 95% of the improvement is reached. With `--x time`, the x axis is the computing time of the meta-optimizer. |
| `plot_parameter_space.py` | Prints a parameter space YAML file as a text tree, or draws it as a compact figure. |
| `critical_difference_plots.py` | Critical difference plots of a jMetal validation study (`QualityIndicatorSummary.csv`), generated with [SAES](https://github.com/jMetal/SAES): average Friedman ranks, with bars joining the algorithms whose differences are not significant (Nemenyi). Needs `pip install SAES`. |
| `plot_meta_population.py` | Plots the population of the meta-optimizer at several checkpoints of a training run, in the space of two meta-objectives, with its non-dominated configurations highlighted. Needs a training run with `writePopulation: true`. |
| `plot_median_fronts.py` | For a jMetal validation study, plots the front of the run with the median value of an indicator (HV by default) for each problem and algorithm, over the reference front: one row per problem, one column per algorithm (2D or 3D). |
| `boxplots.py` | Boxplots of a jMetal validation study (`QualityIndicatorSummary.csv`), generated with [SAES](https://github.com/jMetal/SAES): the distribution of the runs of each algorithm, one panel per problem. Needs `pip install SAES`. |
| `effect_size_tables.py` | Vargha-Delaney A12 effect sizes of a jMetal validation study: the probability that a run of the pivot is better than a run of each other algorithm, per problem, with its magnitude (negligible, small, medium, large). |
| `friedman_holm_tables.py` | Friedman test over the problems of a jMetal validation study (SciPy), and post-hoc comparison of a control algorithm with the others with Holm's adjusted p-values ([SAES](https://github.com/jMetal/SAES)). Needs `pip install SAES`. |
| `bayesian_plots.py` | Bayesian sign test of a jMetal validation study ([SAES](https://github.com/jMetal/SAES)): the probabilities that the pivot is better, practically equivalent (within a ROPE) or worse than each other algorithm, and the posterior on a triangle. Needs `pip install SAES`. |
| `wilcoxon_pivot_tables.py` | Wilcoxon pivot tables of a jMetal validation study (`QualityIndicatorSummary.csv`), generated with [SAES](https://github.com/jMetal/SAES): median and IQR of each algorithm per problem, with the tuned configuration as pivot in the last column. LaTeX, and optionally PNG. Needs `pip install SAES`. |

Run any script without arguments (or with `--help`) for its full usage.

### Fronts

```bash
# one front over its reference front (overlay by default; --mode side|both for panels)
python scripts/plot_front.py FUN.csv resources/referenceFronts/DTLZ2.3D.csv
python scripts/plot_front_interactive.py FUN.csv resources/referenceFronts/DTLZ2.3D.csv --output front.html

# several labelled fronts over the reference front (bi-objective)
python scripts/plot_fronts.py resources/referenceFronts/ZDT4.csv \
    --front "Default=path/to/default/FUN.csv" --front "Tuned=path/to/tuned/FUN.csv" \
    --output fronts.png
```

The reference front is always passed explicitly: reference fronts follow no single naming
convention (`DTLZ1.3D.csv` vs `RE31.csv`), so the file name is not guessed from the problem.

### Validation tables

```bash
# needs SAES (pip install SAES); --png also needs pdflatex and pdftoppm
python scripts/wilcoxon_pivot_tables.py results/tutorial-e7/validation/QualityIndicatorSummary.csv \
    --pivot NSGAIIDTLZ --order NSGAII,NSGAIII,MOEAD,SMSEMOA,AGEMOEA,NSGAIIDTLZ \
    --output-dir results/tutorial-e7/tables --png
```

In each cell, `+` means that the pivot (last column) is significantly better than that algorithm,
`-` that it is worse, and `=` that the difference is not significant.

```bash
# critical difference plots (average Friedman ranks, Nemenyi test), with SAES as well
python scripts/critical_difference_plots.py results/tutorial-e7/validation/QualityIndicatorSummary.csv \
    --indicators HV,IGD+ --output-dir results/tutorial-e7/tables
```

The other analyses of tutorial E9, on the same file:

```bash
S=results/tutorial-e9/validation/QualityIndicatorSummary.csv
# boxplots of some problems
python scripts/boxplots.py $S --problems DTLZ1,DTLZ3,WFG1,WFG8 --indicators HV
# effect sizes (Vargha-Delaney A12) of the pivot against the others
python scripts/effect_size_tables.py $S --pivot NSGAIIWFG --indicators HV,IGD+
# Friedman test and Holm's post-hoc procedure, with a control algorithm
python scripts/friedman_holm_tables.py $S --control NSGAIIWFG --indicators HV,IGD+
# Bayesian sign test; the ROPE is in the units of the indicator
python scripts/bayesian_plots.py $S --pivot NSGAIIWFG --indicators HV --rope 0.001
```

### Population of the meta-optimizer

```bash
# needs a training run with writePopulation: true; default checkpoints: first, middle and last
python scripts/plot_meta_population.py results/tutorial-e7/training --evaluations 100,1000,2000
```

### Training convergence

```bash
# one training run (the output directory of a training, with INDICATORS.csv)
python scripts/plot_training_convergence.py results/tutorial/E3 --primary NHV

# several replications: a directory with one training output directory per replication
python scripts/plot_training_convergence.py path/to/campaign --primary IGD+ --output-dir plots

# the same over the computing time of the meta-optimizer instead of the meta-evaluations
python scripts/plot_training_convergence.py path/to/campaign --primary IGD+ --x time
```

It writes one `convergence_<indicator>.png` (`convergence_<indicator>_time.png` with `--x time`)
per meta-objective and prints the final value of the primary indicator and when 95% of its
improvement was reached.

The time comes from the `# Time (min)` line of each checkpoint of `VAR_CONF.txt`, written by
Evolver 2.2 and later whatever the stopping condition (older runs only support
`--x evaluations`). The unit is chosen from the longest run: seconds below 2 minutes, minutes below
2 hours, hours otherwise. One run is plotted at its own checkpoints; replications, whose
checkpoints fall at different instants, are pooled at 100 common instants, each run contributing
the front of its last checkpoint up to that instant. `plot_meta_population.py` also adds the time
of each checkpoint to its panel titles.

`training_time.py` holds the shared code that reads these times; it is not a script, and neither
is `study_summary.py`, which reads the `QualityIndicatorSummary.csv` of a validation study for the
statistical scripts.

### Parameter spaces

```bash
python scripts/plot_parameter_space.py src/main/resources/parameterSpaces/NSGAIIDouble.yaml --depth 3
```

## Dependencies

`requirements.txt` (and `../environment.yml`): pandas, numpy and matplotlib for every script, plus
Plotly for `plot_front_interactive.py`, PyYAML for `plot_parameter_space.py` and pytest for the
tests.

## Tests

`tests/` has pytest tests of the time axis of the training plots and of the statistics of the
validation scripts (A12, average ranks, median tables). From the root of the repository:

```bash
python -m pytest scripts/tests
```
