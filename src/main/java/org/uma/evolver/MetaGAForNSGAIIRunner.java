package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.algorithm.ConfigurableAlgorithm;
import org.uma.evolver.algorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.problem.ConfigurableAlgorithmProblem;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.MuPlusLambdaReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.impl.PMXCrossover;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PermutationSwapMutation;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.solution.permutationsolution.PermutationSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.comparator.constraintcomparator.impl.OverallConstraintViolationDegreeComparator;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.FitnessPlotObserver;

/**
 * Class configuring a genetic algorithm
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class MetaGAForNSGAIIRunner {

  public static void main(String[] args) throws IOException {

    List<QualityIndicator> indicators = List.of(new Epsilon());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT4();
    ConfigurableAlgorithm configurableAlgorithm = new ConfigurableNSGAII(
        problemWhoseConfigurationIsSearchedFor, 100, 5000);
    var configurableProblem = new ConfigurableAlgorithmProblem(configurableAlgorithm,
        "resources/referenceFronts/ZDT4.csv",
        indicators, 1);

    int populationSize = 100;
    int offspringPopulationSize = populationSize;

    var createInitialPopulation = new RandomSolutionsCreation<>(configurableProblem, populationSize);

    var comparator = new MultiComparator<DoubleSolution>(
        List.of(new OverallConstraintViolationDegreeComparator<>(),
            new ObjectiveComparator<>(0)));

    var replacement =
        new MuPlusLambdaReplacement<>(comparator);

    var crossover = new SBXCrossover(0.9, 20.0);

    double mutationProbability = 1.0 / configurableProblem.numberOfVariables();
    var mutation = new PolynomialMutation(mutationProbability, 20.0);
    var variation =
        new CrossoverAndMutationVariation<>(
            offspringPopulationSize, crossover, mutation);

    var selection =
        new NaryTournamentSelection<DoubleSolution>(
            2,
            variation.getMatingPoolSize(),
            new ObjectiveComparator<>(0));

    var termination = new TerminationByEvaluations(500000);

    var evaluation = new SequentialEvaluation<>(configurableProblem);

    EvolutionaryAlgorithm<DoubleSolution> geneticAlgorithm = new EvolutionaryAlgorithm<>(
        "GGA",
        createInitialPopulation, evaluation, termination, selection, variation, replacement) {

      @Override
      public void updateProgress() {
        DoubleSolution bestFitnessSolution = population().stream()
            .min(new MultiComparator<>(List.of(new OverallConstraintViolationDegreeComparator<>(),
                new ObjectiveComparator<>(0)))).get();
        attributes().put("BEST_SOLUTION", bestFitnessSolution);

        super.updateProgress();
      }
    };

    var chartObserver = new FitnessPlotObserver<>("Genetic algorithm", "Evaluations", indicators.get(0).name(),
        indicators.get(0).name(), 100);
    geneticAlgorithm.observable().register(chartObserver);

    geneticAlgorithm.run();

    //System.exit(0) ;
  }
}
