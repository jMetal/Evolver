# Tutorials: catalogue and development plan

**Status:** living document — the catalogue of planned tutorials for Evolver and Evolver-Studio,
their level and their development status. Tutorials are developed one at a time: each one gets a
short design (goal, audience, prerequisites, example, expected output), is implemented, and is
reviewed before moving on to the next. A tutorial that has passed review is marked "Done, vX.Y
(date)" here, with the same `:Version:` field added to its own page; a later revision bumps the
version and the date in both places.

## Goals

Evolver has two uses, and the tutorials cover both:

1. **Configuring and running algorithms.** Evolver's configurable algorithms (`algorithm`,
   `parameter`) can be used on their own, as an alternative to jMetal, to solve concrete problems.
2. **Meta-optimization.** Finding configurations of those algorithms automatically, then analyzing
   and validating them.

The same applies to **Evolver-Studio**: it is not only a front end for training and validation,
but also a platform to configure and run algorithms on concrete problems, in the style of jMetal's
runners (pick a problem, an algorithm and a configuration, run it, inspect the front).

The Evolver tutorials live in the Sphinx documentation (`docs/tutorials/`), each backed by a
dedicated, CI-checked example so it cannot silently rot. The Evolver-Studio tutorials are
interactive, inside the app, and link to their Evolver counterpart when there is one.

Levels: **Introductory**, **Intermediate**, **Advanced**.

## Evolver tutorials

