package org.uma.evolver.configurablealgorithm.runner;

import org.uma.evolver.configurablealgorithm.impl.ConfigurableMOEAD;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ConfigurableMOEADForSolving3DProblemRunner {

  public static void main(String[] args) {
    DoubleProblem problem = new DTLZ3() ;
    String referenceFrontFileName = "resources/referenceFronts/DTLZ3.3D.csv";

    String[] parameters =
        ("--neighborhoodSize 28 --maximumNumberOfReplacedSolutions 3 --aggregationFunction modifiedTschebyscheff --normalizeObjectives 0 --epsilonParameterForNormalizing 18.91825688344554 --pbiTheta 27.002664367792633 --algorithmResult externalArchive --externalArchive unboundedArchive --createInitialSolutions random --variation differentialEvolutionVariation --mutation uniform --mutationProbabilityFactor 0.71805645059367 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.64807007261866 --linkedPolynomialMutationDistributionIndex 290.7168861207539 --uniformMutationPerturbation 0.758623725468837 --nonUniformMutationPerturbation 0.5342853911732168 --crossover BLX_ALPHA --crossoverProbability 0.24596337969888932 --crossoverRepairStrategy round --sbxDistributionIndex 117.47879046272575 --blxAlphaCrossoverAlphaValue 0.30473701527745767 --differentialEvolutionCrossover RAND_2_BIN --CR 0.21039497224343165 --F 0.4960370128457501 --selection populationAndNeighborhoodMatingPoolSelection --neighborhoodSelectionProbability 0.4219768430267933 \n")
            .split("\\s+");

    var autoMOEAD = new ConfigurableMOEAD(problem, 91, 40000,
        "resources/weightVectors");
    autoMOEAD.parse(parameters);

    ConfigurableMOEAD.print(autoMOEAD.configurableParameterList());

    EvolutionaryAlgorithm<DoubleSolution> moead = autoMOEAD.build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
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
