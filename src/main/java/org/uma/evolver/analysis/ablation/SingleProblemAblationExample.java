package org.uma.evolver.analysis.ablation;

import org.uma.evolver.analysis.ablation.AblationAnalysis;
import org.uma.evolver.analysis.ablation.AblationResult;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.evolver.parameter.ParameterSpace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;

public class SingleProblemAblationExample {
  public static void main(String[] args) throws IOException {
    String yamlParameterSpaceFile = "src/main/resources/parameterSpaces/NSGAIIDoubleFull.yaml";
    String referenceFrontFileName = "resources/referenceFronts/DTLZ3.3D.csv";

    var problem = new DTLZ3();
    int populationSize = 100;
    int maxEvaluations = 25000;
    int numberOfRuns = 20;

    double[][] referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
    var indicator = new NormalizedHypervolume(referenceFront);

    YAMLParameterSpace parameterSpace = new YAMLParameterSpace(yamlParameterSpaceFile, new DoubleParameterFactory());

    String startConfig = "--algorithmResult population --createInitialSolutions default --offspringPopulationSize 100 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2";

    String endConfig = "--algorithmResult externalArchive --populationSizeWithArchive 44 --archiveType unboundedArchive --createInitialSolutions default --offspringPopulationSize 5 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9065458700884546 --crossoverRepairStrategy round --sbxDistributionIndex 44.87999328084076 --mutation uniform --mutationProbabilityFactor 0.737155491583473 --mutationRepairStrategy random --uniformMutationPerturbation 0.23330400136847657 --selection tournament --selectionTournamentSize 2 ";

    // Builder/Runner: Creates, builds, runs, returns result.
    Function<ParameterSpace, List<DoubleSolution>> algorithmRunner = space -> {
      var alg = new DoubleNSGAII(problem, populationSize, maxEvaluations, space).build();
      alg.run();

      return alg.result();
    };

    AblationAnalysis analysis = new AblationAnalysis(
        parameterSpace,
        algorithmRunner,
        indicator,
        numberOfRuns);

    String outputDir = "results/ablation/" + problem.getClass().getSimpleName();
    Files.createDirectories(Paths.get(outputDir));

    System.out.println("Starting Path Ablation...");
    AblationResult pathResult = analysis.performPathAblation(startConfig, endConfig);
    String pathFile = outputDir + "/path_ablation.csv";
    Files.writeString(Paths.get(pathFile), pathResult.toCSV());
    System.out.println("Path Ablation Done. Saved to " + pathFile);

    System.out.println("Starting Leave-One-Out...");
    AblationResult looResult = analysis.performLeaveOneOut(endConfig, startConfig);
    String looFile = outputDir + "/loo_ablation.csv";
    Files.writeString(Paths.get(looFile), looResult.toCSV());
    System.out.println("LOO Ablation Done. Saved to " + looFile);
  }
}
