package org.uma.evolver.parameter.catalogue.mutationparameter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.LinkedPolynomialMutation;
import org.uma.jmetal.operator.mutation.impl.NonUniformMutation;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.mutation.impl.UniformMutation;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.errorchecking.exception.InvalidConditionException;

class DoubleMutationParameterTest {
  @Nested
  @DisplayName("The constructor")
  class ConstructorTestCases {

    @DisplayName("throws an exception when an invalid mutation operator is provided")
    @Test
    void shouldThrowExceptionWhenInvalidMutationOperatorIsProvided() {
      // Arrange
      List<String> invalidOperators = List.of("INVALID_OPERATOR");

      // Act & Assert
      assertThrows(JMetalException.class, () -> new DoubleMutationParameter(invalidOperators));
    }

    @DisplayName("throws an exception when an empty mutation operators list is provided")
    @Test
    void shouldThrowExceptionWhenEmptyListIsProvided() {
      assertThrows(InvalidConditionException.class, () -> new DoubleMutationParameter(List.of()));
    }

    @DisplayName(
        "does not throw an exception when the full list of a valid mutation operators is provided")
    @Test
    void shouldNotThrowExceptionWhenValidMutationOperatorsAreProvided() {
      // Arrange
      List<String> validOperators =
          List.of("polynomial", "linkedPolynomial", "uniform", "nonUniform");

      // Act & Assert
      assertDoesNotThrow(() -> new DoubleMutationParameter(validOperators));
    }

    @DisplayName(
        "does not throw an exception when the a sublist of a valid mutation operators is provided")
    @Test
    void shouldNotThrowExceptionWhenSublistOfValidMutationOperatorsAreProvided() {
      // Arrange
      List<String> validOperators = List.of("polynomial", "linkedPolynomial");

      // Act & Assert
      assertDoesNotThrow(() -> new DoubleMutationParameter(validOperators));
    }
  }

  @Nested
  @DisplayName("The getMutation method")
  class GetMutationTestCases {
    private DoubleMutationParameter mutationParameter;
    private int numberOfProblemVariables = 20;

    @BeforeEach
    void setUp() {
      mutationParameter = new DoubleMutationParameter(List.of("polynomial", "linkedPolynomial", "uniform", "nonUniform"));
      mutationParameter.addGlobalSubParameter(
          new DoubleParameter("mutationProbabilityFactor", 0.5, 2.0));
      mutationParameter.addGlobalSubParameter(
          new RepairDoubleSolutionStrategyParameter("mutationRepairStrategy", List.of("random")));
      mutationParameter.addNonConfigurableSubParameter(
          "numberOfProblemVariables", numberOfProblemVariables);
    }

    @DisplayName("returns a polynomial mutation operator when the operator is polynomial")
    @Test
    void shouldReturnMutationOperatorWhenValidOperatorIsProvided() {
      // Arrange
      double mutationProbabilityFactor = 0.8;
      double polynomialMutationDistributionIndex = 20.0;
      String parameterString =
          "--mutation polynomial "
              + "--mutationProbabilityFactor "
              + mutationProbabilityFactor
              + " --mutationRepairStrategy random --polynomialMutationDistributionIndex "
              + polynomialMutationDistributionIndex
              + " ";
      mutationParameter.addSpecificSubParameter(
          "polynomial", new DoubleParameter("polynomialMutationDistributionIndex", 1.0, 200.0));

      // Act
      mutationParameter.parse(parameterString.split(" "));
      MutationOperator<DoubleSolution> mutationOperator = mutationParameter.getMutation();

      // Assert
      double expectedMutationProbability =
          mutationProbabilityFactor * 1.0 / numberOfProblemVariables;

      assertInstanceOf(PolynomialMutation.class, mutationOperator);
      assertEquals(expectedMutationProbability, mutationOperator.mutationProbability());
      assertEquals(polynomialMutationDistributionIndex, ((PolynomialMutation) mutationOperator).getDistributionIndex());
    }

