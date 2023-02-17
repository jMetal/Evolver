package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.problem.ConfigurableNSGAIIProblem;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.archive.impl.NonDominatedSolutionListArchive;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observer.impl.EvaluationObserver;
import org.uma.jmetal.util.observer.impl.RunTimeChartObserver;

/**
 * Class configuring NSGA-II using arguments in the form <key, value> and the
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class NSGAIIRunner {

  public static void main(String[] args) throws IOException {
    var nonConfigurableParameterString = new StringBuilder() ;
    nonConfigurableParameterString.append("--maximumNumberOfEvaluations 5000 " ) ;
    nonConfigurableParameterString.append("--problemName org.uma.jmetal.problem.multiobjective.zdt.ZDT1 " ) ;
    nonConfigurableParameterString.append("--populationSize 100 " ) ;
    nonConfigurableParameterString.append("--referenceFrontFileName resources/ZDT1.csv " ) ;

    var indicators = List.of(new InvertedGenerationalDistancePlus(), new NormalizedHypervolume());
    var problem = new ConfigurableNSGAIIProblem(indicators, nonConfigurableParameterString);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / problem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 100;
    int offspringPopulationSize = 100;

    Termination termination = new TerminationByEvaluations(5000);

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = new NSGAIIBuilder<>(
        problem,
        populationSize,
        offspringPopulationSize,
        crossover,
        mutation)
        .setTermination(termination)
        .setEvaluation(new MultiThreadedEvaluation<>(8, problem))
        .build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(10);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II - ZDT1", 80, 100, null,
            indicators.get(0).name(),
            indicators.get(1).name());

    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(runTimeChartObserver);

    nsgaii.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaii.getTotalComputingTime());

    var nonDominatedSolutionsArchive = new NonDominatedSolutionListArchive<DoubleSolution>() ;
    nonDominatedSolutionsArchive.addAll(nsgaii.result()) ;
    new SolutionListOutput(nonDominatedSolutionsArchive.solutions())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    problem.writeDecodedSolutionsFoFile(nonDominatedSolutionsArchive.solutions(),"VAR.Conf.csv");

    //System.exit(0) ;
  }
}
