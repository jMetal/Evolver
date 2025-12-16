package org.uma.evolver.analysis.ablation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Console-based implementation of ProgressReporter that displays progress information
 * to the standard output with timestamps and progress bars.
 * 
 * @author Antonio J. Nebro
 */
public class ConsoleProgressReporter implements ProgressReporter {
    
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final boolean showProgressBar;
    private final boolean showTimestamps;
    
    /**
     * Creates a console progress reporter with default settings.
     */
    public ConsoleProgressReporter() {
        this(true, true);
    }
    
    /**
     * Creates a console progress reporter with custom settings.
     * 
     * @param showProgressBar whether to show ASCII progress bars
     * @param showTimestamps whether to show timestamps
     */
    public ConsoleProgressReporter(boolean showProgressBar, boolean showTimestamps) {
        this.showProgressBar = showProgressBar;
        this.showTimestamps = showTimestamps;
    }
    
    @Override
    public void startPhase(String phase, int totalSteps) {
        String timestamp = showTimestamps ? "[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " : "";
        System.out.println();
        System.out.println(timestamp + "=== Starting " + phase + " ===");
        System.out.println(timestamp + "Total steps: " + totalSteps);
        if (showProgressBar) {
            System.out.println(timestamp + "Progress: " + createProgressBar(0, totalSteps));
        }
    }
    
    @Override
    public void stepCompleted(int currentStep, int totalSteps, String stepDescription, long elapsedTimeMs) {
        String timestamp = showTimestamps ? "[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " : "";
        double percentage = (double) currentStep / totalSteps * 100;
        
        System.out.printf("%sStep %d/%d (%.1f%%) completed: %s (%.2fs)%n", 
                         timestamp, currentStep, totalSteps, percentage, stepDescription, elapsedTimeMs / 1000.0);
        
        if (showProgressBar) {
            System.out.println(timestamp + "Progress: " + createProgressBar(currentStep, totalSteps));
        }
    }
    
    @Override
    public void phaseCompleted(String phase, long totalElapsedTimeMs) {
        String timestamp = showTimestamps ? "[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " : "";
        System.out.println();
        System.out.println(timestamp + "=== " + phase + " Completed ===");
        System.out.printf("%sTotal time: %.2f seconds%n", timestamp, totalElapsedTimeMs / 1000.0);
        System.out.println();
    }
    
    @Override
    public void reportSubProgress(String message, double progress) {
        String timestamp = showTimestamps ? "[" + LocalDateTime.now().format(TIME_FORMATTER) + "] " : "";
        System.out.printf("\r%s%s (%.1f%%)    ", timestamp, message, progress * 100);
        if (progress >= 1.0) {
            System.out.println(); // New line when complete
        }
    }
    
    /**
     * Creates an ASCII progress bar.
     * 
     * @param current current progress
     * @param total total steps
     * @return ASCII progress bar string
     */
    private String createProgressBar(int current, int total) {
        int barLength = 40;
        int filled = (int) ((double) current / total * barLength);
        
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else if (i == filled && current < total) {
                bar.append("▌");
            } else {
                bar.append("░");
            }
        }
        bar.append("] ");
        bar.append(String.format("%d/%d", current, total));
        
        return bar.toString();
    }
}