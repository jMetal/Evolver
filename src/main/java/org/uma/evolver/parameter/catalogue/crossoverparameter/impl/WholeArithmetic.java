package org.uma.evolver.parameter.catalogue.crossoverparameter.impl;

import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class WholeArithmetic {
    public static String name() {
        return "WholeArithmetic";
    }

    public static CrossoverOperator<DoubleSolution> getInstance(
            CrossoverParameter crossoverParameter) {
        Double alpha =
                (Double) crossoverParameter.findSpecificParameter("blxAlphaCrossoverAlphaValue").value();
        return new BLXAlphaCrossover(
                crossoverParameter.crossoverProbability, alpha, crossoverParameter.repairDoubleSolution);
    }
}
