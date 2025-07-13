package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.util.List;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.*;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Parameter class representing the configuration and factory for crossover operators for {@link
 * DoubleSolution} in evolutionary algorithms.
 *
 * <p>This class allows the selection and configuration of a specific crossover operator (such as
 * SBX, BLX-Alpha, or Whole Arithmetic) for real-coded solutions. It retrieves the required
 * parameters (e.g., probability, distribution index, alpha value, repair strategy) from its global
 * and specific sub-parameters and produces a configured {@link CrossoverOperator} instance
 * accordingly.
 *
 * <p>Supported crossover operators:
 *
 * <ul>
 *   <li><b>SBX</b> (Simulated Binary Crossover): requires a distribution index.
 *   <li><b>BLX_ALPHA</b> (BLX-Alpha Crossover): requires an alpha value.
 *   <li><b>wholeArithmetic</b> (Whole Arithmetic Crossover): no additional parameters.
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>
 * DoubleCrossoverParameter crossoverParameter = new DoubleCrossoverParameter(List.of("SBX", "BLX_ALPHA", "wholeArithmetic"));
 * // Configure sub-parameters as needed...
 * CrossoverOperator&lt;DoubleSolution&gt; operator = crossoverParameter.getCrossover();
 * </pre>
 */
public class DoubleCrossoverParameter extends CrossoverParameter<DoubleSolution> {

  private static List<String> validCrossoverNames = List.of("SBX", "blxAlpha", "wholeArithmetic", "arithmetic", "fuzzyRecombination", "laplace", "UNDC", "blxAlphaBeta", "PCX");

  /**
   * Constructs a crossover parameter for double solutions with the given list of supported
   * crossover operator names.
   *
   * @param crossoverOperators the list of supported crossover operator names
   */
  public DoubleCrossoverParameter(List<String> crossoverOperators) {
    super(crossoverOperators);

    crossoverOperators.stream()
        .filter(crossoverOperator -> !validCrossoverNames.contains(crossoverOperator))
        .forEach(
            crossoverOperator -> {
              throw new JMetalException(
                  "Invalid crossover operator name: "
                      + crossoverOperator
                      + ". Supported names are: "
                      + validCrossoverNames);
            });
  }

  /**
   * Returns a configured {@link CrossoverOperator} instance for {@link DoubleSolution} based on the
   * selected crossover type and its associated parameters.
   *
   * @return the configured crossover operator
   * @throws JMetalException if the selected crossover operator does not exist
   */
  @Override
  public CrossoverOperator<DoubleSolution> getCrossover() {
    Double crossoverProbability = (Double) findGlobalSubParameter("crossoverProbability").value();
    RepairDoubleSolutionStrategyParameter repairDoubleSolution =
        (RepairDoubleSolutionStrategyParameter) findGlobalSubParameter("crossoverRepairStrategy");

    CrossoverOperator<DoubleSolution> result;
    switch (value()) {
      case "SBX" -> {
        Double distributionIndex =
            (Double) findConditionalParameter("sbxDistributionIndex").value();
        result =
            new SBXCrossover(
                crossoverProbability,
                distributionIndex,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "blxAlpha" -> {
        Double alpha = (Double) findConditionalParameter("blxAlphaCrossoverAlpha").value();
        result =
            new BLXAlphaCrossover(
                crossoverProbability,
                alpha,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      
      case "blxAlphaBeta" -> {
        Double alpha = (Double) findConditionalParameter("blxAlphaBetaCrossoverAlpha").value();
        Double beta = (Double) findConditionalParameter("blxAlphaBetaCrossoverBeta").value();
        result =
            new BLXAlphaBetaCrossover(
                crossoverProbability,
                alpha,
                beta);
      } 
      case "wholeArithmetic" ->
          result =
              new WholeArithmeticCrossover(
                  crossoverProbability, repairDoubleSolution.getRepairDoubleSolutionStrategy());
      case "arithmetic" -> {
        result =
            new ArithmeticCrossover(
                crossoverProbability,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "fuzzyRecombination" -> {
        Double alpha = (Double) findConditionalParameter("fuzzyRecombinationCrossoverAlpha").value();
        result =
            new FuzzyRecombinationCrossover(
                crossoverProbability,
                alpha,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "laplace" -> {
        Double scale = (Double) findConditionalParameter("laplaceCrossoverScale").value();
        result =
            new LaplaceCrossover(
                crossoverProbability,
                scale,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "PCX" -> {
        Double zeta = (Double) findConditionalParameter("pcxCrossoverZeta").value();
        Double eta = (Double) findConditionalParameter("pcxCrossoverEta").value();

        result =
            new ParentCentricCrossover(
                crossoverProbability,
                zeta,
                eta,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      case "UNDC" -> {
        Double zeta = (Double) findConditionalParameter("undcCrossoverZeta").value();
        Double eta = (Double) findConditionalParameter("undcCrossoverEta").value();

        result =
            new UnimodalNormalDistributionCrossover(
                crossoverProbability,
                zeta,
                eta,
                repairDoubleSolution.getRepairDoubleSolutionStrategy());
      }
      default -> throw new JMetalException("Crossover operator does not exist: " + value());
    }
    return result;
  }
}
