package org.uma.evolver.algorithm.rvea;

import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.mutationparameter.MutationParameter;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.RVEABuilder;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.errorchecking.Check;

/**
 * Configurable RVEA for continuous (real-valued) optimization problems.
 *
 * <p>Extends {@link BaseRVEA} for {@link DoubleSolution}. The initialisation strategy and
 * variation operator (crossover + mutation) are drawn from a {@link ParameterSpace}; RVEA's
 * selection and replacement remain fixed. An optional external archive can be enabled via the
 * {@code algorithmResult} parameter in the YAML parameter space.
 *
 * <p>The population size must match the number of reference vectors supplied, as required by
 * {@link RVEABuilder}.
 *
 * @see BaseRVEA
 */
public class DoubleRVEA extends BaseRVEA<DoubleSolution> {

  public DoubleRVEA(
      Problem<DoubleSolution> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      double alpha,
      double fr,
      List<double[]> referenceVectors) {
    super(problem, populationSize, maximumNumberOfEvaluations, parameterSpace, alpha, fr,
        referenceVectors);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void setNonConfigurableParameters() {
    var mutationParameter = (MutationParameter<DoubleSolution>) parameterSpace.get("mutation");
    Check.notNull(mutationParameter);
    mutationParameter.addNonConfigurableSubParameter(
        "numberOfProblemVariables", problem.numberOfVariables());
    Check.that(maximumNumberOfEvaluations > 0,
        "Maximum number of evaluations must be greater than 0");
    Check.that(populationSize > 0, "Population size must be greater than 0");
    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableSubParameter(
          "maxIterations", maximumNumberOfEvaluations / populationSize);
    }
  }

  @Override
  protected Algorithm<List<DoubleSolution>> buildRVEA(
      SolutionsCreation<DoubleSolution> init,
      Variation<DoubleSolution> variation,
      Archive<DoubleSolution> archive) {

    // SBX and polynomial are placeholder operators required by the RVEABuilder constructor;
    // setVariation() replaces them with the configured variation immediately after.
    EvolutionaryAlgorithm<DoubleSolution> rvea = new RVEABuilder<>(
            problem, populationSize, maximumNumberOfEvaluations,
            new SBXCrossover(0.9, 20.0),
            new PolynomialMutation(1.0 / problem.numberOfVariables(), 20.0),
            alpha, fr, referenceVectors)
        .setCreateInitialPopulation(init)
        .setVariation(variation)
        .build();

    if (archive != null) {
      // The archive is populated from the final population after the run, not from every
      // intermediate evaluation. Using SequentialEvaluationWithArchive would add all evaluated
      // solutions (including those from early generations on local Pareto fronts) to the archive,
      // which for multi-modal problems like DTLZ3 produces a large, low-quality archive.
      // Populating from the final population ensures the archive only reflects solutions that
      // survived RVEA's APD-based replacement.
      return new Algorithm<>() {
        @Override
        public void run() {
          rvea.run();
          rvea.result().forEach(s -> archive.add((DoubleSolution) s.copy()));
        }
        @Override public List<DoubleSolution> result() { return archive.solutions(); }
        @Override public String name() { return rvea.name(); }
        @Override public String description() { return rvea.description(); }
      };
    }
    return rvea;
  }

  @Override
  public synchronized BaseLevelAlgorithm<DoubleSolution> createInstance(
      Problem<DoubleSolution> problem, int maximumNumberOfEvaluations) {
    return new DoubleRVEA(problem, populationSize, maximumNumberOfEvaluations,
        parameterSpace.createInstance(), alpha, fr, referenceVectors);
  }
}
