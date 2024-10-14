package org.uma.evolver.parameter.catalogue.crossoverparameter.impl;

import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.impl.RealParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class SBX {
  private Parameter<?> specificParameter;

  public SBX() {
    specificParameter = new RealParameter("sbxDistributionIndex", 5.0, 400.0);
  }

  public String name() {
    return "SBX";
  }

  public Parameter<?> getSpecificParameter() {
    return specificParameter;
  }

  public CrossoverOperator<DoubleSolution> getInstance(CrossoverParameter crossoverParameter) {
    double distributionIndex =
        (double) crossoverParameter.findSpecificParameter("sbxDistributionIndex").value();
    return new SBXCrossover(
        crossoverParameter.crossoverProbability,
        distributionIndex,
        crossoverParameter.repairDoubleSolution);
  }

  @Override
  public String toString() {
    return ("Operator name: " + name());
  }
}
