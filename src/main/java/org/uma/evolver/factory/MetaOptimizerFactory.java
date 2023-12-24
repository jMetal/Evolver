package org.uma.evolver.factory;

import java.util.List;
import java.util.Map;
import org.uma.evolver.MetaOptimizer;
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
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.MultiComparator;
import org.uma.jmetal.util.comparator.ObjectiveComparator;
import org.uma.jmetal.util.comparator.constraintcomparator.impl.OverallConstraintViolationDegreeComparator;
import org.uma.jmetal.util.observable.Observable;

public class MetaOptimizerFactory {
    public static MetaOptimizer getAlgorithm(String name, DoubleProblem problem, int populationSize, int maxNumberOfEvaluations, int numCores) {
        if (name.equals("GGA") && numCores > 1) {
            JMetalLogger.logger.warning("GGA is not parallelized");
        }

        MetaOptimizer optimizationAlgorithm = switch (name) {
            case "NSGAII" ->
                    MetaOptimizerFactory.createNSGAII(problem, populationSize, maxNumberOfEvaluations, numCores);
            case "ASYNCNSGAII" ->
                    MetaOptimizerFactory.createAsyncNSGAII(problem, populationSize, maxNumberOfEvaluations, numCores);
            case "GGA" ->
                    MetaOptimizerFactory.createGenericGeneticAlgorithm(problem, populationSize, maxNumberOfEvaluations);
            case "SMPSO" -> MetaOptimizerFactory.createSMPSO(problem, populationSize, maxNumberOfEvaluations, numCores);
            default -> throw new RuntimeException("Optimization algorithm not found");
        };
        return optimizationAlgorithm;
    }

    private static MetaOptimizer createSMPSO(DoubleProblem problem, int populationSize, int maxNumberOfEvaluations, int numCores) {
        int swarmSize = populationSize;
        Termination termination = new TerminationByEvaluations(maxNumberOfEvaluations);

        ParticleSwarmOptimizationAlgorithm smpso = new SMPSOBuilder(
                problem,
                swarmSize)
                .setTermination(termination)
                .setEvaluation(new MultiThreadedEvaluation<>(numCores, problem))
                .build();

        MetaOptimizer smpsoOptimization = new MetaOptimizer() {

            @Override
            public void run() {
                smpso.run();
            }

            @Override
            public Observable<Map<String, Object>> observable() {
                return smpso.observable();
            }

            @Override
            public long totalComputingTime() {
                return smpso.totalComputingTime();
            }

            @Override
            public List<DoubleSolution> result() {
                return smpso.result();
            }
        };

        return smpsoOptimization;
    }

    private static MetaOptimizer createGenericGeneticAlgorithm(DoubleProblem problem, int populationSize, int maxNumberOfEvaluations) {

        int offspringPopulationSize = populationSize;

        var createInitialPopulation = new RandomSolutionsCreation<>(problem, populationSize);

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
        MetaOptimizer ggaOptimizer = new MetaOptimizer() {

            @Override
            public void run() {
                geneticAlgorithm.run();
            }

            @Override
            public Observable<Map<String, Object>> observable() {
                return geneticAlgorithm.observable();
            }

            @Override
            public long totalComputingTime() {
                return geneticAlgorithm.totalComputingTime();
            }

            @Override
            public List<DoubleSolution> result() {
                return geneticAlgorithm.result();
            }
        };

        return ggaOptimizer;
    }

    private static MetaOptimizer createAsyncNSGAII(DoubleProblem problem, int population, int maxNumberOfEvaluations, int numCores) {

        double crossoverProbability = 0.9;
        double crossoverDistributionIndex = 20.0;
        CrossoverOperator<DoubleSolution> crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

        double mutationProbability = 1.0 / problem.numberOfVariables();
        double mutationDistributionIndex = 20.0;
        MutationOperator<DoubleSolution> mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

        AsynchronousMultiThreadedNSGAII<DoubleSolution> nsgaii =
                new AsynchronousMultiThreadedNSGAII<>(
                        numCores, problem, population, crossover, mutation,
                        new TerminationByEvaluations(maxNumberOfEvaluations));

        MetaOptimizer nsgaiiOptimizer = new MetaOptimizer() {
            private long computingTime;

            @Override
            public void run() {
                long start = System.currentTimeMillis();
                nsgaii.run();
                computingTime = System.currentTimeMillis() - start;
            }

            @Override
            public Observable<Map<String, Object>> observable() {
                return nsgaii.getObservable();
            }

            @Override
            public long totalComputingTime() {
                return computingTime;
            }

            @Override
            public List<DoubleSolution> result() {
                return nsgaii.getResult();
            }
        };

        return nsgaiiOptimizer;
    }

    private static MetaOptimizer createNSGAII(DoubleProblem problem, int population, int maxNumberOfEvaluations, int numCores) {

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
                .setEvaluation(new MultiThreadedEvaluation<>(numCores, problem))
                .build();
        MetaOptimizer nsgaiiOptimizer = new MetaOptimizer() {

            @Override
            public void run() {
                nsgaii.run();
            }

            @Override
            public Observable<Map<String, Object>> observable() {
                return nsgaii.observable();
            }

            @Override
            public long totalComputingTime() {
                return nsgaii.totalComputingTime();
            }

            @Override
            public List<DoubleSolution> result() {
                return nsgaii.result();
            }
        };

        return nsgaiiOptimizer;
    }
}
