package org.uma.evolver.analysis.ablation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;

/**
 * Performs ablation analysis across multiple problems to understand the
 * contribution of individual
 * parameter changes from a default configuration to an optimized configuration.
 *
 * <p>
 * This analyzer evaluates each configuration on all problems in the training
 * set, normalizes the
 * indicator values per problem to ensure comparable scales, and aggregates the
 * results (using the
 * mean) to obtain a single performance value.
 *
 * <p>
 * Multi-problem ablation analysis is useful when the optimized configuration
 * was obtained by
 * training on a set of problems, and we want to understand which parameter
 * changes contribute most
 * across the entire training set.
 *
 * @param <S> The type of solutions used by the algorithm being analyzed
 * @author Antonio J. Nebro
 */
public class AblationAnalyzer<S extends Solution<?>> {

    private final BaseLevelAlgorithm<S> algorithm;
    private final List<ProblemWithReferenceFront<S>> problems;
    private final List<QualityIndicator> indicators;
    private final int numberOfRuns;
    private final int maxEvaluations;
    private final ParameterSpace parameterSpace;

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
     * Creates a new AblationAnalyzer.
     *
     * @param algorithm      the configurable algorithm to analyze
     * @param problems       the list of problems with their reference fronts
     * @param indicators     quality indicators for measuring performance
     * @param maxEvaluations maximum evaluations for each algorithm run
     * @param numberOfRuns   number of independent runs per configuration per
     *                       problem
     * @param parameterSpace the parameter space definition to resolve conditional
     *                       parameters
     */
    public AblationAnalyzer(
            BaseLevelAlgorithm<S> algorithm,
            List<ProblemWithReferenceFront<S>> problems,
            List<QualityIndicator> indicators,
            int maxEvaluations,
            int numberOfRuns,
            ParameterSpace parameterSpace) {
        this.algorithm = algorithm;
        this.problems = new ArrayList<>(problems);
        this.indicators = new ArrayList<>(indicators);
        this.maxEvaluations = maxEvaluations;
        this.numberOfRuns = numberOfRuns;
        this.parameterSpace = parameterSpace;
    }

    /**
     * Gets the set of all conditional sub-parameter names for a given categorical
     * parameter value.
     */
    private List<String> getConditionalSubParameters(String parameterName, String value) {
        try {
            Parameter<?> parameter = parameterSpace.get(parameterName);
            if (parameter instanceof CategoricalParameter) {
                return parameter.findConditionalParameters(value).stream()
                        .map(Parameter::name)
                        .toList();
            }
        } catch (IllegalArgumentException e) {
            // Parameter not found in space, ignore
        }
        return List.of();
    }

    private Set<String> getAllConditionalSubParameters() {
        Set<String> subParams = new HashSet<>();
        for (Parameter<?> param : parameterSpace.parameters().values()) {
            param
                    .conditionalParameters()
                    .forEach(condParam -> subParams.add(condParam.parameter().name()));
        }
        return subParams;
    }

    private void applyConditionalParameters(
            Map<String, String> config,
            String paramName,
            String targetValue,
            Map<String, String> sourceConfig,
            Map<String, String> otherConfig) {

        String otherValue = otherConfig.get(paramName);
        if (otherValue != null) {
            List<String> otherSubParams = getConditionalSubParameters(paramName, otherValue);
            for (String subParam : otherSubParams) {
                config.remove(subParam);
            }
        }

        List<String> targetSubParams = getConditionalSubParameters(paramName, targetValue);
        for (String subParam : targetSubParams) {
            String value = sourceConfig.get(subParam);
            if (value != null) {
                config.put(subParam, value);
            }
        }
    }

