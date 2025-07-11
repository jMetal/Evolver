package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.operator.crossover.impl.DifferentialEvolutionCrossover;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * A categorical parameter representing different Differential Evolution (DE) crossover variants.
 * This parameter allows selecting and configuring the DE crossover operator, which is a key
 * component of the Differential Evolution algorithm.
 *
 * <p>The available DE crossover variants are:
 * <ul>
 *   <li>RAND_1_BIN: Random/1/Binomial (classic DE/rand/1/bin)</li>
 *   <li>RAND_2_BIN: Random/2/Binomial (DE/rand/2/bin)</li>
 *   <li>RAND_1_EXP: Random/1/Exponential (DE/rand/1/exp)</li>
 *   <li>BEST_1_BIN: Best/1/Binomial (DE/best/1/bin)</li>
 *   <li>BEST_1_EXP: Best/1/Exponential (DE/best/1/exp)</li>
 *   <li>BEST_2_BIN: Best/2/Binomial (DE/best/2/bin)</li>
 *   <li>BEST_2_EXP: Best/2/Exponential (DE/best/2/exp)</li>
 *   <li>RAND_TO_BEST_1_BIN: Random-to-best/1/Binomial (DE/rand-to-best/1/bin)</li>
 *   <li>RAND_TO_BEST_1_EXP: Random-to-best/1/Exponential (DE/rand-to-best/1/exp)</li>
 *   <li>CURRENT_TO_RAND_1_BIN: Current-to-rand/1/Binomial (DE/current-to-rand/1/bin)</li>
 *   <li>CURRENT_TO_RAND_1_EXP: Current-to-rand/1/Exponential (DE/current-to-rand/1/exp)</li>
 * </ul>
 *
 * <p>This parameter requires the following global sub-parameters:
 * <ul>
 *   <li>CR: Crossover probability (Double between 0.0 and 1.0)</li>
 *   <li>F: Differential weight (Double typically between 0.0 and 2.0)</li>
 * </ul>
 */
public class DifferentialEvolutionCrossoverParameter extends CategoricalParameter {
  public static final String DEFAULT_NAME = "differentialEvolutionCrossover";
  
  /**
   * Creates a new DifferentialEvolutionCrossoverParameter with all standard DE crossover variants.
   * The following variants are included by default:
   * <ul>
   *   <li>RAND_1_BIN, RAND_2_BIN, RAND_1_EXP</li>
   *   <li>BEST_1_BIN, BEST_1_EXP, BEST_2_BIN, BEST_2_EXP</li>
   *   <li>RAND_TO_BEST_1_BIN, RAND_TO_BEST_1_EXP</li>
   *   <li>CURRENT_TO_RAND_1_BIN, CURRENT_TO_RAND_1_EXP</li>
   * </ul>
   */
  public DifferentialEvolutionCrossoverParameter() {
    this(List.of(
            "RAND_1_BIN",
            "RAND_2_BIN",
            "RAND_1_EXP",
            "BEST_1_BIN",
            "BEST_1_EXP",
            "BEST_2_BIN",
            "BEST_2_EXP",
            "RAND_TO_BEST_1_BIN",
            "RAND_TO_BEST_1_EXP",
            "CURRENT_TO_RAND_1_BIN",
            "CURRENT_TO_RAND_1_EXP"));
  }

  /**
   * Creates a new DifferentialEvolutionCrossoverParameter with the specified variants.
   *
   * @param variants A list of valid DE crossover variant names. Must be a subset of the standard
   *                variants (e.g., "RAND_1_BIN", "BEST_1_EXP", etc.)
   * @throws IllegalArgumentException if variants is null or empty
   */
  public DifferentialEvolutionCrossoverParameter(List<String> variants) {
    super(DEFAULT_NAME, variants);
  }

  /**
   * Creates and returns a DifferentialEvolutionCrossover instance based on the current parameter value.
   * The specific variant is determined by the current value of this parameter.
   *
   * <p>This method requires the following global sub-parameters to be set:
   * <ul>
   *   <li>CR: Crossover probability (Double between 0.0 and 1.0)</li>
   *   <li>F: Differential weight (Double typically between 0.0 and 2.0)</li>
   * </ul>
   *
   * @return A configured DifferentialEvolutionCrossover instance
   * @throws IllegalStateException if required sub-parameters are missing or have invalid values
   * @throws JMetalException if the current value does not match any known DE variant
   */
  public DifferentialEvolutionCrossover getParameter() {
    DifferentialEvolutionCrossover result;
    Double cr = (Double) findGlobalSubParameter("CR").value();
    Double f = (Double) findGlobalSubParameter("F").value();

    String variant = value() ;

    result =
        new DifferentialEvolutionCrossover(
            cr, f, DifferentialEvolutionCrossover.getVariantFromString(variant));

    return result;
  }

  @Override
  public String name() {
    return "differentialEvolutionCrossover";
  }
}
