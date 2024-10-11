package org.uma.evolver.parameter.catalogue.crossoverparameter;

import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

public interface CrossoverInstance {
    //String name() ;
    CrossoverOperator<DoubleSolution> getInstance() ;
}
