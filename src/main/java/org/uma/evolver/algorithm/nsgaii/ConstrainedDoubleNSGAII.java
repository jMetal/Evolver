package org.uma.evolver.algorithm.nsgaii;

import java.util.Arrays;
import java.util.Comparator;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.ConstraintHandlingParameter;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.util.constrainthandling.ConstrainedBoundedArchives;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.ConstraintHandlingReplacement;
import org.uma.jmetal.component.util.RunProgress;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.constrainthandling.ConstraintHandlingTechnique;

/**
 * The variant of {@link DoubleNSGAII} for problems with constraints: NSGA-II with a
 * constraint-handling technique of jMetal, chosen by the {@code constraintHandling} parameter of its
 * parameter space ({@code NSGAIIDoubleConstrained.yaml}).
 *
 * <p>The technique gives the ranking that the replacement and the mating selection sort the
 * population with, and is updated once per generation with the progress of the run (jMetal's
 * {@link ConstraintHandlingReplacement} and {@link RunProgress}), so that the techniques that change
 * during the run (epsilonConstrained, pushPull) follow it. The bounded external archives compare
 * solutions with the constrained-domination principle, whatever the technique (see {@link
 * ConstrainedBoundedArchives}).
 *
 * <p>{@link DoubleNSGAII}, for problems without constraints, does not change: it ignores the
 * constraints.
 *
 * <pre>{@code
 * var parameterSpace =
 *     new YAMLParameterSpace("NSGAIIDoubleConstrained.yaml", new DoubleParameterFactory());
 * var algorithm = new ConstrainedDoubleNSGAII(problem, 100, 25000, parameterSpace);
 * algorithm.parse(configuration);
 * algorithm.build().run();
 * }</pre>
 */
public class ConstrainedDoubleNSGAII extends DoubleNSGAII {

  /** The constraint-handling technique, created when the algorithm is built. */
  protected ConstraintHandlingTechnique<DoubleSolution> technique;

  /** The progress of the run, which the technique follows. */
  protected RunProgress progress;

  /**
   * Creates an instance with a population size and a parameter space, to be completed by {@link
   * #createInstance}.
   *
   * @param populationSize the population size
   * @param parameterSpace the parameter space, which must have the {@code constraintHandling}
   *     parameter
   */
  public ConstrainedDoubleNSGAII(int populationSize, ParameterSpace parameterSpace) {
    super(populationSize, parameterSpace);
  }

  /**
   * Creates an instance.
   *
   * @param problem the problem to solve
   * @param populationSize the population size
   * @param maximumNumberOfEvaluations the maximum number of evaluations
   * @param parameterSpace the parameter space, which must have the {@code constraintHandling}
   *     parameter
   */
  public ConstrainedDoubleNSGAII(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace);
  }

  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new ConstrainedDoubleNSGAII(
        problem, populationSize, maximumNumberOfEvaluations, parameterSpace.createInstance());
  }

  @Override
  public EvolutionaryAlgorithm<DoubleSolution> build() {
    progress = new RunProgress();
    EvolutionaryAlgorithm<DoubleSolution> algorithm = super.build();
    progress.bind(algorithm::termination, algorithm::attributes);
    return algorithm;
  }

  /**
   * Sets the parameters of {@link DoubleNSGAII} and the constraint-handling technique, whose
   * ranking replaces the default one in the replacement and the mating selection.
   */
  @Override
  protected void setNonConfigurableParameters() {
    super.setNonConfigurableParameters();
    technique = ConstraintHandlingParameter.technique(parameterSpace);
    ranking = technique.ranking();
    rankingAndCrowdingComparator =
        new MultiComparator<>(
            Arrays.asList(
                Comparator.comparing(ranking::getRank),
                Comparator.comparing(densityEstimator::value).reversed()));
  }

  /** The replacement of NSGA-II, which updates the technique before replacing. */
  @Override
  protected Replacement<DoubleSolution> createReplacement() {
    return new ConstraintHandlingReplacement<>(technique, super.createReplacement(), progress);
  }

  /** The external archive, with the constrained-domination principle. */
  @Override
  @SuppressWarnings("unchecked")
  protected Archive<DoubleSolution> createExternalArchive() {
    var parameter = (ExternalArchiveParameter<DoubleSolution>) parameterSpace.get("archiveType");
    return switch (parameter.value()) {
      case "crowdingDistanceArchive" -> ConstrainedBoundedArchives.crowdingDistance(populationSize);
      case "hypervolumeArchive" -> ConstrainedBoundedArchives.hypervolume(populationSize);
      case "spatialSpreadDeviationArchive" ->
          ConstrainedBoundedArchives.spatialSpreadDeviation(populationSize);
      case "knnDistanceArchive" ->
          ConstrainedBoundedArchives.knnDistance(
              populationSize,
              (Integer) parameter.findConditionalParameter("knnDistanceArchiveK").value());
      case "angleArchive" -> ConstrainedBoundedArchives.angle(populationSize, true);
      // The unbounded archive of jMetal already uses the constrained-domination principle
      default -> super.createExternalArchive();
    };
  }
}
