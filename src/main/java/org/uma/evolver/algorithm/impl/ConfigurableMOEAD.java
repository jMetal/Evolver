package org.uma.evolver.algorithm.impl;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.parameter2.Parameter;
import org.uma.evolver.parameter2.catalogue.AggregationFunctionParameter;
import org.uma.evolver.parameter2.catalogue.CreateInitialSolutionsParameter;
import org.uma.evolver.parameter2.catalogue.CrossoverParameter;
import org.uma.evolver.parameter2.catalogue.DifferentialEvolutionCrossoverParameter;
import org.uma.evolver.parameter2.catalogue.ExternalArchiveParameter;
import org.uma.evolver.parameter2.catalogue.MutationParameter;
import org.uma.evolver.parameter2.catalogue.ProbabilityParameter;
import org.uma.evolver.parameter2.catalogue.RepairDoubleSolutionStrategyParameter;
import org.uma.evolver.parameter2.catalogue.SelectionParameter;
import org.uma.evolver.parameter2.catalogue.VariationParameter;
import org.uma.evolver.parameter2.impl.BooleanParameter;
import org.uma.evolver.parameter2.impl.CategoricalParameter;
import org.uma.evolver.parameter2.impl.IntegerParameter;
import org.uma.evolver.parameter2.impl.PositiveIntegerValue;
import org.uma.evolver.parameter2.impl.RealParameter;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.Evaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluationWithArchive;
import org.uma.jmetal.component.catalogue.common.solutionscreation.SolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.MOEADReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.Selection;
import org.uma.jmetal.component.catalogue.ea.selection.impl.PopulationAndNeighborhoodSelection;
import org.uma.jmetal.component.catalogue.ea.variation.Variation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.aggregationfunction.AggregationFunction;
import org.uma.jmetal.util.archive.Archive;
import org.uma.jmetal.util.neighborhood.Neighborhood;
import org.uma.jmetal.util.neighborhood.impl.WeightVectorNeighborhood;
import org.uma.jmetal.util.sequencegenerator.impl.IntegerPermutationGenerator;

/**
 * @autor Antonio J. Nebro
 */
public class ConfigurableMOEAD implements ConfigurableAlgorithmBuilder {
  public List<Parameter<?>> autoConfigurableParameterList = new ArrayList<>();
  private CategoricalParameter algorithmResultParameter;
  private ExternalArchiveParameter<DoubleSolution> externalArchiveParameter;
  private PositiveIntegerValue offspringPopulationSizeParameter;
  private CreateInitialSolutionsParameter createInitialSolutionsParameter;
  private SelectionParameter<DoubleSolution> selectionParameter;
  private VariationParameter variationParameter;
  private ProbabilityParameter neighborhoodSelectionProbabilityParameter;
  private IntegerParameter neighborhoodSizeParameter;
  private IntegerParameter maximumNumberOfReplacedSolutionsParameter;
  private AggregationFunctionParameter aggregationFunctionParameter;
  private BooleanParameter normalizeObjectivesParameter ;
  private int populationSize ;
  private int maximumNumberOfEvaluations;
  private String weightVectorFilesDirectory ;

  @Override
  public List<Parameter<?>> configurableParameterList() {
    return autoConfigurableParameterList;
  }

  private DoubleProblem problem ;

  public ConfigurableMOEAD(DoubleProblem problem, int populationSize, int maximumNumberOfEvaluations,
  String weightVectorFilesDirectory) {
    this.problem = problem ;
    this.populationSize = populationSize ;
    this.maximumNumberOfEvaluations = maximumNumberOfEvaluations ;
    this.weightVectorFilesDirectory = weightVectorFilesDirectory ;
    this.configure() ;
  }

  @Override
  public ConfigurableAlgorithmBuilder createBuilderInstance() {
    return new ConfigurableMOEAD(problem, populationSize, maximumNumberOfEvaluations, weightVectorFilesDirectory) ;
  }

  public ConfigurableAlgorithmBuilder createBuilderInstance(DoubleProblem problem, int maximumNumberOfEvaluations) {
    return new ConfigurableMOEAD(problem, populationSize, maximumNumberOfEvaluations, weightVectorFilesDirectory) ;
  }

