package org.uma.evolver.example.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.algorithm.paes.DoublePAES;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.re.RE21;
import org.uma.jmetal.problem.multiobjective.re.RE22;
import org.uma.jmetal.problem.multiobjective.re.RE23;
import org.uma.jmetal.problem.multiobjective.re.RE24;
import org.uma.jmetal.problem.multiobjective.re.RE25;
import org.uma.jmetal.problem.multiobjective.re.RE31;
import org.uma.jmetal.problem.multiobjective.re.RE32;
import org.uma.jmetal.problem.multiobjective.re.RE33;
import org.uma.jmetal.problem.multiobjective.re.RE34;
import org.uma.jmetal.problem.multiobjective.re.RE35;
import org.uma.jmetal.problem.multiobjective.re.RE36;
import org.uma.jmetal.problem.multiobjective.re.RE37;
import org.uma.jmetal.problem.multiobjective.rwa.RWA1;
import org.uma.jmetal.problem.multiobjective.rwa.RWA2;
import org.uma.jmetal.problem.multiobjective.rwa.RWA3;
import org.uma.jmetal.problem.multiobjective.rwa.RWA4;
import org.uma.jmetal.problem.multiobjective.rwa.RWA5;
import org.uma.jmetal.problem.multiobjective.rwa.RWA6;
import org.uma.jmetal.problem.multiobjective.rwa.RWA7;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

/**
 * Validates the best PAES and MOEA/D configurations (by HV at the last meta-evaluation
 * generation) on all RE and RWA problems with 2 or 3 objectives.
 *
 * <p>Configurations come from the meta-optimization experiments in
 * {@code experimentation/training/PAES_EXT} and {@code experimentation/training/MOEAD_EXT_PTS}.
 * Each algorithm is run once per problem with 10 000 evaluations; the Pareto front is saved
 * to {@code results/validation/PAES_vs_MOEAD/<ProblemName>/<Algorithm>_FUN.csv}.
 *
 * <p>Run the Python report generator afterwards:
 * {@code conda run -n evolver python scripts/plot_paes_vs_moead_validation.py
 *        results/validation/PAES_vs_MOEAD/ resources/referenceFronts/}
 */
public class PAESvsMOEADValidation {

  private static final int MAX_EVALUATIONS = 10_000;
  private static final int POPULATION_SIZE = 100;
  private static final String WEIGHT_VECTORS_DIR = "resources/weightVectors";
  private static final String OUTPUT_DIR = "results/validation/PAES_vs_MOEAD/";

  // Best MOEA/D config: min HVMinus at eval 2000 in MOEAD_EXT_PTS experiment
  private static final String MOEAD_CONFIG =
      String.join(
          " ",
          "--neighborhoodSize 10",
          "--maximumNumberOfReplacedSolutions 4",
          "--aggregationFunction augmentedTschebyscheff",
          "--normalizeObjectives true",
          "--epsilonParameterForNormalization 0.3554846276496209",
          "--algorithmResult externalArchive",
          "--archiveType unboundedArchive",
          "--subProblemIdGenerator cyclicIntegerSequence",
          "--createInitialSolutions cauchy",
          "--variation differentialEvolutionVariation",
          "--differentialEvolutionCrossover RAND_2_BIN",
          "--CR 0.39469219184051985",
          "--F 0.9889020509346556",
          "--mutation uniform",
          "--mutationProbabilityFactor 0.16186713516181672",
          "--mutationRepairStrategy bounds",
          "--uniformMutationPerturbation 0.7718291417423131",
          "--selection populationAndNeighborhoodMatingPoolSelection",
          "--neighborhoodSelectionProbability 0.6324336885795143");

  // Best PAES config: min HVMinus at eval 2000 in PAES_EXT experiment (2026-06-18, re-run with BestSolutionsArchive fix)
  private static final String PAES_CONFIG =
      String.join(
          " ",
          "--paesArchiveType crowdingDistanceArchive",
          "--algorithmResult externalArchive",
          "--archiveSelectionProbability 0.9189795374643632",
          "--mutation levyFlight",
          "--mutationProbabilityFactor 1.494881899307409",
          "--mutationRepairStrategy bounds",
          "--levyFlightMutationBeta 1.6558956256498565",
          "--levyFlightMutationStepSize 0.323402020766443");

  private static final List<Problem<DoubleSolution>> PROBLEMS =
      List.of(
          // RE — 2 objectives
          new RE21(), new RE22(), new RE23(), new RE24(), new RE25(),
          // RE — 3 objectives
          new RE31(), new RE32(), new RE33(), new RE34(), new RE35(), new RE36(), new RE37(),
          // RWA — 2 objectives
          new RWA1(),
          // RWA — 3 objectives (RWA8–10 have 4, 5, 7 objectives — excluded)
          new RWA2(), new RWA3(), new RWA4(), new RWA5(), new RWA6(), new RWA7());

  public static void main(String[] args) throws IOException {
    for (Problem<DoubleSolution> problem : PROBLEMS) {
      System.out.printf("▶ %s (%d objectives)%n",
          problem.name(), problem.numberOfObjectives());
      runAndSave(problem, "PAES",  PAES_CONFIG);
      runAndSave(problem, "MOEAD", MOEAD_CONFIG);
    }
    System.out.println("Done. Results in: " + OUTPUT_DIR);
  }

  private static void runAndSave(
      Problem<DoubleSolution> problem, String label, String config) throws IOException {

    var algorithm =
        switch (label) {
          case "PAES" -> {
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
