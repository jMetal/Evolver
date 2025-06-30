package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.LatinHypercubeSamplingSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.ScatterSearchSolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class CreateInitialSolutionsDoubleParameter
    extends CreateInitialSolutionsParameter<DoubleSolution> {

  // Valid strategies:
  private static final String DEFAULT_STRATEGY = "default" ;
  private static final String SCATTER_SEARCH = "scatterSearch" ;
  private static final String LATIN_HYPERCUBE_SAMPLING = "latinHypercubeSampling" ;

  public CreateInitialSolutionsDoubleParameter(List<String> validValues) {
    super(validValues);
  }

  @Override
  public SolutionsCreation<DoubleSolution> getCreateInitialSolutionsStrategy(
      Problem<DoubleSolution> problem, int populationSize) {
    return switch (value()) {
      case DEFAULT_STRATEGY -> new RandomSolutionsCreation<>(problem, populationSize);
      case SCATTER_SEARCH ->
          new ScatterSearchSolutionsCreation((DoubleProblem) problem, populationSize, 4);
      case LATIN_HYPERCUBE_SAMPLING ->
          new LatinHypercubeSamplingSolutionsCreation((DoubleProblem) problem, populationSize);
      default -> throw new JMetalException(value() + " is not a valid initialization strategy");
    };
  }
}
