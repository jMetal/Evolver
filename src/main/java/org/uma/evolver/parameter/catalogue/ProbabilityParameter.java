package org.uma.evolver.parameter.catalogue;

import org.uma.evolver.parameter.type.DoubleParameter;

public class ProbabilityParameter extends DoubleParameter {
  public ProbabilityParameter(String name)  {
    super(name,0.0, 1.0) ;
  }
}
