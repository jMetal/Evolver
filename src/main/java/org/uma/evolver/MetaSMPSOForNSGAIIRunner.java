package org.uma.evolver;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.problem.ConfigurableNSGAIIProblem;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.SMPSOBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3_2D;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
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
public class MetaSMPSOForNSGAIIRunner {

  public static void main(String[] args) throws IOException {
    var indicators = List.of(new Epsilon(), new NormalizedHypervolume());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new DTLZ3_2D() ;
    var configurableNSGAIIProblem = new ConfigurableNSGAIIProblem(problemWhoseConfigurationIsSearchedFor, "resources/DTLZ3.2D.csv",
        indicators, 100, 5000);

    Termination termination = new TerminationByEvaluations(5000);

    int swarmSize = 50 ;

    ParticleSwarmOptimizationAlgorithm smpso = new SMPSOBuilder(
        configurableNSGAIIProblem,
        swarmSize)
        .setTermination(termination)
        .setEvaluation(new MultiThreadedEvaluation<>(8, configurableNSGAIIProblem))
        .build();

    EvaluationObserver evaluationObserver = new EvaluationObserver(10);
    RunTimeChartObserver<DoubleSolution> runTimeChartObserver =
        new RunTimeChartObserver<>(
            "SMPSO - " + problemWhoseConfigurationIsSearchedFor.name(), 80, 100, null,
            indicators.get(0).name(),
            indicators.get(1).name());

    smpso.observable().register(evaluationObserver);
    smpso.observable().register(runTimeChartObserver);

    smpso.run();

    JMetalLogger.logger.info("Total computing time: " + smpso.totalComputingTime());

    var nonDominatedSolutionsArchive = new NonDominatedSolutionListArchive<DoubleSolution>() ;
    nonDominatedSolutionsArchive.addAll(smpso.result()) ;
    new SolutionListOutput(nonDominatedSolutionsArchive.solutions())
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR." + smpso.name() + "." + problemWhoseConfigurationIsSearchedFor.name()+".csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN." + smpso.name() + "." + problemWhoseConfigurationIsSearchedFor.name()+".csv", ","))
        .print();

    //System.exit(0) ;
  }
}
