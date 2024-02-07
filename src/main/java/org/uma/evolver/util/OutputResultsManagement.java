package org.uma.evolver.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.uma.evolver.problem.BaseMetaOptimizationProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

public class OutputResultsManagement {
  private String suffix = ".csv" ;

  public record OutputResultsManagementParameters(
      String algorithmName,
      BaseMetaOptimizationProblem configurableAlgorithmProblem,
      String problemName,
      List<QualityIndicator> indicators,
      String outputDirectoryName) {
  }

  private final OutputResultsManagementParameters parameters;

  public OutputResultsManagement(OutputResultsManagementParameters outputResultsManagementParameters)
      throws IOException {
    this.parameters = outputResultsManagementParameters;

    File outputDirectory = new File(parameters.outputDirectoryName) ;
    /*
    if (outputDirectory.exists()) {
      FileUtils.deleteDirectory(outputDirectory);
    }

    boolean result = new File(parameters.outputDirectoryName).mkdirs() ;
    if (!result) {
      throw new JMetalException("Error creating directory " + parameters.outputDirectoryName) ;
    }*/
    if (!outputDirectory.exists()) {
      boolean result = new File(parameters.outputDirectoryName).mkdirs() ;
      if (!result) {
        throw new JMetalException("Error creating directory " + parameters.outputDirectoryName) ;
      }
    }
  }

  public void writeResultsToFiles(List<DoubleSolution> solutions)
      throws IOException {
    var nonDominatedSolutionsArchive = new NonDominatedSolutionListArchive<DoubleSolution>();
    nonDominatedSolutionsArchive.addAll(solutions);

    StringBuilder problemDescriptionBuilder = new StringBuilder();
    problemDescriptionBuilder.append(parameters.algorithmName)
        .append(".")
        .append(parameters.problemName)
        .append(".");

    List<QualityIndicator> indicators = parameters.indicators;

    if (!indicators.isEmpty()) {
      for (int i = 0; i < indicators.size() - 1; i++) {
        problemDescriptionBuilder.append(indicators.get(i).name()).append(".");
      }
      problemDescriptionBuilder.append(indicators.get(indicators.size() - 1).name());
    }

    String problemDescription = problemDescriptionBuilder.toString();


    writeFilesWithVariablesAndObjectives(nonDominatedSolutionsArchive, problemDescription);
    writeDecodedVariables(parameters.configurableAlgorithmProblem, nonDominatedSolutionsArchive,
        problemDescription);
    writeDecodedVariablesAsDoubleValues(parameters.configurableAlgorithmProblem, nonDominatedSolutionsArchive,
        problemDescription);
  }

  private void writeDecodedVariablesAsDoubleValues(
      BaseMetaOptimizationProblem configurableAlgorithmProblem,
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription) throws IOException {
    var varWithDecodedDoubleValuesSolutionsFileName =
        parameters.outputDirectoryName + "/VAR." + problemDescription + ".Conf.DoubleValues" + suffix;
    ParameterManagement.writeDecodedSolutionsToDoubleValuesFoFile(
        configurableAlgorithmProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(), varWithDecodedDoubleValuesSolutionsFileName);
  }

  private void writeDecodedVariables(BaseMetaOptimizationProblem configurableAlgorithmProblem,
                                     NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
                                     String problemDescription) throws IOException {
    var varWithDecodedSolutionsFileName =
        parameters.outputDirectoryName + "/VAR." + problemDescription + ".Conf" + suffix ;
    ParameterManagement.writeDecodedSolutionsFoFile(configurableAlgorithmProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(), varWithDecodedSolutionsFileName);
  }

  private void writeFilesWithVariablesAndObjectives(
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription) {
    var varFileName = parameters.outputDirectoryName + "/VAR." + problemDescription + suffix;
    var funFileName = parameters.outputDirectoryName + "/FUN." + problemDescription + suffix;
    new SolutionListOutput(nonDominatedSolutionsArchive.solutions())
        .setVarFileOutputContext(
            new DefaultFileOutputContext(varFileName, ","))
        .setFunFileOutputContext(
            new DefaultFileOutputContext(funFileName, ","))
        .print();
  }

  public void updateSuffix(String newSuffix) {
    this.suffix = newSuffix ;
  }
}
