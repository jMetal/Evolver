package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableMOPSO;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1_2D;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
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
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters =
        ("--swarmSize 13 --leaderArchive hypervolumeArchive --algorithmResult leaderArchive --swarmInitialization latinHypercubeSampling --velocityInitialization defaultVelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation uniform --mutationProbabilityFactor 0.11876099883686435 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 181.57619530014856 --linkedPolynomialMutationDistributionIndex 117.60975880450586 --uniformMutationPerturbation 0.8951498644698818 --nonUniformMutationPerturbation 0.2182987243324064 --frequencyOfApplicationOfMutationOperator 9 --inertiaWeightComputingStrategy constantValue --weight 0.17407019224030695 --weightMin 0.21692334948752032 --weightMax 0.6400839015176296 --weightMin 0.41800964571315113 --weightMax 0.7471849462270344 --weightMin 0.3968028046395683 --weightMax 0.6446280385536365 --velocityUpdate constrainedVelocityUpdate --c1Min 1.6250711154308675 --c1Max 2.0211281727647634 --c2Min 1.0328605672568087 --c2Max 2.660512400942806 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 8 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.5670841047560526 --velocityChangeWhenUpperLimitIsReached 0.06723576098544903 \n ")
            .split("\\s+");

    var autoMOPSO = new ConfigurableMOPSO(new ZDT4(), 100, 8000);
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
