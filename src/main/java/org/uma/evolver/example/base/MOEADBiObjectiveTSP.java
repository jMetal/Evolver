package org.uma.evolver.example.base;

import java.io.IOException;
import org.uma.evolver.algorithm.base.moead.PermutationMOEAD;
import org.uma.evolver.algorithm.base.moead.parameterspace.MOEADPermutationParameterSpace;
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
 * Class configuring MOEA/D using arguments in the form &lt;key, value&gt;
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEADBiObjectiveTSP {

  public static void main(String[] args) throws IOException {
    String referenceFrontFileName = "resources/referenceFrontsTSP/KroAB100TSP.csv";

    String[] parameters =
        ("--neighborhoodSize 20 "
                + "--maximumNumberOfReplacedSolutions 2 "
                + "--aggregationFunction penaltyBoundaryIntersection "
                + "--normalizeObjectives true "
                + "--epsilonParameterForNormalization 6 "
                + "--pbiTheta 5.0 "
                + "--algorithmResult population "
                + "--createInitialSolutions default "
                + "--subProblemIdGenerator randomPermutationCycle "
                + "--variation crossoverAndMutationVariation "
                + "--crossoverProbability 0.9 "
                + "--mutation swap "
                + "--mutationProbability 0.08 "
                + "--crossover PMX "
                + "--selection populationAndNeighborhoodMatingPoolSelection "
                + "--neighborhoodSelectionProbability 0.9")
            .split("\\s+");

    var evMOEAD =
        new PermutationMOEAD(
            new KroAB100TSP(),
            100,
            1000000,
            "resources/weightVectors",
            new MOEADPermutationParameterSpace());

    evMOEAD.parse(parameters);

    evMOEAD.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<PermutationSolution<Integer>> moead = evMOEAD.build();

    RunTimeChartObserver<PermutationSolution<Integer>> runTimeChartObserver =
        new RunTimeChartObserver<>("MOEA/D", 80, 1000, referenceFrontFileName, "F1", "F2");

    IndicatorPlotObserver<DoubleSolution> indicatorPlotObserver =
        new IndicatorPlotObserver<>("MOEA/D", new Epsilon(), referenceFrontFileName, 100);
    IndicatorPlotObserver<DoubleSolution> hvPlotObserver =
        new IndicatorPlotObserver<>("MOEA/D", new HypervolumeMinus(), referenceFrontFileName, 1000);

    moead.observable().register(runTimeChartObserver);
    moead.observable().register(indicatorPlotObserver);
    moead.observable().register(hvPlotObserver);

    moead.run();

    JMetalLogger.logger.info("Total computing time: " + moead.totalComputingTime());

    new SolutionListOutput(moead.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
