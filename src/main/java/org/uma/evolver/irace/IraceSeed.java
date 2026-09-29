package org.uma.evolver.irace;

import java.util.List;
import org.uma.jmetal.util.pseudorandom.JMetalRandom;

/**
 * Applies the seed that irace passes to a target runner.
 *
 * <p>irace runs every configuration on an instance with the same seed, so that configurations are
 * compared under the same random conditions, and a run can be reproduced; the scenario passes it
 * as {@code --randomGeneratorSeed {seed}}. When the argument is absent (e.g. when a runner is
 * invoked by hand), the random generator is left unseeded.
 */
final class IraceSeed {

  static final String ARGUMENT = "--randomGeneratorSeed";

  private IraceSeed() {}

  /** Seeds jMetal's random generator with the value of {@value #ARGUMENT}, if present. */
  static void applyIfPresent(String[] args) {
    int index = List.of(args).indexOf(ARGUMENT);
    if (index != -1 && index < args.length - 1) {
      JMetalRandom.getInstance().setSeed(Long.parseLong(args[index + 1]));
    }
  }
}
