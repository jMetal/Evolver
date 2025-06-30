package org.uma.evolver.parameter.catalogue.variationparameter;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.solution.Solution;

public abstract class VariationParameter<S extends Solution<?>> extends CategoricalParameter {
  protected VariationParameter(List<String> variationStrategies) {
    super("variation", variationStrategies);
  }

  public abstract Variation<S> getVariation() ;
}

