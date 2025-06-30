package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;
import org.uma.jmetal.util.sequencegenerator.impl.IntegerBoundedSequenceGenerator;
import org.uma.jmetal.util.sequencegenerator.impl.IntegerPermutationGenerator;

public class SequenceGeneratorParameter extends CategoricalParameter {
  int sequenceLength;

  public SequenceGeneratorParameter(String name) {
    super(name, List.of("permutation", "integerSequence"));
  }

  public void sequenceLength(int length) {
    this.sequenceLength = length;
  }

  public SequenceGenerator<Integer> getSequenceGenerator() {
    SequenceGenerator<Integer> sequenceGenerator;
    switch (value()) {
      case "permutation" -> sequenceGenerator = new IntegerPermutationGenerator(sequenceLength);
      case "integerSequence" ->
          sequenceGenerator = new IntegerBoundedSequenceGenerator(sequenceLength);
      default -> throw new JMetalException("Sequence generator does not exist: " + name());
    }

    return sequenceGenerator;
  }
}
