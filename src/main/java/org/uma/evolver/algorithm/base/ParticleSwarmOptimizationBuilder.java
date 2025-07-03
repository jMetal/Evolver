package org.uma.evolver.algorithm.base;

import java.util.List;
import java.util.Objects;
import org.uma.jmetal.algorithm.Algorithm;
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
 * A builder class for creating instances of {@link Algorithm} that implement Particle Swarm
 * Optimization (PSO). This builder provides a flexible way to configure and instantiate PSO
 * algorithms with various components.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Algorithm<List<DoubleSolution>> algorithm = new ParticleSwarmOptimizationBuilder<DoubleSolution>()
 *     .setName("MOPSO")
 *     .setSolutionsCreation(initialSwarmCreation)
 *     .setEvaluation(evaluation)
 *     .setTermination(termination)
 *     .setVelocityInitialization(velocityInitialization)
 *     .setLocalBestInitialization(localBestInitialization)
 *     .setGlobalBestInitialization(globalBestInitialization)
 *     .setInertiaWeightStrategy(inertiaWeightStrategy)
 *     .setVelocityUpdate(velocityUpdate)
 *     .setPositionUpdate(positionUpdate)
 *     .setPerturbation(perturbation)
 *     .setGlobalBestUpdate(globalBestUpdate)
 *     .setLocalBestUpdate(localBestUpdate)
 *     .setGlobalBestSelection(globalBestSelection)
 *     .setGlobalBestArchive(globalBestArchive)
 *     .build();
 * }</pre>
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class ParticleSwarmOptimizationBuilder {
  /**
   * Builds and returns a configured PSO algorithm instance.
   *
   * @return a configured PSO algorithm
   * @throws NullPointerException if any required component is not set
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
    if (externalArchive != null) {
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
