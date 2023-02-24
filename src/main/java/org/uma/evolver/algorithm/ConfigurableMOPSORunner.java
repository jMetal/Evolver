package org.uma.evolver.algorithm;

import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoMOPSO;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3_2D;
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
    String referenceFrontFileName = "resources/DTLZ3.2D.csv";

    String[] parameters =
        ("--maximumNumberOfEvaluations 25000 --archiveSize 100 --swarmSize 166 --leaderArchive spatialSpreadDeviationArchive --algorithmResult leaderArchive --swarmInitialization random --velocityInitialization SPSO2011VelocityInitialization --perturbation frequencySelectionMutationBasedPerturbation --mutation nonUniform --mutationProbabilityFactor 0.6535087976642939 --mutationRepairStrategy round --polynomialMutationDistributionIndex 290.7122682710519 --linkedPolynomialMutationDistributionIndex 150.21175814927582 --uniformMutationPerturbation 0.51829880791445 --nonUniformMutationPerturbation 0.8837873661929366 --frequencyOfApplicationOfMutationOperator 7 --inertiaWeightComputingStrategy linearDecreasingValue --weight 0.4103755328778371 --weightMin 0.22863537762127273 --weightMax 0.6608909675571271 --weightMin 0.2115875974823963 --weightMax 0.9321889662543317 --weightMin 0.23217246832810945 --weightMax 0.6419441984370655 --velocityUpdate constrainedVelocityUpdate --c1Min 1.4794190000496916 --c1Max 2.306806012637101 --c2Min 1.8826036747392838 --c2Max 2.044331146102894 --localBestInitialization defaultLocalBestInitialization --globalBestInitialization defaultGlobalBestInitialization --globalBestSelection tournament --selectionTournamentSize 4 --globalBestUpdate defaultGlobalBestUpdate --localBestUpdate defaultLocalBestUpdate --positionUpdate defaultPositionUpdate --velocityChangeWhenLowerLimitIsReached 0.4678704645071452 --velocityChangeWhenUpperLimitIsReached 0.48918946084622594 \n ")
            .split("\\s+");

    var autoMOPSO = new ConfigurableMOPSO(new DTLZ3_2D());
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
