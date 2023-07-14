package org.uma.evolver;

import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.observable.Observable;

import java.util.List;
import java.util.Map;

public abstract class MetaOptimizer {
    public abstract void run();
    public abstract Observable<Map<String, Object>> observable();
    public abstract long totalComputingTime();
    public abstract List<DoubleSolution> result();
}
