package org.uma.evolver;

import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observable.Observable;

import java.util.List;
import java.util.Map;

/*
 * Interface for all meta-optimizers
 * This is required as each MetaOptimizer from jMetal may have different methods,
 * but this is the minimum required for them to work on Evolver
 */

/**
 * Interface for all meta-optimizers.
 * <p>
 * This interface defines the minimum required methods for any MetaOptimizer from jMetal to work on Evolver.
 */
public abstract class MetaOptimizer {
    /**
     * Runs the meta-optimization process.
     * <p>
     * This method should implement the meta-optimization algorithm and control the overall flow of the optimization
     * process for the given Evolver instance.
     */
    public abstract void run();

    /**
     * Returns an observable to track the progress of the meta-optimization.
     * <p>
     * Observers can subscribe to this observable to receive updates on the current state and progress of the
     * meta-optimization process.
     *
     * @return An Observable instance that emits updates on the meta-optimization progress.
     */
    public abstract Observable<Map<String, Object>> observable();

    /**
     * Retrieves the total computing time taken by the meta-optimization process.
     * <p>
     * The computing time includes the execution time of the underlying meta-optimization algorithm and any
     * additional processing time used to obtain the final result.
     *
     * @return The total computing time in milliseconds for the meta-optimization process.
     */
    public abstract long totalComputingTime();

    /**
     * Returns the list of solutions obtained after running the meta-optimization process.
     * <p>
     * The returned solutions represent the best-known results or the final Pareto front, depending on the
     * nature of the optimization problem and the meta-optimization algorithm.
     *
     * @return A list of DoubleSolution instances containing the solutions obtained by the meta-optimization.
     */
    public abstract List<DoubleSolution> result();
}
