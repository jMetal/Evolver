package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.Solution;

public abstract class MutationParameter<S extends Solution<?>> extends CategoricalParameter {

  public MutationParameter(List<String> mutationOperators) {
    super("mutation", mutationOperators);
  }

  public abstract MutationOperator<S> getMutation();
}
