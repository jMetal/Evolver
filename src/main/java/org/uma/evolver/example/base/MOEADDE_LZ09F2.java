package org.uma.evolver.example.base;

import org.uma.evolver.algorithm.base.moead.DoubleMOEAD;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.lz09.LZ09F2;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MOEADDE_LZ09F2 {

  public static void main(String[] args) {
    DoubleProblem problem = new LZ09F2() ;
    String referenceFrontFileName = "resources/referenceFronts/LZ09_F2.csv";
    String yamlParameterSpaceFile = "resources/parameterSpaces/MOEADDouble.yaml" ;


    String[] parameters =
        ("--neighborhoodSize 20 "
            + "--maximumNumberOfReplacedSolutions 2 "
            + "--aggregationFunction tschebyscheff "
            + "--normalizeObjectives true "
            + "--epsilonParameterForNormalization 4 "
            + "--algorithmResult population "
            + "--createInitialSolutions default "
            + "--variation differentialEvolutionVariation "
            + "--subProblemIdGenerator randomPermutationCycle "
            + "--mutation polynomial "
            + "--mutationProbabilityFactor 1.0 "
            + "--mutationRepairStrategy bounds "
            + "--polynomialMutationDistributionIndex 20.0 "
            + "--differentialEvolutionCrossover RAND_1_BIN "
            + "--CR 1.0 "
            + "--F 0.5 "
            + "--selection populationAndNeighborhoodMatingPoolSelection "
            + "--neighborhoodSelectionProbability 0.9 ")
            .split("\\s+");

    var baseMOEAD = new DoubleMOEAD(problem, 300, 175000,
        "resources/weightVectors", new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));
    baseMOEAD.parse(parameters);

    baseMOEAD.parameterSpace().topLevelParameters().forEach(System.out::println);

    EvolutionaryAlgorithm<DoubleSolution> moead = baseMOEAD.build();

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
