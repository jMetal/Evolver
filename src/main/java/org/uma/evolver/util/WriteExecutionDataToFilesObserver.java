package org.uma.evolver.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

  private List<Integer> evaluationsList;
  private OutputResultsManagement outputResultsManagement;
  private int evaluationsListIndex ;

  /**
   * Constructor
   */
  public WriteExecutionDataToFilesObserver(List<Integer> evaluationsList,
      OutputResultsManagement outputResultsManagement) {
    this.evaluationsList = evaluationsList;
    this.outputResultsManagement = outputResultsManagement;
    evaluationsListIndex = 0 ;
  }

  /**
   * Constructor
   */
  public WriteExecutionDataToFilesObserver(int frequency, int evaluationsLimit,
      OutputResultsManagement outputResultsManagement) {
    evaluationsList = IntStream.rangeClosed(1, evaluationsLimit)
        .filter(num -> (num) % frequency == 0)
        .boxed()
        .toList() ;
    //evaluationsList.stream().forEach(i -> System.out.print(i + ", "));
    //System.out.println() ;
    this.outputResultsManagement = outputResultsManagement;
    evaluationsListIndex = 0 ;
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
    if ((evaluationsListIndex < evaluationsList.size()) && (evaluations > evaluationsList.get(evaluationsListIndex))) {
      try {
        JMetalLogger.logger.info("EVAlS -> "+evaluations) ;
        outputResultsManagement.updateSuffix("." + evaluationsList.get(evaluationsListIndex) + ".csv");
        outputResultsManagement.writeResultsToFiles(population);
        evaluationsListIndex ++ ;
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
