package org.uma.evolver.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Utility class for generating estimated reference fronts from existing
 * reference fronts.
 *
 * <h2>Purpose</h2>
 * <p>
 * This utility addresses a realistic scenario in multi-objective optimization
 * where the true
 * Pareto front is unknown. In practice, practitioners often lack access to the
 * exact reference
 * front and must work with estimates based on limited information.
 * </p>
 *
 * <h2>Motivation</h2>
 * <p>
 * When using meta-optimization to configure algorithms for real-world problems
 * (e.g., RE and RWA
 * problem families), using actual reference fronts would be unrealistic. This
 * utility simulates
 * the scenario where only rough estimates of the extreme points are available,
 * as might be
 * derived from domain expertise or preliminary optimization runs.
 * </p>
 *
 * <h2>Approach</h2>
 * <p>
 * The estimation approach consists of the following steps:
 * </p>
 * <ol>
 * <li><strong>Extract Extreme Points:</strong> For each objective, identify the
 * solution in the
 * reference front that has the worst (maximum, for minimization problems) value
 * for that
 * objective. This results in N extreme points for an N-objective problem.</li>
 * <li><strong>Compute Objective Ranges:</strong> Calculate the range (max -
 * min) of each
 * objective across the entire reference front.</li>
 * <li><strong>Apply Range-Based Offset:</strong> Shift each extreme point
 * outward by adding
 * an offset proportional to the range of each objective:
 * 
 * <pre>
 * shifted_value[i] = original_value[i] + (offsetPercentage × range[i])
 * </pre>
 * 
 * This makes the estimated front slightly pessimistic (worse than the true
 * front),
 * simulating uncertainty in the practitioner's knowledge.</li>
 * </ol>
 *
 * <h2>Use Case: Hypervolume and Epsilon Indicator</h2>
 * <p>
 * The estimated reference front is designed to be used with:
 * </p>
 * <ul>
 * <li><strong>Hypervolume (HV):</strong> The extreme points define the
 * reference point for HV
 * calculation. The offset ensures the reference point lies beyond reasonable
 * solutions,
 * avoiding HV = 0 for good configurations.</li>
 * <li><strong>Epsilon Indicator (EP):</strong> Uses the extreme points to
 * compute convergence.
 * Even when HV = 0 (poor configurations), EP provides gradient information to
 * guide
 * the meta-optimization toward better regions (multiobjectivization).</li>
 * </ul>
 *
 * <h2>Range-Based Offset Justification</h2>
 * <p>
 * Using a range-based offset rather than a fixed percentage of the absolute
 * value provides:
 * </p>
 * <ul>
 * <li><strong>Scale invariance:</strong> Works correctly regardless of
 * objective scales
 * (e.g., [0,1] vs [1000,5000])</li>
 * <li><strong>Proportional uncertainty:</strong> Larger ranges suggest more
 * "room" for
 * estimation error</li>
 * <li><strong>Cross-problem consistency:</strong> The same percentage parameter
 * works
 * sensibly for different problems with different objective scales</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <p>
 * For a 3-objective problem with offset = 0.10 (10%):
 * </p>
 * 
 * <pre>
 * Original Reference Front:
 *   obj1: [0.01, 0.05], range = 0.04
 *   obj2: [100, 500],   range = 400
 *   obj3: [2.5, 8.0],   range = 5.5
 *
 * Extreme Points (before offset):
 *   Point A (worst obj1): (0.05, 200, 5.0)
 *   Point B (worst obj2): (0.02, 500, 6.5)
 *   Point C (worst obj3): (0.03, 300, 8.0)
 *
 * Range-based offsets:
 *   offset_obj1 = 0.10 × 0.04 = 0.004
 *   offset_obj2 = 0.10 × 400 = 40
 *   offset_obj3 = 0.10 × 5.5 = 0.55
 *
 * Estimated Extreme Points (after offset):
 *   Point A: (0.054, 240, 5.55)
 *   Point B: (0.024, 540, 7.05)
 *   Point C: (0.034, 340, 8.55)
 * </pre>
 *
 * @author Antonio J. Nebro
 * @see org.uma.jmetal.qualityindicator.impl.Epsilon
 * @see org.uma.jmetal.qualityindicator.impl.hypervolume.Hypervolume
 */
