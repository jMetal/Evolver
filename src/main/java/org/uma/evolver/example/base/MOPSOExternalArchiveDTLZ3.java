package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.mopso.MOPSO;
import org.uma.evolver.algorithm.base.mopso.MOPSOParameterSpace;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class MOPSOExternalArchiveDTLZ3 {
  public static void main(String[] args) {
    DoubleProblem problem = new DTLZ3();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ3.3D.csv";

    String[] parameters =
        ("--swarmSize 100 "
                + "--algorithmResult externalArchive "
                + "--externalArchiveType unboundedArchive "
                + "--leaderArchive crowdingDistanceArchive "
                + "--swarmInitialization default "
                + "--velocityInitialization defaultVelocityInitialization "
                + "--velocityUpdate constrainedVelocityUpdate "
                + "--c1Min 1.5 "
                + "--c1Max 2.5 "
                + "--c2Min 1.5 "
                + "--c2Max 2.5 "
                + "--globalBestInitialization defaultGlobalBestInitialization "
                + "--globalBestUpdate defaultGlobalBestUpdate "
                + "--positionUpdate defaultPositionUpdate "
                + "--globalBestSelection tournamentSelection "
                + "--selectionTournamentSize 2 "
                + "--perturbation frequencySelectionMutationBasedPerturbation "
                + "--frequencyOfApplicationOfMutationOperator 7 "
                + "--mutation polynomial "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--inertiaWeightComputingStrategy randomSelectedValue "
                + "--velocityChangeWhenLowerLimitIsReached -1.0 "
                + "--velocityChangeWhenUpperLimitIsReached -1.0 "
                + "--localBestInitialization defaultLocalBestInitialization "
                + "--localBestUpdate defaultLocalBestUpdate "
                + "--inertiaWeightMin 0.1 "
                + "--inertiaWeightMax 0.5")
            .split("\\s+");

    var mopso = new MOPSO(problem, 100, 40000, new MOPSOParameterSpace());
    mopso.parse(parameters);

    mopso.parameterSpace().topLevelParameters().forEach(System.out::println);

    ParticleSwarmOptimizationAlgorithm algorithm = mopso.build();

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
