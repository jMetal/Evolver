package org.uma.evolver.configurablealgorithm.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter.catalogue.CrossoverParameter;
import org.uma.evolver.parameter.catalogue.DifferentialEvolutionCrossoverParameter;
import org.uma.evolver.parameter.catalogue.DifferentialEvolutionSelectionParameter;
import org.uma.evolver.parameter.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter.catalogue.MutationParameter;
import org.uma.evolver.parameter.catalogue.ProbabilityParameter;
import org.uma.evolver.parameter.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter.catalogue.SelectionParameter;
import org.uma.evolver.parameter.catalogue.VariationParameter;
import org.uma.evolver.parameter.impl.CategoricalIntegerParameter;
import org.uma.evolver.parameter.impl.CategoricalParameter;
import org.uma.evolver.parameter.impl.IntegerParameter;
import org.uma.evolver.parameter.impl.RealParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.DifferentialEvolutionSelection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.component.util.RankingAndDensityEstimatorPreference;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.dominanceComparator.impl.DominanceWithConstraintsComparator;
import org.uma.jmetal.util.densityestimator.DensityEstimator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.ranking.Ranking;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;
import org.uma.jmetal.util.sequencegenerator.impl.IntegerPermutationGenerator;

/**
 * Class to configure a differential evolution variant of  NSGA-II with an argument string using
 * class {@link EvolutionaryAlgorithm}
 *
 * @author Antonio J. Nebro
 */
public class ConfigurableNSGAIIDE implements ConfigurableAlgorithmBuilder<DoubleProblem> {

  private List<Parameter<?>> configurableParameterList = new ArrayList<>();
  private CategoricalParameter algorithmResultParameter;
  private ExternalArchiveParameter<DoubleSolution> externalArchiveParameter;
  private IntegerParameter populationSizeWithArchiveParameter;
  private CategoricalIntegerParameter offspringPopulationSizeParameter;
  private CreateInitialSolutionsParameter createInitialSolutionsParameter;
  private SelectionParameter<DoubleSolution> selectionParameter;
  private VariationParameter variationParameter;

  @Override
  public List<Parameter<?>> configurableParameterList() {
    return configurableParameterList;
  }

  private DoubleProblem problem;
  private int populationSize;
  private int maximumNumberOfEvaluations;
  private DifferentialEvolutionCrossoverParameter deCrossoverParameter ;

  public ConfigurableNSGAIIDE() {
    this.configure() ;
  }

  public ConfigurableNSGAIIDE(DoubleProblem problem, int populationSize,
      int maximumNumberOfEvaluations) {
    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.configure();
  }

  @Override
  public ConfigurableAlgorithmBuilder createBuilderInstance() {
    return new ConfigurableNSGAIIDE(problem, populationSize, maximumNumberOfEvaluations);
  }

  public ConfigurableAlgorithmBuilder createBuilderInstance(DoubleProblem problem,
      int maximumNumberOfEvaluations) {
    return new ConfigurableNSGAIIDE(problem, populationSize, maximumNumberOfEvaluations);
  }

  private void configure() {
    offspringPopulationSizeParameter = new CategoricalIntegerParameter("offspringPopulationSize",
        List.of(1, 2, 5, 10, 20, 50, 100, 200, 400));

    algorithmResult();
    createInitialSolution();
    variation();

    configurableParameterList.add(algorithmResultParameter);
    configurableParameterList.add(createInitialSolutionsParameter);
    configurableParameterList.add(offspringPopulationSizeParameter);
    configurableParameterList.add(variationParameter);
  }

  private void variation() {
    deCrossoverParameter =
        new DifferentialEvolutionCrossoverParameter(List.of("RAND_1_BIN", "RAND_1_EXP", "RAND_2_BIN"));

    RealParameter crParameter = new RealParameter("CR", 0.0, 1.0);
    RealParameter fParameter = new RealParameter("F", 0.0, 1.0);
    deCrossoverParameter.addGlobalParameter(crParameter);
    deCrossoverParameter.addGlobalParameter(fParameter);

    MutationParameter mutationParameter =
        new MutationParameter(
            Arrays.asList("uniform", "polynomial", "linkedPolynomial", "nonUniform"));

    RealParameter mutationProbabilityFactor = new RealParameter("mutationProbabilityFactor",
        0.0, 2.0);
    mutationParameter.addGlobalParameter(mutationProbabilityFactor);
    RepairDoubleSolutionStrategyParameter mutationRepairStrategy =
        new RepairDoubleSolutionStrategyParameter(
            "mutationRepairStrategy", Arrays.asList("random", "round", "bounds"));
    mutationParameter.addGlobalParameter(mutationRepairStrategy);

    RealParameter distributionIndexForPolynomialMutation =
        new RealParameter("polynomialMutationDistributionIndex", 5.0, 400.0);
    mutationParameter.addSpecificParameter("polynomial", distributionIndexForPolynomialMutation);

    RealParameter distributionIndexForLinkedPolynomialMutation =
        new RealParameter("linkedPolynomialMutationDistributionIndex", 5.0, 400.0);
    mutationParameter.addSpecificParameter("linkedPolynomial",
        distributionIndexForLinkedPolynomialMutation);

    RealParameter uniformMutationPerturbation =
        new RealParameter("uniformMutationPerturbation", 0.0, 1.0);
    mutationParameter.addSpecificParameter("uniform", uniformMutationPerturbation);

    RealParameter nonUniformMutationPerturbation =
        new RealParameter("nonUniformMutationPerturbation", 0.0, 1.0);
    mutationParameter.addSpecificParameter("nonUniform", nonUniformMutationPerturbation);

    variationParameter = new VariationParameter(List.of("differentialEvolutionVariation"));
    variationParameter.addSpecificParameter("differentialEvolutionVariation", mutationParameter);
    variationParameter.addSpecificParameter("differentialEvolutionVariation", deCrossoverParameter);
  }

