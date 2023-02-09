package org.uma.evolver.referencefrontupdate.impl;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.referencefrontupdate.ReferenceFrontUpdate;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;

public class DynamicReferenceFrontUpdate<S extends Solution<?>> implements ReferenceFrontUpdate<S> {

  private double[][] referenceFront = null;
  private double[][] normalizedReferenceFront = null;

  private NonDominatedSolutionListArchive<S> nonDominatedSolutionListArchive;

  public DynamicReferenceFrontUpdate() {
    nonDominatedSolutionListArchive = new NonDominatedSolutionListArchive<>();
  }

  @Override
  synchronized public void update(List<S> solutions) {
    for (S solution : solutions) {
      nonDominatedSolutionListArchive.add(solution);
    }

    referenceFront = getMatrixWithObjectiveValues(nonDominatedSolutionListArchive.solutions()) ;
    normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
  }

  @Override
  public double[][] referenceFront() {
    return referenceFront;
  }

  @Override
  public double[][] normalizedReferenceFront() {
    return normalizedReferenceFront;
  }
}
