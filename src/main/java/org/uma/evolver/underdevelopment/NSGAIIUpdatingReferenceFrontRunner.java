package org.uma.evolver.underdevelopment;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.uma.evolver.referencefrontupdate.impl.DynamicReferenceFrontUpdate;
import org.uma.jmetal.auto.autoconfigurablealgorithm.AutoNSGAII;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.Replacement;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.RankingAndDensityEstimatorReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.hypervolume.impl.PISAHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.densityestimator.impl.CrowdingDistanceDensityEstimator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;
import org.uma.jmetal.util.ranking.impl.FastNonDominatedSortRanking;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the {@link AutoNSGAII} class.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIUpdatingReferenceFrontRunner {
  public static void main(String[] args) {

    var indicators = List.of(new Epsilon(), new PISAHypervolume()) ;
    DynamicReferenceFrontUpdate<DoubleSolution> referenceFrontUpdate = new DynamicReferenceFrontUpdate<>();

    var problem = new ConfigurableAlgorithmProblemComputingTheReferenceFront(null, indicators, referenceFrontUpdate) ;

    int populationSize = 100 ;
    int offspringPopulationSize = 100 ;

    var name = "NSGAII";
    var densityEstimator = new CrowdingDistanceDensityEstimator<DoubleSolution>();
    var ranking = new FastNonDominatedSortRanking<DoubleSolution>();

    var createInitialPopulation = new RandomSolutionsCreation<>(problem, populationSize);

    var replacement =
        new RankingAndDensityEstimatorReplacement<>(
            ranking, densityEstimator, Replacement.RemovalPolicy.ONE_SHOT);

    var crossover = new SBXCrossover(0.9, 20.0) ;
    var mutation = new PolynomialMutation(1.0/problem.numberOfVariables(), 20.0) ;
    var variation =
        new CrossoverAndMutationVariation<>(
            offspringPopulationSize, crossover, mutation);

    int tournamentSize = 2 ;
    var selection =
        new NaryTournamentSelection<>(
            tournamentSize,
            variation.getMatingPoolSize(),
            new MultiComparator<>(
                Arrays.asList(
                    Comparator.comparing(ranking::getRank),
                    Comparator.comparing(densityEstimator::value).reversed())));

    var termination = new TerminationByEvaluations(2500);

    var evaluation = new MultiThreadedEvaluation<>(8, problem);
    var nsgaII = new EvolutionaryAlgorithm<>(name, createInitialPopulation, evaluation, termination,
        selection, variation, replacement) {
      @Override
      protected void initProgress() {
        super.initProgress();
      }

      @Override
      protected void updateProgress() {
        super.updateProgress();
        referenceFrontUpdate.update(this.population());
        evaluation().evaluate(population()) ;
      }
    } ;

    EvaluationObserver evaluationObserver = new EvaluationObserver(100);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II", 80, 100, null);

    nsgaII.observable().register(evaluationObserver);
    nsgaII.observable().register(runTimeChartObserver);

    nsgaII.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaII.totalComputingTime());

    new SolutionListOutput(nsgaII.result())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();
  }
}
