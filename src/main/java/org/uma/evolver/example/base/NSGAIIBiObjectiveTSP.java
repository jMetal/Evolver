package org.uma.evolver.example.base;

import java.io.IOException;
import org.uma.evolver.algorithm.base.nsgaii.PermutationNSGAII;
import org.uma.evolver.algorithm.base.nsgaii.parameterspace.NSGAIIPermutationParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * This class demonstrates the configuration and execution of the NSGA-II (Non-dominated Sorting Genetic Algorithm II)
 * for solving bi-objective Traveling Salesman Problem (TSP) instances. It specifically uses the KroAB100 TSP instance
 * as a benchmark problem, which involves finding optimal routes between 100 cities with two objectives:
 * 1. Minimizing the total distance of the tour
 * 2. Minimizing a second objective (specific to the KroAB100 instance)
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
 */
public class NSGAIIBiObjectiveTSP {

  public static void main(String[] args) throws IOException {

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
            new KroAB100TSP(), 100, 1000000, new NSGAIIPermutationParameterSpace());

    baseNSGAII.parse(parameters);

    baseNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<PermutationSolution<Integer>> nsgaII = baseNSGAII.build();

    RunTimeChartObserver<PermutationSolution<Integer>> runTimeChartObserver =
        new RunTimeChartObserver<>("NSGA-II", 80, 1000, null, "F1", "F2");

    nsgaII.observable().register(runTimeChartObserver);
    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
