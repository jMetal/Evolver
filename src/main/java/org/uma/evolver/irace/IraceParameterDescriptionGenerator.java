package org.uma.evolver.irace;

import java.util.List;
import org.uma.evolver.parameter.ConditionalParameter;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.factory.BinaryParameterFactory;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.factory.MOPSOParameterFactory;
import org.uma.evolver.parameter.factory.ParameterFactory;
import org.uma.evolver.parameter.factory.PermutationParameterFactory;
import org.uma.evolver.parameter.type.*;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A generator for creating parameter description files in the irace configuration format.
 * 
 * <p>This class is responsible for converting a {@link ParameterSpace} into a string
 * representation that follows the irace parameter configuration format. It handles different
 * types of parameters and their relationships (global and specific sub-parameters).
 *
 * <p>Example usage:
 * <pre>{@code
 * var generator = new IraceParameterDescriptionGenerator();
 * var parameterSpace = new YAMLParameterSpace("NSGAIIDouble.yaml", new DoubleParameterFactory());
 * String description = generator.description(parameterSpace);
 * }</pre>
 *
 * <p>From the command line, {@link #main} takes two arguments: the YAML file of the parameter space
 * and the parameter factory that reads it, which depends on the encoding of the algorithm ({@code
 * Double}, {@code Binary}, {@code Permutation}, or {@code MOPSO} for the parameter spaces of MOPSO).
 * The YAML file is looked up in the classpath first (the bundled {@code parameterSpaces/}) and then
 * in the filesystem, so a parameter space of your own works too:
 * <pre>{@code
 * java -cp Evolver-*-jar-with-dependencies.jar \
 *     org.uma.evolver.irace.IraceParameterDescriptionGenerator \
 *     NSGAIIDouble.yaml Double > parameters-NSGAII.txt
 * }</pre>
 *
 * @see <a href="https://cran.r-project.org/package=irace">irace package</a>
 * @author Antonio J. Nebro
 */
public class IraceParameterDescriptionGenerator {

  /** Format string for parameter output alignment */
  private static final String FORMAT_STRING = "%-40s %-40s %-7s %-30s %-20s\n";

  /** The parameter factories accepted by {@link #main}. */
  static final List<String> FACTORY_NAMES = List.of("Double", "Binary", "Permutation", "MOPSO");

  private static final String USAGE =
      "Usage: IraceParameterDescriptionGenerator <parameterSpace.yaml> <"
          + String.join("|", FACTORY_NAMES)
          + ">";

  /**
   * Prints the irace parameter file of a parameter space to standard output.
   *
   * @param args the YAML file of the parameter space and the name of its parameter factory (one of
   *     {@link #FACTORY_NAMES})
   */
  public static void main(String[] args) {
    if (args.length != 2) {
      System.err.println(USAGE);
      System.exit(1);
    }
    System.out.println(description(args[0], args[1]));
  }

  /**
   * Returns the irace parameter description of the parameter space in a YAML file.
   *
   * @param parameterSpaceFile the YAML file of the parameter space
   * @param factoryName one of {@link #FACTORY_NAMES}
   * @return the parameter description, in irace's format
   * @throws JMetalException if the factory name is unknown
   */
  static String description(String parameterSpaceFile, String factoryName) {
    var parameterSpace = new YAMLParameterSpace(parameterSpaceFile, parameterFactory(factoryName));
    return new IraceParameterDescriptionGenerator().description(parameterSpace);
  }

  static ParameterFactory<?> parameterFactory(String factoryName) {
    return switch (factoryName) {
      case "Double" -> new DoubleParameterFactory();
      case "Binary" -> new BinaryParameterFactory();
      case "Permutation" -> new PermutationParameterFactory();
      case "MOPSO" -> new MOPSOParameterFactory();
      default -> throw new JMetalException(
          "Unknown parameter factory: " + factoryName + ". " + USAGE);
    };
  }

  /**
   * Generates and prints the irace configuration for the given parameter space.
   *
   * @param parameterSpace The parameter space to generate configuration for
   * @throws NullPointerException if parameterSpace is null
   */
  public void generateConfigurationFile(ParameterSpace parameterSpace) {
    System.out.println(description(parameterSpace));
  }

  /**
   * Returns the irace configuration for the given parameter space.
   *
   * @param parameterSpace The parameter space to generate configuration for
   * @return the parameter description, in irace's format
   * @throws NullPointerException if parameterSpace is null
   */
  public String description(ParameterSpace parameterSpace) {
    List<Parameter<?>> parameterList = parameterSpace.topLevelParameters();

    StringBuilder stringBuilder = new StringBuilder();

    for (Parameter<?> parameter : parameterList) {
      decodeParameter(parameter, stringBuilder);
      stringBuilder.append("#\n");
    }

    return stringBuilder.toString();
  }

  /**
   * Decodes a single parameter and appends its irace configuration to the string builder.
   *
   * @param parameter The parameter to decode
   * @param stringBuilder The string builder to append the configuration to
   * @throws NullPointerException if either parameter or stringBuilder is null
   */
  private void decodeParameter(Parameter<?> parameter, StringBuilder stringBuilder) {
    stringBuilder.append(
        String.format(
            FORMAT_STRING,
            parameter.name(),
            "\"" + "--" + parameter.name() + " \"",
            decodeType(parameter),
            decodeValidValues(parameter),
            ""));

    for (Parameter<?> globalParameter : parameter.globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, parameter);
    }

    for (ConditionalParameter<?> specificParameter : parameter.conditionalParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, parameter);
    }
  }

  /**
   * Decodes a global sub-parameter and appends its irace configuration to the string builder.
   *
   * <p>This method handles the decoding of global sub-parameters, including their dependencies on
   * parent parameters.
   *
   * @param parameter The global sub-parameter to decode
   * @param stringBuilder The string builder to append the configuration to
   * @param parentParameter The parent parameter of this sub-parameter
   * @throws NullPointerException if any parameter is null
   */
  private void decodeGlobalParameter(Parameter<?> parameter, StringBuilder stringBuilder,
                                   Parameter<?> parentParameter) {
    StringBuilder dependenceString = new StringBuilder("\"" + parameter.name() + "\"");
    if (parentParameter instanceof CategoricalParameter) {
      var validValues = ((CategoricalParameter) parentParameter).validValues();
      dependenceString = new StringBuilder();
      for (String value : validValues) {
        dependenceString.append("\"").append(value).append("\"").append(",");
      }
      dependenceString = new StringBuilder(
          dependenceString.substring(0, dependenceString.length() - 1));
    }

    stringBuilder.append(
        String.format(
            FORMAT_STRING, parameter.name(),
            "\"" + "--" + parameter.name() + " \"",
            decodeType(parameter),
            decodeValidValues(parameter),
            "| " + parentParameter.name() + " %in% c(" + dependenceString + ")"));

    for (Parameter<?> globalParameter : parameter.globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, parameter);
    }

    for (ConditionalParameter<?> specificParameter : parameter.conditionalParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, parameter);
    }
  }


  /**
   * Decodes a specific sub-parameter and appends its irace configuration to the string builder.
   *
   * @param subParameter The specific sub-parameter to decode
   * @param stringBuilder The string builder to append the configuration to
   * @param parentParameter The parent parameter of this sub-parameter
   * @throws NullPointerException if any parameter is null
   */
  private void decodeSpecificParameter(
          ConditionalParameter<?> subParameter, StringBuilder stringBuilder, Parameter<?> parentParameter) {
    stringBuilder.append(
        String.format(
            FORMAT_STRING,
            subParameter.parameter().name(),
            "\"" + "--" + subParameter.parameter().name() + " \"",
            decodeType(subParameter.parameter()),
            decodeValidValues(subParameter.parameter()),
            "| " + parentParameter.name() + " %in% c(\"" + subParameter.description() + "\")"));

    for (Parameter<?> globalParameter : subParameter.parameter().
            globalSubParameters()) {
      decodeGlobalParameter(globalParameter, stringBuilder, subParameter.parameter());
    }

    for (ConditionalParameter<?> specificParameter : subParameter.parameter().conditionalParameters()) {
      decodeSpecificParameter(specificParameter, stringBuilder, subParameter.parameter());
    }
  }

  /**
   * Determines the irace parameter type for a given parameter.
   *
   * @param parameter The parameter to get the type for
   * @return A string representing the irace parameter type
   * @throws JMetalException if the parameter type is not supported
   * @throws NullPointerException if parameter is null
   */
  private String decodeType(Parameter<?> parameter) {
    String result = " ";
    if (parameter instanceof CategoricalParameter) {
      result = "c";
    } else if (parameter instanceof CategoricalIntegerParameter) {
      result = "c";
    } else if (parameter instanceof BooleanParameter) {
      result = "c";
    } else if (parameter instanceof IntegerParameter) {
      result = "i";
    } else if (parameter instanceof DoubleParameter) {
      result = "r";
    } else if (parameter != null) {
      result = "o";
    }

    return result;
  }

  /**
   * Generates the valid values string for a parameter in irace format.
   *
   * @param parameter The parameter to get valid values for
   * @return A string representing the valid values in irace format
   * @throws NullPointerException if parameter is null
   */
  private String decodeValidValues(Parameter<?> parameter) {
    String result = " ";

    if (parameter instanceof CategoricalParameter) {
      result = ((CategoricalParameter) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof CategoricalIntegerParameter) {
      result = ((CategoricalIntegerParameter) parameter).validValues().toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof BooleanParameter) {
      result = List.of(true, false).toString();
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof IntegerParameter) {
      result = "[" +((IntegerParameter) parameter).minValue() + " , "
          + ((IntegerParameter) parameter).maxValue() + "]";
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter instanceof DoubleParameter) {
      result = "[" +((DoubleParameter) parameter).minValue() + " , "
          + ((DoubleParameter) parameter).maxValue() + "]";
      result = result.replace("[", "(");
      result = result.replace("]", ")");
    } else if (parameter != null) {
      result = "(" + parameter.value() + ")";
    }

    return result;
  }
}
