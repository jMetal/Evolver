package org.uma.evolver.example.base;

import java.io.IOException;
import org.uma.evolver.algorithm.base.nsgaii.NSGAIIPermutation;
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
 * Class configuring NSGA-II using arguments in the form <key, value>
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIBiObjectiveTSP {

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
                + "--selection tournament --selectionTournamentSize 2 ")
            .split("\\s+");

    var evNSGAII = new NSGAIIPermutation(new KroAB100TSP(), 100, 1000000);

    evNSGAII.parse(parameters);

    evNSGAII.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<PermutationSolution<Integer>> nsgaII = evNSGAII.build();

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
