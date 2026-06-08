package org.uma.evolver.encoding.solution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.evolver.encoding.solution.TreeNode.NodeType;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class TreeNode")
class TreeNodeTest {

  private static final String PARAM_NAME = "testParam";
  private static final double LOWER_BOUND = 0.0;
  private static final double UPPER_BOUND = 1.0;
  private static final int INT_LOWER = 1;
  private static final int INT_UPPER = 100;
  private static final List<String> VALID_VALUES = List.of("SBX", "BLX", "wholeArithmetic");

  // --- Helpers ---

  private TreeNode doubleNode() {
    return new TreeNode(new DoubleParameter(PARAM_NAME, LOWER_BOUND, UPPER_BOUND), 0.5);
  }

  private TreeNode integerNode() {
    return new TreeNode(new IntegerParameter(PARAM_NAME, INT_LOWER, INT_UPPER), 50);
  }

  private TreeNode categoricalNode() {
    return new TreeNode(new CategoricalParameter(PARAM_NAME, VALID_VALUES), "SBX");
  }

  private TreeNode booleanNode() {
    return new TreeNode(new BooleanParameter(PARAM_NAME), true);
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling the constructor")
  class ConstructorTests {

    @Test
    @DisplayName("a NullParameterException is raised if the parameter is null")
    void givenNullParameter_whenConstructingNode_thenNullParameterExceptionIsThrown() {
      // Arrange
      Executable executable = () -> new TreeNode(null, 0.5);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("a node is created with valid parameter and value")
    void givenValidParameterAndValue_whenConstructingNode_thenNodeIsCreated() {
      // Arrange & Act
      var node = doubleNode();

      // Assert
      assertNotNull(node);
      assertEquals(PARAM_NAME, node.grammarSymbol());
      assertEquals(0.5, node.value());
    }

    @Test
    @DisplayName("a new node has no children")
    void givenNewNode_whenCheckingChildren_thenAllChildListsAreEmpty() {
      // Arrange & Act
      var node = doubleNode();

      // Assert
      assertTrue(node.globalChildren().isEmpty());
      assertTrue(node.conditionalChildren().isEmpty());
      assertTrue(node.children().isEmpty());
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling type()")
  class TypeTests {

    @Test
    @DisplayName("CATEGORICAL is returned for a CategoricalParameter")
    void givenCategoricalParameter_whenCallingType_thenReturnsCATEGORICAL() {
      // Arrange
      var node = categoricalNode();

      // Act
      var type = node.type();

      // Assert
      assertEquals(NodeType.CATEGORICAL, type);
    }

    @Test
    @DisplayName("DOUBLE is returned for a DoubleParameter")
    void givenDoubleParameter_whenCallingType_thenReturnsDOUBLE() {
      // Arrange
      var node = doubleNode();

      // Act
      var type = node.type();

      // Assert
      assertEquals(NodeType.DOUBLE, type);
    }

    @Test
    @DisplayName("INTEGER is returned for an IntegerParameter")
    void givenIntegerParameter_whenCallingType_thenReturnsINTEGER() {
      // Arrange
      var node = integerNode();

      // Act
      var type = node.type();

      // Assert
      assertEquals(NodeType.INTEGER, type);
    }

    @Test
    @DisplayName("BOOLEAN is returned for a BooleanParameter")
    void givenBooleanParameter_whenCallingType_thenReturnsBOOLEAN() {
      // Arrange
      var node = booleanNode();

      // Act
      var type = node.type();

      // Assert
      assertEquals(NodeType.BOOLEAN, type);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When getting and setting the value")
  class ValueTests {

    @Test
    @DisplayName("the initial value set at construction is returned")
    void givenInitialValue_whenCallingValue_thenInitialValueIsReturned() {
      // Arrange
      var node = new TreeNode(new DoubleParameter(PARAM_NAME, LOWER_BOUND, UPPER_BOUND), 0.7);

      // Act
      var value = node.value();

      // Assert
      assertEquals(0.7, value);
    }

    @Test
    @DisplayName("the new value is returned after updating it")
    void givenNewValue_whenSettingValue_thenUpdatedValueIsReturned() {
      // Arrange
      var node = doubleNode();

      // Act
      node.value(0.9);

      // Assert
      assertEquals(0.9, node.value());
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling grammarSymbol()")
  class GrammarSymbolTests {

    @Test
    @DisplayName("the parameter name is returned")
    void givenNamedParameter_whenCallingGrammarSymbol_thenParameterNameIsReturned() {
      // Arrange
      var node = new TreeNode(new DoubleParameter("crossoverProbability", 0.0, 1.0), 0.9);

      // Act
      var symbol = node.grammarSymbol();

      // Assert
      assertEquals("crossoverProbability", symbol);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling lowerBound() and upperBound()")
  class BoundsTests {

    @Test
    @DisplayName("the correct lower bound is returned for a DoubleParameter")
    void givenDoubleParameter_whenCallingLowerBound_thenMinValueIsReturned() {
      // Arrange
      var node = doubleNode();

      // Act
      var lower = node.lowerBound();

      // Assert
      assertEquals(LOWER_BOUND, lower);
    }

    @Test
    @DisplayName("the correct upper bound is returned for a DoubleParameter")
    void givenDoubleParameter_whenCallingUpperBound_thenMaxValueIsReturned() {
      // Arrange
      var node = doubleNode();

      // Act
      var upper = node.upperBound();

      // Assert
      assertEquals(UPPER_BOUND, upper);
    }

    @Test
    @DisplayName("the correct bounds are returned for an IntegerParameter")
    void givenIntegerParameter_whenCallingBounds_thenCorrectBoundsAreReturned() {
      // Arrange
      var node = integerNode();

      // Act & Assert
      assertEquals(INT_LOWER, node.lowerBound());
      assertEquals(INT_UPPER, node.upperBound());
    }

    @Test
    @DisplayName("an IllegalStateException is raised when calling lowerBound() on a categorical node")
    void givenCategoricalNode_whenCallingLowerBound_thenIllegalStateExceptionIsThrown() {
      // Arrange
      var node = categoricalNode();
      Executable executable = node::lowerBound;

      // Act & Assert
      assertThrows(IllegalStateException.class, executable);
    }

    @Test
    @DisplayName("an IllegalStateException is raised when calling upperBound() on a boolean node")
    void givenBooleanNode_whenCallingUpperBound_thenIllegalStateExceptionIsThrown() {
      // Arrange
      var node = booleanNode();
      Executable executable = node::upperBound;

      // Act & Assert
      assertThrows(IllegalStateException.class, executable);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling validValues()")
  class ValidValuesTests {

    @Test
    @DisplayName("all valid values are returned for a CategoricalParameter")
    void givenCategoricalParameter_whenCallingValidValues_thenAllValuesAreReturned() {
      // Arrange
      var node = categoricalNode();

      // Act
      var values = node.validValues();

      // Assert
      assertEquals(VALID_VALUES, values);
    }

    @Test
    @DisplayName("an empty list is returned for a DoubleParameter")
    void givenDoubleParameter_whenCallingValidValues_thenEmptyListIsReturned() {
      // Arrange
      var node = doubleNode();

      // Act
      var values = node.validValues();

      // Assert
      assertTrue(values.isEmpty());
    }

    @Test
    @DisplayName("an empty list is returned for a BooleanParameter")
    void givenBooleanParameter_whenCallingValidValues_thenEmptyListIsReturned() {
      // Arrange
      var node = booleanNode();

      // Act
      var values = node.validValues();

      // Assert
      assertTrue(values.isEmpty());
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When managing children")
  class ChildrenTests {

    @Test
    @DisplayName("a global child is added and appears in globalChildren()")
    void givenGlobalChild_whenAdded_thenAppearsInGlobalChildren() {
      // Arrange
      var parent = categoricalNode();
      var child = doubleNode();

      // Act
      parent.addGlobalChild(child);

      // Assert
      assertEquals(1, parent.globalChildren().size());
      assertEquals(child, parent.globalChildren().get(0));
    }

    @Test
    @DisplayName("a conditional child is added and appears in conditionalChildren()")
    void givenConditionalChild_whenAdded_thenAppearsInConditionalChildren() {
      // Arrange
      var parent = categoricalNode();
      var child = doubleNode();

      // Act
      parent.addConditionalChild(child);

      // Assert
      assertEquals(1, parent.conditionalChildren().size());
      assertEquals(child, parent.conditionalChildren().get(0));
    }

    @Test
    @DisplayName("children() returns global and conditional children combined")
    void givenGlobalAndConditionalChildren_whenCallingChildren_thenAllAreReturned() {
      // Arrange
      var parent = categoricalNode();
      var global = doubleNode();
      var conditional = integerNode();

      parent.addGlobalChild(global);
      parent.addConditionalChild(conditional);

      // Act
      var all = parent.children();

      // Assert
      assertEquals(2, all.size());
      assertTrue(all.contains(global));
      assertTrue(all.contains(conditional));
    }

    @Test
    @DisplayName("an UnsupportedOperationException is raised when modifying globalChildren() list")
    void givenGlobalChildrenList_whenModifyingDirectly_thenUnsupportedOperationExceptionIsThrown() {
      // Arrange
      var parent = categoricalNode();
      parent.addGlobalChild(doubleNode());
      Executable executable = () -> parent.globalChildren().add(doubleNode());

      // Act & Assert
      assertThrows(UnsupportedOperationException.class, executable);
    }

    @Test
    @DisplayName("an UnsupportedOperationException is raised when modifying conditionalChildren() list")
    void givenConditionalChildrenList_whenModifyingDirectly_thenUnsupportedOperationExceptionIsThrown() {
      // Arrange
      var parent = categoricalNode();
      parent.addConditionalChild(doubleNode());
      Executable executable = () -> parent.conditionalChildren().add(doubleNode());

      // Act & Assert
      assertThrows(UnsupportedOperationException.class, executable);
    }

    @Test
    @DisplayName("global children are replaced when calling replaceGlobalChildren()")
    void givenNewGlobalChildren_whenReplacing_thenOldChildrenAreGone() {
      // Arrange
      var parent = categoricalNode();
      parent.addGlobalChild(doubleNode());
      var replacement = integerNode();

      // Act
      parent.replaceGlobalChildren(List.of(replacement));

      // Assert
      assertEquals(1, parent.globalChildren().size());
      assertEquals(replacement, parent.globalChildren().get(0));
    }

    @Test
    @DisplayName("conditional children are replaced when calling replaceConditionalChildren()")
    void givenNewConditionalChildren_whenReplacing_thenOldChildrenAreGone() {
      // Arrange
      var parent = categoricalNode();
      parent.addConditionalChild(doubleNode());
      var replacement = booleanNode();

      // Act
      parent.replaceConditionalChildren(List.of(replacement));

      // Assert
      assertEquals(1, parent.conditionalChildren().size());
      assertEquals(replacement, parent.conditionalChildren().get(0));
    }

    @Test
    @DisplayName("a NullParameterException is raised when adding a null global child")
    void givenNullChild_whenAddingGlobalChild_thenNullParameterExceptionIsThrown() {
      // Arrange
      var parent = categoricalNode();
      Executable executable = () -> parent.addGlobalChild(null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("a NullParameterException is raised when adding a null conditional child")
    void givenNullChild_whenAddingConditionalChild_thenNullParameterExceptionIsThrown() {
      // Arrange
      var parent = categoricalNode();
      Executable executable = () -> parent.addConditionalChild(null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }
  }

  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling deepCopy()")
  class DeepCopyTests {

    @Test
    @DisplayName("the copy is a different object with the same value")
    void givenNode_whenDeepCopied_thenCopyIsDifferentObjectWithSameValue() {
      // Arrange
      var original = doubleNode();

      // Act
      var copy = original.deepCopy();

      // Assert
      assertNotSame(original, copy);
      assertEquals(original.value(), copy.value());
      assertEquals(original.grammarSymbol(), copy.grammarSymbol());
    }

    @Test
    @DisplayName("children are recursively copied")
    void givenNodeWithChildren_whenDeepCopied_thenChildrenAreCopied() {
      // Arrange
      var original = categoricalNode();
      original.addGlobalChild(doubleNode());
      original.addConditionalChild(integerNode());

      // Act
      var copy = original.deepCopy();

      // Assert
      assertEquals(1, copy.globalChildren().size());
      assertEquals(1, copy.conditionalChildren().size());
      assertNotSame(original.globalChildren().get(0), copy.globalChildren().get(0));
      assertNotSame(original.conditionalChildren().get(0), copy.conditionalChildren().get(0));
    }

    @Test
    @DisplayName("modifying the copy value does not affect the original")
    void givenDeepCopy_whenModifyingCopyValue_thenOriginalIsUnchanged() {
      // Arrange
      var original = doubleNode();
      var copy = original.deepCopy();

      // Act
      copy.value(0.99);

      // Assert
      assertEquals(0.5, original.value());
    }

    @Test
    @DisplayName("replacing children in the copy does not affect the original")
    void givenDeepCopy_whenReplacingCopyChildren_thenOriginalChildrenAreUnchanged() {
      // Arrange
      var original = categoricalNode();
      var originalChild = doubleNode();
      original.addGlobalChild(originalChild);
      var copy = original.deepCopy();

      // Act
      copy.replaceGlobalChildren(List.of(integerNode()));

      // Assert
      assertEquals(1, original.globalChildren().size());
      assertEquals(originalChild, original.globalChildren().get(0));
    }
  }
}
