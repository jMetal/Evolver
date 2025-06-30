package org.uma.evolver.parameter.catalogue;

import java.util.Comparator;
import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.PopulationAndNeighborhoodSelection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.RandomSelection;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.neighborhood.Neighborhood;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;

public class SelectionParameter<S extends Solution<?>> extends CategoricalParameter {

  public SelectionParameter(List<String> selectionStrategies) {
    super("selection", selectionStrategies);
  }

  public Selection<S> getSelection(int matingPoolSize, Comparator<S> comparator) {
    Selection<S> result;
    switch (value()) {
      case "tournament":
        int tournamentSize =
            (Integer) findSpecificSubParameter("selectionTournamentSize").value();

        result = new NaryTournamentSelection<>(
            tournamentSize, matingPoolSize, comparator);

        break;
      case "random":
        result = new RandomSelection<>(matingPoolSize);
        break;
      case "populationAndNeighborhoodMatingPoolSelection":
        double neighborhoodSelectionProbability =
            (double) findSpecificSubParameter("neighborhoodSelectionProbability").value();
        var neighborhood = (Neighborhood<S>) nonConfigurableSubParameters().get("neighborhood");
        Check.notNull(neighborhood);

        var subProblemIdGenerator = (SequenceGenerator<Integer>) nonConfigurableSubParameters().get(
            "subProblemIdGenerator");
        Check.notNull(subProblemIdGenerator);

        result =
            new PopulationAndNeighborhoodSelection<>(
                matingPoolSize,
                subProblemIdGenerator,
                neighborhood,
                neighborhoodSelectionProbability,
                false);
        break;
      default:
        throw new JMetalException("Selection component unknown: " + value());
    }

    return result;
  }
}
