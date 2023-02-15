package org.uma.evolver.referencefrontupdate.impl;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.referencefrontupdate.ReferenceFrontUpdate;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.NormalizeUtils;
import org.uma.jmetal.util.VectorUtils;

public class ReferenceFrontUpdateFromFile<S extends Solution<?>> implements ReferenceFrontUpdate<S> {
  private double[][] referenceFront ;
  private double[][] normalizedReferenceFront ;

  public ReferenceFrontUpdateFromFile(String referenceFrontFileName) throws IOException {
    referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
    normalizedReferenceFront = NormalizeUtils.normalize(referenceFront);
  }

  @Override
  public void update(List<S> solutions) {
    // void body method
  }

  @Override
  public double[][] referenceFront() {
    return referenceFront;
  }

  @Override
  public double[][] normalizedReferenceFront() {
    return normalizedReferenceFront;
  }
}
