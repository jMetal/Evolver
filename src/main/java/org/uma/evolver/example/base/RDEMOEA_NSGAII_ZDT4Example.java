package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.rdsmoea.DoubleRDEMOEA;
import org.uma.evolver.algorithm.base.rdsmoea.DoubleRDEMOEAV2;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

public class RDEMOEA_NSGAII_ZDT4Example {
  public static void main(String[] args) {
    ParameterSpace parameterSpace = new YAMLParameterSpace("RDEMOEADouble.yaml", new DoubleParameterFactory());
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

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
                + "--ranking dominanceRanking "
                + "--densityEstimator crowdingDistance "
                + "--selection tournament "
                + "--selectionTournamentSize 2 "
                + "--replacement rankingAndDensityEstimator "
                + "--removalPolicy oneShot")
            .split("\\s+");

    var evAlgorithm = new DoubleRDEMOEAV2(new ZDT4(), 100, 25000, parameterSpace);
    evAlgorithm.parse(parameters);

    evAlgorithm.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = evAlgorithm.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>("RDS-MOEAD-NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    System.exit(0);
  }
}
