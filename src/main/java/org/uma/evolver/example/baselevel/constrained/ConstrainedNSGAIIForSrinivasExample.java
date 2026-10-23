package org.uma.evolver.example.baselevel.constrained;

import java.io.IOException;
import org.uma.evolver.algorithm.nsgaii.ConstrainedDoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.Srinivas;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.ConstraintHandling;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Example of running the constrained variant of NSGA-II on Srinivas, a problem with two
 * constraints, with its default configuration.
 *
 * <p>The default configuration of NSGA-II (SBX crossover, polynomial mutation, binary tournament)
 * plus the constraint-handling technique of the default configuration of the constrained variant:
 * Deb's constrained-domination principle ({@code --constraintHandling cdp}). The parameter space
 * is loaded from {@code NSGAIIDoubleConstrained.yaml}. The result is written to {@code
 * VAR.csv}/{@code FUN.csv}, and the number of feasible solutions and the quality indicators of the
 * feasible ones against the Srinivas reference front are printed at the end of the run.
 */
public class ConstrainedNSGAIIForSrinivasExample {

  /**
   * Runs the example.
   *
   * @param args not used
   * @throws IOException if the reference front cannot be read
   */
  public static void main(String[] args) throws IOException {
    DoubleProblem problem = new Srinivas();
    String referenceFrontFileName = "resources/referenceFronts/Srinivas.csv";

    String yamlParameterSpaceFile = "NSGAIIDoubleConstrained.yaml";
    int populationSize = 100;
    int maximumNumberOfEvaluations = 25000;

    String[] parameters =
        """
        --constraintHandling cdp
        --algorithmResult population
        --createInitialSolutions default
        --offspringPopulationSize 100
        --variation crossoverAndMutationVariation
        --crossover SBX
        --crossoverProbability 0.9
        --crossoverRepairStrategy bounds
        --sbxDistributionIndex 20.0
        --mutation polynomial
        --mutationProbabilityFactor 1.0
        --mutationRepairStrategy bounds
        --polynomialMutationDistributionIndex 20.0
        --selection tournament
        --selectionTournamentSize 2
        """
            .split("\\s+");

    var baseNSGAII =
        new ConstrainedDoubleNSGAII(
            problem,
            populationSize,
            maximumNumberOfEvaluations,
            new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory()));

    baseNSGAII.parse(parameters);
    EvolutionaryAlgorithm<DoubleSolution> nsgaii = baseNSGAII.build();
    nsgaii.run();

    JMetalLogger.logger.info("Total execution time : " + nsgaii.totalComputingTime() + "ms");
    JMetalLogger.logger.info("Number of evaluations: " + nsgaii.numberOfEvaluations());

    new SolutionListOutput(nsgaii.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Random seed: " + JMetalRandom.getInstance().getSeed());

    var feasibleSolutions =
        nsgaii.result().stream().filter(ConstraintHandling::isFeasible).toList();
    JMetalLogger.logger.info(
        "Feasible solutions: " + feasibleSolutions.size() + " of " + nsgaii.result().size());
    if (!feasibleSolutions.isEmpty()) {
      QualityIndicatorUtils.printQualityIndicators(
          SolutionListUtils.getMatrixWithObjectiveValues(feasibleSolutions),
          VectorUtils.readVectors(referenceFrontFileName, ","));
    }
  }
}
