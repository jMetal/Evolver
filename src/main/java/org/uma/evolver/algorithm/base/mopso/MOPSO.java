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

import static org.uma.evolver.algorithm.base.mopso.MOPSOParameterSpace.*;

public class MOPSO implements BaseLevelAlgorithm<DoubleSolution> {

  private final MOPSOParameterSpace parameterSpace;

  protected int leaderArchiveSize;
  protected DoubleProblem problem;
  protected int maximumNumberOfEvaluations;
  protected int swarmSize;

  protected BoundedArchive<DoubleSolution> leaderArchive;
  protected Archive<DoubleSolution> externalArchive;

  public MOPSO(int leaderArchiveSize) {
    this.parameterSpace = new MOPSOParameterSpace();
    this.leaderArchiveSize = leaderArchiveSize;
  }

  public MOPSO(
      DoubleProblem problem,
      int leaderArchiveSize,
      int maximumNumberOfEvaluations,
      MOPSOParameterSpace parameterSpace) {
    this.problem = problem;
    this.leaderArchiveSize = leaderArchiveSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.parameterSpace = parameterSpace;
  }

  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

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

  private void setNonConfigurableParameters() {
    swarmSize = (int) parameterSpace.get(SWARM_SIZE).value();

    var leaderArchiveParameter =
        (ExternalArchiveParameter<DoubleSolution>) parameterSpace.get(LEADER_ARCHIVE);
    leaderArchiveParameter.setSize(leaderArchiveSize);
    leaderArchive = (BoundedArchive<DoubleSolution>) leaderArchiveParameter.getExternalArchive();

    var velocityUpdateParameter =
        ((VelocityUpdateParameter) parameterSpace.get(VELOCITY_UPDATE));
    if (velocityUpdateParameter.value().equals(CONSTRAINED_VELOCITY_UPDATE)
        || velocityUpdateParameter.value().equals(SPSO2011_VELOCITY_UPDATE)) {
      velocityUpdateParameter.addNonConfigurableSubParameter("problem", problem);
    }

    var swarmSizeParameter = ((IntegerParameter) parameterSpace.get(SWARM_SIZE));
    var inertiaWeightComputingStrategyParameter =
        ((InertiaWeightComputingParameter) parameterSpace.get(INERTIA_WEIGHT_COMPUTING_STRATEGY));
    if (inertiaWeightComputingStrategyParameter.value().equals(LINEAR_DECREASING_VALUE)
        || inertiaWeightComputingStrategyParameter.value().equals(LINEAR_INCREASING_VALUE)) {
      inertiaWeightComputingStrategyParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / swarmSizeParameter.value());
      inertiaWeightComputingStrategyParameter.addNonConfigurableSubParameter(
          "swarmSize", swarmSizeParameter.value());
    }

