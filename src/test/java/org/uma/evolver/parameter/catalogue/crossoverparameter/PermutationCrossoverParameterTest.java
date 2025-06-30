package org.uma.evolver.parameter.catalogue.crossoverparameter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.operator.crossover.impl.*;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

class PermutationCrossoverParameterTest {

  @Nested
  @DisplayName("The constructor")
  class ConstructorTestCases {
    @DisplayName("throws an exception when an invalid crossover operator is provided")
    @Test
    void shouldThrowExceptionWhenInvalidCrossoverOperatorIsProvided() {
      // Arrange
      List<String> invalidOperators = List.of("INVALID_OPERATOR");

      // Act & Assert
      assertThrows(JMetalException.class, () -> new BinaryCrossoverParameter(invalidOperators));
    }

    @DisplayName(
        "does not throw an exception when the full list of a valid crossover operators is provided")
    @Test
    void shouldNotThrowExceptionWhenValidCrossoverOperatorsAreProvided() {
      // Arrange
      List<String> validOperators =
          List.of("OXD", "CX", "PMX", "positionBased", "edgeRecombination");

      // Act & Assert
      assertDoesNotThrow(() -> new PermutationCrossoverParameter(validOperators));
    }

    @DisplayName(
        "does not throw an exception when the a sublist of a valid crossover operators is provided")
    @Test
    void shouldNotThrowExceptionWhenSublistOfValidCrossoverOperatorsAreProvided() {
      // Arrange
      List<String> validOperators = List.of("OXD", "CX");

      // Act & Assert
      assertDoesNotThrow(() -> new PermutationCrossoverParameter(validOperators));
    }

    @DisplayName("throws an exception when an empty list of crossover operators is provided")
    @Test
    void shouldThrowExceptionWhenEmptyListOfCrossoverOperatorsIsProvided() {
      // Arrange
      List<String> emptyList = List.of();

      // Act & Assert
      assertThrows(
          InvalidConditionException.class, () -> new PermutationCrossoverParameter(emptyList));
    }
  }

  @Nested
  @DisplayName("The getCrossover method")
  class GetCrossoverTestCases {
    private PermutationCrossoverParameter crossoverParameter;

    @BeforeEach
    void setUp() {
      crossoverParameter =
          new PermutationCrossoverParameter(
              List.of("OXD", "CX", "PMX", "positionBased", "edgeRecombination"));
      crossoverParameter.addGlobalSubParameter(new DoubleParameter("crossoverProbability", 0, 1.0));
    }

    @DisplayName("returns a valid crossover operator")
    @Test
    void shouldReturnValidCrossoverOperator() {
      // Arrange
      String parameterString = "--crossover OXD --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      // Assert
      assertTrue(crossoverParameter.getCrossover() instanceof OXDCrossover);
    }

    @DisplayName("returns a CX crossover operator when the operator is CX")
    @Test
    void shouldReturnCXCrossoverOperatorWhenOperatorIsCX() {
      // Arrange
      String parameterString = "--crossover CX --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      // Assert
      assertTrue(crossoverParameter.getCrossover() instanceof CycleCrossover);
    }

    @DisplayName("returns a PMX crossover operator when the operator is PMX")
    @Test
    void shouldReturnPMXCrossoverOperatorWhenOperatorIsPMX() {
      // Arrange
      String parameterString = "--crossover PMX --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      // Assert
      assertTrue(crossoverParameter.getCrossover() instanceof PMXCrossover);
    }

    @DisplayName("returns a positionBased crossover operator when the operator is positionBased")
    @Test
    void shouldReturnPositionBasedCrossoverOperatorWhenOperatorIsPositionBased() {
      // Arrange
      String parameterString = "--crossover positionBased --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      // Assert
      assertTrue(crossoverParameter.getCrossover() instanceof PositionBasedCrossover);
    }

    @DisplayName(
        "returns a edgeRecombination crossover operator when the operator is edgeRecombination")
    @Test
    void shouldReturnEdgeRecombinationCrossoverOperatorWhenOperatorIsEdgeRecombination() {
      // Arrange
      String parameterString = "--crossover edgeRecombination --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      // Assert
      assertTrue(crossoverParameter.getCrossover() instanceof EdgeRecombinationCrossover);
    }

    @DisplayName("throws an exception when an invalid crossover operator is provided")
    @Test
    void shouldThrowExceptionWhenInvalidCrossoverOperatorIsProvided() {
      // Arrange
      String parameterString = "--crossover INVALID_OPERATOR --crossoverProbability 0.8 ";

      // Act & Assert
      assertThrows(
          JMetalException.class, () -> crossoverParameter.parse(parameterString.split(" ")));
    }
  }
}
