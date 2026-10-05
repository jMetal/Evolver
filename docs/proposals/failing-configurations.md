# Configurations that fail during a training

**Status:** proposed (2026-10-05), not implemented. Today a failing configuration aborts the
training, with an error that names the problem, the cause and the configuration (see "Current
behavior").

## Motivation

A configuration can be valid for the parameter space and still fail when the base-level algorithm
is built or run, because the operators have limits that the space does not express. Found while
preparing tutorial E6 (designing your own parameter space), with NSGA-II:

- `selectionTournamentSize` larger than the population the selection draws from (`populationSize`,
  or `populationSizeWithArchive` with an external archive);
- `populationSizeWithArchive` = 1, since the crossover needs two parents;
- `crossoverProbability` outside [0, 1];
- `mutationProbabilityFactor` larger than the number of variables of a problem: the mutation
  probability is the factor divided by that number, so the limit depends on the training problems;
- `offspringPopulationSize` = 0.

The parameter spaces bundled with Evolver respect these limits, but a widened space may not, and
some limits depend on two parameters at once (the tournament and the population) or on the
problem (the number of variables). One such configuration among thousands aborts the whole
training: in a test with a widened space, the training failed in its initial population, with all
its work lost.

## Current behavior

`AbstractMetaOptimizationProblem` lets the exception propagate, wrapped with its context:

```
A configuration failed on problem ZDT1 (2000 evaluations): mutationProbabilityFactor (48.87)
divided by the number of variables of the problem (30) gives a mutation probability of 1.63,
which is not in [0, 1]: for this problem, mutationProbabilityFactor must be in [0, 30]
Configuration: --algorithmResult population ...
```

`TrainingRunnerMain` writes it to `status.yaml` (`status: FAILED`, `errorMessage`). The messages of
the operator parameters name the parameter that causes the error (`ProbabilityChecks`, the
tournament check of `SelectionParameter`).

## Proposal

Penalize the failing configuration instead of aborting:

1. `AbstractMetaOptimizationProblem` catches the exception of a configuration and gives it the
   worst value of each indicator (`Double.MAX_VALUE` for an indicator to minimize, 0 for one to
   maximize: the values already used when an indicator is not finite), so the meta-optimizer
   discards it and learns to avoid that region of the space.
2. It logs a warning with the message of the error and the configuration, the first time each
   distinct message appears (a widened space can produce the same error thousands of times).
3. It counts the failing configurations, and the output writes the count to `METADATA.txt` (and
   `status.yaml`), so that a user who sees a good training also sees that part of the space was
   invalid.
4. An option of the request keeps today's behavior (`failOnInvalidConfiguration: true`), useful
   while writing a new parameter space, where a failure is more likely a mistake in the space.

Open questions:

- Whether the default should be to penalize or to fail. Penalizing makes trainings robust;
  failing makes mistakes in a parameter space visible at once. A middle ground: fail if the
  configurations of the initial population all fail, penalize otherwise.
- Whether to check the known limits when the parameter space is loaded (the tournament against
  the population, probabilities in [0, 1]), which would reject a space before the training starts.
  It cannot cover the limits that depend on the problem.
