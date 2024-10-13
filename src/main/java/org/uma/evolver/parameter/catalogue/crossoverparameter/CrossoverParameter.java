package org.uma.evolver.parameter.catalogue.crossoverparameter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.ProbabilityParameter;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.doublesolution.repairsolution.RepairDoubleSolution;

/** Factory for crossover operators. */
public class CrossoverParameter extends CategoricalParameter {
  public double crossoverProbability;
  public RepairDoubleSolution repairDoubleSolution;
  private Map<String, String> availableImplementations;

  public CrossoverParameter(List<String> crossoverOperators) {
    super("crossover", crossoverOperators);

    getImplementations();
    checkCrossoverOperatorNames(crossoverOperators);

    createGlobalParameters().forEach(this::addGlobalParameter);

    getSpecificParameters();
  }

  private List<Parameter<?>> createGlobalParameters() {
    ProbabilityParameter crossoverProbability = new ProbabilityParameter("crossoverProbability");
    RepairDoubleSolutionStrategyParameter crossoverRepairStrategy =
        new RepairDoubleSolutionStrategyParameter(
            "crossoverRepairStrategy", Arrays.asList("random", "round", "bounds"));

    return List.of(crossoverProbability, crossoverRepairStrategy) ;
  }

  public void getSpecificParameters() {
    for (Map.Entry<String, String> entry : availableImplementations.entrySet()) {
      try {
        Class<?> crossoverClass = Class.forName(entry.getValue());
        var getInstanceMethod =
            crossoverClass.getMethod("getSpecificParameter");

        Constructor<?> constructor = crossoverClass.getConstructor();

        addSpecificParameter(entry.getKey(), (Parameter<?>)getInstanceMethod.invoke(constructor.newInstance()));

      } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      } catch (InstantiationException e) {
          throw new RuntimeException(e);
      }
    }

    //addSpecificParameter("SBX", SBX.getSpecificParameter());
    //addSpecificParameter("BLX_ALPHA", SBX.getSpecificParameter());
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
    crossoverProbability = (double) findGlobalParameter("crossoverProbability").value();
    var repairDoubleSolutionParameter =
        (RepairDoubleSolutionStrategyParameter) findGlobalParameter("crossoverRepairStrategy");
    repairDoubleSolution = repairDoubleSolutionParameter.getParameter();

    String crossoverName = value();
    String crossoverQualifiedName = availableImplementations.get(crossoverName);

    return getCrossoverInstance(crossoverQualifiedName);
  }

  private CrossoverOperator<DoubleSolution> getCrossoverInstance(String crossoverQualifiedName) {
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
      Constructor<?> constructor = crossoverClass.getConstructor();
      crossover =
          (CrossoverOperator<DoubleSolution>) getInstanceMethod.invoke(constructor.newInstance(), this);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
    } catch (InstantiationException e) {
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
    CrossoverParameter crossoverParameter = new CrossoverParameter(List.of("BLXAlpha", "SBX"));

    String[] parameters =
        ("--crossover SBX "
                + "--crossoverProbability 0.9 "
                + "--crossoverRepairStrategy round "
                + "--sbxDistributionIndex 29.0 ")
            .split("\\s+");

    crossoverParameter.parse(parameters).check();

    crossoverParameter.availableOperatorNames().forEach(className -> System.out.println(className));

    CrossoverOperator<DoubleSolution> crossover = crossoverParameter.getParameter();
    System.out.println(crossover);
  }
}
