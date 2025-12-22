package org.uma.evolver.analysis;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import smile.data.DataFrame;
import smile.data.Tuple;
import smile.data.formula.Formula;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.regression.RandomForest;

/**
 * Analyzes feature importance of algorithm parameters using Random Forest.
 *
 * <p>
 * This analyzer reads the output files from a meta-optimization experiment
 * (CONFIGURATIONS.csv
 * and INDICATORS.csv) and trains a Random Forest model to determine which
 * parameters most strongly
 * influence the quality indicators.
 *
 * <p>
 * Two importance measures are provided:
 *
 * <ul>
 * <li><b>Gini Importance</b>: Built-in Random Forest importance based on the
 * decrease in impurity
 * when a variable is used for splitting.
 * <li><b>Permutation Importance</b>: More robust measure that calculates the
 * decrease in model
 * performance when a feature's values are permuted.
 * </ul>
 *
 * @author Antonio J. Nebro
 */
public class FeatureImportanceAnalyzer {

  private final Path resultsDirectory;
  private DataFrame data;
  private String[] featureNames;
  private String targetIndicator;
  private RandomForest model;

  // Model hyperparameters
  private int numberOfTrees = 100;
  private int maxDepth = 10;
  private int minNodeSize = 5;
  private int seed = 42;

  /**
   * Creates a new FeatureImportanceAnalyzer.
   *
   * @param resultsDirectory path to the directory containing CONFIGURATIONS.csv
   *                         and INDICATORS.csv
   */
  public FeatureImportanceAnalyzer(Path resultsDirectory) {
    this.resultsDirectory = resultsDirectory;
  }

