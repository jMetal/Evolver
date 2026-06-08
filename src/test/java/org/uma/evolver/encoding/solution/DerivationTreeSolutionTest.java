package org.uma.evolver.encoding.solution;

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
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class DerivationTreeSolution")
class DerivationTreeSolutionTest {

  private static final int OBJECTIVES = 2;
  private static final int CONSTRAINTS = 0;

  // --- Helpers ---

  private DerivationTreeSolution emptySolution() {
    return new DerivationTreeSolution(OBJECTIVES, CONSTRAINTS);
  }

  private TreeNode doubleNode(String name, double lower, double upper, double value) {
    return new TreeNode(new DoubleParameter(name, lower, upper), value);
  }

  private TreeNode intNode(String name, int lower, int upper, int value) {
    return new TreeNode(new IntegerParameter(name, lower, upper), value);
  }

  private TreeNode categoricalNode(String name, List<String> values, String selected) {
    return new TreeNode(new CategoricalParameter(name, values), selected);
  }

  /**
   * Builds a two-level solution:
   *   root: crossover = "SBX"  (categorical)
   *     conditional child: sbxDistributionIndex = 20.0  (double)
   */
  private DerivationTreeSolution twoLevelSolution() {
    var solution = emptySolution();
    var root = categoricalNode("crossover", List.of("SBX", "BLX"), "SBX");
    var child = doubleNode("sbxDistributionIndex", 5.0, 400.0, 20.0);
    root.addConditionalChild(child);
    solution.addRoot(root);
    return solution;
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("a solution is created with valid objectives and constraints")
    void givenValidObjectivesAndConstraints_whenConstructing_thenSolutionIsCreated() {
      // Arrange & Act
      var solution = new DerivationTreeSolution(3, 1);

      // Assert
      assertEquals(3, solution.objectives().length);
      assertEquals(1, solution.constraints().length);
      assertTrue(solution.roots().isEmpty());
    }

    @Test
    @DisplayName("zero constraints are allowed")
    void givenZeroConstraints_whenConstructing_thenSolutionIsCreated() {
      // Arrange & Act
      var solution = new DerivationTreeSolution(2, 0);

      // Assert
      assertEquals(0, solution.constraints().length);
    }

    @Test
    @DisplayName("an InvalidConditionException is raised when numberOfObjectives is zero")
    void givenZeroObjectives_whenConstructing_thenInvalidConditionExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new DerivationTreeSolution(0, CONSTRAINTS);

      // Act & Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an InvalidConditionException is raised when numberOfObjectives is negative")
    void givenNegativeObjectives_whenConstructing_thenInvalidConditionExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new DerivationTreeSolution(-1, CONSTRAINTS);

      // Act & Assert
      assertThrows(InvalidConditionException.class, executable);
    }

    @Test
    @DisplayName("an InvalidConditionException is raised when numberOfConstraints is negative")
    void givenNegativeConstraints_whenConstructing_thenInvalidConditionExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new DerivationTreeSolution(OBJECTIVES, -1);

      // Act & Assert
      assertThrows(InvalidConditionException.class, executable);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When managing roots")
  class RootsTests {

    @Test
    @DisplayName("added roots are retrievable in insertion order")
    void givenMultipleRoots_whenAdded_thenRetrievedInInsertionOrder() {
      // Arrange
      var solution = emptySolution();
      var root1 = doubleNode("crossoverProbability", 0.0, 1.0, 0.9);
      var root2 = intNode("populationSize", 10, 200, 100);

      // Act
      solution.addRoot(root1);
      solution.addRoot(root2);

      // Assert
      assertEquals(2, solution.roots().size());
      assertEquals(root1, solution.roots().get(0));
      assertEquals(root2, solution.roots().get(1));
    }

