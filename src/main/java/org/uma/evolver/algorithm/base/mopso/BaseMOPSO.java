package org.uma.evolver.algorithm.base.mopso;

import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.base.ParticleSwarmOptimizationBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.GlobalBestInitializationParameter;
import org.uma.evolver.parameter.catalogue.GlobalBestSelectionParameter;
import org.uma.evolver.parameter.catalogue.GlobalBestUpdateParameter;
import org.uma.evolver.parameter.catalogue.InertiaWeightComputingParameter;
import org.uma.evolver.parameter.catalogue.LocalBestInitializationParameter;
import org.uma.evolver.parameter.catalogue.LocalBestUpdateParameter;
import org.uma.evolver.parameter.catalogue.PerturbationParameter;
import org.uma.evolver.parameter.catalogue.PositionUpdateParameter;
import org.uma.evolver.parameter.catalogue.VelocityInitializationParameter;
import org.uma.evolver.parameter.catalogue.VelocityUpdateParameter;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.evolver.parameter.type.IntegerParameter;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.pso.globalbestinitialization.GlobalBestInitialization;
import org.uma.jmetal.component.catalogue.pso.globalbestselection.GlobalBestSelection;
import org.uma.jmetal.component.catalogue.pso.globalbestupdate.GlobalBestUpdate;
import org.uma.jmetal.component.catalogue.pso.inertiaweightcomputingstrategy.InertiaWeightComputingStrategy;
import org.uma.jmetal.component.catalogue.pso.localbestinitialization.LocalBestInitialization;
import org.uma.jmetal.component.catalogue.pso.localbestupdate.LocalBestUpdate;
import org.uma.jmetal.component.catalogue.pso.perturbation.Perturbation;
import org.uma.jmetal.component.catalogue.pso.positionupdate.PositionUpdate;
import org.uma.jmetal.component.catalogue.pso.velocityinitialization.VelocityInitialization;
import org.uma.jmetal.component.catalogue.pso.velocityupdate.VelocityUpdate;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.BoundedArchive;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;

/**
 * Base class for Multi-Objective Particle Swarm Optimization (MOPSO) algorithms.
 * 
 * <p>This class provides a configurable implementation of the MOPSO algorithm, supporting various
 * components and strategies through a flexible parameter space. It serves as a foundation for
 * different MOPSO variants and configurations.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Configurable swarm initialization and evaluation</li>
 *   <li>Flexible velocity and position update strategies</li>
 *   <li>Support for different inertia weight computation methods</li>
 *   <li>Optional external archive for storing non-dominated solutions</li>
 *   <li>Customizable perturbation operators</li>
 *   <li>Extensible architecture for different MOPSO variants</li>
 * </ul>
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // Create a problem instance
 * DoubleProblem problem = new MyDoubleProblem();
 * 
 * // Configure the algorithm
 * int leaderArchiveSize = 100;
 * int maxEvaluations = 25000;
 * ParameterSpace parameterSpace = new ParameterSpace();
 * // Configure parameter space with desired components and parameters
 * 
 * // Create and run the algorithm
 * BaseMOPSO mopso = new BaseMOPSO(problem, leaderArchiveSize, maxEvaluations, parameterSpace);
 * mopso.run();
 * 
 * // Get results
 * List<DoubleSolution> population = mopso.result();
 * }</pre>
 *
 * @see BaseLevelAlgorithm
 * @see ParticleSwarmOptimizationAlgorithm
 * @see DoubleSolution
 * @since version
 */
public class BaseMOPSO implements BaseLevelAlgorithm<DoubleSolution> {

  /** The parameter space containing all configurable components and parameters. */
  private final ParameterSpace parameterSpace;

  /** The size of the leader archive storing non-dominated solutions. */
  protected int leaderArchiveSize;
  
  /** The optimization problem to be solved. */
  protected DoubleProblem problem;
  
  /** The maximum number of evaluations allowed for the algorithm. */
  protected int maximumNumberOfEvaluations;
  
  /** The size of the particle swarm. */
  protected int swarmSize;

  /** Archive storing the best solutions found (leaders). */
  protected BoundedArchive<DoubleSolution> leaderArchive;
  
  /** Optional external archive for storing additional solutions. */
  protected Archive<DoubleSolution> externalArchive;

