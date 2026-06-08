package org.uma.evolver.example.configuration;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.agemoea.DoubleAGEMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;

/**
 * Evolver port of jmetal-component {@code AGEMOEAComparisonExample}.
 *
 * <p>Runs AGE-MOEA and AGE-MOEA-II on the 7 DTLZ problems (3 objectives) using the same default
 * configuration. Quality indicators are printed for each problem/algorithm pair so the results can
 * be compared directly against the jMetal reference example.
 */
public class AGEMOEAComparisonExample {

  private record ProblemConfig(String name, String referenceFront, int evaluations) {}

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "AGEMOEADouble.yaml";

    List<ProblemConfig> problems =
        List.of(
            new ProblemConfig(
                "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1",
                "resources/referenceFronts/DTLZ1.3D.csv",
                40000),
            new ProblemConfig(
                "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2",
                "resources/referenceFronts/DTLZ2.3D.csv",
                25000),
            new ProblemConfig(
                "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3",
                "resources/referenceFronts/DTLZ3.3D.csv",
                40000),
            new ProblemConfig(
                "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4",
                "resources/referenceFronts/DTLZ4.3D.csv",
                25000),
            new ProblemConfig(
                "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ5",
                "resources/referenceFronts/DTLZ5.3D.csv",
                25000),
            new ProblemConfig(
                "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ6",
                "resources/referenceFronts/DTLZ6.3D.csv",
                25000),
            new ProblemConfig(
                "org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7",
                "resources/referenceFronts/DTLZ7.3D.csv",
                25000));

    List<String> variants = List.of("agemoea", "agemoea2");

    int populationSize = 100;

    for (ProblemConfig config : problems) {
      Problem<DoubleSolution> problem = ProblemFactory.<DoubleSolution>loadProblem(config.name());

      for (String variant : variants) {
        String[] parameters =
            ("--agemoeaVariant " + variant + " "
                    + "--algorithmResult population "
                    + "--createInitialSolutions default "
                    + "--offspringPopulationSize 100 "
                    + "--variation crossoverAndMutationVariation "
                    + "--crossover SBX "
                    + "--crossoverProbability 0.9 "
                    + "--crossoverRepairStrategy bounds "
                    + "--sbxDistributionIndex 30.0 "
                    + "--mutation polynomial "
                    + "--mutationProbabilityFactor 1.0 "
                    + "--mutationRepairStrategy bounds "
                    + "--polynomialMutationDistributionIndex 20.0 "
                    + "--selection tournament "
                    + "--selectionTournamentSize 2")
                .split("\\s+");

        var baseAGEMOEA =
            new DoubleAGEMOEA(
                problem,
                populationSize,
                config.evaluations(),
                new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

        baseAGEMOEA.parse(parameters);
        EvolutionaryAlgorithm<DoubleSolution> algorithm = baseAGEMOEA.build();
        algorithm.run();

        JMetalLogger.logger.info("==================================================");
        JMetalLogger.logger.info(
            "Algorithm : " + (variant.equals("agemoea2") ? "AGE-MOEA-II" : "AGE-MOEA"));
        JMetalLogger.logger.info("Problem   : " + problem.name());
        JMetalLogger.logger.info("Time (ms) : " + algorithm.totalComputingTime());
        JMetalLogger.logger.info("Evaluations: " + algorithm.numberOfEvaluations());

        QualityIndicatorUtils.printQualityIndicators(
            SolutionListUtils.getMatrixWithObjectiveValues(algorithm.result()),
            VectorUtils.readVectors(config.referenceFront(), ","));
      }
    }
  }
}
