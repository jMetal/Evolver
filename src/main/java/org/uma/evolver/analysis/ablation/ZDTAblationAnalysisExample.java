package org.uma.evolver.analysis.ablation;

/**
 * Full configuration-based ZDT ablation example.
 */
public class ZDTAblationAnalysisExample {

  /**
   * Entry point.
   *
   * @param args command-line arguments (not used)
   */
  public static void main(String[] args) {
    AblationConfiguration config = AblationConfiguration.forZDTProblems()
        .analysisName("ZDT Ablation Analysis")
        .numberOfRuns(25)
        .numberOfThreads(8)
        .outputPrefix("zdt_ablation")
        .enableProgressReporting(true);

    new AblationRunner(config).run();
  }
}
