package org.uma.evolver.example.base;

import java.io.IOException;

import org.uma.evolver.algorithm.base.moead.MOEADBinary;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.OneZeroMax;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.IndicatorPlotObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value>
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEADOneZeroMax {

  public static void main(String[] args) throws IOException {

    String[] parameters =
        ("--neighborhoodSize 20 "
                + "--maximumNumberOfReplacedSolutions 2 "
                + "--aggregationFunction penaltyBoundaryIntersection "
                + "--normalizeObjectives True "
                + "--epsilonParameterForNormalization 6 "
                + "--pbiTheta 5.0 "
                + "--algorithmResult population "
                + "--createInitialSolutions default "
                + "--subProblemIdGenerator permutation "
                + "--variation crossoverAndMutationVariation "
                + "--crossoverProbability 0.9 "
                + "--mutation bitFlip "
                + "--mutationProbabilityFactor 1.0 "
                + "--crossover singlePoint "
                + "--selection populationAndNeighborhoodMatingPoolSelection "
                + "--neighborhoodSelectionProbability 0.9")
            .split("\\s+");

    var baseAlgorithm = new MOEADBinary(new OneZeroMax(), 100, 10000, "resources/weightVectors");

    baseAlgorithm.parse(parameters);

    baseAlgorithm.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<BinarySolution> moead = baseAlgorithm.build();

    RunTimeChartObserver<BinarySolution> runTimeChartObserver =
        new RunTimeChartObserver<>("MOEA/D", 80, 1000, null, "F1", "F2");

    moead.observable().register(runTimeChartObserver);

    moead.run();

    JMetalLogger.logger.info("Total computing time: " + moead.totalComputingTime());

    new SolutionListOutput(moead.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
