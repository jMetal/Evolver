package org.uma.evolver.problem;

import static org.uma.evolver.util.ParameterManagement.decodeParametersToString;
import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;
import static smile.math.MathEx.median;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * This class represents a meta-optimization problem for configuring an algorithm based on quality indicators.
 * It extends the BaseMetaOptimizationProblem class.
 */
public class MetaOptimizationProblem extends BaseMetaOptimizationProblem {

    private List<QualityIndicator> indicators;
    private List<Parameter<?>> parameters;
    private double[][] normalizedReferenceFront;
    private double[][] referenceFront;
    private final int numberOfIndependentRuns;
    private ConfigurableAlgorithmBuilder configurableAlgorithm;

    /**
     * Constructs a MetaOptimizationProblem with a configurable algorithm, a reference front file name, and a list of quality indicators.
     * Sets number of independent runs to 1.
     *
     * @param configurableAlgorithm  The builder for the configurable algorithm to be optimized.
     * @param referenceFrontFileName The name of the file containing the reference front for quality indicators.
     * @param indicators             The list of quality indicators to be used in the optimization.
     */
    public MetaOptimizationProblem(ConfigurableAlgorithmBuilder configurableAlgorithm,
                                   String referenceFrontFileName, List<QualityIndicator> indicators) {
        this(configurableAlgorithm, referenceFrontFileName, indicators, 1);
    }

    /**
     * Constructs a MetaOptimizationProblem with a configurable algorithm, a reference front file name, a list of quality indicators, and the number of independent runs.
     *
     * @param configurableAlgorithmBuilder The builder for the configurable algorithm to be optimized.
     * @param referenceFrontFileName       The name of the file containing the reference front for quality indicators.
     * @param indicators                   The list of quality indicators to be used in the optimization.
     * @param numberOfIndependentRuns      The number of independent runs to compute the median values of the quality indicators.
     */
    public MetaOptimizationProblem(ConfigurableAlgorithmBuilder configurableAlgorithmBuilder,
                                   String referenceFrontFileName, List<QualityIndicator> indicators,
                                   int numberOfIndependentRuns) {
        this.configurableAlgorithm = configurableAlgorithmBuilder;
        this.indicators = indicators;
        this.numberOfIndependentRuns = numberOfIndependentRuns;

        parameters = ConfigurableAlgorithmBuilder.parameterFlattening(
                configurableAlgorithmBuilder.configurableParameterList());

        List<Double> lowerLimit = new ArrayList<>();
        List<Double> upperLimit = new ArrayList<>();

        for (int i = 0; i < parameters.size(); i++) {
            lowerLimit.add(0.0);
            upperLimit.add(1.0);
        }

        computeNormalizedReferenceFront(referenceFrontFileName);

        variableBounds(lowerLimit, upperLimit);
        for (var parameter : parameters) {
            JMetalLogger.logger.info(parameter.name() + ",");
        }
        JMetalLogger.logger.info("");
    }

    /**
     * Compute the normalized reference front from the reference front file.
     *
     * @param referenceFrontFileName The name of the file containing the reference front.
     */
    private void computeNormalizedReferenceFront(String referenceFrontFileName) {
        referenceFront = null;
        try {
            referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
        } catch (IOException e) {
            throw new JMetalException("The file does not exist", e);
        }
        normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    }

    /**
     * Returns the number of objectives in the optimization problem (number of quality indicators).
     *
     * @return The number of objectives.
     */
    @Override
    public int numberOfVariables() {
        return parameters.size();
    }


    /**
     * Returns the number of objectives in the optimization problem (number of quality indicators).
     *
     * @return The number of objectives.
     */
    @Override
    public int numberOfObjectives() {
        return indicators.size();
    }


    /**
     * Returns the number of constraints in the optimization problem (always 0 for this problem).
     *
     * @return The number of constraints.
     */
    @Override
    public int numberOfConstraints() {
        return 0;
    }

    /**
     * Returns the name of the optimization problem.
     *
     * @return The name of the problem.
     */
    @Override
    public String name() {
        return "Configurable algorithm problem";
    }

    /**
     * Returns the list of parameters of the configurable algorithm to be optimized.
     *
     * @return The list of parameters.
     */
    public List<Parameter<?>> parameters() {
        return parameters;
    }


    /**
     * Evaluates the given solution by running the configurable algorithm with the decoded parameters and computing the median values of quality indicators.
     *
     * @param solution The solution to be evaluated.
     * @return The solution with its objectives updated to the median values of the quality indicators.
     */
    @Override
    public DoubleSolution evaluate(DoubleSolution solution) {
        StringBuilder parameterString = decodeParametersToString(parameters, solution.variables());

        String[] parameterArray = parameterString.toString().split("\\s+");

        double[] medianIndicatorValues = computeIndependentRuns(parameterArray);
        IntStream.range(0, indicators.size()).forEach(i -> solution.objectives()[i] = medianIndicatorValues[i]);

        return solution;
    }

    /**
     * Evaluates the given solution by running the configurable algorithm with the decoded parameters and computing the median values of quality indicators
     * over the number of independent runs.
     *
     * @param parameterArray The array of decoded parameters.
     * @return An array containing the median values of the quality indicators.
     */
    private double[] computeIndependentRuns(String[] parameterArray) {
        double[] medianIndicatorValues = new double[indicators.size()];
        double[][] indicatorValues = new double[indicators.size()][];
        IntStream.range(0, indicators.size()).forEach(i -> indicatorValues[i] = new double[numberOfIndependentRuns]);

        for (int runId = 0; runId < numberOfIndependentRuns; runId++) {
            var algorithm = configurableAlgorithm
                    .createBuilderInstance()
                    .parse(parameterArray)
                    .build();

            String parameters = Arrays.stream(parameterArray).reduce("", String::concat);
            System.out.println("Start run " + parameters) ;
            algorithm.run();
            System.out.println("End run " + parameters) ;

            NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutions = new NonDominatedSolutionListArchive<>();
            nonDominatedSolutions.addAll((List<DoubleSolution>) algorithm.result());

            double[][] front = getMatrixWithObjectiveValues(nonDominatedSolutions.solutions());
            double[][] normalizedFront =
                    NormalizeUtils.normalize(
                            front,
                            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
                            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

            IntStream.range(0, indicators.size()).forEach(index -> {
                indicators.get(index).referenceFront(normalizedReferenceFront);
            });

            for (int indicatorId = 0; indicatorId < indicators.size(); indicatorId++) {
                indicators.get(indicatorId).referenceFront(normalizedReferenceFront);
                var indicatorValue = indicators.get(indicatorId).compute(normalizedFront);
                indicatorValues[indicatorId][runId] = indicatorValue ;
            }
        }

        for (int i = 0; i < indicators.size(); i++) {
            medianIndicatorValues[i] = median(indicatorValues[i]);
        }

        return medianIndicatorValues;
    }
}
