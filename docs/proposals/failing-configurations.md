# Configurations that fail during a training

**Status:** decided (2026-10-05). A configuration that fails makes the training fail, with an error
that explains what went wrong. Tolerating failing configurations is not planned (see
"Alternative considered").

## The problem

A configuration can be valid for the parameter space and still fail when the base-level algorithm
is built or run, because the operators have limits that the space does not express. Found while
preparing tutorial E5 (designing your own parameter space), with NSGA-II:

- `selectionTournamentSize` larger than the population the selection draws from (`populationSize`,
  or `populationSizeWithArchive` with an external archive);
- `populationSizeWithArchive` = 1, since the crossover needs two parents;
- `crossoverProbability` outside [0, 1];
- `mutationProbabilityFactor` larger than the number of variables of a problem: the mutation
  probability is the factor divided by that number, so the limit depends on the training problems;
- `offspringPopulationSize` = 0.

The parameter spaces bundled with Evolver respect these limits, but a widened space may not, and
some limits depend on two parameters at once (the tournament and the population) or on the
problem (the number of variables). One such configuration aborts the whole training.

## Decision: fail, explaining why

The training fails, and the error must say what happened, so that the parameter space can be
fixed:

- `AbstractMetaOptimizationProblem` wraps the error of a configuration with its context: the
  problem, the number of evaluations, the cause and the configuration. `TrainingRunnerMain` writes
  it to `status.yaml` (`status: FAILED`, `errorMessage`):

  ```
  A configuration failed on problem ZDT1 (2000 evaluations): mutationProbabilityFactor (48.87)
  divided by the number of variables of the problem (30) gives a mutation probability of 1.63,
  which is not in [0, 1]: for this problem, mutationProbabilityFactor must be in [0, 30]
  Configuration: --algorithmResult population ...
  ```

- The operator parameters name the parameter that causes an error, and say how to fix it
  (`ProbabilityChecks`, the tournament check of `SelectionParameter`, the offspring size of the
  variation parameters).
- Wrapped exceptions keep their message and their cause (jMetal 7.7 fixes `JMetalException(String,
  Exception)`, which lost both).

Tutorial E5 explains the limits, so that a widened space respects them.

## Alternative considered: tolerating failing configurations

Giving a failing configuration the worst value of each meta-objective, so that the training goes
on and the meta-optimizer avoids that region, was considered and discarded for now:

- **It is a constraint, not a quality.** A configuration that cannot run is infeasible; giving it
  bad objective values mixes validity and quality, and with several meta-objectives an infeasible
  configuration with a "worst" value can still be non-dominated in a degenerate way. Done
  properly, validity would be a constraint of the meta-optimization problem, with its violation
  and a comparator that handles it. `AbstractMetaOptimizationProblem` has no constraints
  (`numberOfConstraints()` is 0), and the meta-optimizers do not use constrained comparators.
- **It hides mistakes.** A failure is more often a mistake in a parameter space than a region to
  avoid, and a training that goes on would hide it.

It could be revisited if a real case needs it, together with constraint handling in the
meta-optimization problems.
