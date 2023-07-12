package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2Minus;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
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
        ("--neighborhoodSize 23 --maximumNumberOfReplacedSolutions 4 --aggregationFunction weightedSum --normalizeObjectives 0 --epsilonParameterForNormalizing 17.045084639659965 --pbiTheta 70.64516638346711 --normalizeObjectives 0 --epsilonParameterForNormalizing 10.142553674795964 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --crossover wholeArithmetic --crossoverProbability 0.07562635082157594 --crossoverRepairStrategy round --sbxDistributionIndex 167.8794674930413 --blxAlphaCrossoverAlphaValue 0.18846572376230328 --mutation nonUniform --mutationProbabilityFactor 0.8729064630668313 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 333.1453537971142 --linkedPolynomialMutationDistributionIndex 127.70681420093203 --uniformMutationPerturbation 0.39863521466813256 --nonUniformMutationPerturbation 0.138244487304449 --mutation linkedPolynomial --mutationProbabilityFactor 1.3268440896307871 --mutationRepairStrategy round --polynomialMutationDistributionIndex 331.0701044531689 --linkedPolynomialMutationDistributionIndex 78.35287324392789 --uniformMutationPerturbation 0.27803303669057816 --nonUniformMutationPerturbation 0.4734424676109973 --differentialEvolutionCrossover RAND_1_BIN --CR 0.19688599174636107 --F 0.3038961791752883 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.08452610428416374 \n")
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
