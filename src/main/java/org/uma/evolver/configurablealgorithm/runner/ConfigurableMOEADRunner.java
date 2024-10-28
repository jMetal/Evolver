package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
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
        ("--algorithmResult externalArchive --populationSizeWithArchive 35 --externalArchive crowdingDistanceArchive --createInitialSolutions latinHypercubeSampling --offspringPopulationSize 1 --variation crossoverAndMutationVariation --crossover BLX_ALPHA --crossoverProbability 0.684027696243013 --crossoverRepairStrategy bounds --sbxDistributionIndex 93.85468965679941 --blxAlphaCrossoverAlphaValue 0.592056613871905 --mutation nonUniform --mutationProbabilityFactor 0.515034955958418 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 296.03900627798873 --linkedPolynomialMutationDistributionIndex 69.52315312527382 --uniformMutationPerturbation 0.9757084398699737 --nonUniformMutationPerturbation 0.2797093804509295 --selection tournament --selectionTournamentSize 9 \n \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(problem, 91, 20000,
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
