package org.uma.evolver.configurablealgorithm.validator;

import static smile.math.MathEx.mean;

import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problemfamilyinfo.ProblemFamilyInfo;
import org.uma.evolver.problemfamilyinfo.ZDTProblemFamilyInfo;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIValidator {
  public double validate(String[] parameters, ProblemFamilyInfo problemFamilyInfo ) throws IOException {
    List<DoubleProblem> trainingSet = problemFamilyInfo.problemList();
    List<String> referenceFrontFileNames = problemFamilyInfo.referenceFronts();

    int populationSize = 100;
    int independentRuns = 2;

    QualityIndicator qualityIndicator = new NormalizedHypervolume();

    double[][] indicatorValues = new double[trainingSet.size()][independentRuns] ;
    double[] means = new double[trainingSet.size()] ;

    for (int problemIndex : IntStream.range(0, trainingSet.size()).toArray()) {
      int maximumNumberOfEvaluations = problemFamilyInfo.evaluationsToOptimize().get(problemIndex) ;
      System.out.println("Problem: " + trainingSet.get(problemIndex).getClass().getName());

      double[][] referenceFront =
          VectorUtils.readVectors(referenceFrontFileNames.get(problemIndex), ",");

      for (int run : IntStream.range(0, independentRuns).toArray()) {
        EvolutionaryAlgorithm<DoubleSolution> nsgaII =
            configureAndBuildNSGAII(
                parameters, problemIndex, trainingSet, populationSize, maximumNumberOfEvaluations);
        nsgaII.run();
        double qualityIndicatorValue =
                computeQualityIndicator(
                        qualityIndicator,
                        SolutionListUtils.getMatrixWithObjectiveValues(nsgaII.result()),
                        referenceFront);
        indicatorValues[problemIndex][run] = qualityIndicatorValue ;
        System.out.println("Run: " + run + ". Quality indicator value: " + qualityIndicatorValue);
      }
      means[problemIndex] = mean(indicatorValues[problemIndex]) ;
      System.out.println("Mean: " + means[problemIndex]) ;

    }

    double globalMean = mean(means) ;

    return globalMean;
  }


  private EvolutionaryAlgorithm<DoubleSolution> configureAndBuildNSGAII(
      String[] parameters,
      int problemIndex,
      List<DoubleProblem> trainingSet,
      int populationSize,
      int maximumNumberOfEvaluations) {
    var configurableNSGAII =
        new ConfigurableNSGAII(
            trainingSet.get(problemIndex), populationSize, maximumNumberOfEvaluations);
    configurableNSGAII.parse(parameters);
    return configurableNSGAII.build();
  }

  private static double[][] getNormalizedReferenceFront(String referenceFrontFileName) {
    double[][] referenceFront;
    double[][] normalizedReferenceFront;
    try {
      referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
      normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    } catch (IOException e) {
      throw new JMetalException("The file does not exist", e);
    }

    return normalizedReferenceFront;
  }

  public double computeQualityIndicator(
      QualityIndicator indicator, double[][] front, double[][] referenceFront) {
    double[][] normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
    double[][] normalizedFront =
        NormalizeUtils.normalize(
            front,
            NormalizeUtils.getMinValuesOfTheColumnsOfAMatrix(referenceFront),
            NormalizeUtils.getMaxValuesOfTheColumnsOfAMatrix(referenceFront));

    indicator.referenceFront(normalizedReferenceFront);
    return indicator.compute(normalizedFront);
  }

  public static void main(String[] args) throws IOException {
    double result = new ConfigurableNSGAIIValidator().validate(args, new ZDTProblemFamilyInfo());
    System.out.println("\nGlobal mean: " + result);
  }
}
