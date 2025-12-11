package org.uma.evolver.util;

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

    void writeResultsToFiles(List<DoubleSolution> solutions) throws IOException;
}
