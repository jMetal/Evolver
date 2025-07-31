package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.mopso.BaseMOPSO;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zcat.DefaultZCATSettings;
import org.uma.jmetal.problem.multiobjective.zcat.ZCAT3;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class MOPSO_ZCAT1 {
  public static void main(String[] args) {
    DefaultZCATSettings.numberOfObjectives = 3 ;
    DoubleProblem problem = new ZCAT3();
    String referenceFrontFileName = "resources/referenceFronts/ZCAT3.3D.csv";

    String[] parameters =
        ("--swarmSize 179 " +
                "--leaderArchive spatialSpreadDeviationArchive " +
                "--algorithmResult externalArchive " +
                "--swarmSizeWithArchive 143 " +
                "--externalArchiveType unboundedArchive " +
                "--swarmInitialization scatterSearch " +
                "--perturbation frequencySelectionMutationBasedPerturbation " +
                "--frequencyOfApplicationOfMutationOperator 7 " +
                "--mutation polynomial " +
                "--mutationProbabilityFactor 0.9732452766287183 " +
                "--mutationRepairStrategy random --uniformMutationPerturbation 0.36364768327674246 --polynomialMutationDistributionIndex 95.42264287729208 --nonUniformMutationPerturbation 0.42884292896360937 --linkedPolynomialMutationDistributionIndex 174.14564612437414 --levyFlightMutationBeta 1.9662214503617323 --levyFlightMutationStepSize 0.7189868819522982 --powerLawMutationDelta 9.790520761945027 --inertiaWeightComputingStrategy constantValue --inertiaWeight 0.6265820945896091 --randomInertiaWeightMin 0.22673029974616288 --randomInertiaWeightMax 0.6939733442959044 --linearIncreasingInertiaWeightMin 0.1625729816902584 --linearIncreasingInertiaWeightMax 0.6886555110746324 --linearDecreasingInertiaWeightMin 0.2920490327024692 --linearDecreasingInertiaWeightMax 0.6916226583153451 --velocityInitialization SPSO2007VelocityInitialization --velocityUpdate SPSO2011VelocityUpdate --c1Min 1.0741670960723346 --c1Max 2.171498683046506 --c2Min 1.0564507149457079 --c2Max 2.100634120223324 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection randomSelection --selectionTournamentSize 7 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached -0.5380292696316398 --velocityChangeWhenUpperLimitIsReached -0.26057612375446926 \n")
            .split("\\s+");

    String yamlParameterSpaceFile = "resources/parameterSpaces/MOPSO.yaml" ;
    var parameterSpace =
            new YAMLParameterSpace(yamlParameterSpaceFile, new MOPSOParameterFactory());

    var baseMOPSO = new BaseMOPSO(problem, 100, 150000, parameterSpace);
    baseMOPSO.parse(parameters);

    baseMOPSO.parameterSpace().topLevelParameters().forEach(System.out::println);

    ParticleSwarmOptimizationAlgorithm algorithm = baseMOPSO.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(1000);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOEAD. " + problem.name(), 80, 1000, referenceFrontFileName, "F1", "F2");

    algorithm.observable().register(evaluationObserver);
    algorithm.observable().register(runTimeChartObserver);

    algorithm.run();

    JMetalLogger.logger.info("Total computing time: " + algorithm.totalComputingTime());

    new SolutionListOutput(algorithm.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
