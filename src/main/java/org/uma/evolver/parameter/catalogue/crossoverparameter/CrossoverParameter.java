package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.RepairDoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.impl.RepairDoubleSolutionWithRandomValue;

/** Factory for crossover operators. */
public class CrossoverParameter extends CategoricalParameter {
  public double crossoverProbability;
  public RepairDoubleSolution repairDoubleSolution;
  private Map<String, String> availableImplementations;

  public CrossoverParameter(List<String> crossoverOperators) {
    super("crossover", crossoverOperators);

    getImplementations();
    checkCrossoverOperatorNames(crossoverOperators);
  }

  private void checkCrossoverOperatorNames(List<String> crossoverOperators) {
    crossoverOperators.stream()
        .filter(crossoverOperator -> !availableImplementations.containsKey(crossoverOperator))
        .forEach(
            crossoverOperator -> {
              throw new RuntimeException("The " + crossoverOperator + " is not available");
            });
  }

  public CrossoverOperator<DoubleSolution> getParameter() {
    // crossoverProbability = (double) findGlobalParameter("crossoverProbability").value();
    // var repairDoubleSolutionParameter =
    //    (RepairDoubleSolutionStrategyParameter) findGlobalParameter("crossoverRepairStrategy");
    // repairDoubleSolution = repairDoubleSolutionParameter.getParameter();
    crossoverProbability = 0.9;
    repairDoubleSolution = new RepairDoubleSolutionWithRandomValue();

    String crossoverName = value();
    crossoverName = "BLXAlpha";
    String crossoverQualifiedName = availableImplementations.get(crossoverName);

    Class<?> crossoverClass = null;
    Method getInstanceMethod;
    try {
      crossoverClass = Class.forName(crossoverQualifiedName);
      getInstanceMethod = crossoverClass.getMethod("getInstance", CrossoverParameter.class);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    CrossoverOperator<DoubleSolution> crossover;
    try {
      crossover =
          (CrossoverOperator<DoubleSolution>) getInstanceMethod.invoke(crossoverClass, this);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    }

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
        new CrossoverParameter(java.util.List.of("BLXAlpha"));

    crossoverParameter.availableOperatorNames().forEach(className -> System.out.println(className));

    CrossoverOperator<DoubleSolution> crossover = crossoverParameter.getParameter();
    System.out.println(crossover);
  }
}