  /**
   * Constructs a new BaseMOPSO instance with the specified leader archive size and parameter space.
   * 
   * <p>Note: This creates a partially configured instance. The {@link #createInstance(Problem, int)}
   * method must be called with a problem instance before the algorithm can be used.
   *
   * @param leaderArchiveSize the size of the leader archive. Must be positive.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   *                      Must not be null.
   * @throws IllegalArgumentException if leaderArchiveSize is not positive or parameterSpace is null
   */
  public BaseMOPSO(int leaderArchiveSize, ParameterSpace parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.leaderArchiveSize = leaderArchiveSize;
  }

  /**
   * Constructs a fully configured BaseMOPSO instance ready for execution.
   *
   * @param problem the double optimization problem to be solved. Must implement the DoubleProblem interface.
   * @param leaderArchiveSize the size of the leader archive. Must be positive.
   * @param maximumNumberOfEvaluations the evaluation budget for the algorithm. The algorithm will
   *                                 terminate once this number of evaluations is reached.
   * @param parameterSpace the parameter space containing configuration parameters for the algorithm.
   * @throws IllegalArgumentException if any parameter is invalid (null or non-positive values where required)
   * @throws ClassCastException if the provided problem does not implement DoubleProblem
   */
  public BaseMOPSO(
      DoubleProblem problem,
      int leaderArchiveSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    this.problem = problem;
    this.leaderArchiveSize = leaderArchiveSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.parameterSpace = parameterSpace;
  }

  /**
   * Returns the parameter space used by this algorithm.
   *
   * @return the parameter space containing all configurable components and parameters
   */
  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  /**
   * Builds and configures the MOPSO algorithm based on the current parameter space.
   * 
   * <p>This method initializes all necessary components for the MOPSO algorithm, including:
   * <ul>
   *   <li>Termination condition</li>
   *   <li>Evaluation strategy</li>
   *   <li>Swarm initialization</li>
   *   <li>Velocity and position update strategies</li>
   *   <li>Best solution tracking (local and global)</li>
   *   <li>Perturbation operators</li>
   *   <li>Archiving mechanisms</li>
   * </ul>
   *
   * @return a fully configured ParticleSwarmOptimizationAlgorithm instance
   * @throws IllegalStateException if required parameters are not properly configured
   */
  @Override
  public ParticleSwarmOptimizationAlgorithm build() {
    setNonConfigurableParameters();

    if (usingExternalArchive()) {
      externalArchive = createExternalArchive() ;
    }

    var termination = createTermination();
    var evaluation = createEvaluation();
    var swarmInitialization = createInitialSolutionsCreation();
    var velocityInitialization = createVelocityInitialization();
    var localBestInitialization = createLocalBestInitialization();
    var globalBestInitialization = createGlobalBestInitialization();
    var inertiaWeightStrategy = createInertiaWeightStrategy();
    var velocityUpdate = createVelocityUpdate();
    var positionUpdate = createPositionUpdate();
    var perturbation = createPerturbation();
    var globalBestUpdate = createGlobalBestUpdate();
    var localBestUpdate = createLocalBestUpdate();
    var globalBestSelection = createGlobalBestSelection();

    return new ParticleSwarmOptimizationBuilder()
        .build(
            "MOPSO",
            swarmInitialization,
            evaluation,
            termination,
            velocityInitialization,
            localBestInitialization,
            globalBestInitialization,
            inertiaWeightStrategy,
            velocityUpdate,
            positionUpdate,
            perturbation,
            globalBestUpdate,
            localBestUpdate,
            globalBestSelection,
            leaderArchive,
            externalArchive);
  }

