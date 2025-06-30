package org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;

public abstract class CreateInitialSolutionsParameter<S extends Solution<?>>
    extends CategoricalParameter {

  protected CreateInitialSolutionsParameter(List<String> validValues) {
    super("createInitialSolutions", validValues);
  }

  public abstract SolutionsCreation<S> getCreateInitialSolutionsStrategy(
      Problem<S> problem, int populationSize);
}
