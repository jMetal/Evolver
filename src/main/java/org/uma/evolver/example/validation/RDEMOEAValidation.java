package org.uma.evolver.example.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.uma.evolver.algorithm.rdemoea.DoubleRDEMOEA;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.trainingset.DTLZ3DTrainingSet;
import org.uma.evolver.trainingset.RE3DTrainingSet;
import org.uma.evolver.trainingset.RWA3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * Validation program for the best RDEMOEA configuration found by the
 * meta-optimization of AsyncNSGAIIOptimizingRDEMOEAForBenchmarkRE3D.
 *
 * <p>Runs the configuration (Solution 0 — knee point) on all DTLZ 3D, RE 3D
 * and RWA 3D problems with 10 000 evaluations, writing FUN*.csv files that a
 * companion Python script turns into an HTML report with Plotly scatter plots.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class RDEMOEAValidation {

  private static final int POPULATION_SIZE = 69;
  private static final int MAX_EVALUATIONS = 10000;

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
    String outputDir = "results/validation/RDEMOEA";
    Files.createDirectories(Path.of(outputDir));

    ParameterSpace parameterSpace =
        new YAMLParameterSpace("RDEMOEADouble.yaml", new DoubleParameterFactory());

    List<TrainingSet<DoubleSolution>> trainingSets =
        List.of(new DTLZ3DTrainingSet(), new RE3DTrainingSet(), new RWA3DTrainingSet());

    for (TrainingSet<DoubleSolution> ts : trainingSets) {
      List<Problem<DoubleSolution>> problems = ts.problemList();
      List<String> refFronts = ts.referenceFronts();

      for (int i = 0; i < problems.size(); i++) {
        Problem<DoubleSolution> problem = problems.get(i);
        String problemName = problem.getClass().getSimpleName();
        String refFrontPath = refFronts.get(i);

        JMetalLogger.logger.info("Running RDEMOEA on " + problemName + " ...");

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

        // Copy the reference front path so the Python script can find it
        Files.writeString(
            Path.of(problemDir + "/REF_FRONT_PATH.txt"), refFrontPath);
      }
    }

    JMetalLogger.logger.info("All runs finished. Results in " + outputDir);
    System.exit(0);
  }
}
