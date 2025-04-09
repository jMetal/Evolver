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
    DoubleProblem problem = new DTLZ3() ;
    String referenceFrontFileName = "resources/referenceFrontsCSV/DTLZ3.3D.csv";

    String[] parameters =
        ("--neighborhoodSize 34 --maximumNumberOfReplacedSolutions 3 --aggregationFunction tschebyscheff --normalizeObjectives True --sequenceGenerator permutation --epsilonParameterForNormalizing 12.999310113949335 --pbiTheta 11.884564010591339 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions random --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.5581797053552503 --mutationRepairStrategy random --polynomialMutationDistributionIndex 41.740088532762925 --linkedPolynomialMutationDistributionIndex 156.33177957980624 --uniformMutationPerturbation 0.25573065645872584 --nonUniformMutationPerturbation 0.3310332522353208 --crossover BLX_ALPHA --crossoverProbability 0.7685659656528498 --crossoverRepairStrategy random --sbxDistributionIndex 184.70902034186506 --blxAlphaCrossoverAlphaValue 0.35323608962356984 --differentialEvolutionCrossover RAND_1_BIN --CR 0.13271870623647516 --F 0.9785205199810745 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.017056921280592524 \n")
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
