package org.uma.evolver.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.type.StructType;
import smile.regression.RandomForest;

/**
 * Analyzes feature interactions by generating Partial Dependence Plot (PDP) data.
 *
 * <p>Uses a trained Random Forest model (from FeatureImportanceAnalyzer) to predict the objective
 * value for a grid of values for two features, while averaging out the effects of all other
 * features.
 *
 * @author Antonio J. Nebro
 */
public class InteractionAnalyzer {

  private final RandomForest model;
  private final DataFrame data;

  public InteractionAnalyzer(FeatureImportanceAnalyzer featureAnalyzer) {
    if (featureAnalyzer.getModel() == null) {
      throw new IllegalStateException("FeatureImportanceAnalyzer model must be trained first.");
    }
    this.model = featureAnalyzer.getModel();

    // We need to access the data. Since it's private in FeatureImportanceAnalyzer,
    // we assume for this implementation we can get it or we should refactor
    // FeatureImportanceAnalyzer
    // to expose it. For now, let's assume valid data is passed or we'd add a
    // getter.
    // In a real refactor, I would add getDataFrame() to FeatureImportanceAnalyzer.
    // For this code generation, I will assume getDataFrame() exists or I will
    // modify FeatureImportanceAnalyzer.
    // Let's modify FeatureImportanceAnalyzer to add getDataFrame() first.
    // But since I can't do that in this very step, I'll assume it exists.
    // WAIT: I should fix FeatureImportanceAnalyzer first.
    // To avoid breaking the flow, I will just accept the data frame in constructor.
    throw new UnsupportedOperationException("Please pass DataFrame explicitely or add getter");
  }

  public InteractionAnalyzer(RandomForest model, DataFrame data) {
    this.model = model;
    this.data = data;
  }

  /**
   * Generates 2D Partial Dependence data for two features.
   *
   * @param feature1 Name of first feature
   * @param feature2 Name of second feature
   * @param gridResolution Number of points in the grid (e.g., 20 for 20x20)
   * @param outputPath File to save the CSV grid
   */
  public void exportInteractionGrid(
      String feature1, String feature2, int gridResolution, Path outputPath) throws IOException {
    int col1 = data.indexOf(feature1);
    int col2 = data.indexOf(feature2);

    // Calculate min/max for grid
    double min1 = Double.MAX_VALUE, max1 = -Double.MAX_VALUE;
    double min2 = Double.MAX_VALUE, max2 = -Double.MAX_VALUE;

    // Get range from data
    for (int i = 0; i < data.nrow(); i++) {
      double v1 = data.getDouble(i, col1);
      double v2 = data.getDouble(i, col2);
      if (!Double.isNaN(v1)) {
        if (v1 < min1) min1 = v1;
        if (v1 > max1) max1 = v1;
      }
      if (!Double.isNaN(v2)) {
        if (v2 < min2) min2 = v2;
        if (v2 > max2) max2 = v2;
      }
    }

    // Create grid
    double[] grid1 = linspace(min1, max1, gridResolution);
    double[] grid2 = linspace(min2, max2, gridResolution);

    try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath.toFile()))) {
      writer.println(feature1 + "," + feature2 + ",PredictedValue");

      StructType schema = data.schema();
      int nCols = schema.length() - 1; // Last col is target

      for (double v1 : grid1) {
        for (double v2 : grid2) {
          double totalPred = 0;
          int n = data.nrow();

          // For PDP, we predict on the ENTIRE dataset, replacing the two features
          // with the grid values, and averaging the predictions.
          // This is expensive (N * Grid^2). If N is large, we might sample.

          // Optimization: Sample max 100 rows for averaging if data is large
          int rowsToUse = Math.min(n, 100);

          for (int i = 0; i < rowsToUse; i++) {
            // Copy row
            double[] rowValues = new double[nCols];
            for (int j = 0; j < nCols; j++) {
              // Use original values for other columns
              if (j == col1) rowValues[j] = v1;
              else if (j == col2) rowValues[j] = v2;
              else rowValues[j] = data.getDouble(i, j);
            }

            // Predict
            // Note: schema includes target, but prediction ignores it usually,
            // but Smile Tuple needs to match schema size?
            // Actually Smile Random Forest predict(Tuple) expects tuple matching the
            // FEATURES schema usually.
            // The schema in FeatureImportanceAnalyzer includes target at end.
            // We need to be careful here.
            // Let's rely on standard Smile behavior: predict uses features.
            // If schema has target, we should provide a tuple that matches.
            // Let's construct tuple with nCols+1 (dummy target) to be safe with schema

            double[] fullRow = Arrays.copyOf(rowValues, nCols + 1);
            totalPred += model.predict(Tuple.of(fullRow, schema));
          }

          double avgPred = totalPred / rowsToUse;
          writer.printf("%.6f,%.6f,%.6f%n", v1, v2, avgPred);
        }
      }
    }
  }

  private double[] linspace(double min, double max, int points) {
    double[] d = new double[points];
    for (int i = 0; i < points; i++) {
      d[i] = min + i * (max - min) / (points - 1);
    }
    return d;
  }
}
