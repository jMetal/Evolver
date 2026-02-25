package org.uma.evolver.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.uma.evolver.meta.problem.MetaOptimizationProblem;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * An implementation of {@link EvaluationOutputWriter} that consolidates
 * execution results into unified files.
 *
 * <p>
 * Generates:
 * <ul>
 * <li>METADATA.txt: Comprehensive experiment metadata including both
 * algorithms.
 * <li>INDICATORS.csv: Quality indicator values for each solution over time.
 * <li>CONFIGURATIONS.csv: Decoded parameter values for each solution over time.
 * <li>VAR_CONF.txt: Human-readable configurations with indicator values,
 * appended per checkpoint.
 * </ul>
 */
public class ConsolidatedOutputResults implements EvaluationOutputWriter {

    private int evaluations;
    private final MetaOptimizationProblem<?> configurableAlgorithmProblem;
    private final String problemName;
    private final List<QualityIndicator> indicators;
    private final String outputDirectoryName;
    private final MetaOptimizerConfig config;

    private boolean headersWritten = false;

    public ConsolidatedOutputResults(
            String algorithmName,
            MetaOptimizationProblem<?> configurableAlgorithmProblem,
            String problemName,
            List<QualityIndicator> indicators,
            String outputDirectoryName) {
        this(configurableAlgorithmProblem, problemName, indicators, outputDirectoryName,
                MetaOptimizerConfig.builder()
                        .baseLevelAlgorithmName(algorithmName)
                        .build());
    }

    public ConsolidatedOutputResults(
            MetaOptimizationProblem<?> configurableAlgorithmProblem,
            String problemName,
            List<QualityIndicator> indicators,
            String outputDirectoryName,
            MetaOptimizerConfig config) {
        this.configurableAlgorithmProblem = configurableAlgorithmProblem;
        this.problemName = problemName;
        this.indicators = indicators;
        this.outputDirectoryName = outputDirectoryName;
        this.config = config;

        createOutputDirectory();
        writeMetadata();
    }

    private void createOutputDirectory() {
        File outputDirectory = new File(outputDirectoryName);
        if (!outputDirectory.exists()) {
            boolean result = new File(outputDirectoryName).mkdirs();
            if (!result) {
                throw new JMetalException("Error creating directory " + outputDirectoryName);
            }
        }
    }

