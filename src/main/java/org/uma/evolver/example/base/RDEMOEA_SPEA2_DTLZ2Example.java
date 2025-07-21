package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.rdsmoea.DoubleRDEMOEA;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class RDEMOEA_SPEA2_DTLZ2Example {
  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/DTLZ2.3D.csv";

    String[] parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--variation crossoverAndMutationVariation "
                + "--offspringPopulationSize 100 "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--ranking strengthRanking "
                + "--densityEstimator knn "
                + "--knnNeighborhoodSize 1 "
                + "--knnNormalizeObjectives false "
                + "--selection tournament "
                + "--selectionTournamentSize 2 "
                + "--replacement rankingAndDensityEstimator "
                + "--removalPolicy sequential")
            .split("\\s+");

    var evAlgorithm = new DoubleRDEMOEA(new DTLZ2(), 100, 50000);
    evAlgorithm.parse(parameters);

    evAlgorithm.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> spea2 = evAlgorithm.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("RDS-MOEAD-NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

    spea2.observable().register(evaluationObserver);
    spea2.observable().register(runTimeChartObserver);

    spea2.run();

    JMetalLogger.logger.info("Total computing time: " + spea2.totalComputingTime());

    new SolutionListOutput(spea2.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0);
  }
}
