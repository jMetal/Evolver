package org.uma.evolver.meta.builder;

import org.uma.evolver.meta.algorithm.RandomSearch;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Builder for {@link RandomSearch} algorithm.
 */
public class MetaRandomSearchBuilder<S extends Solution<?>> {
    private final Problem<S> problem;
    private int maxEvaluations = 25000;
    private boolean maxEvaluationsSet = false;
    private Double maxComputingTimeMinutes = null;
    private int numberOfCores = 1;

    public MetaRandomSearchBuilder(Problem<S> problem) {
        this.problem = problem;
    }

    public MetaRandomSearchBuilder<S> setMaxEvaluations(int maxEvaluations) {
        Check.valueIsNotNegative(maxEvaluations);
        this.maxEvaluations = maxEvaluations;
        this.maxEvaluationsSet = true;
        ComputingTimeLimit.checkExclusive(maxEvaluationsSet, maxComputingTimeMinutes);
        return this;
    }

    /**
     * Bounds the search by computing time instead of by evaluations (the two are mutually
     * exclusive). See {@link RandomSearch#byComputingTime}.
     *
     * @param minutes the maximum computing time in minutes, with decimals (greater than zero)
     */
    public MetaRandomSearchBuilder<S> setMaxComputingTimeMinutes(double minutes) {
        this.maxComputingTimeMinutes = ComputingTimeLimit.checkMinutes(minutes);
        ComputingTimeLimit.checkExclusive(maxEvaluationsSet, maxComputingTimeMinutes);
        return this;
    }

    public MetaRandomSearchBuilder<S> setNumberOfCores(int numberOfCores) {
        Check.valueIsNotNegative(numberOfCores);
        this.numberOfCores = numberOfCores;
        return this;
    }

    public RandomSearch<S> build() {
        if (maxComputingTimeMinutes != null) {
            return RandomSearch.byComputingTime(problem, maxComputingTimeMinutes, numberOfCores);
        }
        return new RandomSearch<>(problem, maxEvaluations, numberOfCores);
    }
}
