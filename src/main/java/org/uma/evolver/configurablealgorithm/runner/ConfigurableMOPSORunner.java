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
        ("--swarmSize 26 --leaderArchive hypervolumeArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation nonUniform --mutationProbabilityFactor 0.48034478732960934 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 37.15095636085647 --linkedPolynomialMutationDistributionIndex 276.0978996169928 --uniformMutationPerturbation 0.44894683069300556 --nonUniformMutationPerturbation 0.30007211223207353 --frequencyOfApplicationOfMutationOperator 7 --inertiaWeightComputingStrategy constantValue --weight 0.2285685440237509 --weightMin 0.3464206865128401 --weightMax 0.7567077459393162 --weightMin 0.4079613608571212 --weightMax 0.5176886784811445 --weightMin 0.2659602247837344 --weightMax 0.7226106972716171 --velocityUpdate constrainedVelocityUpdate --c1Min 1.7165927414813096 --c1Max 2.6476707352487763 --c2Min 1.0885221029663106 --c2Max 2.1160417179736277 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 9 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached -0.5394144207059444 --velocityChangeWhenUpperLimitIsReached 0.610129968623395 \n")
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
