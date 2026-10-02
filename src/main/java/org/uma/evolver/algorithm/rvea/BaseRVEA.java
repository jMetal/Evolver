package org.uma.evolver.algorithm.rvea;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.algorithm.BaseLevelAlgorithm;
import org.uma.evolver.algorithm.EvolutionaryAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.createinitialsolutionsparameter.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.selectionparameter.SelectionParameter;
import org.uma.evolver.parameter.catalogue.variationparameter.VariationParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RVEAReplacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.rvea.IRVEAEnvironmentalSelection;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.rvea.RVEAEnvironmentalSelection;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.rvea.RVEAStarEnvironmentalSelection;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.RandomSelection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DefaultDominanceComparator;
import org.uma.jmetal.util.errorchecking.Check;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Abstract base class for configurable RVEA (Reference Vector Guided Evolutionary Algorithm)
 * implementations, covering RVEA, RVEA* and iRVEA.
 *
 * <p>Follows the same template-method pattern as {@link org.uma.evolver.algorithm.nsgaii.BaseNSGAII}.
 * The configurable components, drawn from a {@link ParameterSpace}, are the initialisation
 * strategy, the variation (crossover and mutation), the mating selection ({@code random} or
 * {@code tournament}) and the replacement, whose value selects the variant: {@code rvea},
 * {@code rveaStar} or {@code iRVEA}, with the global sub-parameters {@code alpha} and {@code fr}
 * and, for iRVEA, {@code numberOfSubregions}, {@code lateStageFraction} and {@code epsilonKappa}.
 * The names and values are those of jMetal's {@code AutoRVEA}.
 *
 * <p>The population size must equal the number of reference vectors. They are given either as a
 * list, or as a directory of weight vector files ({@code W<objectives>D_<populationSize>.dat}, as
 * in MOEA/D), read for each problem; the second form allows training sets whose problems have
 * different numbers of objectives. RVEA requires unconstrained problems and termination by number
 * of evaluations.
 *
 * @param <S> the type of solutions handled by this algorithm
 * @see org.uma.evolver.algorithm.nsgaii.BaseNSGAII
 */
public abstract class BaseRVEA<S extends Solution<?>> implements BaseLevelAlgorithm<S> {

  protected final ParameterSpace parameterSpace;
  protected Problem<S> problem;
  protected int populationSize;
  protected int maximumNumberOfEvaluations;
  protected final List<double[]> referenceVectors;
  protected final String weightVectorFilesDirectory;

  /**
   * Creates an RVEA with a given list of reference vectors.
   *
   * @param problem the problem to solve (unconstrained)
   * @param populationSize the population size, equal to the number of reference vectors
   * @param maximumNumberOfEvaluations the evaluation budget
   * @param parameterSpace the parameter space
   * @param referenceVectors the reference vectors, one per solution of the population
   */
  protected BaseRVEA(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      ParameterSpace parameterSpace,
      List<double[]> referenceVectors) {
    Check.notNull(problem, "problem");
    Check.notNull(referenceVectors, "referenceVectors");
    Check.valueIsPositive(maximumNumberOfEvaluations, "maximumNumberOfEvaluations");
    Check.that(
        populationSize == referenceVectors.size(),
        "populationSize must equal the number of reference vectors ("
            + referenceVectors.size()
            + ")");
    this.problem = problem;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.referenceVectors = referenceVectors;
    this.weightVectorFilesDirectory = null;
    this.parameterSpace = checkedParameterSpace(parameterSpace);
    this.populationSize = checkedPopulationSize(populationSize);
  }

  /**
   * Creates an RVEA whose reference vectors are read, for each problem, from a directory of weight
   * vector files ({@code W<objectives>D_<populationSize>.dat}). The problem and the evaluation
   * budget are given later, through {@link #createInstance}.
   *
   * @param populationSize the population size, equal to the number of vectors of each file
   * @param weightVectorFilesDirectory the directory of the weight vector files
   * @param parameterSpace the parameter space
   */
  protected BaseRVEA(
      int populationSize, String weightVectorFilesDirectory, ParameterSpace parameterSpace) {
    Check.notNull(weightVectorFilesDirectory, "weightVectorFilesDirectory");
    this.referenceVectors = null;
    this.weightVectorFilesDirectory = weightVectorFilesDirectory;
    this.parameterSpace = checkedParameterSpace(parameterSpace);
    this.populationSize = checkedPopulationSize(populationSize);
  }

