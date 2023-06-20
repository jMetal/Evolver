package org.uma.evolver.problem;

import org.uma.evolver.parameter.Parameter;
import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;

import java.util.List;

public abstract class ConfigurableAlgorithmBaseProblem extends AbstractDoubleProblem {

    public abstract List<Parameter<?>> parameters();
}
