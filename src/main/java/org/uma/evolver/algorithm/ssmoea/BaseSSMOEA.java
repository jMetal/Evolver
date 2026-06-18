package org.uma.evolver.algorithm.ssmoea;

import java.util.Arrays;
import java.util.Comparator;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.DensityEstimatorParameter;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.RankingParameter;
import org.uma.evolver.parameter.catalogue.ReplacementParameter;
import org.uma.evolver.parameter.catalogue.SequenceGeneratorParameter;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.util.sequencegenerator.SequenceGenerator;
import org.uma.jmetal.util.sequencegenerator.impl.RandomPermutationCycle;

/**
 * Abstract base class for the Steady-State MOEA (SSMOEA), a generalized steady-state
 * multi-objective evolutionary algorithm with configurable variation, selection, and replacement.
 *
 * <p>The offspring population size is fixed to 1 (non-configurable). The variation branch
 * determines which selection is used: {@code crossoverAndMutationVariation} uses {@code gaSelection},
 * while {@code differentialEvolutionVariation} uses {@link DifferentialEvolutionSelection} with a
 * configurable sequence generator shared with the replacement when
 * {@code singleSolutionReplacement} is active.
 *
 * @param <S> the solution type
 */
@SuppressWarnings("unchecked")
public abstract class BaseSSMOEA<S extends Solution<?>> implements BaseLevelAlgorithm<S> {
  private static final String DIFFERENTIAL_EVOLUTION_VARIATION = "differentialEvolutionVariation";
  private static final String SINGLE_SOLUTION_REPLACEMENT = "singleSolutionReplacement";

  protected final ParameterSpace parameterSpace;
  protected Ranking<S> ranking;
  protected DensityEstimator<S> densityEstimator;
  protected MultiComparator<S> rankingAndCrowdingComparator;
  protected Problem<S> problem;
  protected int populationSize;
  protected int maximumNumberOfEvaluations;

  protected BaseSSMOEA(int populationSize, ParameterSpace parameterSpace) {
    this.parameterSpace = parameterSpace;
    this.populationSize = populationSize;
  }

  protected BaseSSMOEA(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace) {
    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.parameterSpace = parameterSpace;
    ranking = new FastNonDominatedSortRanking<>();
    densityEstimator = new CrowdingDistanceDensityEstimator<>();
    rankingAndCrowdingComparator = new MultiComparator<>(
        Arrays.asList(
            Comparator.comparing(ranking::getRank),
            Comparator.comparing(densityEstimator::value).reversed()));
  }

  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  @Override
  public EvolutionaryAlgorithm<S> build() {
    setRankingAndDensityEstimator();
    setNonConfigurableParameters();

    Archive<S> archive = null;
    if (usingExternalArchive()) {
      archive = createExternalArchive();
      updatePopulationSize(archive);
    }

    SolutionsCreation<S> initialSolutionsCreation = createInitialSolutions();

    boolean usingDE = DIFFERENTIAL_EVOLUTION_VARIATION.equals(
        parameterSpace.get("variation").value());

    // Shared sequence generator: drives DE variation, DE selection, and single-solution replacement
    // in lockstep (getValue() reads without advancing; generateNext() is called by one component).
    SequenceGenerator<Integer> deSequenceGenerator = null;
    if (usingDE) {
      SequenceGeneratorParameter seqGenParam =
          (SequenceGeneratorParameter) parameterSpace.get("sequenceGenerator");
      seqGenParam.sequenceLength(populationSize);
      deSequenceGenerator = seqGenParam.getSequenceGenerator();
    }

    Variation<S> variation = createVariation(usingDE, deSequenceGenerator);
    Selection<S> selection = createSelection(variation, usingDE, deSequenceGenerator);
    Evaluation<S> evaluation = createEvaluation(archive);
    Replacement<S> replacement = createReplacement(usingDE, deSequenceGenerator);
    Termination termination = createTermination();

    return new EvolutionaryAlgorithmBuilder<S>().build(
        "SSMOEA",
        initialSolutionsCreation,
        evaluation,
        termination,
        selection,
        variation,
        replacement,
        archive);
  }