  /**
   * Creates an RVEA for a problem, with its reference vectors read from a directory of weight
   * vector files.
   */
  protected BaseRVEA(
      Problem<S> problem,
      int populationSize,
      int maximumNumberOfEvaluations,
      String weightVectorFilesDirectory,
      ParameterSpace parameterSpace) {
    this(populationSize, weightVectorFilesDirectory, parameterSpace);
    Check.notNull(problem, "problem");
    Check.valueIsPositive(maximumNumberOfEvaluations, "maximumNumberOfEvaluations");
    this.problem = problem;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
  }

  private static ParameterSpace checkedParameterSpace(ParameterSpace parameterSpace) {
    Check.notNull(parameterSpace, "parameterSpace");
    return parameterSpace;
  }

  private static int checkedPopulationSize(int populationSize) {
    Check.valueIsPositive(populationSize, "populationSize");
    return populationSize;
  }

  @Override
  public ParameterSpace parameterSpace() {
    return parameterSpace;
  }

  /**
   * Builds the configured variant. With {@code algorithmResult} = {@code externalArchive}, as in
   * every other algorithm of Evolver, the archive is fed with every evaluated solution (through a
   * {@link SequentialEvaluationWithArchive}) and is the result of the run.
   */
  @Override
  public EvolutionaryAlgorithm<S> build() {
    Check.notNull(problem, "problem");
    Check.that(problem.numberOfConstraints() == 0, "RVEA requires an unconstrained problem");
    Check.that(
        maximumNumberOfEvaluations >= populationSize,
        "The budget must cover the initial population");
    setNonConfigurableParameters();

    Archive<S> archive = usingExternalArchive() ? createExternalArchive() : null;
    SolutionsCreation<S> initialSolutions = createInitialSolutions();
    Variation<S> variation = createVariation();
    Selection<S> selection = createSelection(variation.matingPoolSize());
    RVEAEnvironmentalSelection<S> environmentalSelection =
        createEnvironmentalSelection(variation.offspringPopulationSize());
    Evaluation<S> evaluation =
        archive == null
            ? new SequentialEvaluation<>(problem)
            : new SequentialEvaluationWithArchive<>(problem, archive);

    return new EvolutionaryAlgorithmBuilder<S>()
        .build(
            algorithmName(),
            initialSolutions,
            evaluation,
            new TerminationByEvaluations(maximumNumberOfEvaluations),
            selection,
            variation,
            new RVEAReplacement<>(environmentalSelection),
            archive);
  }

  /** The name of the configured variant: RVEA, RVEA* or iRVEA. */
  protected String algorithmName() {
    return switch (replacementVariant()) {
      case "rvea" -> "RVEA";
      case "rveaStar" -> "RVEA*";
      case "iRVEA" -> "iRVEA";
      default -> throw new JMetalException("RVEA replacement unknown: " + replacementVariant());
    };
  }

  private String replacementVariant() {
    return (String) parameterSpace.get("replacement").value();
  }

  /**
   * Creates the environmental selection of the configured variant. The maximum number of
   * generations, which the selection needs to schedule the adaptation of the reference vectors,
   * follows from the budget and the offspring population size.
   */
  protected RVEAEnvironmentalSelection<S> createEnvironmentalSelection(int offspringPopulationSize) {
    Check.that(offspringPopulationSize > 0, "The offspring population size must be positive");
    int numberOfObjectives = problem.numberOfObjectives();
    int maxGenerations =
        Math.max(
            1,
            (int)
                Math.ceil(
                    (double) (maximumNumberOfEvaluations - populationSize)
                        / offspringPopulationSize));
    Parameter<?> replacement = parameterSpace.get("replacement");
    double alpha = (double) replacement.findGlobalSubParameter("alpha").value();
    double fr = (double) replacement.findGlobalSubParameter("fr").value();
    List<double[]> vectors = checkedReferenceVectors(numberOfObjectives, alpha, fr);

    return switch (replacementVariant()) {
      case "rvea" ->
          new RVEAEnvironmentalSelection<>(numberOfObjectives, maxGenerations, alpha, fr, vectors);
      case "rveaStar" ->
          new RVEAStarEnvironmentalSelection<>(
              numberOfObjectives, maxGenerations, alpha, fr, vectors);
      case "iRVEA" ->
          new IRVEAEnvironmentalSelection<>(
              numberOfObjectives,
              maxGenerations,
              alpha,
              fr,
              vectors,
              (int) replacement.findConditionalParameter("numberOfSubregions").value(),
              (double) replacement.findConditionalParameter("lateStageFraction").value(),
              (double) replacement.findConditionalParameter("epsilonKappa").value());
      default -> throw new JMetalException("RVEA replacement unknown: " + replacementVariant());
    };
  }

