package org.uma.evolver.example.tutorial;

import java.util.List;
import java.util.stream.Collectors;
import org.uma.evolver.parameter.ConditionalParameter;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterManagement;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.type.CategoricalIntegerParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.evolver.parameter.type.DoubleParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;

/**
 * Code of tutorial E1, "Parameter spaces" (see {@code docs/tutorials/parameter_spaces.rst}).
 *
 * <p>It loads the parameter spaces of NSGA-II for continuous ({@code NSGAIIDouble.yaml}) and binary
 * ({@code NSGAIIBinary.yaml}) problems, shows their structure, compares them, and turns a
 * configuration string into a concrete point of the space. No algorithm is run: parameter spaces
 * are the subject of this tutorial, and running algorithms is the subject of the next one.
 *
 * <p>The comments {@code // [step-N-start]}/{@code // [step-N-end]} delimit the fragments that the
 * tutorial page includes; keep them when editing this class.
 */
public class ParameterSpacesTutorial {

  private ParameterSpacesTutorial() {}

  public static void main(String[] args) {
    // [step-1-start]
    ParameterSpace doubleSpace =
        new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());

    for (Parameter<?> parameter : doubleSpace.topLevelParameters()) {
      System.out.println(parameter.name());
    }
    // [step-1-end]

    System.out.println();

    // [step-2-start]
    System.out.println(describe(doubleSpace));

    int topLevel = doubleSpace.topLevelParameters().size();
    int total = numberOfParameters(doubleSpace);
    System.out.println(total + " parameters, " + topLevel + " of them top-level");
    // [step-2-end]

    System.out.println();

    // [step-3-start]
    ParameterSpace binarySpace =
        new YAMLParameterSpace("NSGAIIBinary.yaml", new BinaryParameterFactory());

    for (ParameterSpace space : List.of(doubleSpace, binarySpace)) {
      var crossover = (CategoricalParameter) space.get("crossover");
      System.out.println(
          crossover.getClass().getSimpleName() + " with values " + crossover.validValues());
    }
    // [step-3-end]

    System.out.println();

    // [step-4-start]
    String[] configuration =
        ("--algorithmResult population "
                + "--createInitialSolutions default "
                + "--offspringPopulationSize 100 "
                + "--variation crossoverAndMutationVariation "
                + "--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy bounds "
                + "--sbxDistributionIndex 20.0 "
                + "--mutation polynomial "
                + "--mutationProbabilityFactor 1.0 "
                + "--mutationRepairStrategy bounds "
                + "--polynomialMutationDistributionIndex 20.0 "
                + "--selection tournament "
                + "--selectionTournamentSize 2")
            .split(" ");

    for (Parameter<?> parameter : doubleSpace.topLevelParameters()) {
      parameter.parse(configuration);
    }

    System.out.println(describeActive(doubleSpace));
    // [step-4-end]

    System.out.println();

    // [step-5-start]
    String[] missingSubParameter =
        String.join(" ", configuration).replace(" --sbxDistributionIndex 20.0", "").split(" ");
    String[] invalidValue = String.join(" ", configuration).replace("SBX", "HUX").split(" ");

    for (String[] wrongConfiguration : List.of(missingSubParameter, invalidValue)) {
      ParameterSpace space = doubleSpace.createInstance();
      try {
        for (Parameter<?> parameter : space.topLevelParameters()) {
          parameter.parse(wrongConfiguration);
        }
      } catch (RuntimeException exception) {
        System.out.println("Rejected: " + exception.getMessage());
      }
    }
    // [step-5-end]
  }

  /**
   * Returns the structure of a parameter space as an indented tree: one line per parameter, with
   * its type and its values or range. A global sub-parameter is marked with {@code *}; a
   * conditional parameter is prefixed with the value of its parent that activates it, as in
   * {@code [SBX]}.
   */
  public static String describe(ParameterSpace parameterSpace) {
    StringBuilder tree = new StringBuilder();
    for (Parameter<?> parameter : parameterSpace.topLevelParameters()) {
      describe(parameter, "", "", tree);
    }
    return tree.toString().stripTrailing();
  }

  private static void describe(
      Parameter<?> parameter, String indentation, String prefix, StringBuilder tree) {
    tree.append(indentation)
        .append(prefix)
        .append(parameter.name())
        .append(" (")
        .append(domainOf(parameter))
        .append(")\n");
    for (Parameter<?> globalSubParameter : parameter.globalSubParameters()) {
      describe(globalSubParameter, indentation + "  ", "* ", tree);
    }
    for (ConditionalParameter<?> conditional : parameter.conditionalParameters()) {
      describe(
          conditional.parameter(), indentation + "  ", "[" + conditional.description() + "] ", tree);
    }
  }

  /**
   * Returns the parameters that are active in the current configuration of a parameter space,
   * after it has been parsed, as an indented list of {@code name = value} lines.
   */
  public static String describeActive(ParameterSpace parameterSpace) {
    StringBuilder list = new StringBuilder();
    for (Parameter<?> parameter : parameterSpace.topLevelParameters()) {
      describeActive(parameter, "", list);
    }
    return list.toString().stripTrailing();
  }

  private static void describeActive(Parameter<?> parameter, String indentation, StringBuilder list) {
    list.append(indentation).append(parameter.name()).append(" = ").append(parameter.value());
    list.append("\n");
    for (Parameter<?> globalSubParameter : parameter.globalSubParameters()) {
      describeActive(globalSubParameter, indentation + "  ", list);
    }
    for (ConditionalParameter<?> conditional : parameter.conditionalParameters()) {
      if (conditional.description().equals(String.valueOf(parameter.value()))) {
        describeActive(conditional.parameter(), indentation + "  ", list);
      }
    }
  }

  /**
   * Returns the total number of parameters of a parameter space, counting every sub-parameter —
   * the number of variables a flat encoding of the space has.
   */
  public static int numberOfParameters(ParameterSpace parameterSpace) {
    return ParameterManagement.parameterFlattening(parameterSpace.topLevelParameters()).size();
  }

  private static String domainOf(Parameter<?> parameter) {
    return switch (parameter) {
      case CategoricalIntegerParameter categorical ->
          "categorical: "
              + categorical.validValues().stream()
                  .map(String::valueOf)
                  .collect(Collectors.joining(", "));
      case CategoricalParameter categorical ->
          "categorical: " + String.join(", ", categorical.validValues());
      case IntegerParameter integer ->
          "integer: [" + integer.minValue() + ", " + integer.maxValue() + "]";
      case DoubleParameter real -> "double: [" + real.minValue() + ", " + real.maxValue() + "]";
      default -> parameter.getClass().getSimpleName();
    };
  }
}
