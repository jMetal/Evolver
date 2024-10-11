package org.uma.evolver.parameter.catalogue.crossoverparameter.impl;

import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class BLXAlpha {
  public static String name() {
    return "BLXAlpha";
  }

  public static CrossoverOperator<DoubleSolution> getInstance(
      CrossoverParameter crossoverParameter) {
    Double alpha = 0.5 ;
        //(Double) crossoverParameter.findSpecificParameter("blxAlphaCrossoverAlphaValue").value();
    return new BLXAlphaCrossover(
        crossoverParameter.crossoverProbability, alpha, crossoverParameter.repairDoubleSolution);
  }
}
