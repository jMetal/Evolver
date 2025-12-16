package org.uma.evolver.analysis.ablation;

/**
 * Interface for reporting progress during ablation analysis.
 * 
 * @author Antonio J. Nebro
 */
public interface ProgressReporter {
    
    /**
     * Called when starting a new analysis phase.
     * 
     * @param phase the name of the analysis phase (e.g., "Leave-One-Out Analysis")
     * @param totalSteps the total number of steps in this phase
     */
    void startPhase(String phase, int totalSteps);
    
    /**
     * Called when a step is completed.
     * 
     * @param currentStep the current step number (1-based)
     * @param totalSteps the total number of steps
     * @param stepDescription description of the completed step
     * @param elapsedTimeMs elapsed time for this step in milliseconds
     */
    void stepCompleted(int currentStep, int totalSteps, String stepDescription, long elapsedTimeMs);
    
    /**
     * Called when a phase is completed.
     * 
     * @param phase the name of the completed phase
     * @param totalElapsedTimeMs total elapsed time for the phase in milliseconds
     */
    void phaseCompleted(String phase, long totalElapsedTimeMs);
    
    /**
     * Called to report sub-progress within a step (e.g., problem evaluation progress).
     * 
     * @param message progress message
     * @param progress progress value between 0.0 and 1.0
     */
    default void reportSubProgress(String message, double progress) {
        // Default implementation does nothing
    }
}