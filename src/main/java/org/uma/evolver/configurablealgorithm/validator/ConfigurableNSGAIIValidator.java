package org.uma.evolver.configurablealgorithm.validator;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIValidator {

  public static List<DoubleProblem> ZDT_LIST =
      List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6());
  public static List<String> ZDT_REFERENCE_FRONTS =
      List.of(
          "resources/referenceFronts/ZDT1.csv",
          "resources/referenceFronts/ZDT2.csv",
          "resources/referenceFronts/ZDT3.csv",
          "resources/referenceFronts/ZDT4.csv",
          "resources/referenceFronts/ZDT6.csv");

  public double validate(String[] parameters) throws IOException {
    List<DoubleProblem> trainingSet = ZDT_LIST;
    List<String> referenceFrontFileNames = ZDT_REFERENCE_FRONTS;

    int maximumNumberOfEvaluations = 25000;
    int populationSize = 100;
    int independentRuns = 2;

    QualityIndicator qualityIndicator = new NormalizedHypervolume();

    for (int problemIndex : IntStream.range(0, ZDT_LIST.size()).toArray()) {
      System.out.println("Problem: " + trainingSet.get(problemIndex).getClass().getName());
      double[][] referenceFront =
              VectorUtils.readVectors(referenceFrontFileNames.get(problemIndex), ",");

      for (int run : IntStream.range(0, independentRuns).toArray()) {
        EvolutionaryAlgorithm<DoubleSolution> nsgaII = configureAndBuildNSGAII(parameters, problemIndex, trainingSet, populationSize, maximumNumberOfEvaluations);
        nsgaII.run();

        /*
        new SolutionListOutput(nsgaII.result())
                .setFunFileOutputContext(new DefaultFileOutputContext("FUN" + trainingSet.get(problemIndex).getClass().getName() + ".csv", ","))
                .print();
*/
        double qualityIndicatorValue = computeIndicatorValue(qualityIndicator, nsgaII, referenceFront);
        System.out.println("Run: " + run + ". Quality indicator value: " + qualityIndicatorValue);
      }
    }

    return 0.0 ;
  }

  private double computeIndicatorValue(QualityIndicator qualityIndicator, EvolutionaryAlgorithm<DoubleSolution> nsgaII, double[][] referenceFront) {
      return computeQualityIndicator(
              qualityIndicator,
              SolutionListUtils.getMatrixWithObjectiveValues(nsgaII.result()),
              referenceFront);
  }

  private EvolutionaryAlgorithm<DoubleSolution> configureAndBuildNSGAII(String[] parameters, int problemIndex, List<DoubleProblem> trainingSet, int populationSize, int maximumNumberOfEvaluations) {
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

  public static double computeQualityIndicator(
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
    double result = new ConfigurableNSGAIIValidator().validate(args) ;
    System.out.println("Result: " + result) ;
  }
}