  public void configure() {
    normalizeObjectivesParameter = new BooleanParameter("normalizeObjectives") ;
    RealParameter epsilonParameter = new RealParameter("epsilonParameterForNormalizing", 1.0E-8, 25);
    normalizeObjectivesParameter.addGlobalParameter(epsilonParameter);

    neighborhoodSizeParameter = new IntegerParameter("neighborhoodSize",5, 50);
    neighborhoodSelectionProbabilityParameter =
        new ProbabilityParameter("neighborhoodSelectionProbability");
    maximumNumberOfReplacedSolutionsParameter =
        new IntegerParameter("maximumNumberOfReplacedSolutions",1, 5);
    aggregationFunctionParameter =
        new AggregationFunctionParameter(
            List.of("tschebyscheff", "weightedSum", "penaltyBoundaryIntersection"));
    RealParameter pbiTheta = new RealParameter("pbiTheta",1.0, 200);
    aggregationFunctionParameter.addSpecificParameter("penaltyBoundaryIntersection", pbiTheta);
    aggregationFunctionParameter.addGlobalParameter(normalizeObjectivesParameter);
    algorithmResult();
    createInitialSolution();
    selection();
    variation();

    autoConfigurableParameterList.add(neighborhoodSizeParameter);
    autoConfigurableParameterList.add(maximumNumberOfReplacedSolutionsParameter);
    autoConfigurableParameterList.add(aggregationFunctionParameter);
    autoConfigurableParameterList.add(normalizeObjectivesParameter);

    autoConfigurableParameterList.add(algorithmResultParameter);
    autoConfigurableParameterList.add(createInitialSolutionsParameter);
    autoConfigurableParameterList.add(variationParameter);
    autoConfigurableParameterList.add(selectionParameter);
  }

  private void variation() {
    CrossoverParameter crossoverParameter = new CrossoverParameter(List.of("SBX", "BLX_ALPHA", "wholeArithmetic"));
    ProbabilityParameter crossoverProbability =
        new ProbabilityParameter("crossoverProbability");
    crossoverParameter.addGlobalParameter(crossoverProbability);
    RepairDoubleSolutionStrategyParameter crossoverRepairStrategy =
        new RepairDoubleSolutionStrategyParameter(
            "crossoverRepairStrategy", Arrays.asList("random", "round", "bounds"));
    crossoverParameter.addGlobalParameter(crossoverRepairStrategy);

    RealParameter distributionIndex = new RealParameter("sbxDistributionIndex",5.0, 400.0);
    crossoverParameter.addSpecificParameter("SBX", distributionIndex);

    RealParameter alpha = new RealParameter("blxAlphaCrossoverAlphaValue",0.0, 1.0);
    crossoverParameter.addSpecificParameter("BLX_ALPHA", alpha);

    MutationParameter mutationParameter =
        new MutationParameter(Arrays.asList("uniform", "polynomial", "linkedPolynomial", "nonUniform"));

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
        new RealParameter("linkedPolynomialMutationDistributionIndex",5.0, 400.0);
    mutationParameter.addSpecificParameter("linkedPolynomial",
        distributionIndexForLinkedPolynomialMutation);

    RealParameter uniformMutationPerturbation =
        new RealParameter("uniformMutationPerturbation",0.0, 1.0);
    mutationParameter.addSpecificParameter("uniform", uniformMutationPerturbation);

    RealParameter nonUniformMutationPerturbation =
        new RealParameter("nonUniformMutationPerturbation", 0.0, 1.0);
    mutationParameter.addSpecificParameter("nonUniform", nonUniformMutationPerturbation);

    DifferentialEvolutionCrossoverParameter deCrossoverParameter =
        new DifferentialEvolutionCrossoverParameter(List.of("RAND_1_BIN", "RAND_1_EXP", "RAND_2_BIN"));

    RealParameter crParameter = new RealParameter("CR", 0.0, 1.0);
    RealParameter fParameter = new RealParameter("F", 0.0, 1.0);
    deCrossoverParameter.addGlobalParameter(crParameter);
    deCrossoverParameter.addGlobalParameter(fParameter);

    offspringPopulationSizeParameter = new PositiveIntegerValue("offspringPopulationSize") ;
    offspringPopulationSizeParameter.value(1);

    variationParameter =
        new VariationParameter(List.of("crossoverAndMutationVariation", "differentialEvolutionVariation"));
    variationParameter.addSpecificParameter("crossoverAndMutationVariation", crossoverParameter);
    variationParameter.addSpecificParameter("crossoverAndMutationVariation", mutationParameter);
    variationParameter.addNonConfigurableParameter("offspringPopulationSize", 1);
    variationParameter.addSpecificParameter("differentialEvolutionVariation", mutationParameter);
    variationParameter.addSpecificParameter("differentialEvolutionVariation", deCrossoverParameter);
  }

  private void selection() {
    selectionParameter = new SelectionParameter<>(List.of("populationAndNeighborhoodMatingPoolSelection"));
    neighborhoodSelectionProbabilityParameter =
        new ProbabilityParameter("neighborhoodSelectionProbability");
    selectionParameter.addSpecificParameter(
        "populationAndNeighborhoodMatingPoolSelection", neighborhoodSelectionProbabilityParameter);
  }

