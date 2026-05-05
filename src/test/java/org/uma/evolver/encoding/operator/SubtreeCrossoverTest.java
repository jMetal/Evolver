package org.uma.evolver.encoding.operator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.solution.TreeNode;
import org.uma.evolver.encoding.util.GrammarConverter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.InvalidProbabilityValueException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class SubtreeCrossover")
class SubtreeCrossoverTest {

  private static final double PROB_ZERO = 0.0;
  private static final double PROB_ONE = 1.0;
  private static final String SYMBOL = "crossoverProbability";

  // --- Helpers ---

  /**
   * Single-root solution with a double node holding the given value.
   * Both solutions built with the same symbol → always a compatible crossover point.
   */
  private DerivationTreeSolution singleNodeSolution(double value) {
    var solution = new DerivationTreeSolution(2, 0);
    solution.addRoot(new TreeNode(new DoubleParameter(SYMBOL, 0.0, 1.0), value));
    return solution;
  }

  /**
   * Single-root solution with a categorical node.
   */
  private DerivationTreeSolution categoricalSolution(String paramName, String value) {
    var solution = new DerivationTreeSolution(2, 0);
    solution.addRoot(
        new TreeNode(new CategoricalParameter(paramName, List.of("SBX", "BLX")), value));
    return solution;
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("a crossover operator is created with probability 0.0")
    void givenProbabilityZero_whenConstructing_thenOperatorIsCreated() {
      // Arrange & Act
      var crossover = new SubtreeCrossover(PROB_ZERO);

      // Assert
      assertEquals(PROB_ZERO, crossover.crossoverProbability());
    }

    @Test
    @DisplayName("a crossover operator is created with probability 1.0")
    void givenProbabilityOne_whenConstructing_thenOperatorIsCreated() {
      // Arrange & Act
      var crossover = new SubtreeCrossover(PROB_ONE);

      // Assert
      assertEquals(PROB_ONE, crossover.crossoverProbability());
    }

    @Test
    @DisplayName("an InvalidProbabilityValueException is raised when probability is negative")
    void givenNegativeProbability_whenConstructing_thenInvalidProbabilityValueExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new SubtreeCrossover(-0.1);

      // Act & Assert
      assertThrows(InvalidProbabilityValueException.class, executable);
    }

    @Test
    @DisplayName("an InvalidProbabilityValueException is raised when probability exceeds 1.0")
    void givenProbabilityAboveOne_whenConstructing_thenInvalidProbabilityValueExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new SubtreeCrossover(1.1);

      // Act & Assert
      assertThrows(InvalidProbabilityValueException.class, executable);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When reading operator metadata")
  class MetadataTests {

    @Test
    @DisplayName("numberOfRequiredParents() returns 2")
    void givenCrossoverOperator_whenCallingNumberOfRequiredParents_thenTwoIsReturned() {
      // Arrange
      var crossover = new SubtreeCrossover(0.9);

      // Act
      var result = crossover.numberOfRequiredParents();

      // Assert
      assertEquals(2, result);
    }

    @Test
    @DisplayName("numberOfGeneratedChildren() returns 2")
    void givenCrossoverOperator_whenCallingNumberOfGeneratedChildren_thenTwoIsReturned() {
      // Arrange
      var crossover = new SubtreeCrossover(0.9);

      // Act
      var result = crossover.numberOfGeneratedChildren();

      // Assert
      assertEquals(2, result);
    }

    @Test
    @DisplayName("crossoverProbability() matches the value passed to the constructor")
    void givenProbability_whenReadingCrossoverProbability_thenConstructorValueIsReturned() {
      // Arrange
      double probability = 0.75;
      var crossover = new SubtreeCrossover(probability);

      // Act
      var result = crossover.crossoverProbability();

      // Assert
      assertEquals(probability, result);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling execute()")
  class ExecuteTests {

    @Test
    @DisplayName("a NullParameterException is raised when the parent list is null")
    void givenNullParentList_whenExecuting_thenNullParameterExceptionIsThrown() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ONE);
      Executable executable = () -> crossover.execute(null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("an InvalidConditionException is raised when only one parent is provided")
    void givenOneParent_whenExecuting_thenInvalidConditionExceptionIsThrown() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parents = List.of(singleNodeSolution(0.9));
      Executable executable = () -> crossover.execute(parents);

      // Act & Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("two offspring are returned")
    void givenTwoParents_whenExecuting_thenTwoOffspringAreReturned() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parents = List.of(singleNodeSolution(0.9), singleNodeSolution(0.3));

      // Act
      var offspring = crossover.execute(parents);

      // Assert
      assertEquals(2, offspring.size());
    }

    @Test
    @DisplayName("offspring are different objects from their respective parents")
    void givenTwoParents_whenExecuting_thenOffspringAreDifferentObjects() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parent1 = singleNodeSolution(0.9);
      var parent2 = singleNodeSolution(0.3);

      // Act
      var offspring = crossover.execute(List.of(parent1, parent2));

      // Assert
      assertNotSame(parent1, offspring.get(0));
      assertNotSame(parent2, offspring.get(1));
    }

