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
    String referenceFrontFileName = "resources/referenceFronts/ZDT2.csv";

    String[] parameters =
        ("--neighborhoodSize 44 --maximumNumberOfReplacedSolutions 4 --aggregationFunction tschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 0.21012411098985556 --pbiTheta 133.10287690724581 --algorithmResult externalArchive --externalArchive crowdingDistanceArchive --createInitialSolutions random --variation differentialEvolutionVariation --mutation linkedPolynomial --mutationProbabilityFactor 0.532370620337721 --mutationRepairStrategy round --polynomialMutationDistributionIndex 93.23986564248162 --linkedPolynomialMutationDistributionIndex 13.921818175200437 --uniformMutationPerturbation 0.611275291771117 --nonUniformMutationPerturbation 0.05785527642264886 --crossover BLX_ALPHA --crossoverProbability 0.30594800599269417 --crossoverRepairStrategy random --sbxDistributionIndex 257.5524927853928 --blxAlphaCrossoverAlphaValue 0.9690019845193056 --differentialEvolutionCrossover RAND_1_BIN --CR 0.2991777818966997 --F 0.9917964889392291 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.7997710746761074 \n")
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
