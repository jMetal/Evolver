# irace versus Evolver's meta-optimization: a research line

**Status:** open research question, not explored yet. Tutorial E15
(`docs/tutorials/tuning_with_irace.rst`) only shows how to use irace with Evolver, and makes no
claim about which approach is better.

## Question

Evolver can tune its configurable algorithms in two ways: with its own meta-optimizers
(multi-objective metaheuristics such as NSGA-II, AGE-MOEA or an asynchronous NSGA-II, searching the
parameter space with several quality indicators as objectives), and with irace (iterated racing,
a single value to minimize, statistical elimination of configurations). Both take the same inputs:
the configurable algorithm, its parameter space (the irace file is generated from the same YAML
space) and a training set. Which one finds better configurations, at what cost, and in which
situations?

## Methodological issues to settle first

- **Equal budgets.** irace counts experiments (one configuration on one instance, with one seed);
  a meta-optimizer counts meta-evaluations (one configuration on the whole training set, with
  `numberOfIndependentRuns` runs per problem). A fair comparison needs a common unit: the number of
  base-level runs, the number of base-level evaluations, or wall-clock time with the same number of
  cores (irace launches a JVM per experiment, which adds a fixed cost per run).
- **One objective versus several.** irace minimizes one value per run (e.g. −HV, or HV and EP
  combined as in `AutoNSGAIIIraceHVEP`); the meta-optimizer returns a front of configurations over
  several indicators, from which one is chosen. The comparison has to fix how the meta-optimizer's
  configuration is chosen (e.g. lowest NHV, as in tutorials E3, E6 and E7) and which indicator the
  validation uses.
- **Noise and runs.** irace handles noise by racing (more runs for promising configurations) and
  by blocking on instances and seeds; the meta-optimizer by taking the median of several runs per
  problem (tutorial E7 shows how a single run can mislead it). The settings of both must be
  reported.
- **Replications.** Both tuners are stochastic: each has to be run several times (e.g. 10 or more
  replications with different seeds), and the configurations they find compared as distributions,
  not as single outcomes.
- **Validation.** The configurations have to be validated on held-out budgets and, ideally,
  held-out problems (tutorial E6 shows that a configuration generalizes to problems like the
  training ones, not to others), with statistical tests and effect sizes.

## Possible experimental design

- Base level: NSGA-II with `NSGAIIDouble.yaml`; training sets ZDT1-6 (bi-objective, as in tutorials
  E7 and E15) and DTLZ1-7 (three objectives, as in E6).
- Tuners: irace with `AutoNSGAIIIraceHV`, and Evolver with NSGA-II and asynchronous NSGA-II as
  meta-optimizers, NHV and EP as meta-objectives, with 1 and 5 runs per configuration.
- Budgets: a few levels of the common budget unit, for instance the base-level runs of the E7
  training (50000) and fractions of it.
- Outputs: the configurations found in each replication, their validation indicators, the cost of
  each tuner, and the diversity of the configurations found.

## Related resources

- `org.uma.evolver.irace`: target runners and irace file generators.
- `src/main/resources/irace/`: scenario, instances, parameter file and `run.sh` for NSGA-II on ZDT.
- `/Users/ajnebro/Softw/irace/iraceExample4x`: an earlier irace experiment (MOEA/D on ZCAT) run for
  an irace-versus-Evolver comparison in a paper.
