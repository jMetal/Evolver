package org.uma.evolver.meta.output;

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
 *
 * <p>
 * The three files keep only the non-dominated solutions of each checkpoint. With
 * {@link #writePopulation(boolean)}, the whole population of each checkpoint is also written, in the
 * same formats, to POPULATION_INDICATORS.csv and POPULATION_CONFIGURATIONS.csv.
 */
public class ConsolidatedOutputResults implements EvaluationOutputWriter {

    private int evaluations;
    private final long creationMillis = System.currentTimeMillis();
    private Long computingTimeMillis;
    private final MetaOptimizationProblem<?> configurableAlgorithmProblem;
    private final String problemName;
    private final List<QualityIndicator> indicators;
    private final String outputDirectoryName;
    private final MetaOptimizerConfig config;

    private boolean headersWritten = false;
    private boolean writePopulation = false;

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
            for (String line : config.stoppingConditionLines()) {
                writer.write(line);
                writer.newLine();
            }
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

    /**
     * Sets whether the whole population of each checkpoint is written as well, to
     * POPULATION_INDICATORS.csv and POPULATION_CONFIGURATIONS.csv (false by default).
     *
     * @return this object, for chaining
     */
    public ConsolidatedOutputResults writePopulation(boolean writePopulation) {
        this.writePopulation = writePopulation;
        return this;
    }

    @Override
    public void updateEvaluations(int evaluations) {
        this.evaluations = evaluations;
    }

    @Override
    public void updateComputingTime(long computingTimeMillis) {
        this.computingTimeMillis = computingTimeMillis;
    }

    /**
     * The computing time to write at a checkpoint, in minutes: the one set with {@link
     * #updateComputingTime} (the meta-optimizer's own clock), or else the time elapsed since this
     * object was created. It is written whatever the stopping condition is.
     */
    private double checkpointMinutes() {
        long millis = computingTimeMillis != null
                ? computingTimeMillis
                : System.currentTimeMillis() - creationMillis;
        return millis / 60_000.0;
    }

    static String timeLine(double minutes) {
        return "# Time (min): " + String.format(java.util.Locale.ROOT, "%.3f", minutes);
    }

    /**
     * Appends a wall-clock time record to METADATA.txt. Intended to be called once, after the
     * meta-optimizer has finished running, with the elapsed time in milliseconds.
     */
    public void writeWallClockTime(long elapsedTimeMillis) {
        File metadataFile = new File(outputDirectoryName, "METADATA.txt");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(metadataFile, true))) {
            writer.newLine();
            writer.write("--- Execution ---");
            writer.newLine();
            writer.write("Wall-clock time: " + formatDuration(elapsedTimeMillis)
                    + " (" + elapsedTimeMillis + " ms)");
            writer.newLine();
            writer.write("Meta-evaluations performed: " + evaluations);
            writer.newLine();
        } catch (IOException e) {
            throw new JMetalException("Error writing wall-clock time", e);
        }
    }

    private static String formatDuration(long elapsedTimeMillis) {
        long totalSeconds = elapsedTimeMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return hours + "h " + minutes + "m " + seconds + "s";
    }

    @Override
    public void writeResultsToFiles(List<DoubleSolution> solutions) throws IOException {
        if (!headersWritten) {
            writeHeaders("INDICATORS.csv", "CONFIGURATIONS.csv");
            if (writePopulation) {
                writeHeaders("POPULATION_INDICATORS.csv", "POPULATION_CONFIGURATIONS.csv");
            }
            headersWritten = true;
        }

        Archive<DoubleSolution> archive = new NonDominatedSolutionListArchive<>();
        solutions.forEach(archive::add);
        List<DoubleSolution> nonDominatedSolutions = archive.solutions();

        writeIndicators(nonDominatedSolutions, "INDICATORS.csv");
        writeConfigurations(nonDominatedSolutions, "CONFIGURATIONS.csv");
        writeVarConf(nonDominatedSolutions);
        if (writePopulation) {
            writeIndicators(solutions, "POPULATION_INDICATORS.csv");
            writeConfigurations(solutions, "POPULATION_CONFIGURATIONS.csv");
        }
        computingTimeMillis = null;
    }

    private void writeHeaders(String indicatorsFileName, String configurationsFileName)
            throws IOException {
        // Indicators file header
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, indicatorsFileName), true))) {
            if (new File(outputDirectoryName, indicatorsFileName).length() == 0) {
                writer.write("Evaluation,SolutionId,"
                        + indicators.stream().map(QualityIndicator::name).collect(Collectors.joining(",")));
                writer.newLine();
            }
        }

        // Configurations file header
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, configurationsFileName), true))) {
            if (new File(outputDirectoryName, configurationsFileName).length() == 0) {
                String paramNames = configurableAlgorithmProblem.parameters().stream()
                        .map(Parameter::name)
                        .collect(Collectors.joining(","));
                writer.write("Evaluation,SolutionId," + paramNames);
                writer.newLine();
            }
        }
    }

    private void writeIndicators(List<DoubleSolution> solutions, String fileName)
            throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, fileName), true))) {
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

    private void writeConfigurations(List<DoubleSolution> solutions, String fileName)
            throws IOException {
        List<Parameter<?>> topLevelParams = configurableAlgorithmProblem.topLevelParameters();
        List<Parameter<?>> flattenedParams = configurableAlgorithmProblem.parameters();

        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(new File(outputDirectoryName, fileName), true))) {
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
            writer.write(timeLine(checkpointMinutes()));
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
