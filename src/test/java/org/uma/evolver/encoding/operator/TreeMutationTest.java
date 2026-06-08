package org.uma.evolver.encoding.operator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.uma.evolver.encoding.util.TreeSolutionGenerator;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.InvalidProbabilityValueException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class TreeMutation")
class TreeMutationTest {

  private static final double PROB_ZERO = 0.0;
  private static final double PROB_ONE = 1.0;
  private static final double DISTRIBUTION_INDEX = 20.0;

  // --- Parameter space and generator ---

  /**
   * Minimal parameter space:
   *   crossover (categorical: SBX | BLX)
   *     global child: crossoverProbability (double [0,1])
   *     conditional on SBX: sbxDistributionIndex (double [5, 400])
   *   populationSize (integer [10, 200])
   */
  private static ParameterSpace buildParameterSpace() {
    var space = new ParameterSpace() {
      @Override public ParameterSpace createInstance() { return this; }
    };
    var crossoverProb = new DoubleParameter("crossoverProbability", 0.0, 1.0);
    var sbxIndex = new DoubleParameter("sbxDistributionIndex", 5.0, 400.0);
    var crossover = new CategoricalParameter("crossover", List.of("SBX", "BLX"));
    crossover.addGlobalSubParameter(crossoverProb);
    crossover.addConditionalParameter("SBX", sbxIndex);
    var popSize = new IntegerParameter("populationSize", 10, 200);
    space.put(crossover);
    space.put(crossoverProb);
    space.put(sbxIndex);
    space.put(popSize);
    space.addTopLevelParameter(crossover);
    space.addTopLevelParameter(popSize);
    return space;
  }

  private static TreeSolutionGenerator buildGenerator() {
    return new TreeSolutionGenerator(buildParameterSpace());
  }

  // --- Solution helpers (single-node for deterministic node selection) ---

  private DerivationTreeSolution doubleSolution(double value) {
    var s = new DerivationTreeSolution(2, 0);
    s.addRoot(new TreeNode(new DoubleParameter("p", 0.0, 1.0), value));
    return s;
  }

  private DerivationTreeSolution integerSolution(int value) {
    var s = new DerivationTreeSolution(2, 0);
    s.addRoot(new TreeNode(new IntegerParameter("p", 1, 100), value));
    return s;
  }

  /** Two-valued categorical: with one node, mutation always picks the other value. */
  private DerivationTreeSolution categoricalSolution(String selected) {
    var s = new DerivationTreeSolution(2, 0);
    s.addRoot(new TreeNode(new CategoricalParameter("p", List.of("A", "B")), selected));
    return s;
  }

  private DerivationTreeSolution booleanSolution(boolean value) {
    var s = new DerivationTreeSolution(2, 0);
    s.addRoot(new TreeNode(new BooleanParameter("p"), value));
    return s;
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("a mutation operator is created with valid parameters")
    void givenValidParameters_whenConstructing_thenOperatorIsCreated() {
      // Arrange & Act
      var mutation = new TreeMutation(0.1, DISTRIBUTION_INDEX, buildGenerator());

      // Assert
      assertEquals(0.1, mutation.mutationProbability());
    }

    @Test
    @DisplayName("an InvalidProbabilityValueException is raised when probability is negative")
    void givenNegativeProbability_whenConstructing_thenInvalidProbabilityValueExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new TreeMutation(-0.1, DISTRIBUTION_INDEX, buildGenerator());

      // Act & Assert
      assertThrows(InvalidProbabilityValueException.class, executable);
    }

    @Test
    @DisplayName("an InvalidConditionException is raised when distribution index is negative")
    void givenNegativeDistributionIndex_whenConstructing_thenInvalidConditionExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new TreeMutation(0.1, -1.0, buildGenerator());

