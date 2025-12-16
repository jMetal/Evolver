package org.uma.evolver.analysis.ablation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.uma.jmetal.qualityindicator.QualityIndicator;

/**
 * Stores the results of an ablation analysis, including parameter contributions
 * and the ablation path.
 *
 * @author Antonio J. Nebro
 */
public class AblationResult {
    private final List<QualityIndicator> indicators;
    private double[] defaultPerformance;
    private double[] optimizedPerformance;
    private List<AblationStep> ablationPath = new ArrayList<>();
    private List<ParameterContribution> contributions = new ArrayList<>();

    public AblationResult(List<QualityIndicator> indicators) {
        this.indicators = new ArrayList<>(indicators);
    }

    public void addParameterContribution(ParameterContribution contribution) {
        contributions.add(contribution);
    }

    public List<ParameterContribution> getRankedContributions() {
        List<ParameterContribution> ranked = new ArrayList<>(contributions);
        ranked.sort((a, b) -> Double.compare(
                Arrays.stream(b.contribution()).map(Math::abs).sum(),
                Arrays.stream(a.contribution()).map(Math::abs).sum()));
        return ranked;
    }

    public double[] getTotalImprovement() {
        if (defaultPerformance == null || optimizedPerformance == null) {
            return new double[0];
        }
        double[] improvement = new double[defaultPerformance.length];
        for (int i = 0; i < improvement.length; i++) {
            improvement[i] = defaultPerformance[i] - optimizedPerformance[i];
        }
        return improvement;
    }

    public Map<String, double[]> getPercentageContributions() {
        double[] total = getTotalImprovement();
        Map<String, double[]> percentages = new LinkedHashMap<>();

        for (ParameterContribution pc : getRankedContributions()) {
            double[] pct = new double[total.length];
            for (int i = 0; i < total.length; i++) {
                if (Math.abs(total[i]) > 1e-10) {
                    pct[i] = (pc.contribution()[i] / total[i]) * 100.0;
                }
            }
            percentages.put(pc.paramName(), pct);
        }
        return percentages;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            ╔══════════════════════════════════════════════════════════════╗
            ║              ABLATION ANALYSIS RESULTS                       ║
            ╠══════════════════════════════════════════════════════════════╣
            """);

        sb.append("║ Indicators: ");
        for (int i = 0; i < indicators.size(); i++) {
            sb.append(indicators.get(i).name());
            if (i < indicators.size() - 1)
                sb.append(", ");
        }
        sb.append("\n");

        sb.append("╠══════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Default Performance:   %s%n", formatArray(defaultPerformance)));
        sb.append(String.format("║ Optimized Performance: %s%n", formatArray(optimizedPerformance)));
        sb.append(String.format("║ Total Improvement:     %s%n", formatArray(getTotalImprovement())));

        sb.append("""
            ╠══════════════════════════════════════════════════════════════╣
            ║ PARAMETER CONTRIBUTIONS (ranked by importance)              ║
            ╠══════════════════════════════════════════════════════════════╣
            """);

        int rank = 1;
        for (ParameterContribution pc : getRankedContributions()) {
            sb.append(String.format("║ %2d. %-25s%n", rank++, pc.paramName()));
            sb.append(String.format("║     Value: %s → %s%n", pc.defaultValue(), pc.optimizedValue()));
            sb.append(String.format("║     Contribution: %s%n", formatArray(pc.contribution())));
            sb.append("║\n");
        }

        if (!ablationPath.isEmpty()) {
            sb.append("""
                ╠══════════════════════════════════════════════════════════════╣
                ║ ABLATION PATH (forward selection order)                     ║
                ╠══════════════════════════════════════════════════════════════╣
                """);

            int step = 1;
            for (AblationStep as : ablationPath) {
                sb.append(String.format("║ Step %d: %s%n", step++, as.parameterChanged()));
                sb.append(String.format("║   %s → %s%n", as.fromValue(), as.toValue()));
                sb.append(String.format("║   Improvement: %s%n", formatArray(as.improvement())));
            }
        }

        sb.append("╚══════════════════════════════════════════════════════════════╝\n");
        return sb.toString();
    }

    public String toCSV() {
        StringBuilder sb = new StringBuilder();

        sb.append("Parameter,DefaultValue,OptimizedValue");
        for (QualityIndicator ind : indicators) {
            sb.append(",Contribution_").append(ind.name());
        }
        sb.append("\n");

        for (ParameterContribution pc : getRankedContributions()) {
            sb.append(pc.paramName()).append(",");
            sb.append(pc.defaultValue()).append(",");
            sb.append(pc.optimizedValue());
            for (double c : pc.contribution()) {
                sb.append(",").append(c);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    public String pathToCSV() {
        StringBuilder sb = new StringBuilder();

        sb.append("Step,Parameter,FromValue,ToValue");
        for (QualityIndicator ind : indicators) {
            sb.append(",Performance_").append(ind.name());
        }
        sb.append("\n");

        if (defaultPerformance != null) {
            sb.append("0,Observed_Baseline,-,-");
            for (double val : defaultPerformance) {
                sb.append(",").append(val);
            }
            sb.append("\n");
        }

        int stepNum = 1;
        for (AblationStep step : ablationPath) {
            sb.append(stepNum++).append(",");
            sb.append(step.parameterChanged()).append(",");
            sb.append(step.fromValue()).append(",");
            sb.append(step.toValue());
            for (double val : step.performanceAfter()) {
                sb.append(",").append(val);
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String formatArray(double[] arr) {
        if (arr == null)
            return "null";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append(String.format("%.6f", arr[i]));
            if (i < arr.length - 1)
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    public double[] getDefaultPerformance() { return defaultPerformance; }
    public void setDefaultPerformance(double[] p) { this.defaultPerformance = p; }
    public double[] getOptimizedPerformance() { return optimizedPerformance; }
    public void setOptimizedPerformance(double[] p) { this.optimizedPerformance = p; }
    public List<AblationStep> getAblationPath() { return ablationPath; }
    public void setAblationPath(List<AblationStep> path) { this.ablationPath = path; }
    public List<ParameterContribution> getContributions() { return contributions; }
}
