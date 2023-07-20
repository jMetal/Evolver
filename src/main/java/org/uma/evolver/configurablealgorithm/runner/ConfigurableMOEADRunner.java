package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2Minus;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableMOEADRunner {

  public static void main(String[] args) {
    String referenceFrontFileName = "resources/referenceFronts/DTLZ2Minus.csv";

    String[] parameters =
        ("--neighborhoodSize 15 --maximumNumberOfReplacedSolutions 3 --aggregationFunction weightedSum --normalizeObjectives TRUE --epsilonParameterForNormalizing 16.243263727219677 --pbiTheta 108.64568844959776 --normalizeObjectives 0 --epsilonParameterForNormalizing 8.274413216488153 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions random --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.47813202443593594 --crossoverRepairStrategy random --sbxDistributionIndex 388.8997431246589 --blxAlphaCrossoverAlphaValue 0.010715439006001203 --mutation linkedPolynomial --mutationProbabilityFactor 0.8001376901794441 --mutationRepairStrategy round --polynomialMutationDistributionIndex 26.999443508956222 --linkedPolynomialMutationDistributionIndex 78.35408051283348 --uniformMutationPerturbation 0.29344380512579127 --nonUniformMutationPerturbation 0.6859568724552134 --mutation polynomial --mutationProbabilityFactor 1.1643011115104498 --mutationRepairStrategy random --polynomialMutationDistributionIndex 71.93772624901247 --linkedPolynomialMutationDistributionIndex 87.13221330299996 --uniformMutationPerturbation 0.5899570716565086 --nonUniformMutationPerturbation 0.3614814813850393 --differentialEvolutionCrossover RAND_1_EXP --CR 0.6682516297481558 --F 0.09681411403352258 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.1287896156137347 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(new DTLZ2Minus(), 91, 30000,
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
