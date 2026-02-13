package org.uma.evolver.meta.builder;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Builder for {@link RandomSearch} algorithm.
 */
public class MetaRandomSearchBuilder<S extends Solution<?>> {
    private final Problem<S> problem;
    private int maxEvaluations = 25000;
    private int numberOfCores = 1;

    public MetaRandomSearchBuilder(Problem<S> problem) {
        this.problem = problem;
    }

    public MetaRandomSearchBuilder<S> setMaxEvaluations(int maxEvaluations) {
        Check.valueIsNotNegative(maxEvaluations);
        this.maxEvaluations = maxEvaluations;
        return this;
    }

    public MetaRandomSearchBuilder<S> setNumberOfCores(int numberOfCores) {
        Check.valueIsNotNegative(numberOfCores);
        this.numberOfCores = numberOfCores;
        return this;
    }

    public RandomSearch<S> build() {
        return new RandomSearch<>(problem, maxEvaluations, numberOfCores);
    }
}
