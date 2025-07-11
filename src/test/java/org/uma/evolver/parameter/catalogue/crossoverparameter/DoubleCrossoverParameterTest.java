package org.uma.evolver.parameter.catalogue.crossoverparameter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.crossover.impl.WholeArithmeticCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

@DisplayName("Test cases for class DoubleCrossoverParameter")
class DoubleCrossoverParameterTest {

  @Nested
  @DisplayName("The constructor")
  class ConstructorTestCases {
    @DisplayName("throws an exception when an invalid crossover operator is provided")
    @Test
    void shouldThrowExceptionWhenInvalidCrossoverOperatorIsProvided() {
      // Arrange
      List<String> invalidOperators = List.of("INVALID_OPERATOR");

      // Act & Assert
      assertThrows(JMetalException.class, () -> new DoubleCrossoverParameter(invalidOperators));
    }

    @DisplayName("does not throw an exception when the full list of a valid crossover operators is provided")
    @Test
    void shouldNotThrowExceptionWhenValidCrossoverOperatorsAreProvided() {
      // Arrange
      List<String> validOperators = List.of("SBX", "blxAlpha", "wholeArithmetic");

      // Act & Assert
      assertDoesNotThrow(() -> new DoubleCrossoverParameter(validOperators));
    }
    
    @DisplayName("does not throw an exception when the a sublist of a valid crossover operators is provided")
    @Test
    void shouldNotThrowExceptionWhenSublistOfValidCrossoverOperatorsAreProvided() {
      // Arrange
      List<String> validOperators = List.of("SBX", "blxAlpha");

      // Act & Assert
      assertDoesNotThrow(() -> new DoubleCrossoverParameter(validOperators));
    }

    @DisplayName("throws an exception when an empty list of crossover operators is provided")
    @Test
    void shouldThrowExceptionWhenEmptyListOfCrossoverOperatorsIsProvided() {
      // Arrange
      List<String> emptyOperators = List.of();

      // Act & Assert
      assertThrows(InvalidConditionException.class, () -> new DoubleCrossoverParameter(emptyOperators));
    }
  }

  @Nested
  @DisplayName("The getCrossover method")
  class GetCrossoverTestCases {
    private DoubleCrossoverParameter crossoverParameter;

    @BeforeEach
    void setUp() {
      crossoverParameter = new DoubleCrossoverParameter(List.of("SBX", "blxAlpha", "wholeArithmetic"));
      crossoverParameter.addGlobalSubParameter(new DoubleParameter("crossoverProbability", 0, 1.0));
      crossoverParameter.addGlobalSubParameter(new RepairDoubleSolutionStrategyParameter("crossoverRepairStrategy", List.of("random"))) ;
    }

    @DisplayName("returns a SBXCrossover when the operator is SBX")
    @Test
    void shouldReturnSBXCrossoverWhenOperatorIsSBX() {
      // Arrange
      String parameterString = "--crossover SBX --crossoverProbability 0.8 --crossoverRepairStrategy random --sbxDistributionIndex 20.0" ;
      crossoverParameter.addConditionalSubParameter("SBX", new DoubleParameter("sbxDistributionIndex", 1.0, 200.0)) ;

      // Act  
      crossoverParameter.parse(parameterString.split(" "));
      CrossoverOperator<DoubleSolution> operator = crossoverParameter.getCrossover();

      // Assert
      assertInstanceOf(SBXCrossover.class, operator);
      assertEquals(0.8, ((SBXCrossover) operator).crossoverProbability());
      assertEquals(20.0, ((SBXCrossover) operator).distributionIndex());
    }

    @DisplayName("returns a BLXAlphaCrossover when the operator is blxAlpha")
    @Test
    void shouldReturnBLXAlphaCrossoverWhenOperatorIsBLXAlpha() {
      // Arrange
      String parameterString = "--crossover blxAlpha --crossoverProbability 0.8 --crossoverRepairStrategy random --blxAlphaCrossoverAlpha 0.5" ;
      crossoverParameter.addConditionalSubParameter("blxAlpha", new DoubleParameter("blxAlphaCrossoverAlpha", 0.0, 1.0)) ;

      // Act
      crossoverParameter.parse(parameterString.split(" "));
      CrossoverOperator<DoubleSolution> operator = crossoverParameter.getCrossover();

      // Assert
      assertInstanceOf(BLXAlphaCrossover.class, operator);
      assertEquals(0.8, operator.crossoverProbability());
      assertEquals(0.5, ((BLXAlphaCrossover) operator).alpha());
    }

    @DisplayName("returns a WholeArithmeticCrossover when the operator is wholeArithmetic")
    @Test
    void shouldReturnWholeArithmeticCrossoverWhenOperatorIsWholeArithmetic() {
      // Arrange
      String parameterString = "--crossover wholeArithmetic --crossoverProbability 0.8 --crossoverRepairStrategy random" ;

      // Act
      crossoverParameter.parse(parameterString.split(" "));
      CrossoverOperator<DoubleSolution> operator = crossoverParameter.getCrossover();

      // Assert
      assertInstanceOf(WholeArithmeticCrossover.class, operator);
      assertEquals(0.8, operator.crossoverProbability());
    }
    @DisplayName("throws an exception when the operator is not supported")
    @Test
    void shouldThrowExceptionWhenOperatorIsNotSupported() {
      // Arrange
      String parameterString = "--crossover INVALID_OPERATOR --crossoverProbability 0.8 --crossoverRepairStrategy random" ;

      // Act & Assert
      assertThrows(JMetalException.class, () -> crossoverParameter.parse(parameterString.split(" ")));
    }
  }
}
