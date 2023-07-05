package org.uma.evolver.factory;

import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.algorithm.multiobjective.SMPSOBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.SequentialEvaluation;
import org.uma.jmetal.component.catalogue.common.solutionscreation.impl.RandomSolutionsCreation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.component.catalogue.ea.replacement.impl.MuPlusLambdaReplacement;
import org.uma.jmetal.component.catalogue.ea.selection.impl.NaryTournamentSelection;
import org.uma.jmetal.component.catalogue.ea.variation.impl.CrossoverAndMutationVariation;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.parallel.asynchronous.algorithm.impl.AsynchronousMultiThreadedNSGAII;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.comparator.constraintcomparator.impl.OverallConstraintViolationDegreeComparator;

import java.util.List;

public class OptimizationAlgorithmFactory {
    public static EvolutionaryAlgorithm<DoubleSolution> getAlgorithm(String name, DoubleProblem problem, int population, int maxNumberOfEvaluations) {
        // TODO: extract parallelism parameters
        EvolutionaryAlgorithm<DoubleSolution> optimizationAlgorithm = switch (name) {
            case "NSGAII" -> OptimizationAlgorithmFactory.createNSGAII(problem, population, maxNumberOfEvaluations);
            case "ASYNCNSGAII" ->
                    OptimizationAlgorithmFactory.createAyncNSGAII(problem, population, maxNumberOfEvaluations);
            case "GGA" ->
                    OptimizationAlgorithmFactory.createGenericGeneticAlgorithm(problem, population, maxNumberOfEvaluations);
            default -> throw new RuntimeException("Optimization algorithm not found");
        };
        return optimizationAlgorithm;
    }

    private static EvolutionaryAlgorithm<DoubleSolution> createSMPSO(DoubleProblem problem, int population, int maxNumberOfEvaluations) {
        int swarmSize = population;
        Termination termination = new TerminationByEvaluations(maxNumberOfEvaluations);

        ParticleSwarmOptimizationAlgorithm smpso = new SMPSOBuilder(
                problem,
                swarmSize)
                .setTermination(termination)
                .setEvaluation(new MultiThreadedEvaluation<>(8, problem))
                .build();

        return null;
    }

    private static EvolutionaryAlgorithm<DoubleSolution> createGenericGeneticAlgorithm(DoubleProblem problem, int population, int maxNumberOfEvaluations) {

        int offspringPopulationSize = population;

        var createInitialPopulation = new RandomSolutionsCreation<>(problem, population);

        var comparator = new MultiComparator<DoubleSolution>(
                List.of(new OverallConstraintViolationDegreeComparator<>(),
                        new ObjectiveComparator<>(0)));

        var replacement =
                new MuPlusLambdaReplacement<>(comparator);

        var crossover = new SBXCrossover(0.9, 20.0);

        double mutationProbability = 1.0 / problem.numberOfVariables();
        var mutation = new PolynomialMutation(mutationProbability, 20.0);
        var variation =
                new CrossoverAndMutationVariation<>(
                        offspringPopulationSize, crossover, mutation);

        var selection =
                new NaryTournamentSelection<DoubleSolution>(
                        2,
                        variation.getMatingPoolSize(),
                        new ObjectiveComparator<>(0));

        var termination = new TerminationByEvaluations(maxNumberOfEvaluations);

        var evaluation = new SequentialEvaluation<>(problem);

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

        return geneticAlgorithm;
    }

    private static EvolutionaryAlgorithm<DoubleSolution> createAyncNSGAII(DoubleProblem problem, int population, int maxNumberOfEvaluations) {

        double crossoverProbability = 0.9;
        double crossoverDistributionIndex = 20.0;
        CrossoverOperator<DoubleSolution> crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 20.0;
        MutationOperator<DoubleSolution> mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

        AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
                new AsynchronousMultiThreadedNSGAII<>(
                        8, problem, population, crossover, mutation,
                        new TerminationByEvaluations(maxNumberOfEvaluations));
        return null; // TODO AsyncNSGAII is not an EvolutionaryAlgorithm
    }

    private static EvolutionaryAlgorithm<DoubleSolution> createNSGAII(DoubleProblem problem, int population, int maxNumberOfEvaluations) {

        double crossoverProbability = 0.9;
        double crossoverDistributionIndex = 20.0;
        var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 20.0;
        var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

        int offspringPopulationSize = 50;

        Termination termination = new TerminationByEvaluations(maxNumberOfEvaluations);

        EvolutionaryAlgorithm<DoubleSolution> nsgaii = new NSGAIIBuilder<>(
                problem,
                population,
                offspringPopulationSize,
                crossover,
                mutation)
                .setTermination(termination)
                .setEvaluation(new MultiThreadedEvaluation<>(8, problem))
                .build();

        return nsgaii;
    }
}
