# TreeMutation: analysis and open questions

**Status:** analysis done (2026-09-29); the mutation of integer nodes is fixed (question 2), the
treatment of `offspringPopulationSize` is decided (question 3, nominal, with two related fixes), the
distribution index is set to 5 provisionally, and the mutation strength is still to be studied.

## What it does

`TreeMutation` (`meta/encoding/operator/TreeMutation.java`) chooses, with probability
`probability`, one node uniformly among the active nodes of the derivation tree, and mutates it
according to its type: polynomial mutation (Deb and Goyal, 1996) of double nodes, the same rounded
for integer nodes, a different value chosen uniformly for categorical nodes (regenerating the
conditional subtree at random, keeping the global children), and a flip for boolean nodes.

## Assessment

It is well founded: it is the standard mutation of grammar-guided genetic programming (point
mutation of terminals, subtree regeneration of non-terminals; Whigham, 1995; Poli, Langdon and
McPhee, *A Field Guide to Genetic Programming*, 2008), and it matches the one-exchange neighbourhood
of configurators such as ParamILS and SMAC (change one parameter; re-sample the ones it activates).
It never mutates inactive parameters, unlike the flat encoding.

## Open questions

### 1. Distribution index (pending study)

The distribution index sets the step size of the mutation of numeric nodes. Median and 90th
percentile of the absolute step, as a fraction of the range, from the middle of the range
(simulated):

| Mutation | Median | 90th percentile |
|---|---|---|
| Polynomial, η = 20 | 0.032 | 0.104 |
| Polynomial, η = 5 | 0.106 | 0.303 |
| Gaussian, σ = 0.2 on the normalized range (SMAC) | 0.135 | 0.329 |
| Polynomial, η = 1 | 0.208 | 0.429 |

η = 20 is the usual value for continuous optimization, where fine tuning near the optimum
matters; in a configuration space with a few thousand meta-evaluations, larger steps are more
usual (SMAC's local search uses σ = 0.2 on the normalized range; irace samples around the elite
configurations with a standard deviation that starts at half the range and shrinks). The
meta-optimizer configurations with the tree encoding (`MetaNSGAIITreeConfiguration.yaml`,
`MetaAGEMOEATreeConfiguration.yaml`) and `TreeNSGAIIOptimizingNSGAIIForBenchmarkRE3D` therefore use
η = 5 instead of 20, **provisionally**: the change was not validated experimentally.

Proposed study: `TreeNSGAII` tuning NSGA-II on ZDT1-6 (8000 evaluations per problem, one run per
configuration), η = 20 versus η = 5 (and possibly η = 1 or a Gaussian), several replications each,
comparing the convergence of the meta-objectives (`scripts/plot_training_convergence.py`) and the
final fronts. Estimated cost with 2000 meta-evaluations and 16 cores: about 17 minutes per training
(extrapolated from tutorial E8), so about 3 hours for 5 replications of two values; 1000
meta-evaluations and 3 replications, about 50 minutes.

### 2. Integer nodes with small ranges often did not change (fixed)

Rounding a small polynomial step left the value unchanged in most mutations (simulated, η = 20):
77% for `selectionTournamentSize` [2, 10], 74% for `knnDistanceArchiveK` [1, 10], 7% for
`populationSizeWithArchive` [10, 200]. Each of those mutations spent a whole meta-evaluation on a
configuration identical to its parent. Rounding on `[lower, upper]` also made the bounds half as
likely as the inner values.

Fixed (2026-09-29): integer values are mutated on `[lower - 0.5, upper + 0.5]` and rounded; if the
value does not change, it is moved one unit in the direction of the perturbation (inwards at a
bound); and categorical nodes with a single value, the only nodes that cannot change, are never
selected. Every mutation event now changes exactly one node. With a small range the mutation is
mostly a move to a neighbouring value (simulated on [2, 10]: a change of one unit in 76% of the
mutations with η = 5, 98% with η = 20), as configurators do with integer and ordinal parameters.

A mutant can still be identical to its parent when no mutation is applied (`mutationProbability`
below 1) and there is no crossover. Whether a meta-optimizer should skip evaluating configurations
it has already seen is a separate question: the evaluation is noisy, but the meta-optimizers do not
aggregate repeated evaluations, so a duplicate with a lucky evaluation survives and the population
loses diversity.

### 3. Ordinal categorical parameters (decided: nominal)

The only categorical parameter with numeric values is `offspringPopulationSize` ([1, 5, 10, 20, 50,
100, 200, 400], or with 2 as well), in the NSGA-II, NSGA-III, AGE-MOEA, RDEMOEA and RVEA spaces. An
ordinal type (mutation to neighbouring values, as in irace and SMAC) was considered and **rejected**
for it: the parameter was originally an integer in [1, 400], but the value 1 (steady-state) is so
influential, and so unlikely to be sampled in that range, that it was turned into an enumeration so
that 1 is as likely as any other value. The value 1 is qualitatively different from the others
(steady-state versus generational), so the order does not reflect how the performance changes, and
an ordinal mutation would make steady-state unreachable again from large values. Simulated
probability of reaching 1:

| Sampling or mutation | Probability of 1 |
|---|---|
| Integer in [1, 400], uniform (the original definition) | 0.3% |
| Integer in [1, 400], log scale | 6.8% |
| Enumeration, uniform sampling (current) | 12.5% |
| Nominal mutation, from any other value | 14.3% |
| Ordinal mutation (η = 5), from 5 / 20 / 100 | 50% / 3.8% / 0.3% |

So it stays a nominal categorical parameter. Two related defects were found and fixed
(2026-09-29):

- In the tree encoding, `offspringPopulationSize` was **never mutated**: `TreeNode.validValues()`
  only knows the values of `CategoricalParameter`, so the node had no values to choose from and
  steady-state could only come from the initial population or crossover. `TreeMutation` now mutates
  categorical integer parameters as nominal ones, and `GrammarConverter.validate` checks their
  values.
- In the flat encoding, integer parameters were decoded as `min + floor(x · (max − min))`, so the
  upper bound was only reached with x = 1.0 exactly (e.g. a tournament size of 10 in [2, 10] almost
  never appeared). The range is now split into intervals of the same width.

A limitation remains in the flat encoding: it decodes every categorical parameter by the index of
its value in the list, so its polynomial mutation moves `offspringPopulationSize` mostly to
neighbouring values in the list, and reaches 1 easily only from 5 (or 2). This is the order that
the flat encoding imposes on every categorical parameter, one more of its known weaknesses; it is
left as it is.

### 4. Mutation strength

Exactly one node per mutation is the convention; mutating each node with probability 1/n (one
change on average, sometimes several) is an alternative to consider together with question 1.
