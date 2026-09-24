# Tutorials: catalogue and development plan

**Status:** living document — the catalogue of planned tutorials for Evolver and Evolver-Studio,
their level and their development status. Tutorials are developed one at a time: each one gets a
short design (goal, audience, prerequisites, example, expected output), is implemented, and is
reviewed before moving on to the next.

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
| E1 | Introductory | Parameter spaces | What a parameter space is; parameter types (categorical, integer, double, boolean); how they relate (top-level parameters, global and conditional sub-parameters); the YAML format; how `NSGAIIDouble.yaml` and `NSGAIIBinary.yaml` share their structure and differ in their operators, and the parameter factory of each encoding. | First version (`docs/tutorials/parameter_spaces.rst`, `example.tutorial.ParameterSpacesTutorial`), in review |
| E2 | Introductory | Base-level algorithms | What a base-level algorithm is in Evolver (`BaseLevelAlgorithm`); encodings and which algorithm supports each; configuring an algorithm from a parameter space (configuration string, `parse`, `build`, `run`); outputs and quality indicators; default configurations with `ConfigurationFileReader`. The entry point to using Evolver as an alternative to jMetal. | Planned |
| E3 | Introductory | Meta-optimization workflow | The pieces: base-level algorithm, meta-optimizer, training problems and quality indicators as objectives. A complete run with the CLI (`request.yaml`) and the same run from Java (`TrainingRunner`); the files it produces and what they mean. | Planned |
| E4 | Introductory | Evolver in 10 minutes | Install JDK 21 and Maven, build, run a configurable algorithm and a CLI training request. An evolution of the current quick start, focused on getting everything working. | Planned |
| E5 | Intermediate | Configurable components in depth | External archives, initialization strategies, variation (crossover and mutation, DE in MOEA/D), selection and replacement, and how each choice changes the algorithm's behavior. | Planned |
| E6 | Intermediate | Designing your own parameter space | Reducing or extending a YAML space, adding conditional dependencies, and how that changes the size of the search space (e.g. tuning NSGA-II with SBX only, `NSGAIIDoubleReduced.yaml`). | Planned |
| E7 | Intermediate | Training sets, indicators and budgets | Choosing training problems and their reference fronts; indicators as objectives (EP, NHV, HV−, IGD+, number of evaluations); evaluation budget strategies (fixed, random range); independent runs. | Planned |
| E8 | Intermediate | Analyzing training results | Reading `INDICATORS.csv`, `CONFIGURATIONS.csv` and `VAR_CONF.txt`; the meta-optimizer's front; choosing a configuration (best per indicator, knee point); plots with the scripts in `scripts/`. | Planned |
| E9 | Intermediate | Validating a configuration | Comparing a tuned configuration with the standard one on validation problems with a jMetal `ExperimentBuilder` study: Wilcoxon and Friedman tests, critical-difference plots. | Planned |
| E10 | Intermediate | Problems without a reference front | Using HV− with approximate reference points, with the multi-objective TSP as example (builds on the current `reference_fronts.rst`). | Planned |
| E11 | Intermediate | Binary and permutation encodings | Configuring and meta-optimizing algorithms on binary (OneZeroMax) and permutation (multi-objective TSP) problems. | Planned |
| E12 | Advanced | Choosing the meta-optimizer | NSGA-II, AGE-MOEA, SPEA2, SMPSO, Async NSGA-II and RandomSearch; parallel evaluation and number of cores; the constraints meta-optimizers meet (offspring size equal to the population, no external archive); when to use each. | Planned |
| E13 | Advanced | Tree versus flat encoding | Derivation trees and the grammar of a parameter space; the inactive-variable problem of the flat encoding; when the tree encoding pays off. | Planned |
| E14 | Advanced | Tuning with irace | irace as an alternative to meta-optimization: generating irace files from a YAML space, running it and comparing the results. | Planned |
| E15 | Advanced | Automating Evolver with the CLI | Reusable request files, the `DescribeMain` manifest, batches of runs, integration with external tools. | Planned |
| E16 | Advanced | Extending Evolver | Adding an operator to the catalogue, or a new configurable algorithm (its YAML space, factory entries, `Base*`/`Double*` classes and tests). | Planned |