      // Act & Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("a NullParameterException is raised when the generator is null")
    void givenNullGenerator_whenConstructing_thenNullParameterExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new TreeMutation(0.1, DISTRIBUTION_INDEX, null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling execute()")
  class ExecuteTests {

    @Test
    @DisplayName("a NullParameterException is raised when the solution is null")
    void givenNullSolution_whenExecuting_thenNullParameterExceptionIsThrown() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      Executable executable = () -> mutation.execute(null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("at probability 0.0, the solution parameter array is unchanged")
    void givenProbabilityZero_whenExecuting_thenSolutionIsUnchanged() {
      // Arrange
      var mutation = new TreeMutation(PROB_ZERO, DISTRIBUTION_INDEX, buildGenerator());
      var solution = doubleSolution(0.5);
      var paramsBefore = solution.toParameterArray();

      // Act
      mutation.execute(solution);

      // Assert
      assertArrayEquals(paramsBefore, solution.toParameterArray());
    }

    @Test
    @DisplayName("the same solution instance is returned")
    void givenSolution_whenExecuting_thenSameInstanceIsReturned() {
      // Arrange
      var mutation = new TreeMutation(PROB_ZERO, DISTRIBUTION_INDEX, buildGenerator());
      var solution = doubleSolution(0.5);

      // Act
      var result = mutation.execute(solution);

      // Assert
      assertTrue(result == solution);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When mutating a DOUBLE node")
  class DoubleNodeMutationTests {

    @Test
    @DisplayName("the mutated value stays within the parameter lower bound")
    void givenDoubleNode_whenMutated_thenValueIsAboveOrEqualToLowerBound() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var solution = doubleSolution(0.5);

      // Act
      mutation.execute(solution);
      double mutatedValue = (double) solution.roots().get(0).value();

      // Assert
      assertTrue(mutatedValue >= 0.0, "Value must be >= lowerBound (0.0)");
    }

    @Test
    @DisplayName("the mutated value stays within the parameter upper bound")
    void givenDoubleNode_whenMutated_thenValueIsBelowOrEqualToUpperBound() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var solution = doubleSolution(0.5);

      // Act
      mutation.execute(solution);
      double mutatedValue = (double) solution.roots().get(0).value();

      // Assert
      assertTrue(mutatedValue <= 1.0, "Value must be <= upperBound (1.0)");
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When mutating an INTEGER node")
  class IntegerNodeMutationTests {

    @Test
    @DisplayName("the mutated value stays within the parameter range")
    void givenIntegerNode_whenMutated_thenValueIsWithinRange() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var solution = integerSolution(50);

      // Act
      mutation.execute(solution);
      int mutatedValue = (int) solution.roots().get(0).value();

      // Assert
      assertTrue(mutatedValue >= 1 && mutatedValue <= 100,
          "Value must be in [1, 100], was: " + mutatedValue);
    }

    @Test
    @DisplayName("the mutated value is an integer")
    void givenIntegerNode_whenMutated_thenValueIsAnInteger() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var solution = integerSolution(50);

      // Act
      mutation.execute(solution);
      var value = solution.roots().get(0).value();

      // Assert
      assertTrue(value instanceof Integer, "Value must be an Integer");
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When mutating a CATEGORICAL node")
  class CategoricalNodeMutationTests {

    @Test
    @DisplayName("the mutated value differs from the original")
    void givenCategoricalNodeWithTwoValues_whenMutated_thenValueChanges() {
      // Arrange — two-value categorical: after mutation the only other option is selected
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var solution = categoricalSolution("A");

      // Act
      mutation.execute(solution);
      var mutatedValue = solution.roots().get(0).value();

      // Assert
      assertNotEquals("A", mutatedValue);
    }

    @Test
    @DisplayName("global children are preserved after categorical mutation")
    void givenCategoricalNodeWithGlobalChild_whenMutated_thenGlobalChildrenArePreserved() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var s = new DerivationTreeSolution(2, 0);
      var root = new TreeNode(new CategoricalParameter("p", List.of("A", "B")), "A");
      var globalChild = new TreeNode(new DoubleParameter("prob", 0.0, 1.0), 0.5);
      root.addGlobalChild(globalChild);
      s.addRoot(root);

      // Act
      mutation.execute(s);

      // Assert
      assertEquals(1, s.roots().get(0).globalChildren().size());
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When mutating a BOOLEAN node")
  class BooleanNodeMutationTests {

    @Test
    @DisplayName("a true value is flipped to false")
    void givenBooleanNodeSetToTrue_whenMutated_thenValueIsFalse() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var solution = booleanSolution(true);

      // Act
      mutation.execute(solution);
      var mutatedValue = solution.roots().get(0).value();

      // Assert
      assertEquals(false, mutatedValue);
    }

    @Test
    @DisplayName("a false value is flipped to true")
    void givenBooleanNodeSetToFalse_whenMutated_thenValueIsTrue() {
      // Arrange
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, buildGenerator());
      var solution = booleanSolution(false);

      // Act
      mutation.execute(solution);
      var mutatedValue = solution.roots().get(0).value();

      // Assert
      assertEquals(true, mutatedValue);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When checking solution validity after mutation")
  class ValidityTests {

    @Test
    @DisplayName("a solution generated from the parameter space is valid after mutation")
    void givenGeneratedSolution_whenMutated_thenSolutionRemainsValid() {
      // Arrange
      var generator = buildGenerator();
      var mutation = new TreeMutation(PROB_ONE, DISTRIBUTION_INDEX, generator);
      var solution = generator.generate(2);

      // Act
      mutation.execute(solution);
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertTrue(errors.isEmpty(), "Validation errors after mutation: " + errors);
    }
  }
}
