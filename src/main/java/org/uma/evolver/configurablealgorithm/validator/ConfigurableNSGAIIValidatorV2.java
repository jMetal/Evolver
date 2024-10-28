package org.uma.evolver.configurablealgorithm.validator;

import static smile.math.MathEx.mean;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problemfamilyinfo.ProblemFamilyInfo;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIValidatorV2 {
  public void validate(String[] parameters, ProblemFamilyInfo problemFamilyInfo, String csvFileName) throws IOException {
    List<DoubleProblem> trainingSet = problemFamilyInfo.problemList();
    List<String> referenceFrontFileNames = problemFamilyInfo.referenceFronts();

    int populationSize = 100;
    int independentRuns = 15;

    QualityIndicator qualityIndicator = new NormalizedHypervolume();

    double[][] indicatorValues = new double[trainingSet.size()][independentRuns] ;

    for (int problemIndex : IntStream.range(0, trainingSet.size()).toArray()) {
      int maximumNumberOfEvaluations = problemFamilyInfo.evaluationsToOptimize().get(problemIndex) ;
      System.out.println("Problem: " + trainingSet.get(problemIndex).getClass().getName());

      double[][] referenceFront =
          VectorUtils.readVectors(referenceFrontFileNames.get(problemIndex), ",");


      IntStream.range(0, independentRuns).parallel().forEach(run -> {
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
      });

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
    }

    String csvFileHeader = "Problem,RunId,QualityIndicatorValue" ;
    FileWriter outputFile = openCSVFile(csvFileName);
    writeHeaderToCSVFile(outputFile, csvFileHeader);

    for (int problemIndex : IntStream.range(0, trainingSet.size()).toArray()) {
      for (int run : IntStream.range(0, independentRuns).toArray()) {
        writeLineToCSVFile(problemIndex, run, trainingSet, indicatorValues[problemIndex][run], outputFile);
      }
    }
    outputFile.close();
  }

  private static void writeLineToCSVFile(int problemIndex, int run, List<DoubleProblem> trainingSet, double qualityIndicatorValue, FileWriter outputFile) throws IOException {
    String outputLine = ""+ trainingSet.get(problemIndex).name() ;
    outputLine += ","+ run;
    outputLine += ","+ qualityIndicatorValue;
    outputFile.write(outputLine+"\n");
  }

  private static void writeHeaderToCSVFile(FileWriter outputFile, String csvFileHeader) throws IOException {
    outputFile.write(csvFileHeader +"\n");
  }

  private static FileWriter openCSVFile(String csvFileName) throws IOException {
      return new FileWriter(csvFileName, true);
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
}
