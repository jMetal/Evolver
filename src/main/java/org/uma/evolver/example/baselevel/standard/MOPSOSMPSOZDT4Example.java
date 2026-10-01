package org.uma.evolver.example.baselevel.standard;

import java.io.IOException;
import org.uma.evolver.algorithm.mopso.BaseMOPSO;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
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
 * Runs the configurable MOPSO with a configuration equivalent to SMPSO (constrained velocity update
 * and a crowding distance leader archive) on ZDT4, plotting the front as the run advances.
 */
public class MOPSOSMPSOZDT4Example {

  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new ZDT4();
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String yamlParameterSpaceFile = "MOPSO.yaml";
    int leaderArchiveSize = 100;
    int maximumNumberOfEvaluations = 15000;

    String[] parameters =
        """
        --swarmSize 100
        --algorithmResult leaderArchive
        --leaderArchive crowdingDistanceArchive
        --swarmInitialization default
        --velocityInitialization defaultVelocityInitialization
        --velocityUpdate constrainedVelocityUpdate
        --c1Min 1.5
        --c1Max 2.5
        --c2Min 1.5
        --c2Max 2.5
        --globalBestInitialization defaultGlobalBestInitialization
        --globalBestUpdate defaultGlobalBestUpdate
        --positionUpdate defaultPositionUpdate
        --globalBestSelection tournamentSelection
        --selectionTournamentSize 2
        --perturbation frequencySelectionMutationBasedPerturbation
        --frequencyOfApplicationOfMutationOperator 7
        --mutation polynomial
        --polynomialMutationDistributionIndex 20.0
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --inertiaWeightComputingStrategy randomSelectedValue
        --randomInertiaWeightMin 0.1
        --randomInertiaWeightMax 0.5
        --velocityChangeWhenLowerLimitIsReached -1.0
        --velocityChangeWhenUpperLimitIsReached -1.0
        --localBestInitialization defaultLocalBestInitialization
        --localBestUpdate defaultLocalBestUpdate
        """
            .split("\\s+");

    int evaluationObserverFrequency = 1000;
    int chartUpdateFrequency = 1000;
    int chartDisplayDelay = 80;

    var baseMOPSO =
        new BaseMOPSO(
            problem,
            leaderArchiveSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new MOPSOParameterFactory()));

    baseMOPSO.parse(parameters);
    ParticleSwarmOptimizationAlgorithm mopso = baseMOPSO.build();

    var evaluationObserver = new EvaluationObserver(evaluationObserverFrequency);
    var runTimeChartObserver =
        new RunTimeChartObserver<DoubleSolution>(
            "MOPSO. " + problem.name(),
            chartDisplayDelay,
            chartUpdateFrequency,
            referenceFrontFileName,
            "F1",
            "F2");
    mopso.observable().register(evaluationObserver);
    mopso.observable().register(runTimeChartObserver);

    mopso.run();

    JMetalLogger.logger.info("Total execution time : " + mopso.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + mopso.numberOfEvaluations());

    new SolutionListOutput(mopso.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    QualityIndicatorUtils.printQualityIndicators(
        SolutionListUtils.getMatrixWithObjectiveValues(mopso.result()),
        VectorUtils.readVectors(referenceFrontFileName, ","));
  }
}
