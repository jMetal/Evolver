package org.uma.evolver.parameter.catalogue.selectionparameter;

import java.util.List;

import org.uma.evolver.parameter.catalogue.SequenceGeneratorParameter;
import org.uma.evolver.parameter.type.BooleanParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Specialized selection parameter for double solutions that extends the base CategoricalParameter.
 * This parameter manages selection strategies specifically designed for double solutions in
 * evolutionary algorithms.
 * <p>
 * The class supports differential evolution selection as a selection strategy and allows
 * configuration of specific parameters required for this selection method.
 * </p>
 * <p>
 * Required specific subparameters:
 * <ul>
 *   <li>sequenceGenerator: A SequenceGeneratorParameter that provides the sequence generator
 *       for differential evolution</li>
 *   <li>takeCurrentSolutionAsParent: A BooleanParameter that controls whether the current
 *       solution should be used as a parent in differential evolution</li>
 * </ul>
 * </p>
 *
 * @author Antonio J. Nebro
 */
public class DoubleSelectionParameter extends CategoricalParameter {

  public static final String DEFAULT_NAME = "selection";

  /**
   * Creates a new DoubleSelectionParameter with the specified selection strategies.
   *
   * @param selectionStrategies List of available selection strategy names
   * @throws IllegalArgumentException if selectionStrategies is null or empty
   */
  public DoubleSelectionParameter(List<String> selectionStrategies) {
    super(DEFAULT_NAME, selectionStrategies);
  }

  /**
   * Creates a new DoubleSelectionParameter with the default selection strategy.
   * <p>
   * This constructor initializes the parameter with differential evolution selection
   * as the only available strategy, which is suitable for most use cases involving
   * double solutions in differential evolution algorithms.
   * </p>
   *
   * @see #DoubleSelectionParameter(List) for creating a parameter with custom selection strategies
   */
  public DoubleSelectionParameter() {
    this(List.of("differentialEvolutionSelection"));
  }

  /**
   * Creates and returns a Selection component for double solutions based on the configured
   * selection strategy.
   *
   * @param matingPoolSize The size of the mating pool for selection
   * @return A Selection component configured with the appropriate selection strategy
   * @throws JMetalException if an unknown selection strategy is configured
   */
  public Selection<DoubleSolution> getSelection(int matingPoolSize) {
    Selection<DoubleSolution> result;
    switch (value()) {
      case "differentialEvolutionSelection" -> {
        SequenceGeneratorParameter sequenceGenerator =
            (SequenceGeneratorParameter) findConditionalParameter("sequenceGenerator").value();

        BooleanParameter takeCurrentSolutionAsParent = (BooleanParameter) findConditionalParameter("takeCurrentSolutionAsParent").value();

        result =
            new DifferentialEvolutionSelection(
                matingPoolSize, 3, takeCurrentSolutionAsParent.value(), sequenceGenerator.getSequenceGenerator());
      }

      default -> throw new JMetalException("Selection component unknown: " + value());
    }

    return result;
  }
}
