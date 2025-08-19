package org.uma.evolver.parameter.factory;

import java.util.List;
import org.uma.evolver.parameter.catalogue.*;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsDoubleParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.DoubleMutationParameter;
import org.uma.evolver.parameter.type.CategoricalParameter;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Factory class for creating and configuring categorical parameters specific to the MOPSO algorithm.
 * This factory implements the ParameterFactory interface to provide type-safe creation of various
 * MOPSO-specific components and their parameters.
 *
 * <p>The factory supports the creation of parameters for different MOPSO components including:
 * <ul>
 *   <li>Archive management (leader and external archives)</li>
 *   <li>Swarm initialization and velocity computation</li>
 *   <li>Mutation and perturbation strategies</li>
 *   <li>Inertia weight computation</li>
 *   <li>Velocity and position update mechanisms</li>
 *   <li>Local and global best management</li>
 * </ul>
 *
 * <p>This implementation is specifically designed for the MOPSO algorithm working with double-encoded solutions
 * and provides appropriate parameter types that are compatible with the DoubleSolution interface.
 *
 * @author Antonio J. Nebro
 * @since 1.0
 */
public class MOPSOParameterFactory implements ParameterFactory<DoubleSolution> {

  /**
   * Creates and returns a specific {@link CategoricalParameter} instance based on the provided parameter name.
   * This factory method centralizes the creation of all parameter types used in the MOPSO algorithm.
   *
   * <p>The following parameter types are supported:
   * <table border="1">
   *   <caption>Supported MOPSO Parameter Types</caption>
   *   <tr><th>Parameter Name</th><th>Creates</th><th>Description</th></tr>
   *   <tr><td>leaderArchive</td><td>{@link ExternalArchiveParameter}</td><td>Archive for storing leader particles</td></tr>
   *   <tr><td>externalArchiveType</td><td>{@link ExternalArchiveParameter}</td><td>Type of external archive for non-dominated solutions</td></tr>
   *   <tr><td>swarmInitialization</td><td>{@link CreateInitialSolutionsDoubleParameter}</td><td>Strategy for initializing the swarm</td></tr>
   *   <tr><td>velocityInitialization</td><td>{@link VelocityInitializationParameter}</td><td>Method for initializing particle velocities</td></tr>
   *   <tr><td>perturbation</td><td>{@link PerturbationParameter}</td><td>Perturbation strategy for maintaining diversity</td></tr>
   *   <tr><td>mutation</td><td>{@link DoubleMutationParameter}</td><td>Mutation operator for particles</td></tr>
   *   <tr><td>mutationRepairStrategy</td><td>{@link RepairDoubleSolutionStrategyParameter}</td><td>Repair strategy for mutation operations</td></tr>
   *   <tr><td>inertiaWeightComputingStrategy</td><td>{@link InertiaWeightComputingParameter}</td><td>Strategy for computing inertia weight</td></tr>
   *   <tr><td>velocityUpdate</td><td>{@link VelocityUpdateParameter}</td><td>Velocity update strategy</td></tr>
   *   <tr><td>localBestInitialization</td><td>{@link LocalBestInitializationParameter}</td><td>Method for initializing local best positions</td></tr>
   *   <tr><td>localBestUpdate</td><td>{@link LocalBestUpdateParameter}</td><td>Strategy for updating local best positions</td></tr>
   *   <tr><td>globalBestInitialization</td><td>{@link GlobalBestInitializationParameter}</td><td>Method for initializing global best position</td></tr>
   *   <tr><td>globalBestSelection</td><td>{@link GlobalBestSelectionParameter}</td><td>Strategy for selecting global best</td></tr>
   *   <tr><td>globalBestUpdate</td><td>{@link GlobalBestUpdateParameter}</td><td>Strategy for updating global best position</td></tr>
   *   <tr><td>positionUpdate</td><td>{@link PositionUpdateParameter}</td><td>Position update strategy</td></tr>
   *   <tr><td>any other value</td><td>{@link CategoricalParameter}</td><td>Basic categorical parameter with the given name</td></tr>
   * </table>
   *
   * @param parameterName the name of the parameter to create (case-sensitive)
   * @param values the list of possible string values for the parameter (must not be null or empty)
   * @return an instance of {@code CategoricalParameter} corresponding to the specified parameter name
   * @throws IllegalArgumentException if the values list is null or empty
   * @see CategoricalParameter
   */
  @Override
  public CategoricalParameter createParameter(String parameterName, List<String> values) {
    
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("The list of values cannot be null or empty");
    }

    CategoricalParameter parameter;
    switch (parameterName) {
      case "leaderArchive" -> parameter = new ExternalArchiveParameter<DoubleSolution>(parameterName, values);
      case "externalArchiveType" -> parameter = new ExternalArchiveParameter<DoubleSolution>(parameterName, values);
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
