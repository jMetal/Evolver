package org.uma.evolver.parameter.factory;

import java.util.List;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Factory class for creating categorical parameters specific to double-solution based evolutionary algorithms.
 * This factory provides a centralized way to create different types of parameters used in the configuration
 * of evolutionary algorithms that work with double-encoded solutions.
 * 
 * @author Your Name
 * @since version
 */
public class MOPSOParameterFactory implements ParameterFactory<DoubleSolution> {

  /**
   * Creates and returns a specific CategoricalParameter instance based on the provided parameter name.
   * This factory method supports the creation of various types of parameters used in evolutionary algorithms,
   * such as selection, crossover, mutation, and other algorithm-specific parameters.
   *
   * @param parameterName the name of the parameter to create. Supported values are:
   *                     - "archiveType": Creates an ExternalArchiveParameter
   *                     - "createInitialSolutions": Creates a CreateInitialSolutionsDoubleParameter
   *                     - "mutation": Creates a DoubleMutationParameter
   *                     - "mutationRepairStrategy": Creates a RepairDoubleSolutionStrategyParameter for mutation
   *                     - Any other value: Creates a basic CategoricalParameter
   * @param values the list of possible values for the parameter
   * @return an instance of CategoricalParameter corresponding to the specified parameter name
   * @throws IllegalArgumentException if the values list is null or empty
   */
  @Override
  public CategoricalParameter createParameter(String parameterName, List<String> values) {
    
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The list of values cannot be null or empty");
    }

    CategoricalParameter parameter;
    switch (parameterName) {
      case "leaderArchive" -> parameter = new ExternalArchiveParameter<DoubleSolution>(parameterName, values);
      case "archiveType" -> parameter = new ExternalArchiveParameter<DoubleSolution>(parameterName, values);
      case "swarmInitialization" -> parameter = new CreateInitialSolutionsDoubleParameter(parameterName, values);
      case "velocityInitialization" -> parameter = new VelocityInitializationParameter(values);
      case "perturbation" -> parameter = new PerturbationParameter(values);
      case "mutation" -> parameter = new DoubleMutationParameter(values);
      case "mutationRepairStrategy" -> parameter = new RepairDoubleSolutionStrategyParameter("mutationRepairStrategy", values);
      case "inertiaWeightComputingStrategy" -> parameter = new InertiaWeightComputingParameter(values);
      case "velocityUpdate" -> parameter = new VelocityUpdateParameter(values);
      case "localBestInitialization" -> parameter = new LocalBestInitializationParameter(values);
      case "localBestUpdate" -> parameter = new LocalBestUpdateParameter(values);
      case "globalBestInitialization" -> parameter = new GlobalBestInitializationParameter(values); 
      case "globalBestSelection" -> parameter = new GlobalBestSelectionParameter(values);
      case "globalBestUpdate" -> parameter = new GlobalBestUpdateParameter(values);
      case "positionUpdate" -> parameter = new PositionUpdateParameter(values);
      default -> {
        parameter = new CategoricalParameter(parameterName, values);
      }
    }
    return parameter;
  }
}
