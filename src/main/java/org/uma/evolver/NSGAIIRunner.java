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
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
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
    nonConfigurableParameterString.append("--populationSize 50 " ) ;

    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new ZDT6() ;
    var configurableNSGAIIProblem = new ConfigurableNSGAIIProblem(problemWhoseConfigurationIsSearchedFor, "resources/ZDT6.csv",
        indicators, nonConfigurableParameterString);

    double crossoverProbability = 0.9;
    double crossoverDistributionIndex = 20.0;
    var crossover = new SBXCrossover(crossoverProbability, crossoverDistributionIndex);

    double mutationProbability = 1.0 / configurableNSGAIIProblem.numberOfVariables();
    double mutationDistributionIndex = 20.0;
    var mutation = new PolynomialMutation(mutationProbability, mutationDistributionIndex);

    int populationSize = 100;
    int offspringPopulationSize = 100;

    Termination termination = new TerminationByEvaluations(5000);

    EvolutionaryAlgorithm<DoubleSolution> nsgaii = new NSGAIIBuilder<>(
        configurableNSGAIIProblem,
        populationSize,
        offspringPopulationSize,
        crossover,
        mutation)
        .setTermination(termination)
        .setEvaluation(new MultiThreadedEvaluation<>(8, configurableNSGAIIProblem))
        .build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(10);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "NSGA-II - " + problemWhoseConfigurationIsSearchedFor.name(), 80, 100, null,
            indicators.get(0).name(),
            indicators.get(1).name());

    nsgaii.getObservable().register(evaluationObserver);
    nsgaii.getObservable().register(runTimeChartObserver);

    nsgaii.run();

    JMetalLogger.logger.info("Total computing time: " + nsgaii.getTotalComputingTime());

    var nonDominatedSolutionsArchive = new NonDominatedSolutionListArchive<DoubleSolution>() ;
    nonDominatedSolutionsArchive.addAll(nsgaii.result()) ;
    new SolutionListOutput(nonDominatedSolutionsArchive.solutions())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR." + problemWhoseConfigurationIsSearchedFor.name()+".csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN." + problemWhoseConfigurationIsSearchedFor.name()+".csv", ","))
        .print();

    configurableNSGAIIProblem.writeDecodedSolutionsFoFile(nonDominatedSolutionsArchive.solutions(),"VAR."+ problemWhoseConfigurationIsSearchedFor.name()+".Conf.csv");

    //System.exit(0) ;
  }
}