| Id | Level | Tutorial | Description | Status |
|---|---|---|---|---|
| E1 | Introductory | Parameter spaces | What a parameter space is; parameter types (categorical, integer, double, boolean); how they relate (top-level parameters, global and conditional sub-parameters); the YAML format; how `NSGAIIDouble.yaml` and `NSGAIIBinary.yaml` share their structure and differ in their operators, and the parameter factory of each encoding. | Done, v1.0 (2026-09-25) (`docs/tutorials/parameter_spaces.rst`, `example.tutorial.ParameterSpacesTutorial`) |
| E2 | Introductory | Base-level algorithms | What a base-level algorithm is in Evolver (`BaseLevelAlgorithm`); encodings and which algorithm supports each; configuring an algorithm from a parameter space (configuration string, `parse`, `build`, `run`); outputs and quality indicators; default configurations with `ConfigurationFileReader`. The entry point to using Evolver as an alternative to jMetal. | Done, v1.1 (2026-09-25) (`docs/tutorials/base_level_algorithms.rst`, `example.tutorial.BaseLevelAlgorithmsTutorial`) |
| E3 | Introductory | Meta-optimization workflow | The pieces: base-level algorithm, meta-optimizer, training problems and quality indicators as objectives. A complete run with the CLI (`request.yaml`) and the same run from Java (`TrainingRunner`); the files it produces and what they mean. | Done, v1.0 (2026-09-25) (`docs/tutorials/meta_optimization_workflow.rst`, `example.tutorial.MetaOptimizationWorkflowTutorial`; ZDT4 only, one run per configuration). v1.1 (2026-10-02) adds a note on stopping by time and points to E17, in review. Next version: a training set with several problems |
| E4 | Introductory | Evolver in 10 minutes | Install JDK 21 and Maven, build, run a configurable algorithm and a CLI training request. An evolution of the current quick start, focused on getting everything working. | Done, v1.0 (2026-10-03) (`docs/quick_start.rst`, `tutorial-e4-request.yaml`). Mentions the time limit of the meta-optimizer and points to E17. v1.1 (2026-10-03): the comparison of step 5 no longer promises a clearly better configuration (with one run per configuration, some trainings choose one that is only as good as the default, or that gets stuck in a local front of ZDT4); three runs per configuration were tried and did not avoid it |
| E5 | Intermediate | Configurable components in depth | External archives, initialization strategies, variation (crossover and mutation, DE in MOEA/D), selection and replacement, and how each choice changes the algorithm's behavior. | Planned |
| E6 | Intermediate | Designing your own parameter space | Reducing or extending a YAML space, adding conditional dependencies, and how that changes the size of the search space (e.g. tuning NSGA-II with SBX only, `NSGAIIDoubleReduced.yaml`). Every top-level parameter of the algorithm must stay defined (a missing one is an error, not a default); a parameter with a single value adds nothing in the flat encoding (see `tree-mutation.md`, open question 5). | Planned |
| E7 | Intermediate | Training sets, indicators and budgets | Choosing training problems and their reference fronts; indicators as objectives (EP, NHV, HV−, IGD+); the budgets of the case study; independent runs. The discussion of the training budget and of the stopping condition of the meta-optimizer is in E17. | Done, v1.0 (2026-10-02) (`docs/tutorials/training_sets_indicators_budgets.rst`, `example.training.dtlz.AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ`, `example.tutorial.TrainingSetsValidationTutorial`; DTLZ1-7 training with 10000 evaluations, validation on DTLZ1-7 and WFG1-9 with 50000). v1.1 (2026-10-03) adds a note that a training cannot be reproduced exactly |
| E17 | Intermediate | Budgets: evaluations or time | The two budgets of a training. The base level: the evaluations of each run, the validation budget (the literature's) against the training budget (a design decision). The meta-optimizer: stopping after a number of configurations (`metaMaxEvaluations`) or after a computing time (`metaMaxComputingTimeMinutes`), which are mutually exclusive; when to use each, how and when the stop is checked (the generation in progress is completed; the initial population is always evaluated; the asynchronous meta-optimizer stops after one evaluation), what the output files record, reproducibility, comparisons at equal time. Read after E3. Added on 2026-10-02 because stopping by time is likely to be the main choice in many studies and was not in the original plan. | First version (`docs/tutorials/budgets.rst`, `example.tutorial.BudgetsTutorial`; the training of E3 stopped after 2 minutes), in review |
| E18 | Intermediate | The number of evaluations as an objective | Using the number of evaluations of each base-level run as an objective of the meta-optimizer, together with an indicator, to relate the budget to the quality of the result (for instance, the fewest evaluations that give good enough configurations). It needs a workaround to turn that number into an objective: `EvaluationsQualityIndicator`, a fake indicator that the meta-optimization problem recognizes and gives the evaluations of the run as its value, and `RandomRangeEvaluationsStrategy`, which draws the budget of each run from a range (so the value is not controlled by the configuration). Java only: `cli.training` has fixed budgets. To be analyzed in detail: what it measures and whether it makes sense, which was discussed when it was introduced and left open. The example `NSGAIIOptimizingNSGAIIForProblemZDT4MinimizingEvaluations` was removed from `example.training` (816c4464, 2026-09-19) and can be read in the history (3aa70af1); `concepts/objective_functions.rst` describes it, saying 'evaluations of the meta-optimizer' where the code uses those of the base-level run. Decided on 2026-10-02 to keep it out of E7 and E17. | Planned; the design question is open |
| E8 | Intermediate | Analyzing training results | Reading `INDICATORS.csv`, `CONFIGURATIONS.csv` and `VAR_CONF.txt`; the meta-optimizer's front; choosing a configuration (best per indicator, knee point); plots with the scripts in `scripts/`. | Done, v1.0 (2026-10-03) (`docs/tutorials/analyzing_training_results.rst`, `example.tutorial.TrainingAnalysisTutorial`, `tutorial-e8-request.yaml`; NSGA-II tuned for ZDT1-6 with 5 runs per configuration, validated with 15000 evaluations). Analysis of the parameters of the population left for a later version |
| E9 | Intermediate | Validating a configuration | Comparing a tuned configuration with the standard one on validation problems with a jMetal `ExperimentBuilder` study: Wilcoxon and Friedman tests, critical-difference plots. | Planned |
| E10 | Intermediate | Problems without a reference front | Using HV− with approximate reference points, with the multi-objective TSP as example (builds on the current `reference_fronts.rst`). | Planned |
| E11 | Intermediate | Binary and permutation encodings | Configuring and meta-optimizing algorithms on binary (OneZeroMax) and permutation (multi-objective TSP) problems. | Planned |
| E12 | Advanced | Choosing the meta-optimizer | NSGA-II, AGE-MOEA, SPEA2, SMPSO, Async NSGA-II and RandomSearch; parallel evaluation and number of cores; the constraints meta-optimizers meet (offspring size equal to the population, no external archive); how each one checks the time limit (per generation, per batch of evaluations, per evaluation); when to use each. | Planned |
| E13 | Advanced | Tree versus flat encoding | Derivation trees and the grammar of a parameter space; the inactive-variable problem of the flat encoding; when the tree encoding pays off. | Planned |
| E14 | Advanced | Tuning with irace | irace as an alternative way to tune Evolver's algorithms: generating irace's parameter file from a YAML space, the target runner, the scenario, running irace, and applying the configuration it finds. | First version (`docs/tutorials/tuning_with_irace.rst`, `example.tutorial.IraceTutorial`, `src/main/resources/irace/`; NSGA-II tuned for ZDT1-6 with the HV+EP runner, irace 4.4.3). Shows how to use irace only; the comparison with Evolver is a separate research line (`irace-vs-evolver.md`). In review. A comparison with Evolver at equal computing time (E17) is the fair one, since the two spend different time per evaluation |
| E15 | Advanced | Automating Evolver with the CLI | Reusable request files, the `DescribeMain` manifest, batches of runs, integration with external tools. | Planned |
| E16 | Advanced | Extending Evolver | Adding an operator to the catalogue, or a new configurable algorithm (its YAML space, factory entries, `Base*`/`Double*` classes and tests). | Planned |

### Where the stopping condition of the meta-optimizer is covered

The option of stopping the meta-optimizer by computing time (`metaMaxComputingTimeMinutes`,
mutually exclusive with `metaMaxEvaluations`) did not exist when the tutorials were planned. It is
likely to be the main choice in many studies, so it is introduced where the user first sees
`metaMaxEvaluations` and developed in a tutorial of its own (decided 2026-10-02):

- **Entry points**, one line each: the README example, the quick start (E4) and a short section in E3.
- **Full treatment: E17**, which also takes the budget of the base level out of E7; E7 keeps the
  budgets of its case study.
- **E12**: the granularity of the stop of each meta-optimizer. **E14**: equal-time comparisons.
- **Evolver-Studio** (S4, S7): the time field of the training page, still to be implemented.

## Evolver-Studio tutorials

Two tracks: **solving problems** (configure and run algorithms, jMetal-runner style) and
**meta-optimization** (training, analysis, validation).

| Id | Level | Track | Tutorial | Description | Pairs with | Status |
|---|---|---|---|---|---|---|
| S1 | Introductory | Both | A tour of Evolver-Studio | Installation, connecting to an Evolver checkout or jar, and the pages of the app. | E4 | Planned |
| S2 | Introductory | Both | Exploring a parameter space | The Explore pages: a parameter space as a table with its conditions and a filter, the operators of the meta-optimizers and the quality indicators. | E1 | Done, v1.1 (2026-10-02) (Tutorials page, `evolver_studio/tutorial_parameter_spaces.py`) |
| S3 | Introductory | Solving | Solving a problem with a configurable algorithm | The Run algorithm page: pick a problem, an algorithm and its encoding, and a configuration (default, adjusted within its parameter space); run it; inspect the front, the indicators and the solutions; repeat it, and take it to the command line. Double encoding only. | E2 | Done, v1.0 (2026-10-02) (Tutorials page, `evolver_studio/tutorial_solving.py`) |
| S4 | Introductory | Meta-optimization | Your first guided training | A ready-made scenario (NSGA-II on ZDT4, small budget) with the live front and the resulting files. Show the stopping condition (evaluations or time) from the start, once the training page has the time field. | E3, E17 | Planned |
| S5 | Intermediate | Solving | Comparing configurations on a problem | Run several configurations (standard, tuned, custom) with several independent runs each, and compare their indicator distributions and fronts. | E5, E9 | Blocked (see dependencies) |
| S6 | Intermediate | Solving | Using a tuned configuration | Take a configuration found in a training run and use it to solve a new problem. | E8 | Blocked (see dependencies) |
| S7 | Intermediate | Meta-optimization | Customizing a training run | Base-level algorithm and encoding, multi-problem training set (including TSP), meta-optimizer operators, guided versus expert editing of the parameter space, and the budgets (evaluations or time). | E6, E7, E11, E17 | Planned |
| S8 | Intermediate | Meta-optimization | Analyzing training results | The Analysis page: evolution across checkpoints, fronts, choosing a configuration. | E8 | Planned |
| S9 | Intermediate | Meta-optimization | Validating a configuration | The Validation page: statistical comparison and tables. | E9 | Blocked (Validation page not implemented) |
| S10 | Advanced | Solving | Solving your own problem | Run a configurable algorithm on a user-defined jMetal problem class available on the classpath. | E2 | Blocked (see dependencies) |
| S11 | Advanced | Meta-optimization | Comparing meta-optimizers and encodings | Several training runs on the same problem with different meta-optimizers or encodings, and their comparison. | E12, E13 | Planned |
| S12 | Advanced | Both | Long-running jobs | Background execution, cancelling, reconnecting to a run in progress and resuming the analysis. | — | Planned |

### Dependencies of the solving track

Evolver-Studio launches training runs (through `cli.training`) and, since its Run algorithm page,
single runs (through `cli.solving`); its Validation page is not implemented. The solving-track
tutorials (S3, S5, S6, S10) needed first:

- **Evolver:** a CLI entry point to run a single configurable algorithm on a problem, analogous to
  `cli.training`. Done: `cli.solving.SolveRunnerMain`, see `cli-solving.md`.
- **Evolver-Studio:** a page to configure and run algorithms on problems (jMetal-runner style).
  Done: Run algorithm. It starts from the default configuration of the algorithm and adjusts it
  within its parameter space, and shows the fronts, the indicators and the solutions of the runs.
  S5 (comparing configurations) and S6 (a configuration found by a training) still need it to
  compare runs and to load a configuration from a training; S10 needs problems given by class name,
  which it does not offer yet.

## Development order

1. E1 → E2 → E3: the foundations.
2. Each Evolver-Studio tutorial after its Evolver counterpart (S2 after E1, S4 after E3, …); the
   solving track once its dependencies exist.
3. The remaining tutorials in level order, revisiting priorities as they are written.
