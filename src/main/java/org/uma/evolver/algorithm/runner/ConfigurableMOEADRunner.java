package org.uma.evolver.algorithm.runner;

import org.uma.evolver.algorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoMOPSO;
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
    String referenceFrontFileName = "resources/referenceFronts/DTLZ1.3D.csv";

    String[] parameters =
        ("--neighborhoodSize 43 --maximumNumberOfReplacedSolutions 1 --aggregationFunction penaltyBoundaryIntersection --normalizeObjectives 0 --epsilonParameterForNormalizing 17.46504421386795 --pbiTheta 107.57445663482368 --normalizeObjectives 0 --epsilonParameterForNormalizing 1.4018499168043896 --algorithmResult population --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --crossover SBX --crossoverProbability 0.5867310325782787 --crossoverRepairStrategy random --sbxDistributionIndex 262.0340226214047 --blxAlphaCrossoverAlphaValue 0.9556552375638893 --mutation polynomial --mutationProbabilityFactor 0.2014306664038843 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 185.0958969084788 --linkedPolynomialMutationDistributionIndex 279.7999403335567 --uniformMutationPerturbation 0.9247582536708376 --nonUniformMutationPerturbation 0.8922611123965717 --mutation nonUniform --mutationProbabilityFactor 1.3221488924923648 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 345.980810876771 --linkedPolynomialMutationDistributionIndex 362.41794589899155 --uniformMutationPerturbation 0.7872365536587279 --nonUniformMutationPerturbation 0.09775886868339138 --differentialEvolutionCrossover RAND_1_EXP --CR 0.2697168926319783 --F 0.49424237086567163 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.7859655676020593 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(new DTLZ1(), 91, 30000,
        "resources/weightVectors");
    autoMOEAD.parse(parameters);

    AutoMOPSO.print(autoMOEAD.configurableParameterList());

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
