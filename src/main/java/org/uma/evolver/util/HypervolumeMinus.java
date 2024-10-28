package org.uma.evolver.util;

import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;

/**
 * Class providing an implementation of the normalized hypervolume, which is calculated as follows:
 * relative hypervolume = 1 - (HV of the front / HV of the reference front).
 * Before computing this indicator it must be checked that the HV of the reference front is not zero.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class HypervolumeMinus extends PISAHypervolume {
  @Override
  public double compute(double[][] front) {
    return super.compute(front) * - 1 ;
  }

  @Override
  public String name() {
    return "HV-" ;
  }
}
