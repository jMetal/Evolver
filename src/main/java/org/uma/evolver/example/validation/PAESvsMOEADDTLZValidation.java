package org.uma.evolver.example.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.paes.DoublePAES;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * Validates the best PAES and MOEA/D configurations (by HV at meta-evaluation 2000) obtained
 * using the DTLZ problems as training set, on DTLZ1–7 with 3 objectives.
 * A standard MOEA/D (default parameters) is included as a reference algorithm.
 *
 * <p>Configurations come from:
 * <ul>
 *   <li>{@code experimentation/training/PAES_DTLZ} — best PAES (knnDistanceArchive, k=4)</li>
 *   <li>{@code experimentation/training/MOEAD_DTLZ} — best MOEA/D (augmentedTschebyscheff + DE)</li>
 *   <li>{@code src/main/resources/defaultConfigurations/MOEADDoubleDefault.txt} — standard MOEA/D</li>
 * </ul>
 *
 * <p>Each algorithm is run once per problem with 40 000 evaluations; the Pareto front is saved
 * to {@code results/validation/PAES_vs_MOEAD_DTLZ/<ProblemName>/<Algorithm>_FUN.csv}.
 * Reference fronts used: {@code resources/referenceFronts/DTLZ<N>.3D.csv}.
 *
 * <p>Run the Python report generator afterwards:
 * {@code conda run -n evolver python scripts/plot_paes_vs_moead_validation.py
 *        results/validation/PAES_vs_MOEAD_DTLZ/ resources/referenceFronts/}
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class PAESvsMOEADDTLZValidation {

  private static final int MAX_EVALUATIONS = 40_000;
  private static final int POPULATION_SIZE = 100;
  private static final String WEIGHT_VECTORS_DIR = "resources/weightVectors";
  private static final String OUTPUT_DIR = "results/validation/PAES_vs_MOEAD_DTLZ/";

  // Best MOEA/D config: min HVMinus at eval 2000 in MOEAD_DTLZ experiment
  private static final String MOEAD_DTLZ_CONFIG =
      String.join(
          " ",
          "--neighborhoodSize 35",
          "--maximumNumberOfReplacedSolutions 4",
          "--aggregationFunction augmentedTschebyscheff",
          "--normalizeObjectives false",
          "--algorithmResult externalArchive",
          "--archiveType unboundedArchive",
          "--subProblemIdGenerator randomPermutationCycle",
          "--createInitialSolutions oppositionBased",
          "--variation differentialEvolutionVariation",
          "--mutation uniform",
          "--mutationProbabilityFactor 0.35505719948543835",
          "--mutationRepairStrategy random",
          "--uniformMutationPerturbation 0.28194804518277233",
          "--differentialEvolutionCrossover RAND_1_BIN",
          "--CR 0.230279526587066",
          "--F 0.9915855135940004",
          "--selection populationAndNeighborhoodMatingPoolSelection",
          "--neighborhoodSelectionProbability 0.09276388489411286");

  // Standard MOEA/D: default parameters from MOEADDoubleDefault.txt
  private static final String MOEAD_DEFAULT_CONFIG =
      String.join(
          " ",
          "--neighborhoodSize 20",
          "--maximumNumberOfReplacedSolutions 2",
          "--aggregationFunction penaltyBoundaryIntersection",
          "--normalizeObjectives false",
          "--pbiTheta 5.0",
          "--algorithmResult population",
          "--createInitialSolutions default",
          "--subProblemIdGenerator randomPermutationCycle",
          "--variation crossoverAndMutationVariation",
          "--crossover SBX",
          "--crossoverProbability 0.9",
          "--crossoverRepairStrategy bounds",
          "--sbxDistributionIndex 20.0",
          "--mutation polynomial",
          "--mutationProbabilityFactor 1.0",
          "--mutationRepairStrategy bounds",
          "--polynomialMutationDistributionIndex 20.0",
          "--selection populationAndNeighborhoodMatingPoolSelection",
          "--neighborhoodSelectionProbability 0.9");

  // Best PAES config: min HVMinus at eval 2000 in PAES_DTLZ experiment
  private static final String PAES_DTLZ_CONFIG =
      String.join(
          " ",
          "--paesArchiveType knnDistanceArchive",
          "--knnDistanceArchiveK 4",
          "--algorithmResult externalArchive",
          "--archiveSelectionProbability 0.013727891319631647",
          "--mutation levyFlight",
          "--mutationProbabilityFactor 1.5474172144174396",
          "--mutationRepairStrategy bounds",
          "--levyFlightMutationBeta 1.9048695481272513",
          "--levyFlightMutationStepSize 0.26218782546309416");

  private static final List<Problem<DoubleSolution>> PROBLEMS =
      List.of(
          // DTLZ1–7 with 3 objectives (default constructors)
          new DTLZ1(), new DTLZ2(), new DTLZ3(), new DTLZ4(),
          new DTLZ5(), new DTLZ6(), new DTLZ7());

  public static void main(String[] args) throws IOException {
    for (Problem<DoubleSolution> problem : PROBLEMS) {
      System.out.printf("▶ %s (%d objectives)%n",
          problem.name(), problem.numberOfObjectives());
      runAndSave(problem, "PAES_DTLZ",       PAES_DTLZ_CONFIG);
      runAndSave(problem, "MOEAD_DTLZ",      MOEAD_DTLZ_CONFIG);
      runAndSave(problem, "MOEAD_DEFAULT",   MOEAD_DEFAULT_CONFIG);
    }
    System.out.println("Done. Results in: " + OUTPUT_DIR);
  }

  private static void runAndSave(
      Problem<DoubleSolution> problem, String label, String config) throws IOException {

    var algorithm =
        switch (label) {
          case "PAES_DTLZ" -> {
            var paes = new DoublePAES(
                problem, POPULATION_SIZE, MAX_EVALUATIONS,
                new YAMLParameterSpace("PAESDouble.yaml", new DoubleParameterFactory()));
            paes.parse(config.split("\\s+"));
            yield paes.build();
          }
          default -> {
            var moead = new DoubleMOEAD(
                problem, POPULATION_SIZE, MAX_EVALUATIONS,
                WEIGHT_VECTORS_DIR,
                new YAMLParameterSpace("MOEADDouble.yaml", new DoubleParameterFactory()));
            moead.parse(config.split("\\s+"));
            yield moead.build();
          }
        };

    algorithm.run();

    String dir = OUTPUT_DIR + problem.name() + "/";
    new File(dir).mkdirs();
    new SolutionListOutput(algorithm.result())
        .setFunFileOutputContext(
            new DefaultFileOutputContext(dir + label + "_FUN.csv", ","))
        .print();

    System.out.printf("  %s → %d solutions%n", label, algorithm.result().size());
  }
}
