package org.uma.evolver.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/**
 * This observer stores a solution list in files. Concretely, the variables and objectives are
 * written in files called VAR.x.tsv and VAR.x.tsv, respectively (x is an iteration counter). The
 * frequency of the writes are set by a parameter.
 *
 * @author Antonio J. Nebro
 */
public class WriteExecutionDataToFilesObserver implements Observer<Map<String, Object>> {

  private OutputResultsManagement outputResultsManagement;
  private int frequency;

  /**
   * Constructor
   */
  public WriteExecutionDataToFilesObserver(int frequency, int evaluationsLimit,
      OutputResultsManagement outputResultsManagement) {
    this.outputResultsManagement = outputResultsManagement;
    this.frequency = frequency ;
  }


  /**
   * This method gets the population
   *
   * @param data Map of pairs (key, value)
   */
  @Override
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    List<DoubleSolution> population = (List<DoubleSolution>) data.get("POPULATION");
    int evaluations = (int) data.get("EVALUATIONS");
    if ((evaluations % frequency) == 0) {
      try {
        JMetalLogger.logger.info("EVAlS -> "+evaluations) ;
        outputResultsManagement.updateSuffix("." + evaluations + ".csv");
        outputResultsManagement.writeResultsToFiles(population);
      } catch (IOException e) {
        throw new JMetalException(e);
      }
    }
  }

  @Override
  public String toString() {
    return "Observer that writes output files from a list of numbers representing "
        + "iterations where the outputs are required" ;
  }
}