## Evolver-Studio tutorials

Two tracks: **solving problems** (configure and run algorithms, jMetal-runner style) and
**meta-optimization** (training, analysis, validation).

| Id | Level | Track | Tutorial | Description | Pairs with | Status |
|---|---|---|---|---|---|---|
| S1 | Introductory | Both | A tour of Evolver-Studio | Installation, connecting to an Evolver checkout or jar, and the pages of the app. | E4 | Planned |
| S2 | Introductory | Both | Exploring a parameter space | The Explore page: the tree view of a parameter space and the catalogue of algorithms and meta-optimizers. | E1 | First version (Tutorials page, `evolver_studio/tutorial_parameter_spaces.py`), in review |
| S3 | Introductory | Solving | Solving a problem with a configurable algorithm | Pick a problem, an algorithm and its encoding, and a configuration (default or edited in the guided form); run it; inspect the front and the quality indicators; export `VAR`/`FUN`. | E2 | Blocked (see dependencies) |
| S4 | Introductory | Meta-optimization | Your first guided training | A ready-made scenario (NSGA-II on ZDT4, small budget) with the live front and the resulting files. | E3 | Planned |
| S5 | Intermediate | Solving | Comparing configurations on a problem | Run several configurations (standard, tuned, custom) with several independent runs each, and compare their indicator distributions and fronts. | E5, E9 | Blocked (see dependencies) |
| S6 | Intermediate | Solving | Using a tuned configuration | Take a configuration found in a training run and use it to solve a new problem. | E8 | Blocked (see dependencies) |
| S7 | Intermediate | Meta-optimization | Customizing a training run | Base-level algorithm and encoding, multi-problem training set (including TSP), meta-optimizer operators, guided versus expert editing of the parameter space. | E6, E7, E11 | Planned |
| S8 | Intermediate | Meta-optimization | Analyzing training results | The Analysis page: evolution across checkpoints, fronts, choosing a configuration. | E8 | Planned |
| S9 | Intermediate | Meta-optimization | Validating a configuration | The Validation page: statistical comparison and tables. | E9 | Blocked (Validation page not implemented) |
| S10 | Advanced | Solving | Solving your own problem | Run a configurable algorithm on a user-defined jMetal problem class available on the classpath. | E2 | Blocked (see dependencies) |
| S11 | Advanced | Meta-optimization | Comparing meta-optimizers and encodings | Several training runs on the same problem with different meta-optimizers or encodings, and their comparison. | E12, E13 | Planned |
| S12 | Advanced | Both | Long-running jobs | Background execution, cancelling, reconnecting to a run in progress and resuming the analysis. | — | Planned |

### Dependencies of the solving track

Evolver-Studio currently only launches training runs (through `cli.training`), and its Validation
page is not implemented. The solving-track tutorials (S3, S5, S6, S10) need first:

- **Evolver:** a CLI entry point to run a single configurable algorithm on a problem, analogous to
  `cli.training` (a request with algorithm, encoding, problem, configuration, budget and number of
  runs; status and result files with `VAR`/`FUN` and indicator values). To be designed as its own
  proposal in `docs/proposals/`.
- **Evolver-Studio:** a page to configure and run algorithms on problems (jMetal-runner style),
  reusing the guided parameter form and the live front already used by Training.

## Development order

1. E1 → E2 → E3: the foundations.
2. Each Evolver-Studio tutorial after its Evolver counterpart (S2 after E1, S4 after E3, …); the
   solving track once its dependencies exist.
3. The remaining tutorials in level order, revisiting priorities as they are written.
