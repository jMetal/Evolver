package org.uma.evolver.parameter.catalogue.crossoverparameter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.operator.crossover.impl.HUXCrossover;
import org.uma.jmetal.operator.crossover.impl.SinglePointCrossover;
import org.uma.jmetal.operator.crossover.impl.UniformCrossover;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

class BinaryCrossoverParameterTest {
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
      List<String> validOperators = List.of("singlePoint", "HUX", "uniform");

      // Act & Assert
      assertDoesNotThrow(() -> new BinaryCrossoverParameter(validOperators));
    }

    @DisplayName(
        "does not throw an exception when the a sublist of a valid crossover operators is provided")
    @Test
    void shouldNotThrowExceptionWhenSublistOfValidCrossoverOperatorsAreProvided() {
      // Arrange
      List<String> validOperators = List.of("singlePoint", "HUX");

      // Act & Assert
      assertDoesNotThrow(() -> new BinaryCrossoverParameter(validOperators));
    }

    @DisplayName("throws an exception when an empty list of crossover operators is provided")
    @Test
    void shouldThrowExceptionWhenEmptyListOfCrossoverOperatorsIsProvided() {
      // Arrange
      List<String> emptyList = List.of();

      // Act & Assert
      assertThrows(InvalidConditionException.class, () -> new BinaryCrossoverParameter(emptyList));
    }
  }

  @Nested
  @DisplayName("The getCrossover method")
  class GetCrossoverTestCases {
    private BinaryCrossoverParameter crossoverParameter;

    @BeforeEach
    void setUp() {
      crossoverParameter = new BinaryCrossoverParameter(List.of("singlePoint", "HUX", "uniform"));
      crossoverParameter.addGlobalSubParameter(new DoubleParameter("crossoverProbability", 0, 1.0));
    }

    @DisplayName("returns a HUX crossover operator when the operator is HUX")
    @Test
    void shouldReturnHUXCrossoverOperatorWhenOperatorIsHUX() {
      // Arrange
      String parameterString = "--crossover HUX --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      var crossoverOperator = crossoverParameter.getCrossover();

      // Assert
      assertTrue(crossoverOperator instanceof HUXCrossover);
    }

    @DisplayName("returns a single point crossover operator when the operator is singlePoint")
    @Test
    void shouldReturnSinglePointCrossoverOperatorWhenOperatorIsSinglePoint() {
      // Arrange
      String parameterString = "--crossover singlePoint --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      var crossoverOperator = crossoverParameter.getCrossover();

      // Assert
      assertTrue(
          crossoverOperator instanceof SinglePointCrossover);
    }

    @DisplayName("returns a uniform crossover operator when the operator is uniform")
    @Test
    void shouldReturnUniformCrossoverOperatorWhenOperatorIsUniform() {
      // Arrange
      String parameterString = "--crossover uniform --crossoverProbability 0.8 ";

      // Act
      crossoverParameter.parse(parameterString.split(" "));

      var crossoverOperator = crossoverParameter.getCrossover();

      // Assert
      assertTrue(
          crossoverOperator instanceof UniformCrossover);
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