    var mutationParameter = (MutationParameter<DoubleSolution>) parameterSpace.get(MUTATION);
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());

    if (mutationParameter.value().equals(parameterSpace.NON_UNIFORM)) {
      mutationParameter.addNonConfigurableSubParameter(
              "maxIterations", maximumNumberOfEvaluations / swarmSize);
    }

    if (mutationParameter.value().equals(UNIFORM)) {
      mutationParameter.addNonConfigurableSubParameter(
          UNIFORM_MUTATION_PERTURBATION, parameterSpace.get(UNIFORM_MUTATION_PERTURBATION));
    }

    var PositionUpdateParameter = (PositionUpdateParameter) parameterSpace.get(POSITION_UPDATE);
    if (PositionUpdateParameter.value().equals(DEFAULT_POSITION_UPDATE)) {
      PositionUpdateParameter.addNonConfigurableSubParameter(
          "positionBounds", problem.variableBounds());
    }
  }

    /**
   * Checks whether the algorithm is configured to use an external archive.
   *
   * @return {@code true} if an external archive is used, {@code false} otherwise
   */
  private boolean usingExternalArchive() {
    return parameterSpace
        .get(ALGORITHM_RESULT)
        .value()
        .equals(EXTERNAL_ARCHIVE);
  }

   /**
   * Creates and configures the external archive if required by the parameter space.
   * The archive size is set according to the current population size.
   *
   * @return the configured external archive, or {@code null} if not used
   */
  protected Archive<DoubleSolution> createExternalArchive() {
    ExternalArchiveParameter<DoubleSolution> externalArchiveParameter =
        (ExternalArchiveParameter<DoubleSolution>) parameterSpace.get(EXTERNAL_ARCHIVE_TYPE);
  
    externalArchiveParameter.setSize(leaderArchiveSize);
    Archive<DoubleSolution> archive = externalArchiveParameter.getExternalArchive();

    return archive;
  }

  @Override
  public BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new MOPSO(
        (DoubleProblem) problem,
        leaderArchiveSize,
        maximumNumberOfEvaluations,
        new MOPSOParameterSpace());
  }

  /**
   * Creates the evaluation component for the algorithm.
   *
   * @return the evaluation component
   */
  protected Evaluation<DoubleSolution> createEvaluation() {
    if (usingExternalArchive()) {
      return new SequentialEvaluationWithArchive<>(problem, externalArchive);
    } else {
      return new SequentialEvaluation<>(problem);
    }
  }

  /**
   * Creates the termination condition for the algorithm. By default, termination is based on the
   * maximum number of evaluations.
   *
   * @return the termination condition
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
            parameterSpace.get(SWARM_INITIALIZATION))
        .getCreateInitialSolutionsStrategy(problem, swarmSize);  }

  /**
   * Creates the strategy for initializing particle velocities.
   *
   * @return the velocity initialization strategy
   */
  protected VelocityInitialization createVelocityInitialization() {
    return ((VelocityInitializationParameter) parameterSpace.get(VELOCITY_INITIALIZATION))
        .getVelocityInitialization();
  }

  /**
   * Creates the strategy for initializing local best solutions.
   *
   * @return the local best initialization strategy
   */
  protected LocalBestInitialization createLocalBestInitialization() {
    return ((LocalBestInitializationParameter) parameterSpace.get(LOCAL_BEST_INITIALIZATION))
        .getLocalBestInitialization();
  }

  /**
   * Creates the strategy for initializing global best solutions.
   *
   * @return the global best initialization strategy
   */
  protected GlobalBestInitialization createGlobalBestInitialization() {
    return ((GlobalBestInitializationParameter) parameterSpace.get(GLOBAL_BEST_INITIALIZATION))
        .getGlobalBestInitialization();
  }

  /**
   * Creates the strategy for computing inertia weight during the search.
   *
   * @return the inertia weight computing strategy
   */
  protected InertiaWeightComputingStrategy createInertiaWeightStrategy() {
    return ((InertiaWeightComputingParameter) parameterSpace.get(INERTIA_WEIGHT_COMPUTING_STRATEGY))
        .getInertiaWeightComputingStrategy();
  }

  /**
   * Creates the operator for updating particle velocities.
   *
   * @return the velocity update operator
   */
  protected VelocityUpdate createVelocityUpdate() {
    return ((VelocityUpdateParameter) parameterSpace.get(VELOCITY_UPDATE))
        .getVelocityUpdate();
  }

  /**
   * Creates the operator for updating particle positions.
   *
   * @return the position update operator
   */
  protected PositionUpdate createPositionUpdate() {
    return ((PositionUpdateParameter) parameterSpace.get(POSITION_UPDATE))
        .getPositionUpdate();
  }

  /**
   * Creates the operator for applying perturbations to solutions.
   *
   * @return the perturbation operator
   */
  protected Perturbation createPerturbation() {
    return ((PerturbationParameter) parameterSpace.get(PERTURBATION))
        .getPerturbation();
  }

  /**
   * Creates the strategy for updating the global best solution.
   *
   * @return the global best update strategy
   */
  protected GlobalBestUpdate createGlobalBestUpdate() {
    return ((GlobalBestUpdateParameter) parameterSpace.get(GLOBAL_BEST_UPDATE))
        .getGlobalBestUpdate();
  }

  /**
   * Creates the strategy for updating local best solutions.
   *
   * @return the local best update strategy
   */
  protected LocalBestUpdate createLocalBestUpdate() {
    return ((LocalBestUpdateParameter) parameterSpace.get(LOCAL_BEST_UPDATE))
        .getLocalBestUpdate(new DefaultDominanceComparator<>());
  }

  /**
   * Creates the strategy for selecting the global best solution.
   *
   * @return the global best selection strategy
   */
  protected GlobalBestSelection createGlobalBestSelection() {
    return ((GlobalBestSelectionParameter) parameterSpace.get(GLOBAL_BEST_SELECTION))
        .getGlobalBestSelection(leaderArchive.comparator());
  }
}