    @Test
    @DisplayName("a NullParameterException is raised when adding a null root")
    void givenNullRoot_whenAdding_thenNullParameterExceptionIsThrown() {
      // Arrange
      var solution = emptySolution();
      Executable executable = () -> solution.addRoot(null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("an UnsupportedOperationException is raised when modifying the roots list directly")
    void givenRootsList_whenModifyingDirectly_thenUnsupportedOperationExceptionIsThrown() {
      // Arrange
      var solution = emptySolution();
      solution.addRoot(doubleNode("p", 0.0, 1.0, 0.5));
      Executable executable = () -> solution.roots().add(doubleNode("q", 0.0, 1.0, 0.5));

      // Act & Assert
      assertThrows(UnsupportedOperationException.class, executable);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling allNodes()")
  class AllNodesTests {

    @Test
    @DisplayName("only the root is returned for a single-level solution")
    void givenSingleRoot_whenCallingAllNodes_thenOnlyRootIsReturned() {
      // Arrange
      var solution = emptySolution();
      solution.addRoot(doubleNode("p", 0.0, 1.0, 0.5));

      // Act
      var nodes = solution.allNodes();

      // Assert
      assertEquals(1, nodes.size());
    }

    @Test
    @DisplayName("root and all descendants are returned for a multi-level tree")
    void givenMultiLevelTree_whenCallingAllNodes_thenAllNodesAreReturned() {
      // Arrange
      var solution = twoLevelSolution();

      // Act
      var nodes = solution.allNodes();

      // Assert
      assertEquals(2, nodes.size());
    }

    @Test
    @DisplayName("nodes from multiple roots are all included")
    void givenMultipleRoots_whenCallingAllNodes_thenNodesFromAllRootsAreReturned() {
      // Arrange
      var solution = emptySolution();
      solution.addRoot(doubleNode("p1", 0.0, 1.0, 0.5));
      solution.addRoot(intNode("p2", 1, 100, 50));

      // Act
      var nodes = solution.allNodes();

      // Assert
      assertEquals(2, nodes.size());
    }

    @Test
    @DisplayName("an empty list is returned for a solution with no roots")
    void givenNoRoots_whenCallingAllNodes_thenEmptyListIsReturned() {
      // Arrange
      var solution = emptySolution();

      // Act
      var nodes = solution.allNodes();

      // Assert
      assertTrue(nodes.isEmpty());
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling nodesBySymbol()")
  class NodesBySymbolTests {

    @Test
    @DisplayName("the matching node is returned when the symbol exists at the root level")
    void givenSymbolAtRootLevel_whenSearching_thenMatchingNodeIsReturned() {
      // Arrange
      var solution = emptySolution();
      var root = doubleNode("crossoverProbability", 0.0, 1.0, 0.9);
      solution.addRoot(root);

      // Act
      var result = solution.nodesBySymbol("crossoverProbability");

      // Assert
      assertEquals(1, result.size());
      assertEquals(root, result.get(0));
    }

    @Test
    @DisplayName("a nested node is found when the symbol exists at a deeper level")
    void givenSymbolAtDeepLevel_whenSearching_thenMatchingNodeIsReturned() {
      // Arrange
      var solution = twoLevelSolution();

      // Act
      var result = solution.nodesBySymbol("sbxDistributionIndex");

      // Assert
      assertEquals(1, result.size());
    }

    @Test
    @DisplayName("an empty list is returned when the symbol does not exist in the tree")
    void givenAbsentSymbol_whenSearching_thenEmptyListIsReturned() {
      // Arrange
      var solution = twoLevelSolution();

      // Act
      var result = solution.nodesBySymbol("nonExistentParameter");

      // Assert
      assertTrue(result.isEmpty());
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling toParameterArray()")
  class ToParameterArrayTests {

    @Test
    @DisplayName("each parameter produces a '--name value' pair in the array")
    void givenSingleRoot_whenCallingToParameterArray_thenArrayContainsNameValuePair() {
      // Arrange
      var solution = emptySolution();
      solution.addRoot(doubleNode("crossoverProbability", 0.0, 1.0, 0.9));

      // Act
      var params = solution.toParameterArray();

      // Assert
      assertEquals("--crossoverProbability", params[0]);
      assertEquals("0.9", params[1]);
    }

    @Test
    @DisplayName("all active nodes (root and children) appear in the array")
    void givenMultiLevelTree_whenCallingToParameterArray_thenAllActiveNodesAppear() {
      // Arrange
      var solution = twoLevelSolution();

      // Act
      var params = solution.toParameterArray();
      var paramString = String.join(" ", params);

      // Assert
      assertTrue(paramString.contains("--crossover"));
      assertTrue(paramString.contains("SBX"));
      assertTrue(paramString.contains("--sbxDistributionIndex"));
      assertTrue(paramString.contains("20.0"));
    }

    @Test
    @DisplayName("the array length equals twice the number of active nodes")
    void givenSolution_whenCallingToParameterArray_thenArrayLengthIsTwiceNodeCount() {
      // Arrange
      var solution = twoLevelSolution();
      int expectedLength = solution.allNodes().size() * 2;

      // Act
      var params = solution.toParameterArray();

      // Assert
      assertEquals(expectedLength, params.length);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling copy()")
  class CopyTests {

    @Test
    @DisplayName("the copy is a different object")
    void givenSolution_whenCopied_thenCopyIsDifferentObject() {
      // Arrange
      var solution = twoLevelSolution();

      // Act
      var copy = (DerivationTreeSolution) solution.copy();

      // Assert
      assertNotSame(solution, copy);
    }

    @Test
    @DisplayName("the objectives array in the copy is independent of the original")
    void givenSolution_whenCopied_thenObjectivesArrayIsIndependent() {
      // Arrange
      var solution = twoLevelSolution();
      solution.objectives()[0] = 1.0;
      solution.objectives()[1] = 2.0;

      // Act
      var copy = (DerivationTreeSolution) solution.copy();
      copy.objectives()[0] = 99.0;

      // Assert
      assertEquals(1.0, solution.objectives()[0]);
    }

    @Test
    @DisplayName("modifying the copy node values does not affect the original")
    void givenCopiedSolution_whenModifyingCopyNodeValue_thenOriginalIsUnchanged() {
      // Arrange
      var solution = twoLevelSolution();
      double originalValue = (double) solution.roots().get(0).children().get(0).value();
      var copy = (DerivationTreeSolution) solution.copy();

      // Act
      copy.roots().get(0).children().get(0).value(999.0);

      // Assert
      assertEquals(originalValue, solution.roots().get(0).children().get(0).value());
    }

    @Test
    @DisplayName("the copy preserves the same objective values as the original")
    void givenSolutionWithObjectives_whenCopied_thenObjectiveValuesArePreserved() {
      // Arrange
      var solution = twoLevelSolution();
      solution.objectives()[0] = 0.5;
      solution.objectives()[1] = 0.3;

      // Act
      var copy = (DerivationTreeSolution) solution.copy();

      // Assert
      assertArrayEquals(solution.objectives(), copy.objectives());
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling variables()")
  class VariablesTests {

    @Test
    @DisplayName("variables() returns the same content as roots()")
    void givenSolutionWithRoots_whenCallingVariables_thenSameContentAsRoots() {
      // Arrange
      var solution = twoLevelSolution();

      // Act
      var variables = solution.variables();
      var roots = solution.roots();

      // Assert
      assertEquals(roots.size(), variables.size());
      assertEquals(roots.get(0), variables.get(0));
    }
  }
}
