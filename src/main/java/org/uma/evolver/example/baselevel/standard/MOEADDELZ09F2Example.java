package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Runs MOEA/D-DE (differential evolution variation with the Tschebyscheff aggregation) on LZ09F2,
 * plotting the front as the run advances.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEADDELZ09F2Example {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new LZ09F2();
    String referenceFrontFileName = "resources/referenceFronts/LZ09_F2.csv";

    String yamlParameterSpaceFile = "MOEADDouble.yaml";
    String weightVectorFilesDirectory = "resources/weightVectors";
    int populationSize = 300;
    int maximumNumberOfEvaluations = 175000;

    String[] parameters =
        """
        --neighborhoodSize 20
        --maximumNumberOfReplacedSolutions 2
        --aggregationFunction tschebyscheff
        --normalizeObjectives true
        --epsilonParameterForNormalization 4
        --algorithmResult population
        --createInitialSolutions default
        --variation differentialEvolutionVariation
        --subProblemIdGenerator randomPermutationCycle
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --differentialEvolutionCrossover RAND_1_BIN
        --CR 1.0
        --F 0.5
        --selection populationAndNeighborhoodMatingPoolSelection
        --neighborhoodSelectionProbability 0.9
        """
            .split("\\s+");

    int evaluationObserverFrequency = 1000;
    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseMOEAD =
        new DoubleMOEAD(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            weightVectorFilesDirectory,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseMOEAD.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> moead = baseMOEAD.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<DoubleSolution>(
            "MOEAD. " + problem.name(),
            chartDisplayDelay,
            chartUpdateFrequency,
            referenceFrontFileName,
            "F1",
            "F2");
    moead.observable().register(evaluationObserver);
    moead.observable().register(runTimeChartObserver);

    moead.run();

    JMetalLogger.logger.info("Total execution time : " + moead.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + moead.numberOfEvaluations());

    new SolutionListOutput(moead.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(moead.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
