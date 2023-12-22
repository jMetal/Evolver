package org.uma.evolver.util;

import java.util.Map;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

/**
 * This observer prints the current evaluation number of an algorithm. It expects a pair
 * (EVALUATIONS, int) in the map used in the update() method.
 *
 * @author Antonio J. Nebro
 */
public class EvaluationObserver implements Observer<Map<String, Object>> {

  private Integer frequency ;

  /**
   * Constructor
   * @param frequency Print frequency in terms of times the update method has been invoked
   */
  public EvaluationObserver(Integer frequency) {
    this.frequency = frequency ;
  }

  public EvaluationObserver() {
    this(1) ;
  }

  /**
   * This method gets the evaluation number from the dada map and prints it in the screen.
   * @param data Map of pairs (key, value)
   */
  @Override
  public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
    Integer evaluations = (Integer)data.get("EVALUATIONS") ;
    long computing_time = (long)data.get("COMPUTING_TIME") ;

    if (evaluations!=null) {
      if (evaluations % frequency == 0) {
        JMetalLogger.logger.info("Evaluations: " + evaluations + ". Time: " + computing_time);
      }
    } else {
      JMetalLogger.logger.warning(getClass().getName()+
          ": The algorithm has not registered yet any info related to the EVALUATIONS key");
    }
  }

  public String getName() {
    return "Evaluation observer";
  }
}
