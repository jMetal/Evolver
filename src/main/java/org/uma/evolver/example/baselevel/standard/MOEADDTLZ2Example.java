package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.moead.DoubleMOEAD;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
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
 * Runs MOEA/D with the penalty-based boundary intersection (PBI) aggregation on DTLZ2 with three
 * objectives, plotting the front as the run advances.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEADDTLZ2Example {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new DTLZ2();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ2.3D.csv";

    String yamlParameterSpaceFile = "MOEADDouble.yaml";
    String weightVectorFilesDirectory = "resources/weightVectors";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 40000;

    String[] parameters =
        """
        --neighborhoodSize 20
        --maximumNumberOfReplacedSolutions 2
        --aggregationFunction penaltyBoundaryIntersection
        --normalizeObjectives false
        --pbiTheta 5.0
        --algorithmResult population
        --createInitialSolutions default
        --subProblemIdGenerator randomPermutationCycle
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
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
