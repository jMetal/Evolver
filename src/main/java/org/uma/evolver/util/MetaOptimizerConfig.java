package org.uma.evolver.util;

/**
 * Configuration record for meta-optimization experiment metadata.
 * Used by {@link ConsolidatedOutputResults} to generate comprehensive
 * METADATA.txt files.
 */
public record MetaOptimizerConfig(
        // Meta-optimizer information
        String metaOptimizerName,
        int metaMaxEvaluations,
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
            return new MetaOptimizerConfig(
                metaOptimizerName,
                metaMaxEvaluations,
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
