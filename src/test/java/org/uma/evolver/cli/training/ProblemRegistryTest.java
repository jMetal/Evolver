package org.uma.evolver.cli.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.wfg.WFG1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.util.errorchecking.JMetalException;

@DisplayName("Unit tests for class ProblemRegistry")
class ProblemRegistryTest {

  @Nested
  @DisplayName("When resolving a curated problem name: ")
  class CuratedTestCases {

    @Test
    @DisplayName("given ZDT1, when resolved with no args, then it returns a ZDT1 instance")
    void givenCuratedName_whenResolved_thenItReturnsTheRightType() {
      // Arrange & Act
      Problem<?> problem = ProblemRegistry.resolve(new ProblemSpec("ZDT1"));

      // Assert
      assertEquals("org.uma.jmetal.problem.multiobjective.zdt.ZDT1", problem.getClass().getName());
    }
  }

  @Nested
  @DisplayName("When resolving a fully-qualified class name not in the curated catalogue: ")
  class ReflectiveTestCases {

    @Test
    @DisplayName(
        "given ZDT2's fully-qualified name, when resolved with no args, then it returns a ZDT2"
            + " instance")
    void givenUncuratedFqn_whenResolved_thenItReturnsTheRightType() {
      // Arrange & Act
      Problem<?> problem = ProblemRegistry.resolve(new ProblemSpec(ZDT2.class.getName()));

      // Assert
      assertEquals(ZDT2.class, problem.getClass());
    }

    @Test
    @DisplayName("given an unresolvable name, when resolved, then it fails naming the class")
    void givenUnresolvableName_whenResolved_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class,
              () -> ProblemRegistry.resolve(new ProblemSpec("not.a.real.Class")));
      assertTrue(exception.getMessage().contains("not.a.real.Class"));
    }
  }

  @Nested
  @DisplayName("When resolving a problem with constructor arguments: ")
  class ParametrizedTestCases {

    @Test
    @DisplayName(
        "given DTLZ1's fully-qualified name with (variables, objectives) args, when resolved,"
            + " then it builds a DTLZ1 with that shape")
    void givenParametrizedFqn_whenResolvedWithArgs_thenItBuildsTheRightShape() {
      // Arrange & Act
      Problem<?> problem =
          ProblemRegistry.resolve(new ProblemSpec(DTLZ1.class.getName(), List.of(12, 3)));

      // Assert
      assertEquals(DTLZ1.class, problem.getClass());
      assertEquals(12, problem.numberOfVariables());
      assertEquals(3, problem.numberOfObjectives());
    }

    @Test
    @DisplayName(
        "given WFG1's fully-qualified name with (k, l, m) args, when resolved, then it builds a"
            + " WFG1 instance")
    void givenThreeArgParametrizedFqn_whenResolved_thenItBuildsTheRightType() {
      // Arrange & Act
      Problem<?> problem =
          ProblemRegistry.resolve(new ProblemSpec(WFG1.class.getName(), List.of(4, 20, 2)));

      // Assert
      assertEquals(WFG1.class, problem.getClass());
    }

    @Test
    @DisplayName(
        "given a curated name with an argument count no constructor accepts, when resolved, then"
            + " it fails naming the argument count")
    void givenWrongArity_whenResolved_thenItFails() {
      // Arrange & Act & Assert
      JMetalException exception =
          assertThrows(
              JMetalException.class,
              () -> ProblemRegistry.resolve(new ProblemSpec("ZDT1", List.of(1, 2, 3, 4, 5))));
      assertTrue(exception.getMessage().contains("5"));
    }
  }

  @Nested
  @DisplayName("When listing registered names: ")
  class RegisteredNamesTestCases {

    @Test
    @DisplayName("given the curated catalogue, when listed, then it contains the reference names")
    void givenCuratedCatalogue_whenListed_thenItContainsReferenceNames() {
      // Arrange & Act & Assert
      assertTrue(ProblemRegistry.registeredNames().contains("ZDT1"));
      assertTrue(ProblemRegistry.registeredNames().contains("DTLZ3"));
      assertTrue(ProblemRegistry.registeredNames().contains("RE31"));
    }
  }
}