  private void createInitialSolution() {
    createInitialSolutionsParameter =
        new CreateInitialSolutionsParameter(
            Arrays.asList("random", "latinHypercubeSampling", "scatterSearch"));
  }

  private void algorithmResult() {
    algorithmResultParameter =
        new CategoricalParameter("algorithmResult", List.of("population", "externalArchive"));

    populationSizeWithArchiveParameter = new IntegerParameter("populationSizeWithArchive", 10,
        200);
    externalArchiveParameter = new ExternalArchiveParameter<>(
        List.of("crowdingDistanceArchive", "unboundedArchive"));
    algorithmResultParameter.addSpecificParameter(
        "externalArchive", populationSizeWithArchiveParameter);

    algorithmResultParameter.addSpecificParameter(
        "externalArchive", externalArchiveParameter);

  }

  @Override
  public ConfigurableAlgorithmBuilder parse(String[] arguments) {
    for (Parameter<?> parameter : configurableParameterList()) {
      parameter.parse(arguments).check();
    }

    return this;
  }

  /**
   * Creates an instance of NSGA-II from the parsed parameters
   *
   * @return
   */
  @Override
  public EvolutionaryAlgorithm<DoubleSolution> build() {
    Archive<DoubleSolution> archive = null;

    if (algorithmResultParameter.value().equals("externalArchive")) {
      externalArchiveParameter.setSize(populationSize);
      archive = externalArchiveParameter.getParameter();
      populationSize = populationSizeWithArchiveParameter.value();
    }

    Ranking<DoubleSolution> ranking = new FastNonDominatedSortRanking<>(
        new DominanceWithConstraintsComparator<>());
    DensityEstimator<DoubleSolution> densityEstimator = new CrowdingDistanceDensityEstimator<>();
    MultiComparator<DoubleSolution> rankingAndCrowdingComparator =
        new MultiComparator<>(
            Arrays.asList(
                Comparator.comparing(ranking::getRank),
                Comparator.comparing(densityEstimator::value).reversed()));

    var initialSolutionsCreation =
        (SolutionsCreation<DoubleSolution>) createInitialSolutionsParameter.getParameter(
            problem,
            populationSize);

    MutationParameter mutationParameter = (MutationParameter) variationParameter.findSpecificParameter(
        "mutation");
    mutationParameter.addNonConfigurableParameter("numberOfProblemVariables",
        problem.numberOfVariables());

    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableParameter("maxIterations",
          maximumNumberOfEvaluations / populationSize);
    }

    var subProblemIdGenerator = new IntegerPermutationGenerator(populationSize);
    variationParameter.addNonConfigurableParameter("subProblemIdGenerator", subProblemIdGenerator);
    var variation = (Variation<DoubleSolution>) variationParameter.getDoubleSolutionParameter();

    //variationParameter.addNonConfigurableParameter("offspringPopulationSize",
    //    offspringPopulationSizeParameter.value());
    //var variation = (Variation<DoubleSolution>) variationParameter.getDoubleSolutionParameter();


    Selection<DoubleSolution> selection = new DifferentialEvolutionSelection(
        variation.getMatingPoolSize(),
        deCrossoverParameter.getParameter().numberOfRequiredParents() ,
        false,
        subProblemIdGenerator) ;

    Evaluation<DoubleSolution> evaluation;
    if (algorithmResultParameter.value().equals("externalArchive")) {
      evaluation = new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      evaluation = new SequentialEvaluation<>(problem);
    }

    RankingAndDensityEstimatorPreference<DoubleSolution> preferenceForReplacement = new RankingAndDensityEstimatorPreference<>(
        ranking, densityEstimator);
    Replacement<DoubleSolution> replacement =
        new RankingAndDensityEstimatorReplacement<>(preferenceForReplacement,
            Replacement.RemovalPolicy.ONE_SHOT);

    Termination termination =
        new TerminationByEvaluations(maximumNumberOfEvaluations);

    class EvolutionaryAlgorithmWithArchive extends EvolutionaryAlgorithm<DoubleSolution> {

      private Archive<DoubleSolution> archive;

      /**
       * Constructor
       *
       * @param name                      Algorithm name
       * @param initialPopulationCreation
       * @param evaluation
       * @param termination
       * @param selection
       * @param variation
       * @param replacement
       */
      public EvolutionaryAlgorithmWithArchive(String name,
          SolutionsCreation<DoubleSolution> initialPopulationCreation,
          Evaluation<DoubleSolution> evaluation, Termination termination,
          Selection<DoubleSolution> selection, Variation<DoubleSolution> variation,
          Replacement<DoubleSolution> replacement,
          Archive<DoubleSolution> archive) {
        super(name, initialPopulationCreation, evaluation, termination, selection, variation,
            replacement);
        this.archive = archive;
      }

      @Override
      public List<DoubleSolution> result() {
        return archive.solutions();
      }
    }

    if (algorithmResultParameter.value().equals("externalArchive")) {
      return new EvolutionaryAlgorithmWithArchive(
          "NSGA-II",
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement,
          archive);
    } else {
      return new EvolutionaryAlgorithm<>(
          "NSGA-II",
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement);
    }
  }

  public static void print(List<Parameter<?>> parameterList) {
    parameterList.forEach(System.out::println);
  }
}
