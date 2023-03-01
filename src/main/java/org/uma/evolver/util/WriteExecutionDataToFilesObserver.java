package org.uma.evolver.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.uma.evolver.util.OutputResultsManagement.OutputResultsManagementParameters;
import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/**
 * This observer stores a solution list in files. Concretely, the variables and objectives are
 * written in files called VAR.x.tsv and VAR.x.tsv, respectively (x is an iteration counter). The
 * frequency of the writes are set by a parameter.
 *
 * @author Antonio J. Nebro <antonio@lcc.uma.es>
 */
public class WriteExecutionDataToFilesObserver implements Observer<Map<String, Object>> {

  private List<Integer> evaluationsList;
  private OutputResultsManagement outputResultsManagement;

  /**
   * Constructor
   */

  public WriteExecutionDataToFilesObserver(List<Integer> evaluationsList,
      OutputResultsManagement outputResultsManagement) {
    this.evaluationsList = evaluationsList;
    this.outputResultsManagement = outputResultsManagement;
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

    if (!evaluationsList.isEmpty() && evaluations > evaluationsList.get(0)) {
      try {
        outputResultsManagement.updateSuffix("" + evaluationsList.get(0) + ".csv");
        outputResultsManagement.writeResultsToFiles(population);
      } catch (IOException e) {
        throw new JMetalException(e);
      }

      evaluationsList.remove(0);
    }
  }

  public String name() {
    return "Write execution data to files observer";
  }

  @Override
  public String toString() {
    return name();
  }
}
