package org.uma.evolver.algorithm.base;

import java.util.List;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
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
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.archive.BoundedArchive;

/**
 * A builder class for creating instances of {@link ParticleSwarmOptimizationAlgorithm} with optional archive support.
 * This builder provides a flexible way to configure and instantiate Particle Swarm Optimization algorithms,
 * with or without an external archive for storing non-dominated solutions.
 *
 * <p>Example usage:
 * <pre>{@code
 * Algorithm<List<DoubleSolution>> algorithm = new ParticleSwarmOptimizationBuilder()
 *     .build(
 *         "MOPSO",
 *         initialSwarmCreation,
 *         evaluation,
 *         termination,
 *         velocityInitialization,
 *         localBestInitialization,
 *         globalBestInitialization,
 *         inertiaWeightStrategy,
 *         velocityUpdate,
 *         positionUpdate,
 *         perturbation,
 *         globalBestUpdate,
 *         localBestUpdate,
 *         globalBestSelection,
 *         globalBestArchive,
 *         externalArchive  // can be null if no external archive is needed
 *     );
 * }</pre>
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ParticleSwarmOptimizationBuilder {
  /**
   * Builds an instance of ParticleSwarmOptimizationAlgorithm with the specified components.
   *
   * @param name the name of the algorithm (used for identification and logging)
   * @param solutionsCreation component responsible for creating the initial swarm
   * @param evaluation component that handles solution evaluation
   * @param termination condition that determines when the algorithm should stop
   * @param velocityInitialization strategy for initializing particle velocities
   * @param localBestInitialization strategy for initializing local best solutions
   * @param globalBestInitialization strategy for initializing global best solutions
   * @param inertiaWeightStrategy strategy for computing inertia weight during the search
   * @param velocityUpdate operator for updating particle velocities
   * @param positionUpdate operator for updating particle positions
   * @param perturbation operator for applying perturbations to solutions
   * @param globalBestUpdate strategy for updating the global best solution
   * @param localBestUpdate strategy for updating local best solutions
   * @param globalBestSelection strategy for selecting the global best solution
   * @param globalBestArchive archive for storing global best solutions
   * @param externalArchive optional external archive for storing non-dominated solutions (can be null)
   * @return a configured instance of ParticleSwarmOptimizationAlgorithm
   * @throws NullPointerException if any required parameter is null (except externalArchive)
   */
  public ParticleSwarmOptimizationAlgorithm build(
      String name,
      SolutionsCreation<DoubleSolution> solutionsCreation,
      Evaluation<DoubleSolution> evaluation,
      Termination termination,
      VelocityInitialization velocityInitialization,
      LocalBestInitialization localBestInitialization,
      GlobalBestInitialization globalBestInitialization,
      InertiaWeightComputingStrategy inertiaWeightStrategy,
      VelocityUpdate velocityUpdate,
      PositionUpdate positionUpdate,
      Perturbation perturbation,
      GlobalBestUpdate globalBestUpdate,
      LocalBestUpdate localBestUpdate,
      GlobalBestSelection globalBestSelection,
      BoundedArchive<DoubleSolution> globalBestArchive,
      Archive<DoubleSolution> externalArchive) {
    if (externalArchive == null) {
      return new ParticleSwarmOptimizationAlgorithm(
          name,
          solutionsCreation,
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
          globalBestArchive);
    } else {
      return new ParticleSwarmOptimizationAlgorithmWithArchive(
          name,
          solutionsCreation,
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
          globalBestArchive,
          externalArchive);
    }
  }

  class ParticleSwarmOptimizationAlgorithmWithArchive extends ParticleSwarmOptimizationAlgorithm {

    private Archive<DoubleSolution> externalArchive;

    /** Constructor */
    public ParticleSwarmOptimizationAlgorithmWithArchive(
        String name,
        SolutionsCreation<DoubleSolution> createInitialSwarm,
        Evaluation<DoubleSolution> evaluation,
        Termination termination,
        VelocityInitialization velocityInitialization,
        LocalBestInitialization localBestInitialization,
        GlobalBestInitialization globalBestInitialization,
        InertiaWeightComputingStrategy inertiaWeightComputingStrategy,
        VelocityUpdate velocityUpdate,
        PositionUpdate positionUpdate,
        Perturbation perturbation,
        GlobalBestUpdate globalBestUpdate,
        LocalBestUpdate localBestUpdate,
        GlobalBestSelection globalBestSelection,
        BoundedArchive<DoubleSolution> leaderArchive,
        Archive<DoubleSolution> externalArchive) {
      super(
          name,
          createInitialSwarm,
          evaluation,
          termination,
          velocityInitialization,
          localBestInitialization,
          globalBestInitialization,
          inertiaWeightComputingStrategy,
          velocityUpdate,
          positionUpdate,
          perturbation,
          globalBestUpdate,
          localBestUpdate,
          globalBestSelection,
          leaderArchive);
      this.externalArchive = externalArchive;
    }

    @Override
    public List<DoubleSolution> result() {
      return externalArchive.solutions();
    }
  }
}