public class EstimatedReferenceFrontGenerator {

  private final double offsetPercentage;
  private final String outputDirectory;

  /**
   * Constructs a generator with the specified offset percentage and output
   * directory.
   *
   * @param offsetPercentage the percentage of each objective's range to use as
   *                         offset
   *                         (e.g., 0.10 for 10%)
   * @param outputDirectory  the directory where estimated reference fronts will
   *                         be saved
   * @throws IllegalArgumentException if offsetPercentage is negative or
   *                                  outputDirectory is null
   */
  public EstimatedReferenceFrontGenerator(double offsetPercentage, String outputDirectory) {
    if (offsetPercentage < 0) {
      throw new IllegalArgumentException("Offset percentage must be non-negative: " + offsetPercentage);
    }
    if (outputDirectory == null || outputDirectory.isBlank()) {
      throw new IllegalArgumentException("Output directory cannot be null or blank");
    }
    this.offsetPercentage = offsetPercentage;
    this.outputDirectory = outputDirectory;
  }

  /**
   * Generates an estimated reference front from the given reference front file.
   *
   * @param referenceFrontPath the path to the original reference front CSV file
   * @param outputFileName     the name of the output file (without directory)
   * @return the generated estimated reference front as a 2D array
   * @throws JMetalException if the file cannot be read or written
   */
  public double[][] generateEstimatedReferenceFront(String referenceFrontPath, String outputFileName) {
    try {
      // Read the original reference front
      double[][] referenceFront = VectorUtils.readVectors(referenceFrontPath, ",");

      if (referenceFront.length == 0) {
        throw new JMetalException("Reference front is empty: " + referenceFrontPath);
      }

      int numObjectives = referenceFront[0].length;

      // Compute min and max values for each objective
      double[] minValues = computeMinValues(referenceFront);
      double[] maxValues = computeMaxValues(referenceFront);
      double[] ranges = new double[numObjectives];
      for (int i = 0; i < numObjectives; i++) {
        ranges[i] = maxValues[i] - minValues[i];
      }

      // Find extreme points (one per objective)
      List<double[]> extremePoints = new ArrayList<>();
      for (int objIndex = 0; objIndex < numObjectives; objIndex++) {
        double[] extremePoint = findExtremeSolutionForObjective(referenceFront, objIndex);
        extremePoints.add(extremePoint);
      }

      // Apply range-based offset to all extreme points
      double[][] estimatedFront = new double[numObjectives][numObjectives];
      for (int pointIndex = 0; pointIndex < numObjectives; pointIndex++) {
        double[] originalPoint = extremePoints.get(pointIndex);
        for (int objIndex = 0; objIndex < numObjectives; objIndex++) {
          double offset = offsetPercentage * ranges[objIndex];
          estimatedFront[pointIndex][objIndex] = originalPoint[objIndex] + offset;
        }
      }

      // Save to file
      saveEstimatedReferenceFront(estimatedFront, outputFileName);

      return estimatedFront;

    } catch (IOException e) {
      throw new JMetalException("Error processing reference front: " + referenceFrontPath, e);
    }
  }

  /**
   * Finds the solution with the maximum value for the specified objective.
   *
   * @param front    the reference front
   * @param objIndex the objective index
   * @return a copy of the extreme solution for that objective
   */
  private double[] findExtremeSolutionForObjective(double[][] front, int objIndex) {
    double[] bestSolution = front[0];
    double bestValue = front[0][objIndex];

    for (int i = 1; i < front.length; i++) {
      if (front[i][objIndex] > bestValue) {
        bestValue = front[i][objIndex];
        bestSolution = front[i];
      }
    }

    return Arrays.copyOf(bestSolution, bestSolution.length);
  }