    @DisplayName("returns a linked polynomial mutation operator when the operator is linkedPolynomial")
    @Test 
    void shouldReturnLinkedPolynomialMutationOperatorWhenValidOperatorIsProvided() {
      // Arrange
      double mutationProbabilityFactor = 1.2;
      double linkedPolynomialMutationDistributionIndex = 30.0;
      String parameterString =
          "--mutation linkedPolynomial "
              + "--mutationProbabilityFactor "
              + mutationProbabilityFactor
              + " --mutationRepairStrategy random --linkedPolynomialMutationDistributionIndex "
              + linkedPolynomialMutationDistributionIndex
              + " ";
      mutationParameter.addSpecificSubParameter(
          "linkedPolynomial",
          new DoubleParameter("linkedPolynomialMutationDistributionIndex", 2.0, 200.0));

      // Act
      mutationParameter.parse(parameterString.split(" "));
      MutationOperator<DoubleSolution> mutationOperator = mutationParameter.getMutation();

      // Assert
      double expectedMutationProbability =
          mutationProbabilityFactor * 1.0 / numberOfProblemVariables;

      assertInstanceOf(LinkedPolynomialMutation.class, mutationOperator);
      assertEquals(expectedMutationProbability, mutationOperator.mutationProbability());
      assertEquals(linkedPolynomialMutationDistributionIndex, ((LinkedPolynomialMutation) mutationOperator).getDistributionIndex());
    }

    @DisplayName("returns a uniform mutation operator when the operator is uniform")
    @Test 
    void shouldReturnUniformMutationOperatorWhenValidOperatorIsProvided() {
      // Arrange
      double mutationProbabilityFactor = 1.5;
      double uniformMutationPerturbation = 0.6 ;

      String parameterString =
          "--mutation uniform "
              + "--mutationProbabilityFactor "
              + mutationProbabilityFactor
              + " --mutationRepairStrategy random " + "--uniformMutationPerturbation " + uniformMutationPerturbation ;

      mutationParameter.addSpecificSubParameter(
              "uniform",
              new DoubleParameter("uniformMutationPerturbation", 0.0, 1.0));

      // Act
      mutationParameter.parse(parameterString.split(" "));
      MutationOperator<DoubleSolution> mutationOperator = mutationParameter.getMutation();

      // Assert
      double expectedMutationProbability =
          mutationProbabilityFactor * 1.0 / numberOfProblemVariables;

      assertInstanceOf(UniformMutation.class, mutationOperator);
      assertEquals(expectedMutationProbability, mutationOperator.mutationProbability());
    }

    @DisplayName("returns a non-uniform mutation operator when the operator is nonUniform")
    @Test 
    void shouldReturnNonUniformMutationOperatorWhenValidOperatorIsProvided() {
      // Arrange
      double mutationProbabilityFactor = 1.5;
      double nonUniformMutationPerturbation = 0.6 ;
      String parameterString =
          "--mutation nonUniform "
              + "--mutationProbabilityFactor "
              + mutationProbabilityFactor
              + " --mutationRepairStrategy random " + "--nonUniformMutationPerturbation " + nonUniformMutationPerturbation ;

      mutationParameter.addSpecificSubParameter(
              "nonUniform",
              new DoubleParameter("nonUniformMutationPerturbation", 0.0, 1.0));

      mutationParameter.addNonConfigurableSubParameter("maxIterations", 250);


      // Act
      mutationParameter.parse(parameterString.split(" "));
      MutationOperator<DoubleSolution> mutationOperator = mutationParameter.getMutation();

      // Assert
      double expectedMutationProbability =
          mutationProbabilityFactor * 1.0 / numberOfProblemVariables;

      assertInstanceOf(NonUniformMutation.class, mutationOperator);
      assertEquals(expectedMutationProbability, mutationOperator.mutationProbability());
    }


    @DisplayName("throws an exception when the operator is not supported")
    @Test   
    void shouldThrowExceptionWhenOperatorIsNotSupported() {
      // Arrange
      String parameterString =
          "--mutation INVALID_OPERATOR "
              + "--mutationProbabilityFactor 0.8 --mutationRepairStrategy random";

      // Act & Assert
      assertThrows(JMetalException.class, () -> mutationParameter.parse(parameterString.split(" ")));
    }
  }
}
