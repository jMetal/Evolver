package org.uma.evolver.configurablealgorithm.impl;

import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter.Parameter;
import org.uma.evolver.parameter.catalogue.*;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Class to configure NSGA-II with an argument string using class {@link EvolutionaryAlgorithm}
 *
 * @author Antonio J. Nebro
 */
public class ConfigurableNSGAIIWithMultiVariation implements ConfigurableAlgorithmBuilder {

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

  public ConfigurableNSGAIIWithMultiVariation() {
    this.configure() ;
  }

  public ConfigurableNSGAIIWithMultiVariation(DoubleProblem problem, int populationSize,
                                              int maximumNumberOfEvaluations) {
    this.problem = problem;
    this.populationSize = populationSize;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations;
    this.configure();
  }

  @Override
  public ConfigurableAlgorithmBuilder  createBuilderInstance() {
    return new ConfigurableNSGAIIWithMultiVariation(problem, populationSize, maximumNumberOfEvaluations);
  }

  @Override
  public ConfigurableAlgorithmBuilder createBuilderInstance(DoubleProblem problem,
      int maximumNumberOfEvaluations) {
    return new ConfigurableNSGAIIWithMultiVariation(problem, populationSize, maximumNumberOfEvaluations);
  }

  private void configure() {
    offspringPopulationSizeParameter = new CategoricalIntegerParameter("offspringPopulationSize",
        List.of(1, 2, 5, 10, 20, 50, 100, 200, 400));

    algorithmResult();
    createInitialSolution();
    selection();
    variation();

    configurableParameterList.add(algorithmResultParameter);
    configurableParameterList.add(createInitialSolutionsParameter);
    configurableParameterList.add(offspringPopulationSizeParameter);
    configurableParameterList.add(variationParameter);
    configurableParameterList.add(selectionParameter);
  }

  private void variation() {
    CrossoverParameter crossoverParameter = new CrossoverParameter(
        List.of("multiCrossover"));
    ProbabilityParameter crossoverProbability =
        new ProbabilityParameter("crossoverProbability");
    crossoverParameter.addGlobalParameter(crossoverProbability);
    RepairDoubleSolutionStrategyParameter crossoverRepairStrategy =
        new RepairDoubleSolutionStrategyParameter(
            "crossoverRepairStrategy", Arrays.asList("random", "round", "bounds"));
    crossoverParameter.addGlobalParameter(crossoverRepairStrategy);

    RealParameter BLXCrossoverProbability = new RealParameter("blxCrossoverProbability", 0.0, 1.0);
    crossoverParameter.addSpecificParameter("multiCrossover", BLXCrossoverProbability);
    RealParameter SBXCrossoverProbability = new RealParameter("sbxCrossoverProbability", 0.0, 1.0);
    crossoverParameter.addSpecificParameter("multiCrossover", SBXCrossoverProbability);
    RealParameter WACrossoverProbability = new RealParameter("waCrossoverProbability", 0.0, 1.0);
    crossoverParameter.addSpecificParameter("multiCrossover", WACrossoverProbability);
    RealParameter distributionIndex = new RealParameter("sbxDistributionIndex", 5.0, 400.0);
    crossoverParameter.addSpecificParameter("multiCrossover", distributionIndex);
    RealParameter alpha = new RealParameter("blxAlphaCrossoverAlphaValue", 0.0, 1.0);
    crossoverParameter.addSpecificParameter("multiCrossover", alpha);

    MutationParameter mutationParameter =
        new MutationParameter(
            Arrays.asList("multiMutation"));

    RealParameter mutationProbabilityFactor = new RealParameter("mutationProbabilityFactor",
        0.0, 2.0);
    mutationParameter.addGlobalParameter(mutationProbabilityFactor);

    RepairDoubleSolutionStrategyParameter mutationRepairStrategy =
        new RepairDoubleSolutionStrategyParameter(
            "mutationRepairStrategy", Arrays.asList("random", "round", "bounds"));
    mutationParameter.addGlobalParameter(mutationRepairStrategy);

    RealParameter distributionIndexForPolynomialMutation =
        new RealParameter("polynomialMutationDistributionIndex", 5.0, 400.0);
    mutationParameter.addSpecificParameter("multiMutation", distributionIndexForPolynomialMutation);
    RealParameter distributionIndexForLinkedPolynomialMutation =
        new RealParameter("linkedPolynomialMutationDistributionIndex", 5.0, 400.0);
    mutationParameter.addSpecificParameter("multiMutation",
        distributionIndexForLinkedPolynomialMutation);
    RealParameter uniformMutationPerturbation =
        new RealParameter("uniformMutationPerturbation", 0.0, 1.0);
    mutationParameter.addSpecificParameter("multiMutation", uniformMutationPerturbation);
    RealParameter nonUniformMutationPerturbation =
        new RealParameter("nonUniformMutationPerturbation", 0.0, 1.0);
    mutationParameter.addSpecificParameter("multiMutation", nonUniformMutationPerturbation);

    RealParameter polyMutationProbability = new RealParameter("polyMutationProbability", 0.0, 1.0);
    mutationParameter.addSpecificParameter("multiMutation", polyMutationProbability);
    RealParameter linkedPolyMutationProbability = new RealParameter("linkedPolyMutationProbability", 0.0, 1.0);
    mutationParameter.addSpecificParameter("multiMutation", linkedPolyMutationProbability);
    RealParameter uniMutationProbability = new RealParameter("uniMutationProbability", 0.0, 1.0);
    mutationParameter.addSpecificParameter("multiMutation", uniMutationProbability);
    RealParameter nonUniMutationProbability = new RealParameter("nonUniMutationProbability", 0.0, 1.0);
    mutationParameter.addSpecificParameter("multiMutation", nonUniMutationProbability);

    variationParameter =
        new VariationParameter(List.of("crossoverAndMutationVariation"));
    variationParameter.addSpecificParameter("crossoverAndMutationVariation", crossoverParameter);
    variationParameter.addSpecificParameter("crossoverAndMutationVariation", mutationParameter);
  }

  private void selection() {
    selectionParameter = new SelectionParameter<>(Arrays.asList("tournament", "random"));
    IntegerParameter selectionTournamentSize =
        new IntegerParameter("selectionTournamentSize", 2, 10);
    selectionParameter.addSpecificParameter("tournament", selectionTournamentSize);
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


    mutationParameter.addNonConfigurableParameter("maxIterations",
        maximumNumberOfEvaluations / populationSize);


    variationParameter.addNonConfigurableParameter("offspringPopulationSize",
        offspringPopulationSizeParameter.value());
    var variation = (Variation<DoubleSolution>) variationParameter.getDoubleSolutionParameter();


    Selection<DoubleSolution> selection =
        selectionParameter.getParameter(
            variation.getMatingPoolSize(), rankingAndCrowdingComparator);

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
          "NSGA-II-MULTI_VARIATION",
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement,
          archive);
    } else {
      return new EvolutionaryAlgorithm<>(
          "NSGA-II-MULTI_VARIATION",
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
