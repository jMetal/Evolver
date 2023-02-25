package org.uma.evolver.algorithm;

import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoMOPSO;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3_2D;
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
    String referenceFrontFileName = "resources/ZDT1.csv";

    String[] parameters =
        ("--neighborhoodSize 41 --maximumNumberOfReplacedSolutions 4 --aggregationFunction weightedSum --pbiTheta 37.09405784937688 --normalizeObjectives 0 --algorithmResult externalArchive --externalArchive crowdingDistanceArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --crossover BLX_ALPHA --crossoverProbability 0.12398856205047229 --crossoverRepairStrategy bounds --sbxDistributionIndex 128.30103610901682 --blxAlphaCrossoverAlphaValue 0.09195876308331126 --mutation nonUniform --mutationProbabilityFactor 0.10872669810645894 --mutationRepairStrategy round --polynomialMutationDistributionIndex 235.02566297628192 --linkedPolynomialMutationDistributionIndex 265.2419384945447 --uniformMutationPerturbation 0.7992184102359726 --nonUniformMutationPerturbation 0.44570976889667474 --mutation uniform --mutationProbabilityFactor 0.6283638146514654 --mutationRepairStrategy round --polynomialMutationDistributionIndex 362.01547002572465 --linkedPolynomialMutationDistributionIndex 297.6862984765796 --uniformMutationPerturbation 0.15524189122298251 --nonUniformMutationPerturbation 0.8740478495903546 --differentialEvolutionCrossover RAND_1_BIN --CR 0.4863467818476792 --F 0.964351570706898 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.03298074450258348 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(new ZDT1(), 100, 10000);
    autoMOEAD.parse(parameters);

    AutoMOPSO.print(autoMOEAD.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> mopso = autoMOEAD.create();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "MOPSO", 80, 100,
            referenceFrontFileName, "F1", "F2");

    mopso.observable().register(evaluationObserver);
    mopso.observable().register(runTimeChartObserver);

    mopso.run();

    JMetalLogger.logger.info("Total computing time: " + mopso.totalComputingTime());
    ;

    new SolutionListOutput(mopso.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
