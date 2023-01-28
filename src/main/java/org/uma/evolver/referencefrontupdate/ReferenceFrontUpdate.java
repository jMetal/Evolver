package org.uma.evolver.referencefrontupdate;

import java.util.List;
import org.uma.jmetal.solution.Solution;

public interface ReferenceFrontUpdate<S extends Solution<?>>{

  void update(List<S> solutions);

  double[][] referenceFront();
  double[][] normalizedReferenceFront();
}
