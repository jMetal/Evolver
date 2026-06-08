package org.uma.evolver.encoding.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.evolver.encoding.solution.TreeNode;
import org.uma.evolver.encoding.solution.TreeNode.NodeType;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class TreeSolutionGenerator")
class TreeSolutionGeneratorTest {

  /**
   * Parameter space used across all tests:
   *   crossover (categorical: SBX | BLX)
   *     global child : crossoverProbability (double [0, 1])
   *     conditional SBX: sbxDistributionIndex (double [5, 400])
   *   populationSize (integer [10, 200])
   *   useArchive (boolean)
   */
  private ParameterSpace parameterSpace;
  private TreeSolutionGenerator generator;

  @BeforeEach
  void setUp() {
    parameterSpace = buildParameterSpace();
    generator = new TreeSolutionGenerator(parameterSpace);
  }

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
    var useArchive = new BooleanParameter("useArchive");
    space.put(crossover);
    space.put(crossoverProb);
    space.put(sbxIndex);
    space.put(popSize);
    space.put(useArchive);
    space.addTopLevelParameter(crossover);
    space.addTopLevelParameter(popSize);
    space.addTopLevelParameter(useArchive);
    return space;
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("a NullParameterException is raised when the parameter space is null")
    void givenNullParameterSpace_whenConstructing_thenNullParameterExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new TreeSolutionGenerator(null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling generate()")
  class GenerateTests {

    @Test
    @DisplayName("a non-null solution is returned")
    void givenValidParameterSpace_whenGenerating_thenNonNullSolutionIsReturned() {
      // Arrange & Act
      var solution = generator.generate(2);

      // Assert
      assertNotNull(solution);
    }

    @Test
    @DisplayName("the solution has the requested number of objectives")
    void givenNumberOfObjectives_whenGenerating_thenSolutionHasCorrectObjectiveCount() {
      // Arrange & Act
      var solution = generator.generate(3);

      // Assert
      assertEquals(3, solution.objectives().length);
    }

    @Test
    @DisplayName("the number of roots equals the number of top-level parameters")
    void givenParameterSpace_whenGenerating_thenRootCountMatchesTopLevelParameters() {
      // Arrange
      int expectedRoots = parameterSpace.topLevelParameters().size();

      // Act
      var solution = generator.generate(2);

      // Assert
      assertEquals(expectedRoots, solution.roots().size());
    }

    @Test
    @DisplayName("the generated solution passes grammar validation")
    void givenParameterSpace_whenGenerating_thenSolutionIsValid() {
      // Arrange & Act
      var solution = generator.generate(2);
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertTrue(errors.isEmpty(), "Validation errors: " + errors);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling generateSubtree()")
  class GenerateSubtreeTests {

    @Test
    @DisplayName("a DOUBLE node is returned for a DoubleParameter")
    void givenDoubleParameter_whenGeneratingSubtree_thenDoubleNodeIsReturned() {
      // Arrange
      var param = new DoubleParameter("p", 0.0, 1.0);

      // Act
      var node = generator.generateSubtree(param);

      // Assert
      assertEquals(NodeType.DOUBLE, node.type());
    }

    @Test
    @DisplayName("the double node value is within the parameter bounds")
    void givenDoubleParameter_whenGeneratingSubtree_thenValueIsWithinBounds() {
      // Arrange
      var param = new DoubleParameter("p", 0.0, 1.0);

      // Act
      var node = generator.generateSubtree(param);
      double value = (double) node.value();

      // Assert
      assertTrue(value >= 0.0 && value <= 1.0, "Value " + value + " out of [0.0, 1.0]");
    }

    @Test
    @DisplayName("an INTEGER node is returned for an IntegerParameter")
    void givenIntegerParameter_whenGeneratingSubtree_thenIntegerNodeIsReturned() {
      // Arrange
      var param = new IntegerParameter("p", 1, 100);

      // Act
      var node = generator.generateSubtree(param);

      // Assert
      assertEquals(NodeType.INTEGER, node.type());
    }

    @Test
    @DisplayName("the integer node value is within the parameter range")
    void givenIntegerParameter_whenGeneratingSubtree_thenValueIsWithinRange() {
      // Arrange
      var param = new IntegerParameter("p", 1, 100);

      // Act
      var node = generator.generateSubtree(param);
      int value = (int) node.value();

      // Assert
      assertTrue(value >= 1 && value <= 100, "Value " + value + " out of [1, 100]");
    }

    @Test
    @DisplayName("a BOOLEAN node is returned for a BooleanParameter")
    void givenBooleanParameter_whenGeneratingSubtree_thenBooleanNodeIsReturned() {
      // Arrange
      var param = new BooleanParameter("p");

      // Act
      var node = generator.generateSubtree(param);

      // Assert
      assertEquals(NodeType.BOOLEAN, node.type());
    }

    @Test
    @DisplayName("a CATEGORICAL node is returned for a CategoricalParameter with a valid value")
    void givenCategoricalParameter_whenGeneratingSubtree_thenCategoricalNodeWithValidValueIsReturned() {
      // Arrange
      var validValues = List.of("SBX", "BLX");
      var param = new CategoricalParameter("p", validValues);

      // Act
      var node = generator.generateSubtree(param);

      // Assert
      assertEquals(NodeType.CATEGORICAL, node.type());
      assertTrue(validValues.contains(node.value()),
          "Value '" + node.value() + "' not in valid values");
    }

    @Test
    @DisplayName("global children are generated for a categorical node with global sub-parameters")
    void givenCategoricalParameterWithGlobalSubParam_whenGeneratingSubtree_thenGlobalChildIsPresent() {
      // Arrange — use the crossover parameter from the space (has crossoverProbability as global)
      var crossover = (CategoricalParameter) parameterSpace.get("crossover");

      // Act
      var node = generator.generateSubtree(crossover);

      // Assert
      assertFalse(node.globalChildren().isEmpty(),
          "Global children must be present for a parameter with global sub-parameters");
    }

    @Test
    @DisplayName("conditional children are generated only for the selected production")
    void givenCategoricalParameterWithConditional_whenSBXSelected_thenConditionalChildIsPresent() {
      // Arrange — run generate() many times; at least one should pick SBX and have a conditional child
      var crossover = (CategoricalParameter) parameterSpace.get("crossover");
      boolean foundSBXWithConditional = false;

      // Act — up to 50 attempts to get an SBX selection
      for (int i = 0; i < 50; i++) {
        TreeNode node = generator.generateSubtree(crossover);
        if ("SBX".equals(node.value()) && !node.conditionalChildren().isEmpty()) {
          foundSBXWithConditional = true;
          break;
        }
        if ("BLX".equals(node.value()) && node.conditionalChildren().isEmpty()) {
          // BLX has no conditionals — correct
          continue;
        }
      }

      // Assert
      assertTrue(foundSBXWithConditional,
          "Expected at least one SBX selection with its conditional child in 50 attempts");
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling generateConditionalChildren()")
  class GenerateConditionalChildrenTests {

    @Test
    @DisplayName("the conditional children for 'SBX' are returned")
    void givenCategoricalParameterAndSBXValue_whenGeneratingConditionalChildren_thenChildrenAreReturned() {
      // Arrange
      var crossover = parameterSpace.get("crossover");

      // Act
      var children = generator.generateConditionalChildren(crossover, "SBX");

      // Assert
      assertFalse(children.isEmpty(),
          "Expected conditional children for 'SBX'");
    }

    @Test
    @DisplayName("an empty list is returned for a value with no conditional parameters")
    void givenCategoricalParameterAndBLXValue_whenGeneratingConditionalChildren_thenEmptyListIsReturned() {
      // Arrange
      var crossover = parameterSpace.get("crossover");

      // Act
      var children = generator.generateConditionalChildren(crossover, "BLX");

      // Assert
      assertTrue(children.isEmpty(),
          "Expected no conditional children for 'BLX'");
    }

    @Test
    @DisplayName("an empty list is returned for a parameter that has no conditionals at all")
    void givenNonCategoricalParameter_whenGeneratingConditionalChildren_thenEmptyListIsReturned() {
      // Arrange
      var popSize = parameterSpace.get("populationSize");

      // Act
      var children = generator.generateConditionalChildren(popSize, "50");

      // Assert
      assertTrue(children.isEmpty(),
          "Expected no conditional children for a non-categorical parameter");
    }
  }
}
