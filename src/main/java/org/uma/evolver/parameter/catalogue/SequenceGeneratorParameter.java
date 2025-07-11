package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;
import org.uma.jmetal.util.sequencegenerator.impl.CyclicIntegerSequence;
import org.uma.jmetal.util.sequencegenerator.impl.RandomPermutationCycle;

/**
 * A categorical parameter representing different sequence generation strategies.
 * This parameter allows selecting and configuring how sequences of integers are generated,
 * which is commonly used in evolutionary algorithms for various purposes like indexing solutions.
 *
 * <p>The available sequence generation strategies are:
 * <ul>
 *   <li>permutation: Generates a random permutation of integers from 0 to sequenceLength-1</li>
 *   <li>integerSequence: Generates a sequence of integers from 0 to sequenceLength-1 in order</li>
 * </ul>
 *
 * <p>Note: The sequenceLength must be set using the sequenceLength() method before
 * getting a sequence generator.
 */
public class SequenceGeneratorParameter extends CategoricalParameter {
  private static String DEFAULT_NAME = "sequenceGenerator";
  private int sequenceLength;

  /**
   * Creates a new SequenceGeneratorParameter with the specified name.
   * The available sequence generation strategies are "randomPermutationCycle" and "cyclicIntegerSequence".
   *
   * @param name The name of the parameter
   * @throws IllegalArgumentException if name is null or empty
   */
  public SequenceGeneratorParameter(String name, List<String> values) {
    super(name, values);
  }

  public SequenceGeneratorParameter() {
    this(DEFAULT_NAME, List.of("randomPermutationCycle", "cyclicIntegerSequence")) ;
  }


  /**
   * Sets the length of the sequence to be generated.
   * This must be called before getSequenceGenerator().
   *
   * @param length The length of the sequence (must be positive)
   * @throws IllegalArgumentException if length is not positive
   */
  public void sequenceLength(int length) {
    if (length <= 0) {
      throw new IllegalArgumentException("Sequence length must be positive: " + length);
    }
    this.sequenceLength = length;
  }

  /**
   * Creates and returns a SequenceGenerator based on the current parameter value.
   * The specific implementation depends on the current strategy:
   * <ul>
   *   <li>"permutation": Returns an IntegerPermutationGenerator that produces random permutations</li>
   *   <li>"integerSequence": Returns an IntegerBoundedSequenceGenerator that produces sequences in order</li>
   * </ul>
   *
   * @return A configured SequenceGenerator instance
   * @throws JMetalException if the current value does not match any known sequence generation strategy
   * @throws IllegalStateException if sequenceLength has not been set or is not positive
   */
  public SequenceGenerator<Integer> getSequenceGenerator() {
    SequenceGenerator<Integer> sequenceGenerator;
    switch (value()) {
      case "randomPermutationCycle" -> sequenceGenerator = new RandomPermutationCycle(sequenceLength);
      case "cyclicIntegerSequence" ->
          sequenceGenerator = new CyclicIntegerSequence(sequenceLength);
      default -> throw new JMetalException("Sequence generator does not exist: " + name());
    }

    return sequenceGenerator;
  }
  
  /**
   * Returns the name of this parameter.
   *
   * @return The name of this parameter as specified in the constructor
   */
  @Override
  public String name() {
    return super.name();
  }
}
