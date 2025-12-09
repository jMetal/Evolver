package org.uma.evolver.ablation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;

/**
 * Performs ablation analysis across multiple problems to understand the contribution 
 * of individual parameter changes from a default configuration to an optimized configuration.
 *
 * <p>This analyzer evaluates each configuration on all problems in the training set,
 * normalizes the indicator values per problem to ensure comparable scales, and
 * aggregates the results (using the mean) to obtain a single performance value.</p>
 *
 * <p>Multi-problem ablation analysis is useful when the optimized configuration was
 * obtained by training on a set of problems, and we want to understand which
 * parameter changes contribute most across the entire training set.</p>
 *
 * @param <S> The type of solutions used by the algorithm being analyzed
 * @author Antonio J. Nebro
 */
public class MultiProblemAblationAnalyzer<S extends Solution<?>> {

    private final BaseLevelAlgorithm<S> algorithm;
    private final List<ProblemWithReferenceFront<S>> problems;
    private final List<QualityIndicator> indicators;
    private final int numberOfRuns;
    private final int maxEvaluations;

    // Maps categorical parameters to their conditional sub-parameters
    private final Map<String, Map<String, List<String>>> conditionalParameters;

    /**
     * Represents a problem along with its reference front for normalization.
     *
     * @param <S> The type of solutions
     */
    public static class ProblemWithReferenceFront<S extends Solution<?>> {
        private final Problem<S> problem;
        private final double[][] referenceFront;
        private final String name;

        public ProblemWithReferenceFront(Problem<S> problem, double[][] referenceFront, String name) {
            this.problem = problem;
            this.referenceFront = referenceFront;
            this.name = name;
        }

        public Problem<S> problem() {
            return problem;
        }

        public double[][] referenceFront() {
            return referenceFront;
        }

        public String name() {
            return name;
        }
    }

    /**
     * Creates a new MultiProblemAblationAnalyzer.
     *
     * @param algorithm      the configurable algorithm to analyze
     * @param problems       the list of problems with their reference fronts
     * @param indicators     quality indicators for measuring performance
     * @param maxEvaluations maximum evaluations for each algorithm run
     * @param numberOfRuns   number of independent runs per configuration per problem
     */
    public MultiProblemAblationAnalyzer(
            BaseLevelAlgorithm<S> algorithm,
            List<ProblemWithReferenceFront<S>> problems,
            List<QualityIndicator> indicators,
            int maxEvaluations,
            int numberOfRuns) {
        this.algorithm = algorithm;
        this.problems = new ArrayList<>(problems);
        this.indicators = new ArrayList<>(indicators);
        this.maxEvaluations = maxEvaluations;
        this.numberOfRuns = numberOfRuns;
        this.conditionalParameters = initializeConditionalParameters();
    }

    /**
     * Initializes the mapping of categorical parameters to their conditional sub-parameters.
     */
    private Map<String, Map<String, List<String>>> initializeConditionalParameters() {
        Map<String, Map<String, List<String>>> params = new HashMap<>();

        // Mutation parameter and its conditional sub-parameters
        Map<String, List<String>> mutationParams = new HashMap<>();
        mutationParams.put("polynomial", List.of("polynomialMutationDistributionIndex"));
        mutationParams.put("linkedPolynomial", List.of("linkedPolynomialMutationDistributionIndex"));
        mutationParams.put("uniform", List.of("uniformMutationPerturbation"));
        mutationParams.put("nonUniform", List.of("nonUniformMutationPerturbation"));
        params.put("mutation", mutationParams);

        // Crossover parameter and its conditional sub-parameters
        Map<String, List<String>> crossoverParams = new HashMap<>();
        crossoverParams.put("SBX", List.of("sbxDistributionIndex"));
        crossoverParams.put("BLX_ALPHA", List.of("blxAlphaCrossoverAlpha"));
        crossoverParams.put("wholeArithmetic", List.of());
        params.put("crossover", crossoverParams);

        // Algorithm result and its conditional sub-parameters
        Map<String, List<String>> algorithmResultParams = new HashMap<>();
        algorithmResultParams.put("population", List.of());
        algorithmResultParams.put("externalArchive", List.of("populationSizeWithArchive", "archiveType"));
        params.put("algorithmResult", algorithmResultParams);

        // Selection parameter
        Map<String, List<String>> selectionParams = new HashMap<>();
        selectionParams.put("tournament", List.of("selectionTournamentSize"));
        selectionParams.put("random", List.of());
        params.put("selection", selectionParams);

        return params;
    }

    /**
     * Adds a custom conditional parameter mapping.
     *
     * @param parentParam the categorical parameter name
     * @param value       the value of the categorical parameter
     * @param subParams   the list of sub-parameter names associated with this value
     */
    public void addConditionalParameter(String parentParam, String value, List<String> subParams) {
        conditionalParameters
                .computeIfAbsent(parentParam, k -> new HashMap<>())
                .put(value, new ArrayList<>(subParams));
    }

