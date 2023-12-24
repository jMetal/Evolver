package org.uma.evolver.referencefrontupdate.impl;

import static org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues;

import java.util.List;
import org.uma.evolver.referencefrontupdate.ReferenceFrontUpdate;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.point.impl.IdealPoint;
import org.uma.jmetal.util.point.impl.NadirPoint;

public class DynamicReferenceFrontUpdate<S extends Solution<?>> implements ReferenceFrontUpdate<S> {

  private double[][] referenceFront = null;
  private double[][] normalizedReferenceFront = null;

  private NadirPoint estimatedNadirPoint = null ;
  private IdealPoint estimatedIdealPoint = null;

  private NonDominatedSolutionListArchive<S> nonDominatedSolutionListArchive;

  public DynamicReferenceFrontUpdate() {
    nonDominatedSolutionListArchive = new NonDominatedSolutionListArchive<>();
  }

  @Override
  synchronized public void update(List<S> solutions) {
    if (estimatedIdealPoint == null) {
      estimatedIdealPoint = new IdealPoint(solutions.get(0).objectives().length) ;
    }

    estimatedNadirPoint = new NadirPoint(solutions.get(0).objectives().length) ;
    for (S solution : solutions) {
      nonDominatedSolutionListArchive.add(solution);
      estimatedIdealPoint.update(solution.objectives());
      estimatedNadirPoint.update(solution.objectives());
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

  @Override
  public NadirPoint estimatedNadirPoint() {
    return estimatedNadirPoint ;
  }

  @Override
  public IdealPoint estimatedIdealPoint() {
    return estimatedIdealPoint ;
  }
}
