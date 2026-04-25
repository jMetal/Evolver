package org.uma.evolver.irace;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@DisplayName("Integration tests for AutoNSGAIIIraceHV and AutoNSGAIIIraceHVEP")
class AutoNSGAIIIraceIT {

  private static final String NSGAII_PARAMS =
      "--algorithmResult population "
          + "--createInitialSolutions default "
          + "--variation crossoverAndMutationVariation "
          + "--offspringPopulationSize 100 "
          + "--crossover SBX "
          + "--crossoverProbability 0.9 "
          + "--crossoverRepairStrategy bounds "
          + "--sbxDistributionIndex 20.0 "
          + "--mutation polynomial "
          + "--mutationProbabilityFactor 1.0 "
          + "--mutationRepairStrategy bounds "
          + "--polynomialMutationDistributionIndex 20.0 "
          + "--selection tournament "
          + "--selectionTournamentSize 2";

  private static final String ZDT1_ARGS =
      "--problemName org.uma.jmetal.problem.multiobjective.zdt.ZDT1 "
          + "--referenceFrontFileName ZDT1.csv "
          + "--populationSize 100 "
          + "--maximumNumberOfEvaluations 10000 "
          + NSGAII_PARAMS;

  private static final String ZDT2_ARGS =
      "--problemName org.uma.jmetal.problem.multiobjective.zdt.ZDT2 "
          + "--referenceFrontFileName ZDT2.csv "
          + "--populationSize 100 "
          + "--maximumNumberOfEvaluations 10000 "
          + NSGAII_PARAMS;

  private String captureMainOutput(RunnableWithException action) throws Exception {
    var out = new ByteArrayOutputStream();
    var original = System.out;
    System.setOut(new PrintStream(out));
    try {
      action.run();
    } finally {
      System.setOut(original);
    }
    return out.toString().trim();
  }

  @FunctionalInterface
  interface RunnableWithException {
    void run() throws Exception;
  }

  @Nested
  @DisplayName("AutoNSGAIIIraceHV")
  class HVTests {

    @Tag("integration")
    @Test
    @DisplayName("Should print a negative double when solving ZDT1")
    void shouldPrintANegativeDoubleWhenSolvingZDT1() throws Exception {
      String output = captureMainOutput(
          () -> AutoNSGAIIIraceHV.main(ZDT1_ARGS.split("\\s+")));
      double value = Double.parseDouble(output);
      assertTrue(value < 0, "HV output should be negative (negated for irace minimization), got: " + value);
    }

    @Tag("integration")
    @Test
    @DisplayName("Should print a value greater than -1.0 when solving ZDT1")
    void shouldPrintAValueGreaterThanMinusOneWhenSolvingZDT1() throws Exception {
      String output = captureMainOutput(
          () -> AutoNSGAIIIraceHV.main(ZDT1_ARGS.split("\\s+")));
      double value = Double.parseDouble(output);
      assertTrue(value > -1.0, "Normalized HV is in [0,1], so negated value should be in (-1,0), got: " + value);
    }

    @Tag("integration")
    @Test
    @DisplayName("Should print a negative double when solving ZDT2")
    void shouldPrintANegativeDoubleWhenSolvingZDT2() throws Exception {
      String output = captureMainOutput(
          () -> AutoNSGAIIIraceHV.main(ZDT2_ARGS.split("\\s+")));
      double value = Double.parseDouble(output);
      assertTrue(value < 0, "HV output should be negative (negated for irace minimization), got: " + value);
    }
  }

  @Nested
  @DisplayName("AutoNSGAIIIraceHVEP")
  class HVEPTests {

    @Tag("integration")
    @Test
    @DisplayName("Should print a parseable double when solving ZDT1")
    void shouldPrintAParsableDoubleWhenSolvingZDT1() {
      assertDoesNotThrow(() -> {
        String output = captureMainOutput(
            () -> AutoNSGAIIIraceHVEP.main(ZDT1_ARGS.split("\\s+")));
        Double.parseDouble(output);
      });
    }

    @Tag("integration")
    @Test
    @DisplayName("Should print a parseable double when solving ZDT2")
    void shouldPrintAParsableDoubleWhenSolvingZDT2() {
      assertDoesNotThrow(() -> {
        String output = captureMainOutput(
            () -> AutoNSGAIIIraceHVEP.main(ZDT2_ARGS.split("\\s+")));
        Double.parseDouble(output);
      });
    }
  }
}
