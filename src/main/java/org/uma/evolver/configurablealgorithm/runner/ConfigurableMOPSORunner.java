package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableMOPSORunner {

  public static void main(String[] args) {
    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    String[] parameters =
        ("--swarmSize 145 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization scatterSearch --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation nonUniform --mutationProbabilityFactor 0.03566121960270807 --mutationRepairStrategy random --polynomialMutationDistributionIndex 361.10340057213523 --linkedPolynomialMutationDistributionIndex 84.76657256846384 --uniformMutationPerturbation 0.7750088626631135 --nonUniformMutationPerturbation 0.5950092299356947 --frequencyOfApplicationOfMutationOperator 6 --inertiaWeightComputingStrategy linearDecreasingValue --weight 0.40323860712257964 --weightMin 0.41585315035081083 --weightMax 0.8311424052682814 --weightMin 0.29736089576010316 --weightMax 0.875583268755388 --weightMin 0.45844201746847146 --weightMax 0.9787357050196652 --velocityUpdate constrainedVelocityUpdate --c1Min 1.7725435766277897 --c1Max 2.1738973339591934 --c2Min 1.7667947437605462 --c2Max 2.108236667520124 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 9 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.8770617275038382 --velocityChangeWhenUpperLimitIsReached -0.24164697200601226 \n")
            .split("\\s+");

    var autoMOPSO = new ConfigurableMOPSO(new ZDT1(), 100, 20000);
    autoMOPSO.parse(parameters);

    ConfigurableMOPSO.print(autoMOPSO.configurableParameterList());

    ParticleSwarmOptimizationAlgorithm mopso = autoMOPSO.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOPSO", 80, 100,
            referenceFrontFileName, "F1", "F2");

    mopso.observable().register(evaluationObserver);
    mopso.observable().register(runTimeChartObserver);

    mopso.run();

    JMetalLogger.logger.info("Total computing time: " + mopso.totalComputingTime());

    new SolutionListOutput(mopso.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
