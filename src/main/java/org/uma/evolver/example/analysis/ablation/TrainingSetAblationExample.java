package org.uma.evolver.example.analysis.ablation;

import org.uma.evolver.analysis.ablation.AblationAnalysis;
import org.uma.evolver.analysis.ablation.AblationResult;
import org.uma.evolver.algorithm.base.nsgaii.DoubleNSGAII;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.evolver.parameter.yaml.YAMLParameterSpace;
import org.uma.evolver.parameter.factory.DoubleParameterFactory;
import org.uma.evolver.trainingset.RWA3DTrainingSet;
import org.uma.evolver.trainingset.TrainingSet;
import org.uma.evolver.util.TrainingSetRunner;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;

import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class TrainingSetAblationExample {
  public static void main(String[] args) throws IOException {
    // 1. Define Training Set
    TrainingSet<DoubleSolution> trainingSet = new RWA3DTrainingSet();

    // 2. Define Quality Indicator
    // Easy switch: Change this line to use a different indicator
    // var indicator = new NormalizedHypervolume();
    var indicator = new InvertedGenerationalDistancePlus();

    boolean isMaximization = indicator.name().contains("Hypervolume");

    // 3. Define Parameter Space
    ParameterSpace parameterSpace = new YAMLParameterSpace("src/main/resources/parameterSpaces/NSGAIIDoubleFull.yaml",
        new DoubleParameterFactory());

    // 4. Define Configurations
    String defaultConfigStr = "--algorithmResult population --createInitialSolutions default --offspringPopulationSize 100 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.9 --crossoverRepairStrategy bounds --sbxDistributionIndex 20.0 --mutation polynomial --mutationProbabilityFactor 1.0 --mutationRepairStrategy bounds --polynomialMutationDistributionIndex 20.0 --selection tournament --selectionTournamentSize 2 ";
    String optimizedConfigStr = "--algorithmResult externalArchive --populationSizeWithArchive 120 --archiveType unboundedArchive --createInitialSolutions scatterSearch --offspringPopulationSize 200 --variation crossoverAndMutationVariation --crossover SBX --crossoverProbability 0.19904489319113755 --crossoverRepairStrategy random --sbxDistributionIndex 282.8235939482752 --mutation powerLaw --mutationProbabilityFactor 1.5826118932648738 --mutationRepairStrategy round --powerLawMutationDelta 6.502168163442743 --selection random ";

    // 5. Define Evaluation Function
    Function<Map<String, String>, Double> evaluator = config -> {

      List<String> argsList = new java.util.ArrayList<>();
      for (var entry : config.entrySet()) {
        argsList.add("--" + entry.getKey());
        argsList.add(entry.getValue());
      }
      String[] configArgs = argsList.toArray(new String[0]);

      TrainingSetRunner<DoubleSolution> runner = new TrainingSetRunner.Builder<>(
          trainingSet, new DoubleNSGAII(100, parameterSpace), configArgs)
          .outputDir("results/temp_ablation_run")
          .numberOfThreads(8)
          .indicators(List.of(indicator)) // Use the shared indicator instance
          .silent()
          .noOutput() // Disable file writing for intermediate steps
          .build();

      double sumMetricOverRuns = 0.0;
      int runs = 10; // Number of repetitions for scientific consistency

      for (int i = 0; i < runs; i++) {
        Map<String, List<DoubleSolution>> results = runner.run();

        double sumMetricThisRun = 0.0;
        int count = 0;

        List<String> refFronts = trainingSet.referenceFronts();
        List<org.uma.jmetal.problem.Problem<DoubleSolution>> problems = trainingSet.problemList();

        for (int j = 0; j < problems.size(); j++) {
          String problemName = problems.get(j).getClass().getSimpleName();
          List<DoubleSolution> result = results.get(problemName);
          if (result != null && !result.isEmpty()) {
            try {
              double[][] front = org.uma.jmetal.util.SolutionListUtils.getMatrixWithObjectiveValues(result);
              double[][] ref = VectorUtils.readVectors(refFronts.get(j), ",");

              indicator.referenceFront(ref);
              double value = indicator.compute(front);

              sumMetricThisRun += value;
              count++;
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }

        if (count == 0) {
          throw new RuntimeException("No valid results obtained for configuration.");
        }
        sumMetricOverRuns += (sumMetricThisRun / count);
      }

      return sumMetricOverRuns / runs;
    };

    // 6. Run Ablation
    AblationAnalysis analysis = new AblationAnalysis(evaluator, isMaximization);

    System.out.println("Starting Training Set Path Ablation...");
    AblationResult pathResults = analysis.performPathAblation(defaultConfigStr, optimizedConfigStr);

    Path outputDir = Paths.get("results/ablation/RWA3D");
    Files.createDirectories(outputDir);
    Files.writeString(outputDir.resolve("path_ablation.csv"), pathResults.toCSV());

    System.out.println("Analysis complete. Saved to " + outputDir);

    System.out.println("Starting Training Set Leave-One-Out Ablation...");
    AblationResult looResults = analysis.performLeaveOneOut(optimizedConfigStr, defaultConfigStr);
    Files.writeString(outputDir.resolve("loo_ablation.csv"), looResults.toCSV());

    System.out.println("LOO Analysis complete. Saved to " + outputDir);
  }
}
