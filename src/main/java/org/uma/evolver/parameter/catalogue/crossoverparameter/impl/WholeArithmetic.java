package org.uma.evolver.parameter.catalogue.crossoverparameter.impl;

import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.impl.RealParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.operator.crossover.impl.WholeArithmeticCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class WholeArithmetic {

  public WholeArithmetic() {}

  public String name() {
    return "WholeArithmetic";
  }

  public Parameter<?> getSpecificParameter() {
    return null;
  }

  public CrossoverOperator<DoubleSolution> getInstance(CrossoverParameter crossoverParameter) {
    return new WholeArithmeticCrossover(
        crossoverParameter.crossoverProbability, crossoverParameter.repairDoubleSolution);
  }
}
