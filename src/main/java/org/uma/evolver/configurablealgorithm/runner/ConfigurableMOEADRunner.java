package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
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
    DoubleProblem problem = new ZDT4() ;
    String referenceFrontFileName = "resources/referenceFronts/ZDT4.csv";

    String[] parameters =
        ("--neighborhoodSize 48 --maximumNumberOfReplacedSolutions 4 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 19.15857995488684 --pbiTheta 75.01135927054058 --algorithmResult externalArchive --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --variation differentialEvolutionVariation --mutation linkedPolynomial --mutationProbabilityFactor 0.7428526365390735 --mutationRepairStrategy random --polynomialMutationDistributionIndex 101.07249679078507 --linkedPolynomialMutationDistributionIndex 21.418890277007872 --uniformMutationPerturbation 0.16659171459604447 --nonUniformMutationPerturbation 0.6112999400845327 --crossover SBX --crossoverProbability 0.5555305045856336 --crossoverRepairStrategy round --sbxDistributionIndex 345.18939253215086 --blxAlphaCrossoverAlphaValue 0.16298882362835568 --differentialEvolutionCrossover RAND_1_BIN --CR 0.20088168478657417 --F 0.694084178188174 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.19654547687676577 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(problem, 100, 25000,
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
