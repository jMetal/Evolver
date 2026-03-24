package org.uma.evolver.util;

import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.bounds.Bounds;

import java.util.List;

public class SingleObjectiveWrapper extends AbstractDoubleProblem {
  private final DoubleProblem originalProblem;
  private final int objectiveIndex;

  public SingleObjectiveWrapper(DoubleProblem problem, int objectiveIndex) {
    this.originalProblem = problem;
    this.objectiveIndex = objectiveIndex;
    numberOfObjectives(1);
    numberOfConstraints(0);

    name(problem.name() + "_obj" + objectiveIndex);
    
    List<Double> lowerLimit = problem.variableBounds().stream().map(Bounds::getLowerBound).toList();
    List<Double> upperLimit = problem.variableBounds().stream().map(Bounds::getUpperBound).toList();
    variableBounds(lowerLimit, upperLimit);
  }

  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    DoubleSolution tempSolution = originalProblem.createSolution();
    for (int i = 0; i < solution.variables().size(); i++) {
      tempSolution.variables().set(i, solution.variables().get(i));
    }
    originalProblem.evaluate(tempSolution);
    double value = tempSolution.objectives()[objectiveIndex];
    solution.objectives()[0] = value;
    return solution;
  }
}