  private void createInitialSolution() {
    createInitialSolutionsParameter =
        new CreateInitialSolutionsParameter(Arrays.asList("random", "latinHypercubeSampling", "scatterSearch"));
  }

  private void algorithmResult() {
    algorithmResultParameter =
        new CategoricalParameter("algorithmResult", List.of("externalArchive", "population"));
    externalArchiveParameter = new ExternalArchiveParameter<>(List.of("crowdingDistanceArchive", "unboundedArchive"));

    algorithmResultParameter.addSpecificParameter(
        "externalArchive", externalArchiveParameter);
  }

  @Override
  public ConfigurableAlgorithmBuilder parse(String[] arguments) {
    for (Parameter<?> parameter : configurableParameterList()) {
      parameter.parse(arguments).check();
    }

    return this ;
  }

  /**
   * Creates an instance of NSGA-II from the parsed parameters
   *
   * @return
   */
  public EvolutionaryAlgorithm<DoubleSolution> build() {
    Archive<DoubleSolution> archive = null;
    Evaluation<DoubleSolution> evaluation ;
    if (algorithmResultParameter.value().equals("externalArchive")) {
      externalArchiveParameter.setSize(populationSize);
      archive = externalArchiveParameter.getParameter();
      evaluation = new SequentialEvaluationWithArchive<>(problem, archive);
    } else {
      evaluation = new SequentialEvaluation<>(problem);
    }

    var initialSolutionsCreation =
        (SolutionsCreation<DoubleSolution>) createInitialSolutionsParameter.getParameter(problem,
            populationSize);

    Termination termination =
        new TerminationByEvaluations(maximumNumberOfEvaluations);

    MutationParameter mutationParameter = (MutationParameter) variationParameter.findSpecificParameter(
        "mutation");
    mutationParameter.addNonConfigurableParameter("numberOfProblemVariables",
        problem.numberOfVariables());

    if (mutationParameter.value().equals("nonUniform")) {
      mutationParameter.addNonConfigurableParameter("nonUniformMutationPerturbation", maximumNumberOfEvaluations);
      mutationParameter.addNonConfigurableParameter("maxIterations",
          maximumNumberOfEvaluations / populationSize);
    }

    Neighborhood<DoubleSolution> neighborhood = null ;

    if (problem.numberOfObjectives() == 2) {
      neighborhood =
          new WeightVectorNeighborhood<>(
              populationSize, neighborhoodSizeParameter.value());
    } else {
      try {
        neighborhood =
            new WeightVectorNeighborhood<>(
                populationSize,
                problem.numberOfObjectives(),
                neighborhoodSizeParameter.value(),
                weightVectorFilesDirectory);
      } catch (FileNotFoundException exception) {
        exception.printStackTrace();
      }
    }

    var subProblemIdGenerator = new IntegerPermutationGenerator(populationSize);
    selectionParameter.addNonConfigurableParameter("neighborhood", neighborhood);
    selectionParameter.addNonConfigurableParameter("subProblemIdGenerator", subProblemIdGenerator);

    variationParameter.addNonConfigurableParameter("subProblemIdGenerator", subProblemIdGenerator);

    var variation = (Variation<DoubleSolution>) variationParameter.getDoubleSolutionParameter();

    var selection =
        (PopulationAndNeighborhoodSelection<DoubleSolution>)
            selectionParameter.getParameter(variation.getMatingPoolSize(), null);

    int maximumNumberOfReplacedSolutions = maximumNumberOfReplacedSolutionsParameter.value();

    aggregationFunctionParameter.normalizedObjectives(normalizeObjectivesParameter.value());
    AggregationFunction aggregativeFunction = aggregationFunctionParameter.getParameter();
    var replacement =
        new MOEADReplacement<>(
            selection,
            (WeightVectorNeighborhood<DoubleSolution>) neighborhood,
            aggregativeFunction,
            subProblemIdGenerator,
            maximumNumberOfReplacedSolutions, normalizeObjectivesParameter.value());

    class EvolutionaryAlgorithmWithArchive extends EvolutionaryAlgorithm<DoubleSolution> {
      private Archive<DoubleSolution> archive ;
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
        this.archive = archive ;
      }

      @Override
      public List<DoubleSolution> result() {
        return archive.solutions() ;
      }
    }

    if (algorithmResultParameter.value().equals("externalArchive")) {
      return new EvolutionaryAlgorithmWithArchive(
          "MOEAD",
          initialSolutionsCreation,
          evaluation,
          termination,
          selection,
          variation,
          replacement,
          archive) ;
    } else {
      return new EvolutionaryAlgorithm<>(
          "MOEAD",
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