  protected abstract void setNonConfigurableParameters();

  private void setRankingAndDensityEstimator() {
    ranking = ((RankingParameter<S>) parameterSpace.get("ranking")).getRanking();
    densityEstimator =
        ((DensityEstimatorParameter<S>) parameterSpace.get("densityEstimator")).getDensityEstimator();
    rankingAndCrowdingComparator = new MultiComparator<>(
        Arrays.asList(
            Comparator.comparing(ranking::getRank),
            Comparator.comparing(densityEstimator::value).reversed()));
  }

  private boolean usingExternalArchive() {
    return parameterSpace.get("algorithmResult").value().equals("externalArchive");
  }

  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> externalArchiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("archiveType");
    externalArchiveParameter.setSize(populationSize);
    return externalArchiveParameter.getExternalArchive();
  }

  private void updatePopulationSize(Archive<S> archive) {
    if (archive != null) {
      populationSize = (int) parameterSpace.get("populationSizeWithArchive").value();
    }
  }

  protected SolutionsCreation<S> createInitialSolutions() {
    return ((CreateInitialSolutionsParameter<S>) parameterSpace.get("createInitialSolutions"))
        .getCreateInitialSolutionsStrategy(problem, populationSize);
  }

  protected Termination createTermination() {
    return new TerminationByEvaluations(maximumNumberOfEvaluations);
  }

  protected Evaluation<S> createEvaluation(Archive<S> archive) {
    if (usingExternalArchive()) {
      return new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      return new SequentialEvaluation<>(problem);
    }
  }

  protected Variation<S> createVariation(boolean usingDE, SequenceGenerator<Integer> sequenceGenerator) {
    VariationParameter<S> variationParameter =
        (VariationParameter<S>) parameterSpace.get("variation");
    variationParameter.addNonConfigurableSubParameter("offspringPopulationSize", 1);
    if (usingDE) {
      variationParameter.addNonConfigurableSubParameter("subProblemIdGenerator", sequenceGenerator);
    }
    return variationParameter.getVariation();
  }

  protected Selection<S> createSelection(
      Variation<S> variation, boolean usingDE, SequenceGenerator<Integer> sequenceGenerator) {
    if (usingDE) {
      boolean takeCurrentSolutionAsParent = "true".equals(
          (String) parameterSpace.get("takeCurrentSolutionAsParent").value());
      // DifferentialEvolutionSelection implements Selection<DoubleSolution>; safe for DoubleSSMOEA
      return (Selection<S>) new DifferentialEvolutionSelection(
          populationSize, variation.matingPoolSize(), takeCurrentSolutionAsParent, sequenceGenerator);
    } else {
      SelectionParameter<S> gaSelectionParameter =
          (SelectionParameter<S>) parameterSpace.get("gaSelection");
      return gaSelectionParameter.getSelection(variation.matingPoolSize(), rankingAndCrowdingComparator);
    }
  }

  protected Replacement<S> createReplacement(
      boolean usingDE, SequenceGenerator<Integer> deSequenceGenerator) {
    ReplacementParameter<S> replacementParameter =
        (ReplacementParameter<S>) parameterSpace.get("replacement");
    replacementParameter.setRanking(ranking);
    replacementParameter.setDensityEstimator(densityEstimator);

    if (SINGLE_SOLUTION_REPLACEMENT.equals(replacementParameter.value())) {
      // Dominance comparator for DEMO-style one-to-one comparison (no pre-computed ranking needed)
      replacementParameter.setComparator(new DefaultDominanceComparator<>());
      if (usingDE) {
        // Share the DE sequence generator so replacement competes offspring against the same
        // population index used by variation and selection.
        replacementParameter.setSequenceGenerator(deSequenceGenerator);
      } else {
        replacementParameter.setSequenceGenerator(new RandomPermutationCycle(populationSize));
      }
    }
    return replacementParameter.getReplacement();
  }
}