  /**
   * Loads data from CONFIGURATIONS.csv and INDICATORS.csv files.
   *
   * @param targetIndicator the name of the indicator to use as target variable
   * @throws IOException if files cannot be read
   */
  public void loadData(String targetIndicator) throws IOException {
    this.targetIndicator = targetIndicator;

    Path configPath = resultsDirectory.resolve("CONFIGURATIONS.csv");
    Path indicatorsPath = resultsDirectory.resolve("INDICATORS.csv");

    // Load configurations
    var configRows = readCSV(configPath);
    if (configRows.isEmpty()) {
      throw new IOException("CONFIGURATIONS.csv is empty");
    }
    final String[] configHeader = configRows.get(0);
    final List<String[]> configData = configRows.subList(1, configRows.size());

    // Load indicators
    var indicatorRows = readCSV(indicatorsPath);
    if (indicatorRows.isEmpty()) {
      throw new IOException("INDICATORS.csv is empty");
    }
    String[] indicatorHeader = indicatorRows.get(0);
    final List<String[]> indicatorData = indicatorRows.subList(1, indicatorRows.size());

    // Find target indicator column
    int targetCol = -1;
    for (int i = 0; i < indicatorHeader.length; i++) {
      if (indicatorHeader[i].equals(targetIndicator)) {
        targetCol = i;
        break;
      }
    }
    if (targetCol == -1) {
      throw new IllegalArgumentException(
          "Target indicator '"
              + targetIndicator
              + "' not found. Available: "
              + Arrays.toString(indicatorHeader));
    }

    // Build merged dataset (matching by Evaluation and SolutionId)
    // Config columns (excluding Evaluation, SolutionId) + target indicator
    int evalColConfig = indexOf(configHeader, "Evaluation");
    int solIdColConfig = indexOf(configHeader, "SolutionId");
    int evalColInd = indexOf(indicatorHeader, "Evaluation");
    int solIdColInd = indexOf(indicatorHeader, "SolutionId");

    // Feature columns (all config columns except Evaluation and SolutionId)
    List<String> featureNamesList = new ArrayList<>();
    List<Integer> featureCols = new ArrayList<>();
    for (int i = 0; i < configHeader.length; i++) {
      if (i != evalColConfig && i != solIdColConfig) {
        featureNamesList.add(configHeader[i]);
        featureCols.add(i);
      }
    }
    this.featureNames = featureNamesList.toArray(new String[0]);

    // Create index map for indicators (Evaluation_SolutionId -> row)
    var indicatorIndex = new LinkedHashMap<String, Integer>();
    for (int i = 0; i < indicatorData.size(); i++) {
      String key = indicatorData.get(i)[evalColInd] + "_" + indicatorData.get(i)[solIdColInd];
      indicatorIndex.put(key, i);
    }

    // Build merged data arrays
    var mergedFeatures = new ArrayList<double[]>();
    var mergedTargets = new ArrayList<Double>();
    final int finalTargetCol = targetCol;

    for (var configRow : configData) {
      String key = configRow[evalColConfig] + "_" + configRow[solIdColConfig];
      Integer indRowIdx = indicatorIndex.get(key);

      if (indRowIdx != null) {
        // Extract features (handling NaN values - store as NaN for now)
        double[] features = new double[featureCols.size()];
        for (int j = 0; j < featureCols.size(); j++) {
          String val = configRow[featureCols.get(j)];
          try {
            if (val == null || val.isBlank() || val.equalsIgnoreCase("NaN")) {
              features[j] = Double.NaN;
            } else {
              features[j] = Double.parseDouble(val);
            }
          } catch (NumberFormatException e) {
            // Log warning? For now just treat as missing
            features[j] = Double.NaN;
          }
        }

        mergedFeatures.add(features);
        try {
          String targetVal = indicatorData.get(indRowIdx)[finalTargetCol];
          mergedTargets.add(Double.parseDouble(targetVal));
        } catch (NumberFormatException e) {
          // Skip row if target is invalid
          mergedFeatures.remove(mergedFeatures.size() - 1);
        }
      }
    }

    if (mergedFeatures.isEmpty()) {
      throw new IOException("No valid data rows after merging");
    }

    // Impute NaN values with column median
    int nCols = featureNames.length;
    double[] medians = new double[nCols];

    for (int col = 0; col < nCols; col++) {
      // Collect non-NaN values for this column
      List<Double> validValues = new ArrayList<>();
      for (double[] row : mergedFeatures) {
        if (!Double.isNaN(row[col])) {
          validValues.add(row[col]);
        }
      }

      // Calculate median
      if (validValues.isEmpty()) {
        medians[col] = 0.0; // Default if all values are NaN
      } else {
        validValues.sort(Double::compareTo);
        int mid = validValues.size() / 2;
        medians[col] = validValues.size() % 2 == 0
            ? (validValues.get(mid - 1) + validValues.get(mid)) / 2.0
            : validValues.get(mid);
      }
    }

    // Replace NaN values with medians
    for (double[] row : mergedFeatures) {
      for (int col = 0; col < nCols; col++) {
        if (Double.isNaN(row[col])) {
          row[col] = medians[col];
        }
      }
    }

    // Create Smile DataFrame
    int nRows = mergedFeatures.size();

    // Build schema
    StructField[] fields = new StructField[nCols + 1];
    for (int i = 0; i < nCols; i++) {
      fields[i] = new StructField(featureNames[i], DataTypes.DoubleType);
    }
    fields[nCols] = new StructField(targetIndicator, DataTypes.DoubleType);
    StructType schema = new StructType(fields);

    // Build data vectors
    List<Tuple> tuples = new ArrayList<>();
    for (int i = 0; i < nRows; i++) {
      double[] row = new double[nCols + 1];
      System.arraycopy(mergedFeatures.get(i), 0, row, 0, nCols);
      row[nCols] = mergedTargets.get(i);
      tuples.add(Tuple.of(row, schema));
    }

    this.data = DataFrame.of(tuples);

    System.out.println("Loaded " + nRows + " samples with " + nCols + " features");
    System.out.println("Target indicator: " + targetIndicator);
  }

  /** Trains a Random Forest model on the loaded data. */
  public void trainModel() {
    if (data == null) {
      throw new IllegalStateException("Data not loaded. Call loadData() first.");
    }

    Formula formula = Formula.lhs(targetIndicator);

    model = RandomForest.fit(
        formula,
        data,
        numberOfTrees, // ntrees
        featureNames.length / 3 + 1, // mtry (sqrt for classification, /3 for regression)
        maxDepth, // maxDepth
        512, // maxNodes
        minNodeSize, // nodeSize
        1.0 // subsample ratio
    );

    System.out.println("Trained Random Forest with " + numberOfTrees + " trees");
    System.out.println("Model R²: " + String.format("%.4f", calculateR2()));
  }

