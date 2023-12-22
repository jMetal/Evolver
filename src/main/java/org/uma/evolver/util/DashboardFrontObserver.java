package org.uma.evolver.util;

import org.uma.jmetal.solution.Solution;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.observable.Observable;
import org.uma.jmetal.util.observer.Observer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author José F. Aldana-Martín
 */
public class DashboardFrontObserver<S extends Solution<?>> implements Observer<Map<String, Object>> {
    private Integer evaluations;
    private final int plotUpdateFrequency;

    private String template;

    /**
     * Constructor
     */
    public DashboardFrontObserver(String title, String xAxisTitle, String yAxisTitle, String legend,
                                  int plotUpdateFrequency) {
        template = "{" +
                "\"title\": \"" + title + "\", " +
                "\"xAxis\": \"" + xAxisTitle + "\", " +
                "\"yAxis\": \"" + yAxisTitle + "\", " +
                "\"xValues\": %s, " +
                "\"yValues\": %s, " +
                "\"evaluations\": %d, " +
                "\"legend\": \"" + legend + "\"" +
                "}";
        this.plotUpdateFrequency = plotUpdateFrequency;
    }

    /**
     * This method displays a front (population)
     *
     * @param data Map of pairs (key, value)
     */
    @Override
    public void update(Observable<Map<String, Object>> observable, Map<String, Object> data) {
        evaluations = (Integer) data.get("EVALUATIONS");
        List<S> population = (List<S>) data.get("POPULATION");

        if (evaluations != null && population != null) {
            if (evaluations % plotUpdateFrequency == 0) {
                List<Double> objective1 = population.stream().map(s -> s.objectives()[0]).collect(Collectors.toList());
                List<Double> objective2 = population.stream().map(s -> s.objectives()[1]).collect(Collectors.toList());
                String graphJson = String.format(template, getFormattedList(objective1), getFormattedList(objective2), evaluations);
                JMetalLogger.logger.info("Evolver dashboard front plot: " + graphJson);
            }
        } else {
            JMetalLogger.logger.warning(getClass().getName() +
                    " : insufficient for generating real time information." +
                    " Either EVALUATIONS or POPULATION keys have not been registered yet by the algorithm");
        }
    }

    private static String getFormattedList(List<Double> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));

            if (i != list.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append("]");
        return sb.toString();
    }
}