package org.uma.evolver.example;

import java.io.IOException;
import java.util.List;
import org.uma.evolver.configurablealgorithm.ConfigurableAlgorithmBuilder;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableNSGAII;
import org.uma.evolver.configurablealgorithm.impl.ConfigurableSMSEMOA;
import org.uma.evolver.problem.MetaOptimizationProblem;
import org.uma.evolver.util.EvaluationObserver;
import org.uma.evolver.util.OutputResultsManagement;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.evolver.util.WriteExecutionDataToFilesObserver;
import org.uma.jmetal.component.algorithm.EvolutionaryAlgorithm;
import org.uma.jmetal.component.algorithm.ParticleSwarmOptimizationAlgorithm;
import org.uma.jmetal.component.algorithm.multiobjective.NSGAIIBuilder;
import org.uma.jmetal.component.algorithm.multiobjective.SMPSOBuilder;
import org.uma.jmetal.component.catalogue.common.evaluation.impl.MultiThreadedEvaluation;
import org.uma.jmetal.component.catalogue.common.termination.Termination;
import org.uma.jmetal.component.catalogue.common.termination.impl.TerminationByEvaluations;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observer.impl.FrontPlotObserver;

/**
 * Class for running SMPSO as meta-optimizer to configure {@link ConfigurableSMSEMOA} using
 * problem {@link DTLZ2} as training set.
 *
 * @author Antonio J. Nebro (ajnebro@uma.es)
 */
public class SMPSOOptimizingSMSEMOAForProblemDTLZ2 {

  public static void main(String[] args) throws IOException {

    // Step 1: Select the target problem (DTLZ2)
    var indicators = List.of(new Epsilon(), new InvertedGenerationalDistancePlus());
    DoubleProblem problemWhoseConfigurationIsSearchedFor = new DTLZ2();
    String referenceFrontFileName = "resources/referenceFronts/DTLZ2.3D.csv";

    // Step 2: Set the parameters for the algorithm to be configured (ConfigurableNSGAII})
    ConfigurableAlgorithmBuilder configurableAlgorithm = new ConfigurableSMSEMOA(
        problemWhoseConfigurationIsSearchedFor, 100, 12000);
    var configurableProblem = new MetaOptimizationProblem(configurableAlgorithm,
        referenceFrontFileName,
        indicators, 1);

    // Step 3: Set the parameters for the meta-optimizer (SMPSO)
    int swarmSize = 100 ;
    int maxEvaluations = 2000;
    Termination termination = new TerminationByEvaluations(maxEvaluations);

    ParticleSwarmOptimizationAlgorithm smpso = new SMPSOBuilder(
         configurableProblem,
        swarmSize)
        .setTermination(termination)
        .setEvaluation(new MultiThreadedEvaluation<>(8, configurableProblem))
        .build();

    // Step 4: Create observers for the meta-optimizer
    OutputResultsManagementParameters outputResultsManagementParameters = new OutputResultsManagementParameters(
        "SMSEMOA", configurableProblem, problemWhoseConfigurationIsSearchedFor.name(), indicators,
        "RESULTS/SMSEMOA/DTLZ2");

    var evaluationObserver = new EvaluationObserver(50);
    var frontChartObserver =
        new FrontPlotObserver<DoubleSolution>(
            "SMS-EMOA, " + problemWhoseConfigurationIsSearchedFor.name(), indicators.get(0).name(),
            indicators.get(1).name(), problemWhoseConfigurationIsSearchedFor.name(), 50);
    var outputResultsManagement = new OutputResultsManagement(outputResultsManagementParameters);

    var writeExecutionDataToFilesObserver = new WriteExecutionDataToFilesObserver(25,
        maxEvaluations, outputResultsManagement);

    smpso.observable().register(evaluationObserver);
    smpso.observable().register(frontChartObserver);
    smpso.observable().register(writeExecutionDataToFilesObserver);

    // Step 5: Run the meta-optimizer
    smpso.run();

    // Step 6: Write results
    JMetalLogger.logger.info(() -> "Total computing time: " + smpso.totalComputingTime());

    outputResultsManagement.updateSuffix("." + maxEvaluations + ".csv");
    outputResultsManagement.writeResultsToFiles(smpso.result());

    System.exit(0);
  }
}