  /** Calculates R² score for the trained model. */
  private double calculateR2() {
    double[] predictions = new double[data.nrow()];
    double[] actuals = new double[data.nrow()];
    Formula formula = Formula.lhs(targetIndicator);

    for (int i = 0; i < data.nrow(); i++) {
      predictions[i] = model.predict(data.get(i));
      actuals[i] = formula.y(data).getDouble(i);
    }

    double meanActual = Arrays.stream(actuals).average().orElse(0);
    double ssRes = 0, ssTot = 0;
    for (int i = 0; i < actuals.length; i++) {
      ssRes += Math.pow(actuals[i] - predictions[i], 2);
      ssTot += Math.pow(actuals[i] - meanActual, 2);
    }

    return 1 - (ssRes / ssTot);
  }

  /**
   * Returns the Gini-based feature importance from the Random Forest.
   *
   * @return map of feature names to importance scores, sorted by importance
   */
  public Map<String, Double> getGiniImportance() {
    if (model == null) {
      throw new IllegalStateException("Model not trained. Call trainModel() first.");
    }

    double[] importance = model.importance();

    // Normalize by max importance for readability (0-1 scale)
    double maxImp = Arrays.stream(importance).max().orElse(1.0);
    if (maxImp > 0) {
      for (int i = 0; i < importance.length; i++) {
        importance[i] /= maxImp;
      }
    }

    // Create sorted map
    var result = new LinkedHashMap<String, Double>();
    Integer[] indices = IntStream.range(0, featureNames.length).boxed().toArray(Integer[]::new);
    // Sort indices based on importance
    final double[] finalImp = importance;
    Arrays.sort(indices, (a, b) -> Double.compare(finalImp[b], finalImp[a]));

    for (int idx : indices) {
      result.put(featureNames[idx], importance[idx]);
    }

    return result;
  }

  /**
   * Calculates Permutation Feature Importance.
   *
   * <p>
   * For each feature, randomly permutes its values and measures the decrease in
   * model
   * performance. Features that cause larger performance drops when permuted are
   * more important.
   *
   * @param numPermutations number of permutations per feature
   * @return map of feature names to importance scores, sorted by importance
   */
  public Map<String, Double> getPermutationImportance(int numPermutations) {
    if (model == null) {
      throw new IllegalStateException("Model not trained. Call trainModel() first.");
    }

    Random random = new Random(seed);
    Formula formula = Formula.lhs(targetIndicator);

    // Calculate baseline MSE
    double baselineMSE = calculateMSE(data, formula);

    double[] importances = new double[featureNames.length];

    for (int f = 0; f < featureNames.length; f++) {
      double sumIncrease = 0;

      for (int p = 0; p < numPermutations; p++) {
        // Create permuted data
        DataFrame permuted = permuteColumn(data, featureNames[f], random);
        double permutedMSE = calculateMSE(permuted, formula);
        sumIncrease += (permutedMSE - baselineMSE);
      }

      importances[f] = sumIncrease / numPermutations;
    }

    // Normalize by max importance
    double maxImp = Arrays.stream(importances).max().orElse(1.0);
    if (maxImp > 0) {
      for (int i = 0; i < importances.length; i++) {
        importances[i] /= maxImp;
      }
    }

    // Create sorted map
    var result = new LinkedHashMap<String, Double>();
    Integer[] indices = IntStream.range(0, featureNames.length).boxed().toArray(Integer[]::new);
    final double[] finalImportances = importances;
    Arrays.sort(indices, (a, b) -> Double.compare(finalImportances[b], finalImportances[a]));

    for (int idx : indices) {
      result.put(featureNames[idx], importances[idx]);
    }

    return result;
  }

  private double calculateMSE(DataFrame df, Formula formula) {
    double mse = 0;
    for (int i = 0; i < df.nrow(); i++) {
      double pred = model.predict(df.get(i));
      double actual = formula.y(df).getDouble(i);
      mse += Math.pow(actual - pred, 2);
    }
    return mse / df.nrow();
  }

  private DataFrame permuteColumn(DataFrame df, String columnName, Random random) {
    int colIdx = df.indexOf(columnName);
    int n = df.nrow();

    // Create permuted indices
    int[] perm = IntStream.range(0, n).toArray();
    for (int i = n - 1; i > 0; i--) {
      int j = random.nextInt(i + 1);
      int tmp = perm[i];
      perm[i] = perm[j];
      perm[j] = tmp;
    }

    // Build new tuples with permuted column
    List<Tuple> newTuples = new ArrayList<>();
    StructType schema = df.schema();

    for (int i = 0; i < n; i++) {
      double[] values = new double[schema.length()];
      for (int j = 0; j < schema.length(); j++) {
        if (j == colIdx) {
          values[j] = df.getDouble(perm[i], j);
        } else {
          values[j] = df.getDouble(i, j);
        }
      }
      newTuples.add(Tuple.of(values, schema));
    }

    return DataFrame.of(newTuples);
  }

