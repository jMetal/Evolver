package org.uma.evolver.parameter.catalogue.crossoverparameter.impl;

import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class SBX {
  public static String name() {
    return "SBX";
  }

  public static CrossoverOperator<DoubleSolution> getInstance(
      CrossoverParameter crossoverParameter) {
    Double distributionIndex =
        (Double) crossoverParameter.findSpecificParameter("sbxDistributionIndex").value();
    return new SBXCrossover(
        crossoverParameter.crossoverProbability,
        distributionIndex,
        crossoverParameter.repairDoubleSolution);
  }
}
