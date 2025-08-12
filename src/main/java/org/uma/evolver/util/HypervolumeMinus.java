package org.uma.evolver.util;

import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;

/**
 * Class that returns the negative of the hypervolume value. This is useful when the hypervolume needs to be
 * minimized instead of maximized, for example when used as an objective in optimization problems.
 * The hypervolume is computed using the PISAHypervolume implementation and then multiplied by -1.
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
