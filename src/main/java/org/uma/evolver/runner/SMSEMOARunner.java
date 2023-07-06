package org.uma.evolver.runner;

import java.io.IOException;
import org.uma.evolver.MetaOptimizer;
import org.uma.evolver.MetaRunner;

public class SMSEMOARunner {
/*
Expected arguments:
 - External algorithm arguments:
   * 0 - meta-optimizer algorithm: The name of the meta-optimizer algorithm.
   * 1 - meta-optimizer population: The population size for the meta-optimizer algorithm.
   * 2 - meta-optimizer max evaluations: The maximum number of evaluations for the meta-optimizer algorithm.
   * 3 - independent runs: The number of independent runs for the meta-optimizer algorithm.
   * 4 - indicators names: The names of the indicators used as objectives by the meta-optimizer algorithm.
   * 5 - output directory: The directory where the output will be saved.
 - Internal algorithm arguments:
   * 6 - configurable algorithm: The name of the internal configurable algorithm.
   * 7 - population: The population size for the internal algorithm.
   * 8 - problem names: The names of the problems to be solved. It can be provided as a list of comma separated values E.g.: "ZDT1,ZDT4"
   * 9 - reference front file name: The file name of the reference front. It can be provided as a list of comma separated values E.g.: "ZDT1.csv,ZDT4.csv"
   * 10 - max number of evaluations: The maximum number of evaluations for the internal algorithm. It can be provided as a list of comma separated values E.g.: "8000,16000"
 - Optional specific arguments:
   * 11 - weight vector files directory: The directory containing weight vector files. Only used for the MOEAD internal algorithm.

     */

  private static String[] parameters = ("" +
      "NSGAII " +
      "50 " +
      "2000 " +
      "1 " +
      "NormalizedHypervolume,Epsilon " +
      "resuls/smsemoa " +
      "NSGAII " +
      "50 " +
      "ZDT1 " +
      "resources/referenceFronts/ZDT1.csv " +
      "6000 "
  )
      .split(" ");

  public static void main(String[] args) throws IOException {
    MetaRunner runner = new MetaRunner();

    runner.main(parameters);


  }
}