    /**
     * Gets the set of all conditional sub-parameter names.
     */
    private Set<String> getConditionalSubParameters() {
        Set<String> subParams = new HashSet<>();
        for (Map<String, List<String>> valueMap : conditionalParameters.values()) {
            for (List<String> params : valueMap.values()) {
                subParams.addAll(params);
            }
        }
        return subParams;
    }

    /**
     * Applies conditional parameters when changing a categorical parameter.
     */
    private void applyConditionalParameters(
            Map<String, String> config,
            String paramName,
            String targetValue,
            Map<String, String> sourceConfig,
            Map<String, String> otherConfig) {

        Map<String, List<String>> paramMapping = conditionalParameters.get(paramName);
        if (paramMapping == null) {
            return;
        }

        // Remove sub-parameters from the other value
        String otherValue = otherConfig.get(paramName);
        List<String> otherSubParams = paramMapping.getOrDefault(otherValue, List.of());
        for (String subParam : otherSubParams) {
            config.remove(subParam);
        }

        // Add sub-parameters from the target value
        List<String> targetSubParams = paramMapping.getOrDefault(targetValue, List.of());
        for (String subParam : targetSubParams) {
            String value = sourceConfig.get(subParam);
            if (value != null) {
                config.put(subParam, value);
            }
        }
    }

    /**
     * Performs leave-one-out ablation analysis across all problems.
     * For each parameter that differs between configurations, reverts it to the
     * default value and measures the aggregated performance loss across all problems.
     *
     * @param defaultConfig   the default/baseline configuration
     * @param optimizedConfig the optimized configuration found by meta-optimization
     * @return the ablation analysis results
     */
    public AblationResult leaveOneOutAnalysis(
            Map<String, String> defaultConfig,
            Map<String, String> optimizedConfig) {

        AblationResult result = new AblationResult(indicators);

        // Evaluate optimized configuration across all problems
        double[] optimizedPerformance = evaluateConfigurationAcrossProblems(optimizedConfig);
        result.setOptimizedPerformance(optimizedPerformance);

        // Evaluate default configuration across all problems
        double[] defaultPerformance = evaluateConfigurationAcrossProblems(defaultConfig);
        result.setDefaultPerformance(defaultPerformance);

        // Get the set of conditional sub-parameters to skip in main loop
        Set<String> conditionalSubParams = getConditionalSubParameters();

        // For each parameter that differs, revert and measure impact
        for (String paramName : optimizedConfig.keySet()) {
            // Skip conditional sub-parameters
            if (conditionalSubParams.contains(paramName)) {
                continue;
            }

            String optValue = optimizedConfig.get(paramName);
            String defValue = defaultConfig.getOrDefault(paramName, null);

            if (defValue != null && !Objects.equals(optValue, defValue)) {
                // Create config with this parameter reverted to default
                Map<String, String> revertedConfig = new HashMap<>(optimizedConfig);
                revertedConfig.put(paramName, defValue);

                // Handle conditional sub-parameters
                applyConditionalParameters(revertedConfig, paramName, defValue, defaultConfig, optimizedConfig);

                double[] revertedPerformance = evaluateConfigurationAcrossProblems(revertedConfig);

                // Calculate contribution (performance loss when reverting)
                double[] contribution = new double[indicators.size()];
                for (int i = 0; i < indicators.size(); i++) {
                    contribution[i] = revertedPerformance[i] - optimizedPerformance[i];
                }

                result.addParameterContribution(
                        new ParameterContribution(paramName, optValue, defValue, contribution));
            }
        }

        return result;
    }

