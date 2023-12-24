package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

public class DifferentialEvolutionSelectionParameter<S extends Solution<?>> extends
    CategoricalParameter {

  public DifferentialEvolutionSelectionParameter(List<String> selectionStrategies) {
    super("selection", selectionStrategies);
  }

  public Selection<DoubleSolution> getParameter(int matingPoolSize, int numberOfParentsToSelect,
      SequenceGenerator<Integer> sequenceGenerator) {
    return new DifferentialEvolutionSelection(matingPoolSize, numberOfParentsToSelect, false,
        sequenceGenerator);
  }
}
