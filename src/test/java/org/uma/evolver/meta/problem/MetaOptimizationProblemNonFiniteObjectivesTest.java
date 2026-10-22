package org.uma.evolver.meta.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.DoublePredicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.uma.evolver.algorithm.nsgaii.DoubleNSGAII;
import org.uma.evolver.meta.strategy.FixedEvaluationsStrategy;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.util.HypervolumeMinus;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * The evaluation of a configuration when the base-level algorithm returns solutions with infinite
 * objective values (for instance, a division by a variable whose lower bound is zero): they are left
 * out of the front, and a run with no finite solution gets the worst value of each indicator. jMetal
 * rejects the normalization of a front with non-finite values, so without this a single such run
 * would stop the whole meta-optimization.
 */
@DisplayName("MetaOptimizationProblem with non-finite objective values")
class MetaOptimizationProblemNonFiniteObjectivesTest {

  private static final String DEFAULT_NSGAII =
      "--algorithmResult population --createInitialSolutions default --offspringPopulationSize 100 "
          + "--variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9 "
          + "--crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial "
          + "--mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0 --selection tournament "
          + "--selectionTournamentSize 2";

  /** ZDT1 whose second objective is infinite when the first variable satisfies a condition. */
  private static final class InfiniteZDT1 extends ZDT1 {
    private final DoublePredicate infiniteWhen;

    InfiniteZDT1(DoublePredicate infiniteWhen) {
      this.infiniteWhen = infiniteWhen;
    }

    @Override
    public DoubleSolution evaluate(DoubleSolution solution) {
      super.evaluate(solution);
      if (infiniteWhen.test(solution.variables().get(0))) {
        solution.objectives()[1] = Double.POSITIVE_INFINITY;
      }
      return solution;
    }
  }

  private static double[] evaluate(DoublePredicate infiniteWhen) {
    var problem =
        new MetaOptimizationProblem<>(
            new DoubleNSGAII(
                100, new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory())),
            List.of(new InfiniteZDT1(infiniteWhen)),
            List.of("resources/referenceFronts/ZDT1.csv"),
            List.of(new Epsilon(), new HypervolumeMinus()),
            new FixedEvaluationsStrategy(List.of(10000)),
            1);
    return problem.evaluateConfiguration(DEFAULT_NSGAII.split(" "));
  }

  @Test
  @DisplayName("given every objective vector infinite, when evaluating, then the indicators get their worst values")
  void givenEveryObjectiveVectorInfinite_whenEvaluating_thenTheIndicatorsGetTheirWorstValues() {
    // Act
    double[] values = evaluate(x -> true);

    // Assert: EP is better when lower (worst: Double.MAX_VALUE); HV- is a negated hypervolume
    // (worst: zero hypervolume)
    assertEquals(Double.MAX_VALUE, values[0]);
    assertEquals(0.0, values[1]);
  }

  @Test
  @DisplayName("given some infinite objective vectors, when evaluating, then the finite ones are measured")
  void givenSomeInfiniteObjectiveVectors_whenEvaluating_thenTheFiniteOnesAreMeasured() {
    // Act
    double[] values = evaluate(x -> x < 0.5);

    // Assert
    assertTrue(Double.isFinite(values[0]) && values[0] < Double.MAX_VALUE, "EP: " + values[0]);
    assertTrue(values[1] < 0.0, "HV- should be negative (positive hypervolume): " + values[1]);
  }
}
