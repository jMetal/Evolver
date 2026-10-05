package org.uma.evolver.cli;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @Test
    @DisplayName(
        "given every curated name, when resolved with no args, then none of them fail (each has"
            + " a public no-arg constructor)")
    void givenEveryCuratedName_whenResolvedWithNoArgs_thenNoneFail() {
      // Arrange
      var names = ProblemRegistry.registeredNames();
      assertTrue(names.size() >= 90, "expected the full benchmark catalogue, found: " + names.size());

      // Act & Assert
      for (String name : names) {
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
            () -> ProblemRegistry.resolve(new ProblemSpec(name)),
            "registered but not resolvable with no args: " + name);
      }
    }
  }

  @Nested
  @DisplayName("When describing the registered problems: ")
  class DescriptorTestCases {

    private ProblemDescriptor descriptor(String name) {
      return ProblemRegistry.registeredProblems().stream()
          .filter(descriptor -> descriptor.name().equals(name))
          .findFirst()
          .orElseThrow();
    }

    @Test
    @DisplayName(
        "given the registry, when described, then there is one descriptor per registered name,"
            + " sorted")
    void givenTheRegistry_whenDescribed_thenThereIsOneDescriptorPerName() {
      // Act
      List<String> described =
          ProblemRegistry.registeredProblems().stream().map(ProblemDescriptor::name).toList();

      // Assert
      assertEquals(ProblemRegistry.registeredNames().stream().sorted().toList(), described);
    }

    @ParameterizedTest(name = "{0} is {1}-encoded")
    @CsvSource({
      "ZDT1, Double",
      "DTLZ2, Double",
      "RE31, Double",
      "ZDT5, Binary",
      "OneZeroMax, Binary",
      "KroAB100TSP, Permutation",
      "EuclidAB300, Permutation"
    })
    @DisplayName("given a problem, when described, then its encoding is the one of its solutions")
    void givenAProblem_whenDescribed_thenItsEncodingIsRight(String name, String encoding) {
      assertEquals(encoding, descriptor(name).encoding());
    }

    @Test
    @DisplayName("given every descriptor, when checked, then it has a known encoding and a family")
    void givenEveryDescriptor_whenChecked_thenItHasAnEncodingAndAFamily() {
      for (ProblemDescriptor descriptor : ProblemRegistry.registeredProblems()) {
        assertTrue(
            List.of("Double", "Binary", "Permutation").contains(descriptor.encoding()),
            descriptor.name());
        assertFalse(descriptor.family().isBlank(), descriptor.name());
      }
    }

    @Test
    @DisplayName(
        "given every descriptor with dimensions, when the problem is built, then they are its"
            + " dimensions")
    void givenEveryDescriptor_whenTheProblemIsBuilt_thenTheDimensionsAgree() {
      for (ProblemDescriptor descriptor : ProblemRegistry.registeredProblems()) {
        if (descriptor.numberOfObjectives() == null) {
          continue;
        }
        Problem<?> problem = ProblemRegistry.resolve(new ProblemSpec(descriptor.name()));
        assertEquals(
            descriptor.numberOfObjectives(), problem.numberOfObjectives(), descriptor.name());
        assertEquals(descriptor.numberOfVariables(), problem.numberOfVariables(), descriptor.name());
      }
    }

    @Test
    @DisplayName(
        "given the arguments of every problem, when built with their defaults, then the problem"
            + " has the dimensions of the one built with no arguments")
    void givenTheDefaults_whenTheProblemIsBuilt_thenItIsTheSameProblem() {
      int checked = 0;
      List<String> failures = new ArrayList<>();
      for (ProblemDescriptor descriptor : ProblemRegistry.registeredProblems()) {
        List<Object> defaults =
            descriptor.arguments().stream().map(ProblemDescriptor.Argument::defaultValue).toList();
        if (defaults.isEmpty() || defaults.contains(null)) {
          continue;
        }
        try {
          Problem<?> problem =
              ProblemRegistry.resolve(new ProblemSpec(descriptor.name(), defaults));
          if (problem.numberOfObjectives() != descriptor.numberOfObjectives()
              || problem.numberOfVariables() != descriptor.numberOfVariables()) {
            failures.add(descriptor.name() + ": other dimensions");
          }
        } catch (JMetalException e) {
          failures.add(descriptor.name() + ": " + e.getMessage());
        }
        checked++;
      }
      assertTrue(failures.isEmpty(), failures.toString());
      assertTrue(checked > 50, "only " + checked + " problems checked");
    }

    @Test
    @DisplayName(
        "given the three arguments of LZ09F, which have no known default, when given, then the"
            + " problem is built")
    void givenTheArgumentsOfLz09f_whenGiven_thenTheProblemIsBuilt() {
      // Arrange
      ProblemDescriptor descriptor = descriptor("LZ09F1");

      // Act
      Problem<?> problem = ProblemRegistry.resolve(new ProblemSpec("LZ09F1", List.of(21, 1, 21)));

      // Assert
      assertEquals(List.of("ptype", "dtype", "ltype"), names(descriptor));
      assertEquals(2, problem.numberOfObjectives());
    }

    @Test
    @DisplayName("given DTLZ2, when described, then its arguments are the variables and objectives")
    void givenDtlz2_whenDescribed_thenItsArgumentsAreVariablesAndObjectives() {
      // Act
      ProblemDescriptor descriptor = descriptor("DTLZ2");

      // Assert
      assertEquals(List.of("numberOfVariables", "numberOfObjectives"), names(descriptor));
      assertEquals(
          List.of(12, 3),
          descriptor.arguments().stream().map(ProblemDescriptor.Argument::defaultValue).toList());
    }

    @Test
    @DisplayName("given a problem with no arguments, when described, then it lists none")
    void givenAProblemWithNoArguments_whenDescribed_thenItListsNone() {
      assertTrue(descriptor("RE31").arguments().isEmpty());
    }

    private List<String> names(ProblemDescriptor descriptor) {
      return descriptor.arguments().stream().map(ProblemDescriptor.Argument::name).toList();
    }
  }

  @Nested
  @DisplayName("When checking the encoding of a problem against the algorithm's: ")
  class EncodingCheckTestCases {

    @Test
    @DisplayName("given a problem of the algorithm's encoding, when checked, then it passes")
    void givenTheSameEncoding_whenChecked_thenItPasses() {
      assertDoesNotThrow(() -> ProblemRegistry.checkEncoding(new ZDT2(), "Double"));
    }

    @Test
    @DisplayName(
        "given a Double problem and the Permutation encoding, when checked, then it fails naming"
            + " both encodings and the problem, and suggesting another")
    void givenAMismatch_whenChecked_thenItSaysWhatToChange() {
      // Act
      JMetalException exception =
          assertThrows(
              JMetalException.class, () -> ProblemRegistry.checkEncoding(new ZDT2(), "Permutation"));

      // Assert
      String message = exception.getMessage();
      assertTrue(message.contains("ZDT2"), message);
      assertTrue(message.contains("Double-encoded"), message);
      assertTrue(message.contains("Permutation encoding"), message);
      assertTrue(message.contains("for instance"), message);
    }
  }
}
