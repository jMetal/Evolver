# TreeMutation: analysis and open questions

**Status:** analysis done (2026-09-29); the mutation of integer nodes is fixed (question 2), the
distribution index is set to 5 provisionally, and the other questions are still to be studied.

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

### 3. Ordinal categorical parameters

Categorical parameters with ordered numeric values, such as `offspringPopulationSize` ([1, 5, 10,
20, 50, 100, 200, 400]), are mutated as nominal ones: any other value with the same probability.
irace and SMAC have an ordinal type mutated to neighbouring values. It would need an ordinal type in
Evolver's YAML parameter spaces.

### 4. Mutation strength

Exactly one node per mutation is the convention; mutating each node with probability 1/n (one
change on average, sometimes several) is an alternative to consider together with question 1.
