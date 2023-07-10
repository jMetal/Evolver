package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
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
    String referenceFrontFileName = "resources/referenceFronts/ZDT1.csv";

    String[] parameters =
        ("--neighborhoodSize 34 --maximumNumberOfReplacedSolutions 4 --aggregationFunction weightedSum --normalizeObjectives 0 --epsilonParameterForNormalizing 20.5995117344667 --pbiTheta 9.139127338877545 --normalizeObjectives 0 --epsilonParameterForNormalizing 6.662158789484649 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --crossover BLX_ALPHA --crossoverProbability 0.9135106032396654 --crossoverRepairStrategy bounds --sbxDistributionIndex 291.01390681811205 --blxAlphaCrossoverAlphaValue 0.4468430391724214 --mutation nonUniform --mutationProbabilityFactor 0.32345623094163917 --mutationRepairStrategy round --polynomialMutationDistributionIndex 261.49502249942816 --linkedPolynomialMutationDistributionIndex 259.8710935008961 --uniformMutationPerturbation 0.48386006194616116 --nonUniformMutationPerturbation 0.2246020392397615 --mutation nonUniform --mutationProbabilityFactor 1.002269612106008 --mutationRepairStrategy random --polynomialMutationDistributionIndex 64.02879159199267 --linkedPolynomialMutationDistributionIndex 19.248561217649627 --uniformMutationPerturbation 0.8162702465275766 --nonUniformMutationPerturbation 0.9832416979732699 --differentialEvolutionCrossover RAND_2_BIN --CR 0.8053113531043147 --F 0.34790139693778654 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.49048369979000705 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(new ZDT1(), 100, 20000,
        "resources/weightVectors");
    autoMOEAD.parse(parameters);

    ConfigurableMOEAD.print(autoMOEAD.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> mopso = autoMOEAD.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOPSO", 80, 100,
            referenceFrontFileName, "F1", "F2");

    mopso.observable().register(evaluationObserver);
    mopso.observable().register(runTimeChartObserver);

    mopso.run();

    JMetalLogger.logger.info("Total computing time: " + mopso.totalComputingTime());

    new SolutionListOutput(mopso.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
