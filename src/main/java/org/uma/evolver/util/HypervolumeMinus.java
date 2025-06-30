package org.uma.evolver.util;

import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;

/**
 * Class providing an implementation of the normalized hypervolume, which is calculated as follows:
 * relative hypervolume = 1 - (HV of the front / HV of the reference front).
 * Before computing this indicator it must be checked that the HV of the reference front is not zero.
 *
 * @author Antonio J. Nebro
 */
public class HypervolumeMinus extends PISAHypervolume {
  @Override
  public double compute(double[][] front) {
    return -1.0 * super.compute(front) ;
  }

  @Override
  public QualityIndicator newInstance() {
    return new HypervolumeMinus() ;
  }

  @Override
  public String name() {
    return "HVMinus" ;
  }
}
