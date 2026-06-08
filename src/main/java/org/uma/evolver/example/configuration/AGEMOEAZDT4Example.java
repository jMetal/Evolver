package org.uma.evolver.example.configuration;

import java.io.IOException;
import org.uma.evolver.algorithm.agemoea.DoubleAGEMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Example: running AGE-MOEA (AGE-MOEA-II variant by default) on the ZDT4 problem.
 *
 * <p>The {@code agemoeaVariant} parameter selects between the original AGE-MOEA and AGE-MOEA-II.
 * Results are saved to {@code VAR.csv} and {@code FUN.csv}; a real-time chart shows the evolution
 * of the Pareto front.
 */
public class AGEMOEAZDT4Example {

  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "AGEMOEADouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters;
    if (args.length > 0) {
      parameters = args;
    } else {
      parameters =
          """
          --agemoeaVariant agemoea2
          --algorithmResult population
          --createInitialSolutions default
          --offspringPopulationSize 100
          --variation crossoverAndMutationVariation
          --crossover SBX
          --crossoverProbability 0.9
          --crossoverRepairStrategy bounds
          --sbxDistributionIndex 20.0
          --mutation polynomial
          --mutationProbabilityFactor 1.0
          --mutationRepairStrategy bounds
          --polynomialMutationDistributionIndex 20.0
          --selection tournament
          --selectionTournamentSize 2
          """
              .split("\\s+");
    }

    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    var baseAGEMOEA =
        new DoubleAGEMOEA(
            new ZDT4(),
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseAGEMOEA.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> agemoea = baseAGEMOEA.build();

    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("AGE-MOEA", 80, 1000, referenceFrontFileName, "F1", "F2");
    agemoea.observable().register(runTimeChartObserver);

    agemoea.run();

    JMetalLogger.logger.info("Total computing time: " + agemoea.totalComputingTime());

    new SolutionListOutput(agemoea.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(agemoea.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
