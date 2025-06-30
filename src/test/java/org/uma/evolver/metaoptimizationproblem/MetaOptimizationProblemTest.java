package org.uma.evolver.metaoptimizationproblem;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.nsgaii.NSGAIIDouble;
import org.uma.evolver.algorithm.base.nsgaii.NSGAIIPermutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.multiobjectivetsp.instance.KroAB100TSP;
import org.uma.jmetal.problem.multiobjective.zdt.*;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

class MetaOptimizationProblemTest {
  @Test
  void workingTestForAContinuousProblemWithNSGAII() {
    List<Problem<DoubleSolution>> trainingSet = List.of(new ZDT1());
    List<String> referenceFrontFileNames = List.of("resources/referenceFronts/ZDT1.csv");

    BaseLevelAlgorithm<DoubleSolution> configurableAlgorithm = new NSGAIIDouble(100);

    List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
        new MetaOptimizationProblem<>(
            configurableAlgorithm,
            trainingSet,
            referenceFrontFileNames,
            indicators,
            List.of(25000),
            1);

    DoubleSolution solution = metaOptimizationProblem.createSolution();

    metaOptimizationProblem.evaluate(solution);

    assertTrue(solution.objectives()[0] <= 1.0);
    assertEquals(1, metaOptimizationProblem.numberOfObjectives());
  }

  @Test
  void workingTestForTheZDTProblemsWithNSGAII() {
    List<Problem<DoubleSolution>> trainingSet =
        List.of(new ZDT1(), new ZDT2(), new ZDT3(), new ZDT4(), new ZDT6());
    List<String> referenceFrontFileNames = List.of(
            "resources/referenceFronts/ZDT1.csv",
            "resources/referenceFronts/ZDT2.csv",
            "resources/referenceFronts/ZDT3.csv",
            "resources/referenceFronts/ZDT4.csv",
            "resources/referenceFronts/ZDT6.csv");

    BaseLevelAlgorithm<DoubleSolution> configurableAlgorithm = new NSGAIIDouble(100);

    List<QualityIndicator> indicators = List.of(new NormalizedHypervolume());

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
            new MetaOptimizationProblem<>(
                    configurableAlgorithm,
                    trainingSet,
                    referenceFrontFileNames,
                    indicators,
                    List.of(15000, 15000, 15000, 15000, 15000),
                    1);

    DoubleSolution solution = metaOptimizationProblem.createSolution();

    metaOptimizationProblem.evaluate(solution);

    assertTrue(solution.objectives()[0] <= 1.0);
    assertEquals(1, metaOptimizationProblem.numberOfObjectives());
  }

  @Test
  void workingTestForTheZDT4ProblemAndTwoIndicatorsWithNSGAII() {
    JMetalRandom.getInstance().setSeed(1);
    List<Problem<DoubleSolution>> trainingSet = List.of(new ZDT4());
    List<String> referenceFrontFileNames = List.of("resources/referenceFronts/ZDT4.csv");

    BaseLevelAlgorithm<DoubleSolution> configurableAlgorithm = new NSGAIIDouble(100);

    List<QualityIndicator> indicators = List.of(new NormalizedHypervolume(), new Epsilon());

    MetaOptimizationProblem<DoubleSolution> metaOptimizationProblem =
            new MetaOptimizationProblem<>(
                    configurableAlgorithm,
                    trainingSet,
                    referenceFrontFileNames,
                    indicators,
                    List.of(25000),
                    1);

    DoubleSolution solution = metaOptimizationProblem.createSolution();

    metaOptimizationProblem.evaluate(solution);

    assertTrue(solution.objectives()[0] <= 1.0);
    assertEquals(2, metaOptimizationProblem.numberOfObjectives());
  }

  @Test
  void workingTestForATSPProblemWithNSGAII() throws IOException {
    List<Problem<PermutationSolution<Integer>>> trainingSet = List.of(new KroAB100TSP());
    List<String> referenceFrontFileNames = List.of("resources/referenceFrontsTSP/KroAB100TSP.csv");

    var configurableAlgorithm = new NSGAIIPermutation(100);

    List<QualityIndicator> indicators = List.of(new PISAHypervolume());

    MetaOptimizationProblem<PermutationSolution<Integer>> metaOptimizationProblem =
            new MetaOptimizationProblem<>(
                    configurableAlgorithm,
                    trainingSet,
                    referenceFrontFileNames,
                    indicators,
                    List.of(25000),
                    1);

    DoubleSolution solution = metaOptimizationProblem.createSolution();

    metaOptimizationProblem.evaluate(solution);

    assertEquals(1, metaOptimizationProblem.numberOfObjectives());
  }
}