  /**
   * Generates a formatted report of feature importance.
   *
   * @return formatted string report
   */
  public String generateReport() {
    StringBuilder sb = new StringBuilder();
    sb.append("""
        ╔══════════════════════════════════════════════════════════════════════════╗
        ║           FEATURE IMPORTANCE ANALYSIS REPORT                            ║
        ╠══════════════════════════════════════════════════════════════════════════╣
        """);
    sb.append(String.format("║ Target Indicator: %-55s ║%n", targetIndicator));
    sb.append(String.format("║ Samples: %-64d ║%n", data.nrow()));
    sb.append(String.format("║ Features: %-63d ║%n", featureNames.length));
    sb.append(String.format("║ Model R²: %-63.4f ║%n", calculateR2()));
    sb.append("""
        ╠══════════════════════════════════════════════════════════════════════════╣
        ║ Rank │ Parameter                        │ Gini Imp. │ Perm. Imp. ║
        ╠══════════════════════════════════════════════════════════════════════════╣
        """);

    final Map<String, Double> giniImp = getGiniImportance();
    final Map<String, Double> permImp = getPermutationImportance(10);

    int rank = 1;
    for (Map.Entry<String, Double> entry : giniImp.entrySet()) {
      String param = entry.getKey();
      double gini = entry.getValue();
      double perm = permImp.getOrDefault(param, 0.0);
      sb.append(
          String.format(
              "║ %4d │ %-32s │ %9.4f │ %10.4f ║%n", rank++, truncate(param, 32), gini, perm));

      if (rank > 20) {
        sb.append(
            String.format(
                "║      │ ... (%d more parameters)         │           │            ║%n",
                featureNames.length - 20));
        break;
      }
    }

    sb.append("╚══════════════════════════════════════════════════════════════════════════╝\n");

    return sb.toString();
  }

  /**
   * Exports importance data to CSV format.
   *
   * @return CSV string with parameter importance rankings
   */
  public String toCSV() {
    Map<String, Double> giniImp = getGiniImportance();
    Map<String, Double> permImp = getPermutationImportance(10);

    StringBuilder sb = new StringBuilder();
    sb.append("Rank,Parameter,GiniImportance,PermutationImportance\n");

    int rank = 1;
    for (Map.Entry<String, Double> entry : giniImp.entrySet()) {
      String param = entry.getKey();
      double gini = entry.getValue();
      double perm = permImp.getOrDefault(param, 0.0);
      sb.append(String.format("%d,%s,%.6f,%.6f%n", rank++, param, gini, perm));
    }

    return sb.toString();
  }

  // Helper methods

  private List<String[]> readCSV(Path path) throws IOException {
    var rows = new ArrayList<String[]>();
    var lines = java.nio.file.Files.readAllLines(path, StandardCharsets.UTF_8);
    for (String line : lines) {
      if (!line.trim().isEmpty()) {
        // Simple splitting, assuming no quoted commas
        rows.add(line.split(","));
      }
    }
    return rows;
  }

  private int indexOf(String[] array, String value) {
    for (int i = 0; i < array.length; i++) {
      if (value.equals(array[i])) {
        return i;
      }
    }
    return -1;
  }

  private String truncate(String s, int maxLen) {
    return s.length() > maxLen ? s.substring(0, maxLen - 3) + "..." : s;
  }

  // Configuration methods

  public FeatureImportanceAnalyzer setNumberOfTrees(int numberOfTrees) {
    this.numberOfTrees = numberOfTrees;
    return this;
  }

  public FeatureImportanceAnalyzer setMaxDepth(int maxDepth) {
    this.maxDepth = maxDepth;
    return this;
  }

  public FeatureImportanceAnalyzer setSeed(int seed) {
    this.seed = seed;
    return this;
  }

  public String[] getFeatureNames() {
    return featureNames;
  }

  public RandomForest getModel() {
    return model;
  }

  public DataFrame getData() {
    return data;
  }
}
