package org.uma.evolver.parameter.catalogue;

import org.uma.evolver.parameter.impl.RealParameter;

public class ProbabilityParameter extends RealParameter {
  public ProbabilityParameter(String name)  {
    super(name,0.0, 1.0) ;
  }
}
