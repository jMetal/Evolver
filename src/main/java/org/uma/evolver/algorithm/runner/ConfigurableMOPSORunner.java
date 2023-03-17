package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableMOPSO;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoMOPSO;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1_2D;
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
    String referenceFrontFileName = "resources/DTLZ1.2D.csv";

    String[] parameters =
        ("--swarmSize 44 --leaderArchive hypervolumeArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation linkedPolynomial --mutationProbabilityFactor 1.7135605802715577 --mutationRepairStrategy random --polynomialMutationDistributionIndex 49.7654417633381 --linkedPolynomialMutationDistributionIndex 296.46353811514064 --uniformMutationPerturbation 0.1936723718086634 --nonUniformMutationPerturbation 0.36453911451549414 --frequencyOfApplicationOfMutationOperator 8 --inertiaWeightComputingStrategy linearIncreasingValue --weight 0.5462013772332143 --weightMin 0.11371082633805242 --weightMax 0.7028821415471258 --weightMin 0.2655296553186227 --weightMax 0.5705688594717577 --weightMin 0.1235910691976732 --weightMax 0.6774517726070671 --velocityUpdate constrainedVelocityUpdate --c1Min 1.1642792452475084 --c1Max 2.25655473542795 --c2Min 1.174906217296941 --c2Max 2.7157514535698115 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 3 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached -0.7581382751394341 --velocityChangeWhenUpperLimitIsReached -0.3478599960328099 \n ")
            .split("\\s+");

    var autoMOPSO = new ConfigurableMOPSO(new DTLZ1_2D(), 100, 8000);
    autoMOPSO.parse(parameters);

    AutoMOPSO.print(autoMOPSO.configurableParameterList());

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
