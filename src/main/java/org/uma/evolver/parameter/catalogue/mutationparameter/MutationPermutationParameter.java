package org.uma.evolver.parameter.catalogue.mutationparameter;

import java.util.List;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.*;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class MutationPermutationParameter extends MutationParameter<PermutationSolution<Integer>> {

  public MutationPermutationParameter(List<String> mutationOperators) {
    super(mutationOperators);
  }

  @Override
  public MutationOperator<PermutationSolution<Integer>> getMutation() {
    MutationOperator<PermutationSolution<Integer>> result;

    double mutationProbability =
            (double) findGlobalSubParameter("mutationProbability").value() ;

      switch (value()) {
          case "swap" -> result = new PermutationSwapMutation<>(mutationProbability);
          case "displacement" -> result = new DisplacementMutation<>(mutationProbability);
          case "insert" -> result = new InsertMutation<>(mutationProbability);
          case "scramble" -> result = new ScrambleMutation<>(mutationProbability);
          case "inversion" -> result = new InversionMutation<>(mutationProbability);
          case "simpleInversion" -> result = new SimpleInversionMutation<>(mutationProbability);
          default -> throw new JMetalException("Mutation operator does not exist: " + name());
      }

    return result;
  }
}

