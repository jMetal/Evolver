package org.uma.evolver.algorithm;

import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoMOPSO;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
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
    String referenceFrontFileName = "resources/ZDT1.csv";

    String[] parameters =
        ("--maximumNumberOfEvaluations 25000 --archiveSize 100 --swarmSize 36 --leaderArchive hypervolumeArchive --algorithmResult leaderArchive --swarmInitialization latinHypercubeSampling --velocityInitialization SPSO2007VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation polynomial --mutationProbabilityFactor 0.6694349585541994 --mutationRepairStrategy random --polynomialMutationDistributionIndex 237.87903868398354 --linkedPolynomialMutationDistributionIndex 83.6615133121268 --uniformMutationPerturbation 0.24743610468327507 --nonUniformMutationPerturbation 0.4434974481270127 --frequencyOfApplicationOfMutationOperator 8 --inertiaWeightComputingStrategy linearDecreasingValue --weight 0.9531985222162719 --weightMin 0.4468438347552981 --weightMax 0.7022321092754226 --weightMin 0.1938456512652309 --weightMax 0.7508062530789632 --weightMin 0.29872798492369435 --weightMax 0.7876425851551943 --velocityUpdate defaultVelocityUpdate --c1Min 1.3955986855971776 --c1Max 2.251920493366374 --c2Min 1.3343628297621377 --c2Max 2.045489954826703 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 3 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.9450011413627195 --velocityChangeWhenUpperLimitIsReached -0.3602172883291481  \n ")
            .split("\\s+");

    var autoMOPSO = new ConfigurableMOPSO(new ZDT1());
    autoMOPSO.parse(parameters);

    AutoMOPSO.print(autoMOPSO.fixedParameterList());
    AutoMOPSO.print(autoMOPSO.configurableParameterList());

    ParticleSwarmOptimizationAlgorithm mopso = autoMOPSO.create();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOPSO", 80, 100,
            referenceFrontFileName, "F1", "F2");

    mopso.observable().register(evaluationObserver);
    mopso.observable().register(runTimeChartObserver);

    mopso.run();

    JMetalLogger.logger.info("Total computing time: " + mopso.totalComputingTime()); ;

    new SolutionListOutput(mopso.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