  /**
   * The reference vectors as RVEA uses them: validated and normalized by an environmental
   * selection, exactly as jMetal's {@code RVEABuilder} does, so that both build the same algorithm.
   */
  private List<double[]> checkedReferenceVectors(int numberOfObjectives, double alpha, double fr) {
    List<double[]> vectors = rawReferenceVectors(numberOfObjectives);
    Check.that(
        vectors.size() == populationSize,
        "Population size must match the number of reference vectors (" + vectors.size() + ")");
    return List.of(
        new RVEAEnvironmentalSelection<S>(numberOfObjectives, 1, alpha, fr, vectors)
            .referenceVectors());
  }

  private List<double[]> rawReferenceVectors(int numberOfObjectives) {
    if (referenceVectors != null) {
      return referenceVectors;
    }
    String fileName =
        weightVectorFilesDirectory + "/W" + numberOfObjectives + "D_" + populationSize + ".dat";
    try {
      return Arrays.asList(VectorUtils.readVectors(fileName));
    } catch (IOException exception) {
      throw new JMetalException("Error reading the weight vector file " + fileName, exception);
    }
  }

  private boolean usingExternalArchive() {
    return parameterSpace.get("algorithmResult").value().equals("externalArchive");
  }

  @SuppressWarnings("unchecked")
  protected Archive<S> createExternalArchive() {
    ExternalArchiveParameter<S> archiveParameter =
        (ExternalArchiveParameter<S>) parameterSpace.get("archiveType");
    archiveParameter.setSize(populationSize);
    return archiveParameter.getExternalArchive();
  }

  /**
   * Sets parameters that are fixed or derived from the problem (e.g. number of variables for
   * mutation operators). Called at the start of {@link #build()}.
   */
  protected abstract void setNonConfigurableParameters();

  /** The initial population, checked to have exactly one solution per reference vector. */
  @SuppressWarnings("unchecked")
  protected SolutionsCreation<S> createInitialSolutions() {
    SolutionsCreation<S> creation =
        ((CreateInitialSolutionsParameter<S>) parameterSpace.get("createInitialSolutions"))
            .getCreateInitialSolutionsStrategy(problem, populationSize);
    return () -> {
      List<S> population = creation.create();
      Check.notNull(population);
      Check.that(
          population.size() == populationSize,
          "The initial population must contain one solution per reference vector");
      return population;
    };
  }

  @SuppressWarnings("unchecked")
  protected Variation<S> createVariation() {
    VariationParameter<S> variationParameter =
        (VariationParameter<S>) parameterSpace.get("variation");
    variationParameter.addNonConfigurableSubParameter(
        "offspringPopulationSize", parameterSpace.get("offspringPopulationSize").value());
    return variationParameter.getVariation();
  }

  /**
   * The mating selection. RVEA populations can shrink below the tournament size (empty niches
   * contribute no survivor), so tournament selection falls back to random selection then, as in
   * jMetal's {@code AutoRVEA}.
   */
  @SuppressWarnings("unchecked")
  protected Selection<S> createSelection(int matingPoolSize) {
    SelectionParameter<S> selectionParameter =
        (SelectionParameter<S>) parameterSpace.get("selection");
    Selection<S> selection =
        selectionParameter.getSelection(matingPoolSize, new DefaultDominanceComparator<>());
    if (!selectionParameter.value().equals("tournament")) {
      return selection;
    }
    int tournamentSize = (int) parameterSpace.get("selectionTournamentSize").value();
    Selection<S> random = new RandomSelection<>(matingPoolSize);
    return population ->
        population.size() < tournamentSize ? random.select(population) : selection.select(population);
  }
}
