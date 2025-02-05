package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.*;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
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
    DoubleProblem problem = new DTLZ1() ;
    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";

    String[] parameters =
        ("--neighborhoodSize 17 --maximumNumberOfReplacedSolutions 3 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 16.74427554837178 --pbiTheta 102.33621092188483 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions latinHypercubeSampling --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.365757626005199 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 330.9192060953075 --linkedPolynomialMutationDistributionIndex 259.6234057548009 --uniformMutationPerturbation 0.25039687229006147 --nonUniformMutationPerturbation 0.06369128222969085 --crossover SBX --crossoverProbability 0.7718296929024707 --crossoverRepairStrategy random --sbxDistributionIndex 39.14026628165764 --blxAlphaCrossoverAlphaValue 0.9664474833035351 --differentialEvolutionCrossover RAND_1_BIN --CR 0.16954180521284054 --F 0.996915805902218 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.6398452490955031 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(problem, 100, 40000,
        "resources/weightVectors");
    autoMOEAD.parse(parameters);

    ConfigurableMOEAD.print(autoMOEAD.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> moead = autoMOEAD.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(1000);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOEAD. " + problem.name(), 80, 1000,
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
