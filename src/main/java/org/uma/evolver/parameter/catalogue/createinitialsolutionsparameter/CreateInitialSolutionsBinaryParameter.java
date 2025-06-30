package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class CreateInitialSolutionsBinaryParameter
    extends CreateInitialSolutionsParameter<BinarySolution> {

  private static final String DEFAULT_STRATEGY = "default" ;

  public CreateInitialSolutionsBinaryParameter(List<String> validValues) {
    super(validValues);
  }

  @Override
  public SolutionsCreation<BinarySolution> getCreateInitialSolutionsStrategy(
      Problem<BinarySolution> problem, int populationSize) {
    if (value().equals("DEFAULT_STRATEGY")) {
      return new RandomSolutionsCreation<>(problem, populationSize);
    }
    throw new JMetalException(value() + " is not a valid initialization strategy");
  }
}