    /**
     * Performs forward ablation path analysis across all problems.
     * Starting from the default configuration, greedily adds parameter changes
     * one at a time, selecting the change that gives the best improvement
     * when averaged across all problems.
     *
     * @param defaultConfig   the default/baseline configuration
     * @param optimizedConfig the optimized configuration
     * @return the ablation analysis results with the path
     */
    public AblationResult forwardPathAnalysis(
            Map<String, String> defaultConfig,
            Map<String, String> optimizedConfig) {

        AblationResult result = new AblationResult(indicators);

        // Get the set of conditional sub-parameters to skip
        Set<String> conditionalSubParams = getConditionalSubParameters();

        // Get parameters that differ (excluding conditional sub-parameters)
        List<String> changedParams = new ArrayList<>();
        for (String param : optimizedConfig.keySet()) {
            if (conditionalSubParams.contains(param)) {
                continue;
            }
            String defValue = defaultConfig.getOrDefault(param, null);
            if (defValue != null && !Objects.equals(optimizedConfig.get(param), defValue)) {
                changedParams.add(param);
            }
        }

        // Start with default configuration
        Map<String, String> currentConfig = new HashMap<>(defaultConfig);
        double[] currentPerformance = evaluateConfigurationAcrossProblems(currentConfig);
        result.setDefaultPerformance(currentPerformance.clone());

        // Greedy forward selection
        Set<String> remaining = new HashSet<>(changedParams);
        List<AblationStep> path = new ArrayList<>();

        while (!remaining.isEmpty()) {
            String bestParam = null;
            double bestImprovement = Double.NEGATIVE_INFINITY;
            double[] bestPerformance = null;
            Map<String, String> bestConfig = null;

            for (String param : remaining) {
                Map<String, String> testConfig = new HashMap<>(currentConfig);
                testConfig.put(param, optimizedConfig.get(param));

                // Apply conditional sub-parameters
                applyConditionalParameters(testConfig, param, optimizedConfig.get(param),
                        optimizedConfig, currentConfig);

                double[] testPerformance = evaluateConfigurationAcrossProblems(testConfig);
                double improvement = aggregateImprovement(currentPerformance, testPerformance);

                if (improvement > bestImprovement) {
                    bestImprovement = improvement;
                    bestParam = param;
                    bestPerformance = testPerformance;
                    bestConfig = testConfig;
                }
            }

            if (bestParam != null) {
                path.add(new AblationStep(
                        bestParam,
                        currentConfig.get(bestParam),
                        optimizedConfig.get(bestParam),
                        currentPerformance.clone(),
                        bestPerformance.clone()
                ));

                currentConfig = bestConfig;
                currentPerformance = bestPerformance;
                remaining.remove(bestParam);
            }
        }

        result.setAblationPath(path);
        result.setOptimizedPerformance(currentPerformance);

        return result;
    }

    /**
     * Evaluates a configuration across all problems.
     * Each problem's indicator values are normalized before aggregation to ensure
     * comparable scales.
     *
     * @param config the configuration to evaluate
     * @return the mean normalized indicator values across all problems
     */
    private double[] evaluateConfigurationAcrossProblems(Map<String, String> config) {
        double[] aggregatedIndicators = new double[indicators.size()];
        
        for (ProblemWithReferenceFront<S> problemData : problems) {
            double[] problemIndicators = evaluateConfigurationOnProblem(config, problemData);
            for (int i = 0; i < indicators.size(); i++) {
                aggregatedIndicators[i] += problemIndicators[i];
            }
        }

        // Compute mean across problems
        for (int i = 0; i < indicators.size(); i++) {
            aggregatedIndicators[i] /= problems.size();
        }

        return aggregatedIndicators;
    }

    /**
     * Evaluates a configuration on a single problem by running multiple times.
     */
    private double[] evaluateConfigurationOnProblem(
            Map<String, String> config, 
            ProblemWithReferenceFront<S> problemData) {
        
        String[] args = configToArgs(config);
        double[] avgIndicators = new double[indicators.size()];

        // Normalize reference front for this problem
        double[][] normalizedReferenceFront = NormalizeUtils.normalize(problemData.referenceFront());

        for (int run = 0; run < numberOfRuns; run++) {
            BaseLevelAlgorithm<S> instance = algorithm.createInstance(problemData.problem(), maxEvaluations);
            instance.parse(args);

            Algorithm<List<S>> alg = instance.build();
            alg.run();

            List<S> front = alg.result();
            double[][] frontMatrix = solutionsToMatrix(front);

            // Normalize the obtained front
            double[][] normalizedFront = NormalizeUtils.normalize(frontMatrix);

            for (int i = 0; i < indicators.size(); i++) {
                indicators.get(i).referenceFront(normalizedReferenceFront);
                avgIndicators[i] += indicators.get(i).compute(normalizedFront);
            }
        }

        for (int i = 0; i < indicators.size(); i++) {
            avgIndicators[i] /= numberOfRuns;
        }

        return avgIndicators;
    }

    /**
     * Aggregates improvement across all indicators (assuming minimization).
     */
    private double aggregateImprovement(double[] before, double[] after) {
        double improvement = 0;
        for (int i = 0; i < before.length; i++) {
            improvement += (before[i] - after[i]);
        }
        return improvement;
    }

    /**
     * Converts a configuration map to command-line arguments array.
     */
    private String[] configToArgs(Map<String, String> config) {
        List<String> args = new ArrayList<>();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            args.add("--" + entry.getKey());
            args.add(entry.getValue());
        }
        return args.toArray(new String[0]);
    }

    /**
     * Converts a list of solutions to a matrix of objective values.
     */
    private double[][] solutionsToMatrix(List<S> solutions) {
        if (solutions.isEmpty()) {
            return new double[0][0];
        }
        double[][] matrix = new double[solutions.size()][solutions.get(0).objectives().length];
        for (int i = 0; i < solutions.size(); i++) {
            matrix[i] = solutions.get(i).objectives().clone();
        }
        return matrix;
    }

    /**
     * Returns the list of problems used for evaluation.
     */
    public List<ProblemWithReferenceFront<S>> getProblems() {
        return new ArrayList<>(problems);
    }

    /**
     * Returns the number of problems in the training set.
     */
    public int getNumberOfProblems() {
        return problems.size();
    }
}
