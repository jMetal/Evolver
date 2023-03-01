package org.uma.evolver.util;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

public class OutputResultsManagement {

  private final String outputDirectory;

  public OutputResultsManagement(String outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  public void writeResultsToFiles(List<DoubleSolution> solutions,
      String algorithmName,
      ConfigurableAlgorithmProblem configurableAlgorithmProblem,
      DoubleProblem problem,
      List<QualityIndicator> indicators)
      throws IOException {
    var nonDominatedSolutionsArchive = new NonDominatedSolutionListArchive<DoubleSolution>();
    nonDominatedSolutionsArchive.addAll(solutions);
    String problemDescription =
        algorithmName + "." + problem.name() + "."
            + indicators.get(0).name() + "." + indicators.get(1).name() + "."
            + problem.numberOfObjectives();

    writeFilesWithVariablesAndObjectives(nonDominatedSolutionsArchive, problemDescription);
    writeDecodedVariables(configurableAlgorithmProblem, nonDominatedSolutionsArchive,
        problemDescription);
    writeDecodedVariablesAsDoubleValues(configurableAlgorithmProblem, nonDominatedSolutionsArchive,
        problemDescription);
  }

  private void writeDecodedVariablesAsDoubleValues(
      ConfigurableAlgorithmProblem configurableAlgorithmProblem,
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription) throws IOException {
    var varWithDecodedDoubleValuesSolutionsFileName =
        outputDirectory + "/VAR." + problemDescription + ".Conf.DoubleValues.csv";
    ParameterManagement.writeDecodedSolutionsToDoubleValuesFoFile(
        configurableAlgorithmProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(), varWithDecodedDoubleValuesSolutionsFileName);
  }

  private void writeDecodedVariables(ConfigurableAlgorithmProblem configurableAlgorithmProblem,
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription) throws IOException {
    var varWithDecodedSolutionsFileName =
        outputDirectory + "/VAR." + problemDescription + ".Conf.csv";
    ParameterManagement.writeDecodedSolutionsFoFile(configurableAlgorithmProblem.parameters(),
        nonDominatedSolutionsArchive.solutions(), varWithDecodedSolutionsFileName);
  }

  private void writeFilesWithVariablesAndObjectives(
      NonDominatedSolutionListArchive<DoubleSolution> nonDominatedSolutionsArchive,
      String problemDescription) {
    var varFileName = outputDirectory + "/VAR." + problemDescription + ".csv";
    var funFileName = outputDirectory + "/FUN." + problemDescription + ".csv";
    new SolutionListOutput(nonDominatedSolutionsArchive.solutions())
        .setVarFileOutputContext(
            new DefaultFileOutputContext(varFileName, ","))
        .setFunFileOutputContext(
            new DefaultFileOutputContext(funFileName, ","))
        .print();
  }
}
