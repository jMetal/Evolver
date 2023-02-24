package org.uma.evolver.algorithm;

import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoMOPSO;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3_2D;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableMOEADRunner {

  public static void main(String[] args) {
    String referenceFrontFileName = "resources/DTLZ3.2D.csv";

    String[] parameters =
        ("--neighborhoodSize 15 --maximumNumberOfReplacedSolutions 3 --aggregationFunction penaltyBoundaryIntersection --pbiTheta 23.65190804836346 --normalizeObjectives 0 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions scatterSearch --variation differentialEvolutionVariation --crossover SBX --crossoverProbability 0.48290078356281824 --crossoverRepairStrategy round --sbxDistributionIndex 81.67322450445674 --blxAlphaCrossoverAlphaValue 0.9576371983336044 --mutation nonUniform --mutationProbabilityFactor 0.9118578407729966 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 148.6675750875389 --linkedPolynomialMutationDistributionIndex 306.5346554659594 --uniformMutationPerturbation 0.045846246515091345 --nonUniformMutationPerturbation 0.6147663346435684 --mutation linkedPolynomial --mutationProbabilityFactor 1.8734511699915455 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 122.91843406195596 --linkedPolynomialMutationDistributionIndex 205.51821628193827 --uniformMutationPerturbation 0.5568144819729235 --nonUniformMutationPerturbation 0.6056125937820276 --differentialEvolutionCrossover RAND_2_BIN --CR 0.8905320647134094 --F 0.5915575710480543 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.23684236050681037 --maximumNumberOfEvaluations 5000 --populationSize 100 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(new DTLZ3_2D());
    autoMOEAD.parse(parameters);

    AutoMOPSO.print(autoMOEAD.fixedParameterList());
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

    JMetalLogger.logger.info("Total computing time: " + mopso.totalComputingTime()); ;

    new SolutionListOutput(mopso.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

  }
}
