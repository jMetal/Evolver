package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.agemoea.DoubleAGEMOEA;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Evolver port of jmetal-component {@code AGEMOEA2DefaultConfigurationExample}.
 *
 * <p>Runs AGE-MOEA-II on DTLZ3 with SBX(p=0.9, di=30), Polynomial(p=1/n, di=20), pop=100, 40000
 * evaluations.
 */
public class AGEMOEAIIForDTLZ3Example {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new DTLZ3();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ3.3D.csv";

    String yamlParameterSpaceFile = "AGEMOEADouble.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 40000;

    String[] parameters =
        """
        --replacement agemoeaII
        --algorithmResult population
        --createInitialSolutions default
        --offspringPopulationSize 100
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 30.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --selection tournament
        --selectionTournamentSize 2
        """
            .split("\\s+");

    var baseAGEMOEA =
        new DoubleAGEMOEA(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseAGEMOEA.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> agemoeaII = baseAGEMOEA.build();
    agemoeaII.run();

    JMetalLogger.logger.info("Total execution time : " + agemoeaII.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + agemoeaII.numberOfEvaluations());

    new SolutionListOutput(agemoeaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(agemoeaII.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