    public AblationResult leaveOneOutAnalysis(
            Map<String, String> defaultConfig, Map<String, String> optimizedConfig) {

        AblationResult result = new AblationResult(indicators);

        double[] optimizedPerformance = evaluateConfigurationAcrossProblems(optimizedConfig);
        result.setOptimizedPerformance(optimizedPerformance);

        double[] defaultPerformance = evaluateConfigurationAcrossProblems(defaultConfig);
        result.setDefaultPerformance(defaultPerformance);

        Set<String> conditionalSubParams = getAllConditionalSubParameters();

        for (String paramName : optimizedConfig.keySet()) {
            if (conditionalSubParams.contains(paramName)) {
                continue;
            }

            String optValue = optimizedConfig.get(paramName);
            String defValue = defaultConfig.getOrDefault(paramName, null);

            if (defValue != null && !Objects.equals(optValue, defValue)) {
                Map<String, String> revertedConfig = new HashMap<>(optimizedConfig);
                revertedConfig.put(paramName, defValue);

                applyConditionalParameters(
                        revertedConfig, paramName, defValue, defaultConfig, optimizedConfig);

                double[] revertedPerformance = evaluateConfigurationAcrossProblems(revertedConfig);

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

    public AblationResult forwardPathAnalysis(
            Map<String, String> defaultConfig, Map<String, String> optimizedConfig) {

        AblationResult result = new AblationResult(indicators);

        Set<String> conditionalSubParams = getAllConditionalSubParameters();

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

        Map<String, String> currentConfig = new HashMap<>(defaultConfig);
        double[] currentPerformance = evaluateConfigurationAcrossProblems(currentConfig);
        result.setDefaultPerformance(currentPerformance.clone());

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

                applyConditionalParameters(
                        testConfig, param, optimizedConfig.get(param), optimizedConfig, currentConfig);

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
                path.add(
                        new AblationStep(
                                bestParam,
                                currentConfig.get(bestParam),
                                optimizedConfig.get(bestParam),
                                currentPerformance.clone(),
                                bestPerformance.clone()));

                currentConfig = bestConfig;
                currentPerformance = bestPerformance;
                remaining.remove(bestParam);
            }
        }

        result.setAblationPath(path);
        result.setOptimizedPerformance(currentPerformance);

        return result;
    }

    private double[] evaluateConfigurationAcrossProblems(Map<String, String> config) {
        double[] aggregatedIndicators = new double[indicators.size()];

        for (ProblemWithReferenceFront<S> problemData : problems) {
            double[] problemIndicators = evaluateConfigurationOnProblem(config, problemData);
            for (int i = 0; i < indicators.size(); i++) {
                aggregatedIndicators[i] += problemIndicators[i];
            }
        }

        for (int i = 0; i < indicators.size(); i++) {
            aggregatedIndicators[i] /= problems.size();
        }

        return aggregatedIndicators;
    }

    private double[] evaluateConfigurationOnProblem(
            Map<String, String> config, ProblemWithReferenceFront<S> problemData) {

        String[] args = configToArgs(config);
        double[] avgIndicators = new double[indicators.size()];

        double[][] normalizedReferenceFront = NormalizeUtils.normalize(problemData.referenceFront());

        for (int run = 0; run < numberOfRuns; run++) {
            BaseLevelAlgorithm<S> instance = algorithm.createInstance(problemData.problem(), maxEvaluations);
            instance.parse(args);

            Algorithm<List<S>> alg = instance.build();
            alg.run();

            List<S> front = alg.result();
            double[][] frontMatrix = solutionsToMatrix(front);

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

    private double aggregateImprovement(double[] before, double[] after) {
        double improvement = 0;
        for (int i = 0; i < before.length; i++) {
            improvement += (before[i] - after[i]);
        }
        return improvement;
    }

    private String[] configToArgs(Map<String, String> config) {
        List<String> args = new ArrayList<>();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            args.add("--" + entry.getKey());
            args.add(entry.getValue());
        }
        return args.toArray(new String[0]);
    }

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

    public List<ProblemWithReferenceFront<S>> getProblems() {
        return new ArrayList<>(problems);
    }
}
