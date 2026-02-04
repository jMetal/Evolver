package org.uma.evolver.analysis.ablation;

import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

/**
 * Bundles a problem instance with its reference front and display name.
 *
 * @param <S> the solution type
 */
public record ProblemWithReferenceFront<S extends Solution<?>>(
    Problem<S> problem,
    double[][] referenceFront,
    String name) {
}