    @Test
    @DisplayName("parents are not modified after crossover")
    void givenTwoParents_whenExecuting_thenParentsAreUnchanged() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parent1 = singleNodeSolution(0.9);
      var parent2 = singleNodeSolution(0.3);
      var p1ParamsBefore = parent1.toParameterArray();
      var p2ParamsBefore = parent2.toParameterArray();

      // Act
      crossover.execute(List.of(parent1, parent2));

      // Assert
      assertArrayEquals(p1ParamsBefore, parent1.toParameterArray());
      assertArrayEquals(p2ParamsBefore, parent2.toParameterArray());
    }

    @Test
    @DisplayName("at probability 0.0, offspring parameter arrays equal those of their parents")
    void givenProbabilityZero_whenExecuting_thenOffspringEqualParents() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ZERO);
      var parent1 = singleNodeSolution(0.9);
      var parent2 = singleNodeSolution(0.3);

      // Act
      var offspring = crossover.execute(List.of(parent1, parent2));

      // Assert
      assertArrayEquals(parent1.toParameterArray(), offspring.get(0).toParameterArray());
      assertArrayEquals(parent2.toParameterArray(), offspring.get(1).toParameterArray());
    }

    @Test
    @DisplayName("at probability 1.0, single-node solutions have their values swapped")
    void givenProbabilityOne_whenExecutingOnSingleNodeSolutions_thenValuesAreSwapped() {
      // Arrange — each solution has exactly one node with the same symbol,
      // so the crossover point selection is deterministic (nextInt(0,0) = 0).
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parent1 = singleNodeSolution(0.9);
      var parent2 = singleNodeSolution(0.3);

      // Act
      var offspring = crossover.execute(List.of(parent1, parent2));

      // Assert
      assertEquals(0.3, offspring.get(0).roots().get(0).value());
      assertEquals(0.9, offspring.get(1).roots().get(0).value());
    }

    @Test
    @DisplayName("swapped nodes share the same grammar symbol")
    void givenTwoCompatibleSolutions_whenExecuting_thenOffspringNodesHaveSameSymbols() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parent1 = singleNodeSolution(0.9);
      var parent2 = singleNodeSolution(0.3);

      // Act
      var offspring = crossover.execute(List.of(parent1, parent2));

      // Assert — both offspring must have exactly one node with the expected symbol
      var symbols1 = offspring.get(0).allNodes().stream()
          .map(TreeNode::grammarSymbol).toList();
      var symbols2 = offspring.get(1).allNodes().stream()
          .map(TreeNode::grammarSymbol).toList();
      assertTrue(symbols1.contains(SYMBOL));
      assertTrue(symbols2.contains(SYMBOL));
    }

    @Test
    @DisplayName("offspring are valid after crossover")
    void givenTwoCompatibleSolutions_whenExecuting_thenOffspringAreValid() {
      // Arrange
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parent1 = categoricalSolution("crossover", "SBX");
      var parent2 = categoricalSolution("crossover", "BLX");

      // Act
      var offspring = crossover.execute(List.of(parent1, parent2));

      // Assert
      assertTrue(GrammarConverter.validate(offspring.get(0)).isEmpty());
      assertTrue(GrammarConverter.validate(offspring.get(1)).isEmpty());
    }

    @Test
    @DisplayName("offspring are unchanged when no compatible node exists in the other parent")
    void givenIncompatibleSolutions_whenExecuting_thenOffspringEqualParents() {
      // Arrange — each solution has a different symbol → no compatible crossover point
      var crossover = new SubtreeCrossover(PROB_ONE);
      var parent1 = singleNodeSolution(0.9);                          // symbol: "crossoverProbability"
      var parent2 = new DerivationTreeSolution(2, 0);
      parent2.addRoot(
          new TreeNode(new DoubleParameter("mutationProbability", 0.0, 1.0), 0.1));

      var p1ParamsBefore = parent1.toParameterArray();
      var p2ParamsBefore = parent2.toParameterArray();

      // Act
      var offspring = crossover.execute(List.of(parent1, parent2));

      // Assert
      assertArrayEquals(p1ParamsBefore, offspring.get(0).toParameterArray());
      assertArrayEquals(p2ParamsBefore, offspring.get(1).toParameterArray());
    }
  }
}