    private void writeMetadata() {
        File metadataFile = new File(outputDirectoryName, "METADATA.txt");
        if (metadataFile.exists()) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile))) {
            // Header
            writer.write("=== Meta-Optimization Experiment ===");
            writer.newLine();
            writer.write("Date: " + LocalDateTime.now());
            writer.newLine();
            writer.newLine();

            // Meta-Optimizer Section
            writer.write("--- Meta-Optimizer ---");
            writer.newLine();
            writer.write("Algorithm: " + config.metaOptimizerName());
            writer.newLine();
            writer.write("Max Evaluations: " + config.metaMaxEvaluations());
            writer.newLine();
            writer.write("Population Size: " + config.metaPopulationSize());
            writer.newLine();
            writer.write("Cores: " + config.numberOfCores());
            writer.newLine();
            writer.newLine();

            // Base-Level Algorithm Section
            writer.write("--- Base-Level Algorithm ---");
            writer.newLine();
            writer.write("Algorithm: " + config.baseLevelAlgorithmName());
            writer.newLine();
            writer.write("Population/Swarm Size: " + config.baseLevelPopulationSize());
            writer.newLine();
            writer.write("Max Evaluations: " + config.baseLevelMaxEvaluations());
            writer.newLine();
            writer.write("Evaluation Strategy: " + config.evaluationBudgetStrategy());
            writer.newLine();
            writer.write("Parameter Space: " + config.yamlParameterSpaceFile());
            writer.newLine();
            writer.write("Optimizable Parameters: " + configurableAlgorithmProblem.numberOfVariables());
            writer.newLine();
            writer.newLine();

            // Training Problems Section
            writer.write("--- Training Set ---");
            writer.newLine();
            writer.write("Problem Family: " + problemName);
            writer.newLine();
            writer.write("Problems: "
                    + configurableAlgorithmProblem.problems().stream()
                            .map(Problem::name)
                            .collect(Collectors.joining(", ")));
            writer.newLine();
            writer.newLine();

            // Objectives Section
            writer.write("--- Quality Indicators ---");
            writer.newLine();
            writer.write("Indicators: "
                    + indicators.stream().map(QualityIndicator::name).collect(Collectors.joining(", ")));
            writer.newLine();
        } catch (IOException e) {
            throw new JMetalException("Error writing metadata", e);
        }
    }

    @Override
    public void updateEvaluations(int evaluations) {
        this.evaluations = evaluations;
    }

    @Override
    public void writeResultsToFiles(List<DoubleSolution> solutions) throws IOException {
        if (!headersWritten) {
            writeHeaders();
            headersWritten = true;
        }

        Archive<DoubleSolution> archive = new NonDominatedSolutionListArchive<>();
        solutions.forEach(archive::add);
        List<DoubleSolution> nonDominatedSolutions = archive.solutions();

        writeIndicators(nonDominatedSolutions);
        writeConfigurations(nonDominatedSolutions);
        writeVarConf(nonDominatedSolutions);
    }

    private void writeHeaders() throws IOException {
        // INDICATORS.csv header
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, "INDICATORS.csv"), true))) {
            if (new File(outputDirectoryName, "INDICATORS.csv").length() == 0) {
                writer.write("Evaluation,SolutionId,"
                        + indicators.stream().map(QualityIndicator::name).collect(Collectors.joining(",")));
                writer.newLine();
            }
        }

        // CONFIGURATIONS.csv header
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, "CONFIGURATIONS.csv"), true))) {
            if (new File(outputDirectoryName, "CONFIGURATIONS.csv").length() == 0) {
                String paramNames = configurableAlgorithmProblem.parameters().stream()
                        .map(Parameter::name)
                        .collect(Collectors.joining(","));
                writer.write("Evaluation,SolutionId," + paramNames);
                writer.newLine();
            }
        }
    }

    private void writeIndicators(List<DoubleSolution> solutions) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, "INDICATORS.csv"), true))) {
            for (int i = 0; i < solutions.size(); i++) {
                DoubleSolution solution = solutions.get(i);
                StringBuilder line = new StringBuilder();
                line.append(evaluations).append(",").append(i);
                for (double objective : solution.objectives()) {
                    line.append(",").append(objective);
                }
                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    private void writeConfigurations(List<DoubleSolution> solutions) throws IOException {
        List<Parameter<?>> topLevelParams = configurableAlgorithmProblem.topLevelParameters();
        List<Parameter<?>> flattenedParams = configurableAlgorithmProblem.parameters();

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, "CONFIGURATIONS.csv"), true))) {
            for (int i = 0; i < solutions.size(); i++) {
                DoubleSolution solution = solutions.get(i);
                java.util.Set<Integer> activeIndices = ParameterManagement.getActiveParameterIndices(
                        topLevelParams, flattenedParams, solution.variables());

                StringBuilder line = new StringBuilder();
                line.append(evaluations).append(",").append(i);

                for (int varIndex = 0; varIndex < flattenedParams.size(); varIndex++) {
                    Parameter<?> parameter = flattenedParams.get(varIndex);
                    if (activeIndices.contains(varIndex)) {
                        Double value = solution.variables().get(varIndex);
                        double decoded = ParameterManagement.decodeParameterToDoubleValues(parameter, value);
                        line.append(",").append(decoded);
                    } else {
                        line.append(",NaN");
                    }
                }

                writer.write(line.toString());
                writer.newLine();
            }
        }
    }

    private void writeVarConf(List<DoubleSolution> solutions) throws IOException {
        List<Parameter<?>> topLevelParams = configurableAlgorithmProblem.topLevelParameters();
        List<Parameter<?>> flattenedParams = configurableAlgorithmProblem.parameters();

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, "VAR_CONF.txt"), true))) {
            writer.write("# Evaluation: " + evaluations);
            writer.newLine();
            for (int i = 0; i < solutions.size(); i++) {
                DoubleSolution solution = solutions.get(i);
                java.util.Set<Integer> activeIndices = ParameterManagement.getActiveParameterIndices(
                        topLevelParams, flattenedParams, solution.variables());

                // Build indicator values string
                StringBuilder indicatorValues = new StringBuilder();
                for (int j = 0; j < indicators.size(); j++) {
                    if (j > 0) {
                        indicatorValues.append(" ");
                    }
                    indicatorValues.append(indicators.get(j).name())
                            .append("=")
                            .append(solution.objectives()[j]);
                }
                // Build configuration string with only active parameters
                StringBuilder parameterString = ParameterManagement.decodeActiveParametersToString(
                        flattenedParams, solution.variables(), activeIndices);
                // Write: indicators | configuration
                writer.write(indicatorValues.toString() + " | " + parameterString.toString());
                writer.newLine();
            }
            writer.newLine();
        }
    }
}
