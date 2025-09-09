package org.uma.evolver.example.base;

import java.io.IOException;
import org.uma.evolver.algorithm.base.nsgaii.PermutationNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.IndicatorPlotObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * This class demonstrates the configuration and execution of NSGA-II (Non-dominated Sorting Genetic Algorithm II)
 * for solving bi-objective Traveling Salesman Problem (TSP) instances, with additional runtime visualization.
 * It extends the basic NSGA-II implementation by incorporating multiple observers for monitoring the algorithm's
 * progress in real-time.
 *
 * <p>Key features of this implementation:
 * <ul>
 *   <li>Solves the KroAB100 TSP instance with 100 cities</li>
 *   <li>Uses a reference front from 'resources/referenceFrontsTSP/KroAB100TSP.csv' for comparison</li>
 *   <li>Provides three types of runtime visualization:
 *     <ul>
 *       <li>Run-time chart showing the evolution of solutions in the objective space</li>
 *       <li>Epsilon indicator plot for convergence analysis</li>
 *       <li>Hypervolume indicator plot for diversity and convergence analysis</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>The algorithm is configured with the following components by default:
 * <ul>
 *   <li>Population size: 100 solutions
 *   <li>Maximum evaluations: 1,000,000
 *   <li>Crossover: Cycle Crossover (CX) with probability 0.6848
 *   <li>Mutation: Swap mutation with probability 0.0973
 *   <li>Selection: Binary tournament selection
 * </ul>
 *
 * <p>The results are output to two files:
 * <ul>
 *   <li>VAR.csv: Contains the variable values (permutation of cities)
 *   <li>FUN.csv: Contains the objective function values
 * </ul>
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 * @see org.uma.evolver.algorithm.base.nsgaii.PermutationNSGAII
 * @see org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP
 * @see org.uma.jmetal.util.observer.impl.RunTimeChartObserver
 * @see org.uma.jmetal.util.observer.impl.IndicatorPlotObserver
 */
public class NSGAIIBiObjectiveWithObserversTSP {

  public static void main(String[] args) throws IOException {
    String referenceFrontFileName = "resources/referenceFrontsTSP/KroAB100TSP.csv";

    String[] parameters =
        ("--algorithmResult population "
                + "--createInitialSolutions default  "
                + "--offspringPopulationSize 100 "
                + "--variation crossoverAndMutationVariation "
                + "--crossover CX "
                + "--crossoverProbability 0.6848051886685469 "
                + "--mutation swap "
                + "--mutationProbability 0.09728575979674077 "
                + "--selection tournament "
                + "--selectionTournamentSize 2 ")
            .split("\\s+");

    var baseNSGAII = new PermutationNSGAII(
            new KroAB100TSP(), 100, 100000, new NSGAIIPermutationParameterSpace());

    baseNSGAII.parse(parameters);

    baseNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<PermutationSolution<Integer>> nsgaII = baseNSGAII.build();

    RunTimeChartObserver<PermutationSolution<Integer>> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, referenceFrontFileName, "F1", "F2");

    IndicatorPlotObserver<DoubleSolution> indicatorPlotObserver =
        new IndicatorPlotObserver<>("NSGA-II", new Epsilon(), referenceFrontFileName, 100);
    IndicatorPlotObserver<DoubleSolution> hvPlotObserver =
        new IndicatorPlotObserver<>("NSGA-II", new HypervolumeMinus(), referenceFrontFileName, 1000);

    nsgaII.observable().register(runTimeChartObserver);
    nsgaII.observable().register(indicatorPlotObserver);
    nsgaII.observable().register(hvPlotObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
