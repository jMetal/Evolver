package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
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
        ("--swarmSize 19 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization scatterSearch --velocityInitialization SPSO2007VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation polynomial --mutationProbabilityFactor 0.08415887039602549 --mutationRepairStrategy round --polynomialMutationDistributionIndex 271.4522983377646 --linkedPolynomialMutationDistributionIndex 332.2989831527132 --uniformMutationPerturbation 0.5608130133317952 --nonUniformMutationPerturbation 0.6904700751660505 --frequencyOfApplicationOfMutationOperator 5 --inertiaWeightComputingStrategy linearDecreasingValue --weight 0.20269630637961966 --weightMin 0.3457170849465286 --weightMax 0.8953078972002195 --weightMin 0.14590266255096423 --weightMax 0.5373935218552816 --weightMin 0.3115675915591012 --weightMax 0.7767325892064336 --velocityUpdate constrainedVelocityUpdate --c1Min 1.3405600058860245 --c1Max 2.362687039410096 --c2Min 1.9106349910197937 --c2Max 2.6118720571077683 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 9 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached -0.6241310786886691 --velocityChangeWhenUpperLimitIsReached 0.12282155624462687 \n")
            .split("\\s+");

    var autoMOPSO = new ConfigurableMOPSO(new ZDT4(), 100, 15000);
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
