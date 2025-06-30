package org.uma.evolver.parameter.catalogue;

import java.util.List;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.repairsolution.RepairDoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithBoundValue;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithOppositeBoundValue;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithRandomValue;
import org.uma.jmetal.util.errorchecking.JMetalException;

public class RepairDoubleSolutionStrategyParameter extends CategoricalParameter {
  public RepairDoubleSolutionStrategyParameter(String name, List<String> strategies) {
    super(name, strategies);
  }

  public RepairDoubleSolution getRepairDoubleSolutionStrategy() {
    RepairDoubleSolution result;
    switch (value()) {
      case "random" -> result = new RepairDoubleSolutionWithRandomValue();
      case "bounds" -> result = new RepairDoubleSolutionWithBoundValue();
      case "round" -> result = new RepairDoubleSolutionWithOppositeBoundValue();
      default -> throw new JMetalException("Repair strategy unknown: " + name());
    }

    return result;
  }
}