  /**
   * Configures non-configurable parameters based on the problem's characteristics.
   * 
   * <p>This method is automatically called during algorithm initialization to set up parameters
   * that depend on the specific problem instance, such as:
   * <ul>
   *   <li>Swarm size</li>
   *   <li>Leader archive configuration</li>
   *   <li>Velocity update parameters</li>
   *   <li>Inertia weight computation</li>
   *   <li>Mutation parameters</li>
   *   <li>Position update bounds</li>
   * </ul>
   * 
   * @implNote This method is part of the template method pattern and should not be called directly.
   * It is automatically invoked by the framework during algorithm initialization.
   */
  protected void setNonConfigurableParameters() {
    swarmSize = (int) parameterSpace.get("swarmSize").value();

    var leaderArchiveParameter =
        (ExternalArchiveParameter<DoubleSolution>) parameterSpace.get("leaderArchive");
    leaderArchiveParameter.setSize(leaderArchiveSize);
    leaderArchive = (BoundedArchive<DoubleSolution>) leaderArchiveParameter.getExternalArchive();

    var velocityUpdateParameter =
        ((VelocityUpdateParameter) parameterSpace.get("velocityUpdate"));
    if (velocityUpdateParameter.value().equals("constrainedVelocityUpdate")
        || velocityUpdateParameter.value().equals("SPSO2011VelocityUpdate")) {
      velocityUpdateParameter.addNonConfigurableSubParameter("problem", problem);
    }

    var swarmSizeParameter = ((IntegerParameter) parameterSpace.get("swarmSize"));
    var inertiaWeightComputingStrategyParameter =
        ((InertiaWeightComputingParameter) parameterSpace.get("inertiaWeightComputingStrategy"));
    if (inertiaWeightComputingStrategyParameter.value().equals("linearDecreasingValue")
        || inertiaWeightComputingStrategyParameter.value().equals("linearIncreasingValue")) {
      inertiaWeightComputingStrategyParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / swarmSizeParameter.value());
      inertiaWeightComputingStrategyParameter.addNonConfigurableSubParameter(
          "swarmSize", swarmSizeParameter.value());
    }

    var mutationParameter = (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableSubParameter(
              "maxIterations", maximumNumberOfEvaluations / swarmSize);
    }

    if (mutationParameter.value().equals("uniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "uniformMutationPerturbation", parameterSpace.get("uniformMutationPerturbation"));
    }

    var PositionUpdateParameter = (PositionUpdateParameter) parameterSpace.get("positionUpdate");
    if (PositionUpdateParameter.value().equals("defaultPositionUpdate")) {
      PositionUpdateParameter.addNonConfigurableSubParameter(
          "positionBounds", problem.variableBounds());
    }
  }

    /**
   * Determines if the algorithm is configured to use an external archive.
   *
   * <p>This method checks the 'algorithmResult' parameter in the parameter space to see if
   * an external archive should be maintained during the optimization process.
   *
   * @return {@code true} if an external archive is configured to be used, {@code false} otherwise
   */
  private boolean usingExternalArchive() {
    return parameterSpace
        .get("algorithmResult")
        .value()
        .equals("externalArchive");
  }

   /**
   * Creates and configures the external archive based on the parameter space settings.
   * 
   * <p>The external archive stores additional non-dominated solutions found during the search.
   * The size of the archive is set according to the leader archive size specified during
   * construction.
   *
   * @return the configured external archive instance
   * @throws IllegalStateException if the external archive cannot be created or configured
   * @see #leaderArchiveSize
   */
  protected Archive<DoubleSolution> createExternalArchive() {
    ExternalArchiveParameter<DoubleSolution> externalArchiveParameter =
        (ExternalArchiveParameter<DoubleSolution>) parameterSpace.get("externalArchiveType");
  
    externalArchiveParameter.setSize(leaderArchiveSize);
    Archive<DoubleSolution> archive = externalArchiveParameter.getExternalArchive();

    return archive;
  }

