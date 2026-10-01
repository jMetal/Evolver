package org.uma.evolver.meta.output;

import java.util.List;
import org.uma.evolver.meta.builder.ComputingTimeLimit;

/**
 * Configuration record for meta-optimization experiment metadata.
 * Used by {@link ConsolidatedOutputResults} to generate comprehensive
 * METADATA.txt files.
 */
public record MetaOptimizerConfig(
        // Meta-optimizer information
        String metaOptimizerName,
        int metaMaxEvaluations,
        double metaMaxComputingTimeMinutes,
        int metaPopulationSize,
        int numberOfCores,

        // Base-level algorithm information
    String baseLevelAlgorithmName,
    int baseLevelPopulationSize,
    int baseLevelMaxEvaluations,
    String evaluationBudgetStrategy,

    // Parameter space
    String yamlParameterSpaceFile) {

    /**
     * Whether the meta-optimizer is bounded by computing time (and not by evaluations).
     */
    public boolean boundedByComputingTime() {
        return metaMaxComputingTimeMinutes > 0.0;
    }

    /**
     * The lines of METADATA.txt that describe how the meta-optimizer stops: its limit and the
     * stopping condition. Exactly one of the two limits is given.
     */
    public List<String> stoppingConditionLines() {
        if (boundedByComputingTime()) {
            return List.of(
                "Max Computing Time: " + formatMinutes(metaMaxComputingTimeMinutes) + " min ("
                    + formatDuration(Math.round(metaMaxComputingTimeMinutes * 60_000.0)) + ")",
                "Stopping condition: computing time");
        }
        return List.of(
            "Max Evaluations: " + metaMaxEvaluations, "Stopping condition: evaluations");
    }

    private static String formatMinutes(double minutes) {
        return minutes == Math.rint(minutes) ? String.valueOf((long) minutes) : String.valueOf(minutes);
    }

    /** Formats a duration in milliseconds as {@code Xh Ym Zs}. */
    public static String formatDuration(long elapsedTimeMillis) {
        long totalSeconds = elapsedTimeMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    /**
     * Creates a builder for MetaOptimizerConfig.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String metaOptimizerName = "Unknown";
        private int metaMaxEvaluations = 0;
        private double metaMaxComputingTimeMinutes = 0.0;
        private int metaPopulationSize = 0;
        private int numberOfCores = 1;
        private String baseLevelAlgorithmName = "Unknown";
        private int baseLevelPopulationSize = 0;
        private int baseLevelMaxEvaluations = 0;
        private String evaluationBudgetStrategy = "Unknown";
        private String yamlParameterSpaceFile = "Unknown";

        public Builder metaOptimizerName(String name) {
            this.metaOptimizerName = name;
            return this;
        }

        public Builder metaMaxEvaluations(int evaluations) {
            this.metaMaxEvaluations = evaluations;
            return this;
        }

        /**
         * Sets the limit on the computing time of the meta-optimizer, in minutes (it replaces the
         * limit on the evaluations; the two are mutually exclusive).
         */
        public Builder metaMaxComputingTimeMinutes(double minutes) {
            this.metaMaxComputingTimeMinutes = minutes;
            return this;
        }

        public Builder metaPopulationSize(int size) {
            this.metaPopulationSize = size;
            return this;
        }

        public Builder numberOfCores(int cores) {
            this.numberOfCores = cores;
            return this;
        }

        public Builder baseLevelAlgorithmName(String name) {
            this.baseLevelAlgorithmName = name;
            return this;
        }

        public Builder baseLevelPopulationSize(int size) {
            this.baseLevelPopulationSize = size;
            return this;
        }

        public Builder baseLevelMaxEvaluations(int maxEvaluations) {
            this.baseLevelMaxEvaluations = maxEvaluations;
            return this;
        }

        public Builder evaluationBudgetStrategy(String strategy) {
            this.evaluationBudgetStrategy = strategy;
            return this;
        }

        public Builder yamlParameterSpaceFile(String file) {
            this.yamlParameterSpaceFile = file;
            return this;
        }

        public MetaOptimizerConfig build() {
            if (metaMaxComputingTimeMinutes != 0.0) {
                ComputingTimeLimit.checkMinutes(metaMaxComputingTimeMinutes);
                ComputingTimeLimit.checkExclusive(metaMaxEvaluations > 0, metaMaxComputingTimeMinutes);
            }
            return new MetaOptimizerConfig(
                metaOptimizerName,
                metaMaxEvaluations,
                metaMaxComputingTimeMinutes,
                metaPopulationSize,
                numberOfCores,
                baseLevelAlgorithmName,
                baseLevelPopulationSize,
                baseLevelMaxEvaluations,
                evaluationBudgetStrategy,
                yamlParameterSpaceFile);
        }
    }
}
