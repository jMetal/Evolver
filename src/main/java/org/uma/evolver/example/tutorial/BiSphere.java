package org.uma.evolver.example.tutorial;

import java.util.ArrayList;
import java.util.List;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * A bi-objective problem of the user, as in tutorial E17, "Extending Evolver" (see {@code
 * docs/tutorials/extending_evolver.rst}): minimize the squared distance to the origin and to the
 * point (2, ..., 2).
 *
 * <p>{@code f1 = sum(x_i^2)} and {@code f2 = sum((x_i - 2)^2)}, with {@code x_i} in [-5, 5]. The
 * Pareto set is the segment between the two points, {@code x_i = 2t} for all i with t in [0, 1],
 * so the Pareto front is known: {@code f1 = 4 n t^2}, {@code f2 = 4 n (1 - t)^2}, with n the number
 * of variables (see {@link #referenceFront}).
 *
 * <p>It is a class like any other jMetal problem and is not registered anywhere in Evolver: a
 * request refers to it by its class name, {@code problem: {class:
 * org.uma.evolver.example.tutorial.BiSphere, args: [20]}}, with the class on the classpath.
 */
public class BiSphere extends AbstractDoubleProblem {

  /** The problem with 10 variables. */
  public BiSphere() {
    this(10);
  }

  /**
   * @param numberOfVariables the number of variables
   */
  public BiSphere(int numberOfVariables) {
    numberOfObjectives(2);
    numberOfConstraints(0);
    name("BiSphere");
    List<Double> lowerLimit = new ArrayList<>(numberOfVariables);
    List<Double> upperLimit = new ArrayList<>(numberOfVariables);
    for (int i = 0; i < numberOfVariables; i++) {
      lowerLimit.add(-5.0);
      upperLimit.add(5.0);
    }
    variableBounds(lowerLimit, upperLimit);
  }

  @Override
  public DoubleSolution evaluate(DoubleSolution solution) {
    double toOrigin = 0.0;
    double toTwo = 0.0;
    for (double x : solution.variables()) {
      toOrigin += x * x;
      toTwo += (x - 2.0) * (x - 2.0);
    }
    solution.objectives()[0] = toOrigin;
    solution.objectives()[1] = toTwo;
    return solution;
  }

  /**
   * The Pareto front of the problem with {@code numberOfVariables} variables, as {@code points}
   * points {@code (f1, f2)} evenly spaced along the Pareto set.
   */
  public static double[][] referenceFront(int numberOfVariables, int points) {
    double[][] front = new double[points][2];
    for (int i = 0; i < points; i++) {
      double t = (double) i / (points - 1);
      front[i][0] = 4.0 * numberOfVariables * t * t;
      front[i][1] = 4.0 * numberOfVariables * (1.0 - t) * (1.0 - t);
    }
    return front;
  }
}
