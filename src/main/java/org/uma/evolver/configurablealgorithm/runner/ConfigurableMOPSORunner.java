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
        ("--swarmSize 15 --leaderArchive crowdingDistanceArchive --algorithmResult leaderArchive --swarmInitialization scatterSearch --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation linkedPolynomial --mutationProbabilityFactor 0.4921128536378131 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 265.0381661292642 --linkedPolynomialMutationDistributionIndex 130.56288534815636 --uniformMutationPerturbation 0.7277328828666787 --nonUniformMutationPerturbation 0.9245064823436981 --frequencyOfApplicationOfMutationOperator 5 --inertiaWeightComputingStrategy constantValue --weight 0.9982661668987803 --weightMin 0.3283417082908671 --weightMax 0.780505413656115 --weightMin 0.4659822581898031 --weightMax 0.5410926629595832 --weightMin 0.2529497869699355 --weightMax 0.9736279342753194 --velocityUpdate constrainedVelocityUpdate --c1Min 1.7739841323051184 --c1Max 2.417283405826435 --c2Min 1.5989339193102108 --c2Max 2.008359162164694 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 7 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached -0.7107472181782645 --velocityChangeWhenUpperLimitIsReached -0.682453958976936 \n")
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
