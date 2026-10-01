package org.uma.evolver.meta.output;

import java.io.IOException;
import java.util.List;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Interface for writing execution data (solutions) to files or other output
 * destinations.
 * Used by {@link WriteExecutionDataToFilesObserver}.
 */
public interface EvaluationOutputWriter {
    void updateEvaluations(int evaluations);

    /**
     * Sets the computing time of the meta-optimizer at the next write, in milliseconds. Writers
     * that do not receive it use the time elapsed since they were created.
     */
    default void updateComputingTime(long computingTimeMillis) {}

    void writeResultsToFiles(List<DoubleSolution> solutions) throws IOException;
}
