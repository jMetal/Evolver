package org.uma.evolver.util.irace;

import java.io.IOException;

import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIDoubleParameterSpace;
import org.uma.evolver.parameter.type.StringParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

/**
 * A utility class for running NSGA-II with irace for automatic algorithm configuration.
 * This class is designed to be called from the command line with specific parameters
 * and returns the hypervolume value of the obtained solution set, which is used by irace
 * to evaluate different configurations.
 *
 * @author Antonio J. Nebro
 */
public class AutoNSGAIIIraceHV {
  /**
   * Main method that runs NSGA-II with the provided configuration and returns the hypervolume value.
   * The result is printed to standard output for irace to capture.
   *
   * @param args Command line arguments containing the configuration parameters
   * @throws IOException If there is an error reading the reference front file
   */
  public static void main(String[] args) throws IOException {
    // Parse problem name parameter and load the problem
    StringParameter problemNameParameter = new StringParameter("problemName");
    problemNameParameter.parse(args);
    Problem<DoubleSolution> problem = ProblemFactory.loadProblem(problemNameParameter.value());

    // Parse reference front filename parameter
    StringParameter referenceFrontFilenameParameter = new StringParameter("referenceFrontFileName");
    referenceFrontFilenameParameter.parse(args);
    String referenceFrontFilename = referenceFrontFilenameParameter.value();

    // Parse population size parameter
    StringParameter populationSizeParameter = new StringParameter("populationSize");
    populationSizeParameter.parse(args);
    int populationSize = Integer.parseInt(populationSizeParameter.value());

    // Parse maximum number of evaluations parameter
    StringParameter maximumNumberOfEvaluationsParameter = new StringParameter("maximumNumberOfEvaluations");
    maximumNumberOfEvaluationsParameter.parse(args);
    int maximumNumberOfEvaluations = Integer.parseInt(maximumNumberOfEvaluationsParameter.value());

    // Create and configure the base NSGA-II algorithm
    var baseNSGAII = new DoubleNSGAII(
        problem,
        populationSize,
        maximumNumberOfEvaluations,
        new NSGAIIDoubleParameterSpace());

    // Parse any additional NSGA-II specific parameters
    baseNSGAII.parse(args);

    // Build and run the algorithm
    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();
    nsgaII.run();

    // Load and process the reference front
    String referenceFrontFile = "resources/referenceFrontsCSV/" + referenceFrontFilename;
    double[][] referenceFront = VectorUtils.readVectors(referenceFrontFile, ",");
    double[][] front = getMatrixWithObjectiveValues(nsgaII.result());

    // Normalize the fronts for hypervolume calculation
    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    double[][] normalizedFront = NormalizeUtils.normalize(
        front,
        NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
        NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    // Calculate and output the hypervolume (inverted for minimization)
    var qualityIndicator = new PISAHypervolume(normalizedReferenceFront);
    System.out.println(qualityIndicator.compute(normalizedFront) * -1.0);
  }
}
