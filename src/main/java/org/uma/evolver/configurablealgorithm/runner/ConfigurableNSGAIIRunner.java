package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;

import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.rwa.Goel2007;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link ConfigurableNSGAII}
 * class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableNSGAIIRunner {

  public static void main(String[] args) {

    String referenceFrontFileName = "resources/referenceFronts/Goel2007.csv";

    String[] parameters =
        ("--algorithmResult externalArchive "
            + "--populationSizeWithArchive 45 "
            + "--externalArchive unboundedArchive "
            + "--createInitialSolutions random "
            + "--offspringPopulationSize 100 "
            + "--variation crossoverAndMutationVariation "
            + "--crossover BLX_ALPHA "
            + "--crossoverProbability 0.6748953752524687 "
            + "--crossoverRepairStrategy round "
            + "--sbxDistributionIndex 69.33946841828451 "
            + "--blxAlphaCrossoverAlphaValue 0.3524179610073535 "
            + "--mutation nonUniform "
            + "--mutationProbabilityFactor 1.76602778869229 "
            + "--mutationRepairStrategy round "
            + "--polynomialMutationDistributionIndex 20.465825376938277 "
            + "--linkedPolynomialMutationDistributionIndex 369.76116204526977 "
            + "--uniformMutationPerturbation 0.9230041512352161 "
            + "--nonUniformMutationPerturbation 0.6160655898281514 "
            + "--selection tournament "
            + "--selectionTournamentSize 8 ")
            .split("\\s+");

    var configurableNSGAII = new ConfigurableNSGAII(new Goel2007(), 100, 15000);
    configurableNSGAII.parse(parameters);

    ConfigurableNSGAII.print(configurableNSGAII.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> nsgaII = configurableNSGAII.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 500,
            referenceFrontFileName, "F1", "F2");

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
