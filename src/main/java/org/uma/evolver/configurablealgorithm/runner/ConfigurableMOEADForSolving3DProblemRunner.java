package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2Minus;
import org.uma.jmetal.problem.multiobjective.rwa.Liao2008;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import smile.stat.Hypothesis.F;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableMOEADRunner {

  public static void main(String[] args) {
    String referenceFrontFileName = "resources/referenceFronts/LZ09F3.csv";

    String[] parameters =
        ("--neighborhoodSize 20"
            + " --maximumNumberOfReplacedSolutions 2 "
            + "--aggregationFunction tschebyscheff "
            + "--normalizeObjectives False "
            + "--epsilonParameterForNormalizing 4 "
            + "--algorithmResult population "
            + "--externalArchive unboundedArchive "
            + "--createInitialSolutions scatterSearch "
            + "--variation differentialEvolutionVariation "
            + "--mutation nonUniform "
            + "--mutationProbabilityFactor 2.0 "
            + "--mutationRepairStrategy bounds "
            + "--polynomialMutationDistributionIndex 272.94399707716076 "
            + "--linkedPolynomialMutationDistributionIndex 168.20189406154657 "
            + "--uniformMutationPerturbation 0.1087680939459134 "
            + "--nonUniformMutationPerturbation 0.9296104123455814 "
            + "--crossover BLX_ALPHA "
            + "--crossoverProbability 0.6651913708365154 "
            + "--crossoverRepairStrategy bounds "
            + "--sbxDistributionIndex 45.642021893535386 "
            + "--blxAlphaCrossoverAlphaValue 0.3160289706370018 "
            + "--differentialEvolutionCrossover RAND_1_BIN "
            + "--CR 0.23231128717098734 "
            + "--F 0.7699050764417739 "
            + "--selection populationAndNeighborhoodMatingPoolSelection "
            + "--neighborhoodSelectionProbability 0.3801342148948319 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(new Liao2008(), 91, 25000,
        "resources/weightVectors");
    autoMOEAD.parse(parameters);

    ConfigurableMOEAD.print(autoMOEAD.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> moead = autoMOEAD.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOEAD", 80, 100,
            referenceFrontFileName, "F1", "F2");

    moead.observable().register(evaluationObserver);
    moead.observable().register(runTimeChartObserver);

    moead.run();

    JMetalLogger.logger.info("Total computing time: " + moead.totalComputingTime());

    new SolutionListOutput(moead.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