  /**
   * Creates a new instance of BaseMOPSO configured for the specified problem and evaluation limit.
   * 
   * <p>This method implements the factory method pattern, allowing the creation of algorithm
   * instances with the same configuration but potentially different problems or evaluation limits.
   * The new instance will have its own parameter space configuration.
   *
   * @param problem the optimization problem to solve. Must not be null.
   * @param maximumNumberOfEvaluations the maximum number of evaluations allowed for the new instance.
   *                                 Must be positive.
   * @return a new, fully configured instance of BaseMOPSO
   * @throws IllegalArgumentException if problem is null or maximumNumberOfEvaluations is not positive
   * @throws ClassCastException if the provided problem does not implement DoubleProblem
   */
  @Override
  public BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new BaseMOPSO(
        (DoubleProblem) problem, leaderArchiveSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  /**
   * Creates the evaluation component for the algorithm.
   *
   * <p>This method determines whether to use a basic sequential evaluation or an evaluation
   * with an external archive based on the current configuration. If an external archive is
   * being used, the evaluation will automatically update it with new non-dominated solutions.
   *
   * @return the configured evaluation component
   * @see #usingExternalArchive()
   * @see #externalArchive
   */
  protected Evaluation<DoubleSolution> createEvaluation() {
    if (usingExternalArchive()) {
      return new SequentialEvaluationWithArchive<>(problem, externalArchive);
    } else {
      return new SequentialEvaluation<>(problem);
    }
  }

  /**
   * Creates the termination condition for the algorithm.
   *
   * <p>This implementation creates a termination condition based on the maximum number of
   * evaluations specified during construction. The algorithm will stop once this number of
   * solution evaluations is reached.
   *
   * @return the termination condition based on evaluation count
   * @see #maximumNumberOfEvaluations
   */
  protected Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  /**
   * Creates the component responsible for generating the initial population of solutions.
   *
   * @return the solutions creation component
   */
  protected SolutionsCreation<DoubleSolution> createInitialSolutionsCreation() {
return ((CreateInitialSolutionsParameter<DoubleSolution>)
            parameterSpace.get("swarmInitialization"))
        .getCreateInitialSolutionsStrategy(problem, swarmSize);  }

  /**
   * Creates the strategy for initializing particle velocities.
   *
   * @return the velocity initialization strategy
   */
  protected VelocityInitialization createVelocityInitialization() {
    return ((VelocityInitializationParameter) parameterSpace.get("velocityInitialization"))
        .getVelocityInitialization();
  }

  /**
   * Creates the strategy for initializing local best solutions.
   *
   * @return the local best initialization strategy
   */
  protected LocalBestInitialization createLocalBestInitialization() {
    return ((LocalBestInitializationParameter) parameterSpace.get("localBestInitialization"))
        .getLocalBestInitialization();
  }

  /**
   * Creates the strategy for initializing global best solutions.
   *
   * @return the global best initialization strategy
   */
  protected GlobalBestInitialization createGlobalBestInitialization() {
    return ((GlobalBestInitializationParameter) parameterSpace.get("globalBestInitialization"))
        .getGlobalBestInitialization();
  }

  /**
   * Creates the strategy for computing inertia weight during the search.
   *
   * @return the inertia weight computing strategy
   */
  protected InertiaWeightComputingStrategy createInertiaWeightStrategy() {
    return ((InertiaWeightComputingParameter) parameterSpace.get("inertiaWeightComputingStrategy"))
        .getInertiaWeightComputingStrategy();
  }

  /**
   * Creates the operator for updating particle velocities.
   *
   * @return the velocity update operator
   */
  protected VelocityUpdate createVelocityUpdate() {
    return ((VelocityUpdateParameter) parameterSpace.get("velocityUpdate"))
        .getVelocityUpdate();
  }

  /**
   * Creates the operator for updating particle positions.
   *
   * @return the position update operator
   */
  protected PositionUpdate createPositionUpdate() {
    return ((PositionUpdateParameter) parameterSpace.get("positionUpdate"))
        .getPositionUpdate();
  }

  /**
   * Creates the operator for applying perturbations to solutions.
   *
   * @return the perturbation operator
   */
  protected Perturbation createPerturbation() {
    return ((PerturbationParameter) parameterSpace.get("perturbation"))
        .getPerturbation();
  }

  /**
   * Creates the strategy for updating the global best solution.
   *
   * @return the global best update strategy
   */
  protected GlobalBestUpdate createGlobalBestUpdate() {
    return ((GlobalBestUpdateParameter) parameterSpace.get("globalBestUpdate"))
        .getGlobalBestUpdate();
  }

  /**
   * Creates the strategy for updating local best solutions.
   *
   * @return the local best update strategy
   */
  protected LocalBestUpdate createLocalBestUpdate() {
    return ((LocalBestUpdateParameter) parameterSpace.get("localBestUpdate"))
        .getLocalBestUpdate(new DefaultDominanceComparator<>());
  }

  /**
   * Creates the strategy for selecting the global best solution.
   *
   * @return the global best selection strategy
   */
  protected GlobalBestSelection createGlobalBestSelection() {
    return ((GlobalBestSelectionParameter) parameterSpace.get("globalBestSelection"))
        .getGlobalBestSelection(leaderArchive.comparator());
  }
}
