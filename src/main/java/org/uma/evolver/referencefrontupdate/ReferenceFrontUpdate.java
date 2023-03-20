package org.uma.evolver.referencefrontupdate;

import java.util.List;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.point.impl.IdealPoint;
import org.uma.jmetal.util.point.impl.NadirPoint;

public interface ReferenceFrontUpdate<S extends Solution<?>>{

  void update(List<S> solutions);

  double[][] referenceFront();
  double[][] normalizedReferenceFront();

  NadirPoint estimatedNadirPoint() ;
  IdealPoint estimatedIdealPoint() ;
}
