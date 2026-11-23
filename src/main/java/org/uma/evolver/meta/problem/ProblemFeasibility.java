package org.uma.evolver.meta.problem;

/**
 * The feasibility of the runs of a configuration on one training problem, part of the diagnosis of
 * feasibility of a configuration (see {@link ConstrainedIndicatorValue#FEASIBILITY_BY_PROBLEM}).
 *
 * @param problem the name of the problem
 * @param failedRuns the runs on the problem without feasible solutions
 * @param runs the runs on the problem
 * @param meanScaledViolation the mean scaled violation of the failed runs, 0 if there are none
 */
public record ProblemFeasibility(
    String problem, int failedRuns, int runs, double meanScaledViolation) {}
