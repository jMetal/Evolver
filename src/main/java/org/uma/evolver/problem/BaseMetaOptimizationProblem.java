package org.uma.evolver.problem;

import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;

import java.util.List;

/**
 * Base class for Meta-Optimization Problems.
 * <p>
 * This class extends the AbstractDoubleProblem class from jMetal and provides an abstraction for defining
 * meta-optimization problems. Meta-optimization problems are optimization problems that involve tuning or
 * configuring the parameters of algorithm to achieve better performance on a given set of tasks.
 */
public abstract class BaseMetaOptimizationProblem extends AbstractDoubleProblem {
    /**
     * Retrieves the list of parameters to be tuned in the meta-optimization problem.
     * <p>
     * Each parameter is represented by a Parameter object, which contains information about the parameter's name,
     * boundaries, and other characteristics.
     *
     * @return A list of Parameter objects representing the parameters to be optimized in the meta-optimization.
     */
    public abstract List<Parameter<?>> parameters();
}
