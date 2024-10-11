package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter.catalogue.crossoverparameter.impl.BLXAlpha;
import org.uma.evolver.parameter.catalogue.crossoverparameter.impl.SBX;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.BLXAlphaCrossover;
import org.uma.jmetal.operator.crossover.impl.HUXCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.crossover.impl.SinglePointCrossover;
import org.uma.jmetal.operator.crossover.impl.UniformCrossover;
import org.uma.jmetal.operator.crossover.impl.WholeArithmeticCrossover;
import org.uma.jmetal.solution.binarysolution.BinarySolution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.RepairDoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/** Factory for crossover operators. */
public class CrossoverParameter extends CategoricalParameter {
  public double crossoverProbability;
  public RepairDoubleSolution repairDoubleSolution;

  private Map<String, String> availableImplementations;

  public CrossoverParameter(List<String> crossoverOperators) {
    super("crossover", crossoverOperators);

    getImplementations();

    crossoverOperators.stream()
        .filter(crossoverOperator -> !availableImplementations.containsKey(crossoverOperator))
        .forEach(
            crossoverOperator -> {
              throw new RuntimeException("The " + crossoverOperator + " is not available");
            });
  }

  public CrossoverOperator<DoubleSolution> getParameter() {
    crossoverProbability = (double) findGlobalParameter("crossoverProbability").value();
    var repairDoubleSolutionParameter =
        (RepairDoubleSolutionStrategyParameter) findGlobalParameter("crossoverRepairStrategy");
    repairDoubleSolution = repairDoubleSolutionParameter.getParameter();

    // Invoke the getInstance() method of the crossover using reflection

    // String crossoverName = value() ;
    String crossoverName = "SBX";
    String crossoverQualifiedName = availableImplementations.get(crossoverName);

    Class<?> crossoverClass = null;
    Method getInstanceMethod;
    try {
      crossoverClass = Class.forName(crossoverQualifiedName);
      getInstanceMethod = crossoverClass.getMethod("getInstance", crossoverClass);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    System.out.println(getInstanceMethod.toString());

    CrossoverOperator<DoubleSolution> result;
    CrossoverOperator<DoubleSolution> crossover = SBX.getInstance(this);

    return crossover;
  }

  public List<String> availableOperatorNames() {
    return availableImplementations.keySet().stream().toList();
  }

  private void getImplementations() {
    availableImplementations = new HashMap<>();

    List<String> implementations =
        FindClassInPackage.findClasses(CrossoverParameter.class.getPackageName() + ".impl");
    for (String implementation : implementations) {
      String className = implementation.substring(implementation.lastIndexOf(".") + 1);
      availableImplementations.put(className, implementation);
    }
  }

  public static void main(String[] args) {
    CrossoverParameter crossoverParameter =
        new CrossoverParameter(java.util.List.of("SBX", "BLXAlpha"));
    crossoverParameter.availableOperatorNames().forEach(className -> System.out.println(className));

    crossoverParameter.getParameter();
  }
}
