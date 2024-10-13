package org.uma.evolver.parameter.catalogue.crossoverparameter.impl;

import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.impl.RealParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class BLXAlpha {
  private Parameter<?> specificParameter;

  public BLXAlpha() {
    specificParameter = new RealParameter("blxAlphaCrossoverAlphaValue", 0.0, 1.0);
  }

  public String name() {
    return "BLXAlpha";
  }

  public Parameter<?> getSpecificParameter() {
    return specificParameter;
  }

  public CrossoverOperator<DoubleSolution> getInstance(
      CrossoverParameter crossoverParameter) {
    double alpha =
        (double) crossoverParameter.findSpecificParameter("blxAlphaCrossoverAlphaValue").value();
    return new BLXAlphaCrossover(
        crossoverParameter.crossoverProbability, alpha, crossoverParameter.repairDoubleSolution);
  }

  @Override
  public String toString() {
    return ("Operator name: " + name());
  }
}
