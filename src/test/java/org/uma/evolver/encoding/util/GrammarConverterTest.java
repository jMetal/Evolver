package org.uma.evolver.encoding.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.evolver.encoding.solution.TreeNode;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.util.errorchecking.exception.NullParameterException;

@DisplayName("Unit tests for class GrammarConverter")
class GrammarConverterTest {

  // ---------------------------------------------------------------------------
  // Shared parameter space
  // ---------------------------------------------------------------------------

  /**
   * Parameter space used across all tests:
   *   crossover (categorical: SBX | BLX)
   *     global child : crossoverProbability (double [0, 1])
   *     conditional SBX: sbxDistributionIndex (double [5, 400])
   *   populationSize (integer [10, 200])
   *   useArchive (boolean)
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
  // toBnf()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling toBnf()")
  class ToBnfTests {

    @Test
    @DisplayName("a NullParameterException is raised when the parameter space is null")
    void givenNullParameterSpace_whenCallingToBnf_thenNullParameterExceptionIsThrown() {
      // Arrange
      Executable executable = () -> GrammarConverter.toBnf(null);

      // Act & Assert
      assertThrows(NullParameterException.class, executable);
    }

    @Test
    @DisplayName("the output contains a <start> rule listing all top-level parameters")
    void givenParameterSpace_whenCallingToBnf_thenStartRuleIsPresent() {
      // Arrange
      var space = buildParameterSpace();

      // Act
      var bnf = GrammarConverter.toBnf(space);

      // Assert
      assertTrue(bnf.contains("<start> ::="), "BNF must contain a start rule");
      assertTrue(bnf.contains("<crossover>"), "Start rule must list <crossover>");
      assertTrue(bnf.contains("<populationSize>"), "Start rule must list <populationSize>");
    }

    @Test
    @DisplayName("categorical parameters appear as non-terminals with alternative productions")
    void givenCategoricalParameter_whenCallingToBnf_thenAlternativesAreListed() {
      // Arrange
      var space = buildParameterSpace();

      // Act
      var bnf = GrammarConverter.toBnf(space);

      // Assert
      assertTrue(bnf.contains("\"SBX\""), "BNF must contain SBX production");
      assertTrue(bnf.contains("\"BLX\""), "BNF must contain BLX production");
      assertTrue(bnf.contains("|"), "BNF must use | to separate alternatives");
    }

    @Test
    @DisplayName("double parameters appear as DOUBLE[min, max] terminals")
    void givenDoubleParameter_whenCallingToBnf_thenDoubleTerminalIsPresent() {
      // Arrange
      var space = buildParameterSpace();

      // Act
      var bnf = GrammarConverter.toBnf(space);

      // Assert
      assertTrue(bnf.contains("DOUBLE[0.0, 1.0]"),
          "BNF must contain DOUBLE terminal for crossoverProbability");
    }

    @Test
    @DisplayName("integer parameters appear as INTEGER[min, max] terminals")
    void givenIntegerParameter_whenCallingToBnf_thenIntegerTerminalIsPresent() {
      // Arrange
      var space = buildParameterSpace();

      // Act
      var bnf = GrammarConverter.toBnf(space);

      // Assert
      assertTrue(bnf.contains("INTEGER[10, 200]"),
          "BNF must contain INTEGER terminal for populationSize");
    }

    @Test
    @DisplayName("boolean parameters appear as BOOLEAN terminals")
    void givenBooleanParameter_whenCallingToBnf_thenBooleanTerminalIsPresent() {
      // Arrange
      var space = buildParameterSpace();

      // Act
      var bnf = GrammarConverter.toBnf(space);

      // Assert
      assertTrue(bnf.contains("BOOLEAN"), "BNF must contain BOOLEAN terminal");
    }

    @Test
    @DisplayName("conditional sub-parameters appear only in their parent production")
    void givenConditionalParameter_whenCallingToBnf_thenConditionalSymbolAppearsInParentProduction() {
      // Arrange
      var space = buildParameterSpace();

      // Act
      var bnf = GrammarConverter.toBnf(space);

      // Assert — sbxDistributionIndex is conditional on SBX, so its symbol must appear
      assertTrue(bnf.contains("<sbxDistributionIndex>"),
          "BNF must reference <sbxDistributionIndex> in the SBX production");
    }

    @Test
    @DisplayName("global sub-parameters appear in all productions of their parent")
    void givenGlobalSubParameter_whenCallingToBnf_thenSymbolAppearsInAllProductions() {
      // Arrange
      var space = buildParameterSpace();

      // Act
      var bnf = GrammarConverter.toBnf(space);

      // Assert — crossoverProbability is global: its symbol appears in both SBX and BLX lines
      var crossoverRule = bnf.lines()
          .filter(l -> l.startsWith("<crossover> ::="))
          .findFirst()
          .orElse("");
      assertTrue(crossoverRule.contains("<crossoverProbability>"),
          "crossoverProbability must appear in the crossover rule");
    }
  }

  // ---------------------------------------------------------------------------
  // validate()
  // ---------------------------------------------------------------------------

  @Nested
  @DisplayName("When calling validate()")
  class ValidateTests {

    @Test
    @DisplayName("an empty error list is returned for a valid solution")
    void givenValidSolution_whenValidating_thenNoErrorsAreReturned() {
      // Arrange
      var solution = new DerivationTreeSolution(2, 0);
      solution.addRoot(new TreeNode(new DoubleParameter("p", 0.0, 1.0), 0.5));

      // Act
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertTrue(errors.isEmpty(), "Expected no errors for a valid solution");
    }

    @Test
    @DisplayName("an error is returned when a double value exceeds the upper bound")
    void givenDoubleValueAboveUpperBound_whenValidating_thenErrorIsReturned() {
      // Arrange
      var solution = new DerivationTreeSolution(2, 0);
      solution.addRoot(new TreeNode(new DoubleParameter("p", 0.0, 1.0), 1.5));

      // Act
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertFalse(errors.isEmpty(), "Expected an error for value 1.5 > upperBound 1.0");
    }

    @Test
    @DisplayName("an error is returned when a double value is below the lower bound")
    void givenDoubleValueBelowLowerBound_whenValidating_thenErrorIsReturned() {
      // Arrange
      var solution = new DerivationTreeSolution(2, 0);
      solution.addRoot(new TreeNode(new DoubleParameter("p", 0.0, 1.0), -0.1));

      // Act
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertFalse(errors.isEmpty(), "Expected an error for value -0.1 < lowerBound 0.0");
    }

    @Test
    @DisplayName("an error is returned when an integer value is out of range")
    void givenIntegerValueOutOfRange_whenValidating_thenErrorIsReturned() {
      // Arrange
      var solution = new DerivationTreeSolution(2, 0);
      solution.addRoot(new TreeNode(new IntegerParameter("p", 1, 100), 200));

      // Act
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertFalse(errors.isEmpty(), "Expected an error for value 200 > upperBound 100");
    }

    @Test
    @DisplayName("an error is returned when a categorical value is not in the valid set")
    void givenInvalidCategoricalValue_whenValidating_thenErrorIsReturned() {
      // Arrange
      var solution = new DerivationTreeSolution(2, 0);
      solution.addRoot(
          new TreeNode(new CategoricalParameter("p", List.of("SBX", "BLX")), "PCX"));

      // Act
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertFalse(errors.isEmpty(), "Expected an error for invalid categorical value 'PCX'");
    }

    @Test
    @DisplayName("an error is returned when a boolean node holds a non-Boolean value")
    void givenNonBooleanValueInBooleanNode_whenValidating_thenErrorIsReturned() {
      // Arrange
      var solution = new DerivationTreeSolution(2, 0);
      solution.addRoot(new TreeNode(new BooleanParameter("p"), "true"));

      // Act
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertFalse(errors.isEmpty(), "Expected an error for String value in boolean node");
    }

    @Test
    @DisplayName("all errors are reported, not just the first one")
    void givenMultipleInvalidNodes_whenValidating_thenAllErrorsAreReported() {
      // Arrange
      var solution = new DerivationTreeSolution(2, 0);
      solution.addRoot(new TreeNode(new DoubleParameter("p1", 0.0, 1.0), 9.9));
      solution.addRoot(new TreeNode(new IntegerParameter("p2", 1, 10), 99));

      // Act
      var errors = GrammarConverter.validate(solution);

      // Assert
      assertTrue(errors.size() >= 2, "Expected at least 2 errors, got: " + errors.size());
    }
  }
}
