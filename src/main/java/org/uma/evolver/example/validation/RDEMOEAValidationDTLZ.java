package org.uma.evolver.example.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.algorithm.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.DTLZ3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.wfg.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * Validation program for RDEMOEA on DTLZ and WFG 3-objective problems with an
 * increased evaluation budget of 40 000 (vs. the 10 000 used in the general
 * validation).
 *
 * <p>Uses the same best configuration (Solution 0 — knee point) found by
 * the meta-optimization of AsyncNSGAIIOptimizingRDEMOEAForBenchmarkRE3D.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class RDEMOEAValidationDTLZ {

  private static final int POPULATION_SIZE = 69;
  private static final int MAX_EVALUATIONS = 40000;

  /** Best configuration found (Solution 0 — knee point of the Pareto front). */
  private static final String[] CONFIGURATION =
      ("--algorithmResult externalArchive "
              + "--populationSizeWithArchive 69 "
              + "--archiveType unboundedArchive "
              + "--createInitialSolutions sobol "
              + "--offspringPopulationSize 10 "
              + "--densityEstimator crowdingDistance "
              + "--ranking strengthRanking "
              + "--variation crossoverAndMutationVariation "
              + "--crossover PCX "
              + "--crossoverProbability 0.377367506433774 "
              + "--crossoverRepairStrategy bounds "
              + "--pcxCrossoverZeta 0.30716981506932284 "
              + "--pcxCrossoverEta 0.962654089859959 "
              + "--mutation levyFlight "
              + "--mutationProbabilityFactor 1.5315494053662444 "
              + "--mutationRepairStrategy bounds "
              + "--levyFlightMutationBeta 1.3333777887916205 "
              + "--levyFlightMutationStepSize 0.42432148597212666 "
              + "--selection boltzmann "
              + "--boltzmannTemperature 83.6914942621179 "
              + "--replacement rankingAndDensityEstimator "
              + "--removalPolicy oneShot")
          .split("\\s+");

  public static void main(String[] args) throws IOException {
    String outputDir = "results/validation/RDEMOEA_DTLZ_WFG_40k";
    Files.createDirectories(Path.of(outputDir));

    ParameterSpace parameterSpace =
        new YAMLParameterSpace("RDEMOEADouble.yaml", new DoubleParameterFactory());

    // -- DTLZ 3D problems --
    TrainingSet<DoubleSolution> dtlzSet = new DTLZ3DTrainingSet();
    List<Problem<DoubleSolution>> problems = new ArrayList<>(dtlzSet.problemList());
    List<String> refFronts = new ArrayList<>(dtlzSet.referenceFronts());

    // -- WFG 3D problems --
    DefaultWFGSettings.numberOfObjectives = 3;
    List<Problem<DoubleSolution>> wfgProblems = List.of(
        new WFG1(), new WFG2(), new WFG3(), new WFG4(), new WFG5(),
        new WFG6(), new WFG7(), new WFG8(), new WFG9());
    List<String> wfgRefFronts = List.of(
        "resources/referenceFronts/WFG1.3D.csv",
        "resources/referenceFronts/WFG2.3D.csv",
        "resources/referenceFronts/WFG3.3D.csv",
        "resources/referenceFronts/WFG4.3D.csv",
        "resources/referenceFronts/WFG5.3D.csv",
        "resources/referenceFronts/WFG6.3D.csv",
        "resources/referenceFronts/WFG7.3D.csv",
        "resources/referenceFronts/WFG8.3D.csv",
        "resources/referenceFronts/WFG9.3D.csv");
    problems.addAll(wfgProblems);
    refFronts.addAll(wfgRefFronts);

    for (int i = 0; i < problems.size(); i++) {
      Problem<DoubleSolution> problem = problems.get(i);
      String problemName = problem.getClass().getSimpleName();
      String refFrontPath = refFronts.get(i);

      JMetalLogger.logger.info(
          "Running RDEMOEA on " + problemName + " (" + MAX_EVALUATIONS + " evals) ...");

      var algorithm =
          new DoubleRDEMOEA(problem, POPULATION_SIZE, MAX_EVALUATIONS, parameterSpace.createInstance());
      algorithm.parse(CONFIGURATION);

      EvolutionaryAlgorithm<DoubleSolution> ea = algorithm.build();
      ea.run();

      JMetalLogger.logger.info(
          problemName + " done in " + ea.totalComputingTime() + " ms");

      String problemDir = outputDir + "/" + problemName;
      Files.createDirectories(Path.of(problemDir));

      new SolutionListOutput(ea.result())
          .setFunFileOutputContext(
              new DefaultFileOutputContext(problemDir + "/FUN.csv", ","))
          .setVarFileOutputContext(
              new DefaultFileOutputContext(problemDir + "/VAR.csv", ","))
          .print();

      Files.writeString(Path.of(problemDir + "/REF_FRONT_PATH.txt"), refFrontPath);
    }

    JMetalLogger.logger.info("All DTLZ+WFG runs finished. Results in " + outputDir);
    System.exit(0);
  }
}
