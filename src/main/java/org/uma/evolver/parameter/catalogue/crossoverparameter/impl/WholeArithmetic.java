package org.uma.evolver.parameter.catalogue.crossoverparameter.impl;

import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.CrossoverParameter;
import org.uma.evolver.parameter.impl.RealParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public class WholeArithmetic {
    public static String name() {
        return "wholeArithmetic";
    }

    public static Parameter<?> getSpecificParameter() {
        return null ;
    }

    public static CrossoverOperator<DoubleSolution> getInstance(
            CrossoverParameter crossoverParameter) {
        Double alpha =
                (Double) crossoverParameter.findSpecificParameter("blxAlphaCrossoverAlphaValue").value();
        return new BLXAlphaCrossover(
                crossoverParameter.crossoverProbability, alpha, crossoverParameter.repairDoubleSolution);
    }
}
