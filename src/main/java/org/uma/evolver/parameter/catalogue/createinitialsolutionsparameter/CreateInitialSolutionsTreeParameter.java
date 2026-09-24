package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.evolver.encoding.solution.DerivationTreeSolution;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Initial solutions creation parameter for derivation tree solutions ({@link
 * DerivationTreeSolution}). The only supported strategy is {@code default}: random trees created
 * by the problem itself.
 */
public class CreateInitialSolutionsTreeParameter
    extends CreateInitialSolutionsParameter<DerivationTreeSolution> {

  public static final String DEFAULT_STRATEGY = "default";

  public CreateInitialSolutionsTreeParameter(List<String> validValues) {
    this(DEFAULT_NAME, validValues);
  }

  public CreateInitialSolutionsTreeParameter(String name, List<String> validValues) {
    super(name, validValues);
  }

  @Override
  public SolutionsCreation<DerivationTreeSolution> getCreateInitialSolutionsStrategy(
      Problem<DerivationTreeSolution> problem, int populationSize) {
    if (value().equals(DEFAULT_STRATEGY)) {
      return new RandomSolutionsCreation<>(problem, populationSize);
    }
    throw new JMetalException("Unknown initialization strategy: " + value());
  }
}
