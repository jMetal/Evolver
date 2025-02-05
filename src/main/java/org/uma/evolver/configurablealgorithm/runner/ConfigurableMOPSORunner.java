package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOPSO;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
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
    String referenceFrontFileName = "resources/referenceFronts/DTLZ7.3D.csv";

    String[] parameters =
        ("--algorithmResult leaderArchive "
                + "--swarmSize 78 --leaderArchive spatialSpreadDeviationArchive --algorithmResult leaderArchive --swarmInitialization latinHypercubeSampling --velocityInitialization defaultVelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation linkedPolynomial --mutationProbabilityFactor 0.33644797509129987 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 190.20924310480223 --linkedPolynomialMutationDistributionIndex 374.3335982316379 --uniformMutationPerturbation 0.12694697695037221 --nonUniformMutationPerturbation 0.14956795895089525 --frequencyOfApplicationOfMutationOperator 8 --inertiaWeightComputingStrategy constantValue --weight 0.10478061816458673 --weightMin 0.11816982688291236 --weightMax 0.7354618219503289 --weightMin 0.2621208154617437 --weightMax 0.7176963613275431 --weightMin 0.2240885536094285 --weightMax 0.6152519975936961 --velocityUpdate constrainedVelocityUpdate --c1Min 1.5794561833369 --c1Max 2.0665227780557687 --c2Min 1.9631410232540714 --c2Max 2.574139279070546 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection random --selectionTournamentSize 9 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.0011528938361706764 --velocityChangeWhenUpperLimitIsReached -0.12700540497081358 \n")
            .split("\\s+");

    var autoMOPSO = new ConfigurableMOPSO(new DTLZ7(), 100, 40000);
    autoMOPSO.parse(parameters);

    ConfigurableMOPSO.print(autoMOPSO.configurableParameterList());

    ParticleSwarmOptimizationAlgorithm mopso = autoMOPSO.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOPSO", 80, 1000,
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
