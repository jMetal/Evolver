package org.uma.evolver.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.uma.evolver.metaoptimizationproblem.MetaOptimizationProblem;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

public class OutputResults {
  private int evaluations ;

  String algorithmName;
  MetaOptimizationProblem<?> configurableAlgorithmProblem;
  String problemName;
  List<QualityIndicator> indicators;
  String outputDirectoryName;

  public OutputResults(
      String algorithmName,
      MetaOptimizationProblem<?> configurableAlgorithmProblem,
      String problemName,
      List<QualityIndicator> indicators,
      String outputDirectoryName) {
    this.algorithmName = algorithmName;
    this.configurableAlgorithmProblem = configurableAlgorithmProblem;
    this.problemName = problemName;
    this.indicators = indicators;
    this.outputDirectoryName = outputDirectoryName;

    File outputDirectory = new File(outputDirectoryName);

    if (!outputDirectory.exists()) {
      boolean result = new File(outputDirectoryName).mkdirs();
      if (!result) {
        throw new JMetalException("Error creating directory " + outputDirectoryName);
      }
    }
  }

  public void writeResultsToFiles(List<DoubleSolution> solutions) throws IOException {
    var nonDominatedSolutionsArchive = new NonDominatedSolutionListArchive<DoubleSolution>();
    nonDominatedSolutionsArchive.addAll(solutions);

    StringBuilder problemDescriptionBuilder = new StringBuilder();
    problemDescriptionBuilder.append(algorithmName).append(".").append(problemName).append(".");

    if (!indicators.isEmpty()) {
      for (int i = 0; i < indicators.size() - 1; i++) {
        problemDescriptionBuilder.append(indicators.get(i).name()).append(".");
      }
      problemDescriptionBuilder.append(indicators.get(indicators.size() - 1).name());
    }

    String problemDescription = problemDescriptionBuilder.toString();

    writeFilesWithVariablesAndObjectives(nonDominatedSolutionsArchive, problemDescription);
    writeDecodedVariables(
        configurableAlgorithmProblem, nonDominatedSolutionsArchive, problemDescription);
    writeDecodedVariablesAsDoubleValues(
        configurableAlgorithmProblem, nonDominatedSolutionsArchive, problemDescription);
  }

  private void writeDecodedVariablesAsDoubleValues(
      MetaOptimizationProblem<?> configurableAlgorithmProblem,
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription)
      throws IOException {
    var varWithDecodedDoubleValuesSolutionsFileName =
        outputDirectoryName + "/VAR." + problemDescription + ".Conf.DoubleValues" + "." + evaluations + ".csv" ;

    ParameterManagement.writeDecodedSolutionsToDoubleValuesFoFile(
        configurableAlgorithmProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(),
        varWithDecodedDoubleValuesSolutionsFileName);
  }

  private void writeDecodedVariables(
      MetaOptimizationProblem<?> configurableAlgorithmProblem,
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription)
      throws IOException {
    var varWithDecodedSolutionsFileName =
        outputDirectoryName + "/VAR." + problemDescription + ".Conf." + evaluations + ".txt" ;

    ParameterManagement.writeDecodedSolutionsFoFile(
        configurableAlgorithmProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(),
        varWithDecodedSolutionsFileName);
  }

  private void writeFilesWithVariablesAndObjectives(
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription) {
    var varFileName = outputDirectoryName + "/VAR." + problemDescription + "." + evaluations + ".csv";
    var funFileName = outputDirectoryName + "/FUN." + problemDescription + "." + evaluations + ".csv" ;
    new SolutionListOutput(nonDominatedSolutionsArchive.solutions())
        .setVarFileOutputContext(new DefaultFileOutputContext(varFileName, ","))
        .setFunFileOutputContext(new DefaultFileOutputContext(funFileName, ","))
        .print();
  }

  public void updateEvaluations(int evaluations) {
    this.evaluations = evaluations;
  }
}
