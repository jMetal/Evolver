package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class MutationBinaryParameter extends MutationParameter<BinarySolution> {

  public MutationBinaryParameter(List<String> mutationOperators) {
    super(mutationOperators);
  }

 @Override
  public MutationOperator<BinarySolution> getMutation() {
    MutationOperator<BinarySolution> result;
    int numberOfBitsInASolution = (int) nonConfigurableSubParameters().get("numberOfBitsInASolution");
    double mutationProbability =
            (double) findGlobalSubParameter("mutationProbabilityFactor").value() / numberOfBitsInASolution;

    if ("bitFlip".equals(value())) {
      result = new BitFlipMutation<>(mutationProbability);
    } else {
      throw new JMetalException("Mutation operator does not exist: " + name());
    }
    return result;
  }
}

