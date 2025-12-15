package org.uma.evolver.example.analysis;

import java.io.FileWriter;
import java.nio.file.Path;
import org.uma.evolver.analysis.FeatureImportanceAnalyzer;

/**
 * Example demonstrating feature importance analysis on meta-optimization
 * results.
 *
 * <p>
 * This example loads the output from a meta-optimization experiment and
 * analyzes which
 * parameters have the most influence on the optimization outcome.
 *
 * @author Antonio J. Nebro
 */
public class FeatureImportanceExample {

  public static void main(String[] args) throws Exception {
    // Path to results directory (change as needed)
    Path resultsDir = Path.of("results/nsgaii/ZDT");

    // Create analyzer
    FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir).setNumberOfTrees(100).setMaxDepth(10)
        .setSeed(42);

    // Load data using EP (Epsilon) as target indicator
    // You can also use "NHV" (Normalized Hypervolume) or any other indicator
    System.out.println("Loading data...");
    analyzer.loadData("NHV");

    // Train Random Forest model
    System.out.println("\nTraining Random Forest model...");
    analyzer.trainModel();

    // Generate and print report
    System.out.println("\n" + analyzer.generateReport());

    // Export to CSV
    String csvOutput = analyzer.toCSV();
    String csvFile = resultsDir.resolve("feature_importance.csv").toString();
    try (FileWriter writer = new FileWriter(csvFile)) {
      writer.write(csvOutput);
    }
    System.out.println("Results exported to: " + csvFile);

    // Also show top 10 parameters with both importance types
    System.out.println("\n=== Summary: Top 10 Most Important Parameters ===");
    var giniImp = analyzer.getGiniImportance();
    var permImp = analyzer.getPermutationImportance(10);

    int count = 0;
    for (String param : giniImp.keySet()) {
      if (count++ >= 10)
        break;
      System.out.printf(
          "%2d. %-35s Gini: %.4f, Perm: %.4f%n",
          count, param, giniImp.get(param), permImp.get(param));
    }

    // Interaction Analysis for Top 2 Parameters
    if (giniImp.size() >= 2) {
      System.out.println("\n=== Interaction Analysis for Top 2 Parameters ===");
      String[] params = giniImp.keySet().toArray(new String[0]);
      String param1 = params[0];
      String param2 = params[1];

      org.uma.evolver.analysis.InteractionAnalyzer interaction = new org.uma.evolver.analysis.InteractionAnalyzer(
          analyzer);

      String interactionFile = "interaction_" + param1 + "_" + param2 + ".csv";
      interaction.exportInteractionGrid(
          param1, param2, 20, resultsDir.resolve(interactionFile));
      System.out.println("Interaction grid exported to: " + resultsDir.resolve(interactionFile));
    }
  }
}
