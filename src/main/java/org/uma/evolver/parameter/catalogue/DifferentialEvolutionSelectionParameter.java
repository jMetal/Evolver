package org.uma.evolver.parameter.catalogue;

import java.util.Comparator;
import java.util.List;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.PopulationAndNeighborhoodSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.RandomSelection;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.neighborhood.Neighborhood;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;
import scala.xml.dtd.impl.Base.Sequ;

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
