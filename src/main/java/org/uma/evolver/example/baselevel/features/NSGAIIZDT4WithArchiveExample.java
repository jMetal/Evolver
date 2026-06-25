package org.uma.evolver.example.baselevel.features;

import java.io.IOException;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
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

/**
 * Example: running the configurable NSGA-II on ZDT4 with an external archive.
 *
 * <p>This {@code features} example illustrates the external-archive capability: NSGA-II returns a
 * crowding-distance external archive as the algorithm result
 * ({@code --algorithmResult externalArchive --archiveType crowdingDistanceArchive}) instead of the
 * final population. The rest of the configuration is standard — scatter-search initialization, SBX
 * crossover (probability 0.9, distribution index 20), polynomial mutation (probability 1/n,
 * distribution index 20), binary tournament selection, population 100, 25000 evaluations — so the
 * external archive is the distinguishing feature.
 *
 * <p>The parameter space is loaded from {@code NSGAIIDouble.yaml}. Results are written to
 * {@code VAR.csv}/{@code FUN.csv}, and the quality indicators against the ZDT4 reference front
 * ({@code resources/referenceFronts/ZDT4.csv}) are printed at the end of the run.
 */
public class NSGAIIZDT4WithArchiveExample {

  /**
   * Runs the example.
   *
   * @param args an optional configuration string; when provided, it overrides the built-in
   *     parameters (pass the same {@code --param value} tokens used in the {@code parameters}
   *     variable). When empty, the built-in external-archive configuration is used.
   */
  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "NSGAIIDouble.yaml";
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters;
    if (args.length > 0) parameters = args;
    else
      parameters = """
          --algorithmResult externalArchive
          --populationSizeWithArchive 100
          --archiveType crowdingDistanceArchive
          --createInitialSolutions scatterSearch
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
          """.split("\\s+");

    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    var baseNSGAII = new DoubleNSGAII(
        new ZDT4(),
        populationSize,
        maximumNumberOfEvaluations,
        new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory())
    );

    baseNSGAII.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> nsgaII = baseNSGAII.build();

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    QualityIndicatorUtils.printQualityIndicators(
            SolutionListUtils.getMatrixWithObjectiveValues(nsgaII.result()),
            VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