  /**
   * Computes the minimum value for each objective in the front.
   *
   * @param front the reference front
   * @return array of minimum values
   */
  private double[] computeMinValues(double[][] front) {
    int numObjectives = front[0].length;
    double[] minValues = new double[numObjectives];
    Arrays.fill(minValues, Double.MAX_VALUE);

    for (double[] solution : front) {
      for (int i = 0; i < numObjectives; i++) {
        if (solution[i] < minValues[i]) {
          minValues[i] = solution[i];
        }
      }
    }

    return minValues;
  }

  /**
   * Computes the maximum value for each objective in the front.
   *
   * @param front the reference front
   * @return array of maximum values
   */
  private double[] computeMaxValues(double[][] front) {
    int numObjectives = front[0].length;
    double[] maxValues = new double[numObjectives];
    Arrays.fill(maxValues, Double.MIN_VALUE);

    for (double[] solution : front) {
      for (int i = 0; i < numObjectives; i++) {
        if (solution[i] > maxValues[i]) {
          maxValues[i] = solution[i];
        }
      }
    }

    return maxValues;
  }

  /**
   * Saves the estimated reference front to a CSV file.
   *
   * @param estimatedFront the estimated reference front
   * @param outputFileName the output file name
   * @throws IOException if the file cannot be written
   */
  private void saveEstimatedReferenceFront(double[][] estimatedFront, String outputFileName)
      throws IOException {
    // Create output directory if it doesn't exist
    File directory = new File(outputDirectory);
    if (!directory.exists()) {
      directory.mkdirs();
    }

    String filePath = outputDirectory + "/" + outputFileName;
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      for (double[] point : estimatedFront) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < point.length; i++) {
          if (i > 0) {
            line.append(",");
          }
          line.append(point[i]);
        }
        writer.write(line.toString());
        writer.newLine();
      }
    }

    System.out.println("Generated: " + filePath);
  }

  /**
   * Main method to generate estimated reference fronts for all RE and RWA
   * problems.
   *
   * <p>
   * Usage: java EstimatedReferenceFrontGenerator [offsetPercentage]
   * </p>
   * <p>
   * Default offset percentage is 0.10 (10%)
   * </p>
   *
   * @param args command line arguments (optional: offsetPercentage)
   */
  public static void main(String[] args) {
    double offsetPercentage = 0.30; // Default 30%
    if (args.length > 0) {
      try {
        offsetPercentage = Double.parseDouble(args[0]);
      } catch (NumberFormatException e) {
        System.err.println("Invalid offset percentage: " + args[0] + ". Using default: 0.20");
      }
    }

    String inputDirectory = "resources/referenceFronts";
    String outputDirectory = "resources/estimatedReferenceFronts";

    EstimatedReferenceFrontGenerator generator = new EstimatedReferenceFrontGenerator(offsetPercentage,
        outputDirectory);

    System.out.println("Generating estimated reference fronts with " +
        (offsetPercentage * 100) + "% range-based offset");
    System.out.println("Input directory: " + inputDirectory);
    System.out.println("Output directory: " + outputDirectory);
    System.out.println();

    // Dynamically find all RE and RWA problems
    File inputDirFile = new File(inputDirectory);
    if (!inputDirFile.exists() || !inputDirFile.isDirectory()) {
      System.err.println("Input directory does not exist: " + inputDirectory);
      return;
    }

    File[] files = inputDirFile
        .listFiles((dir, name) -> name.endsWith(".csv") && (name.startsWith("RE") || name.startsWith("RWA")));

    if (files == null || files.length == 0) {
      System.out.println("No matching Reference Front files found in " + inputDirectory);
      return;
    }

    // Sort for consistent output
    Arrays.sort(files, (f1, f2) -> f1.getName().compareTo(f2.getName()));

    System.out.println("Found " + files.length + " RE/RWA reference fronts. Generating estimates...");

    for (File file : files) {
      String problemName = file.getName().replace(".csv", "");
      String outputFileName = file.getName();

      System.out.print("Processing " + problemName + " ... ");
      try {
        generator.generateEstimatedReferenceFront(file.getAbsolutePath(), outputFileName);
        // Success message is printed by save method, but we can add check here if
        // needed.
      } catch (JMetalException e) {
        System.err.println("Failed: " + e.getMessage());
      }
    }

    System.out.println("\nGeneration complete!");
  }
}
