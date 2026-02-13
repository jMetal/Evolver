package org.uma.evolver.analysis.ablation;

/**
 * Full configuration-based DTLZ 3D ablation example.
 */
public class DTLZ3DAblationAnalysisExample {

  /**
   * Entry point.
   *
   * @param args command-line arguments (not used)
   */
  public static void main(String[] args) {
    AblationConfiguration config = AblationConfiguration.forDTLZProblems()
        .analysisName("DTLZ3D Ablation Analysis")
        .numberOfRuns(25)
        .numberOfThreads(8)
        .maxEvaluations(40000)
        .outputPrefix("dtlz_ablation")
        .enableProgressReporting(true);

    new AblationRunner(config).run();
  }
}
