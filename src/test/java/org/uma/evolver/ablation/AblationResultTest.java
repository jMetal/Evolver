package org.uma.evolver.ablation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;

/**
 * Unit tests for the AblationResult class.
 *
 * @author Antonio J. Nebro
 */
class AblationResultTest {

    private AblationResult result;
    private List<QualityIndicator> indicators;

    @BeforeEach
    void setUp() {
        indicators = List.of(new Epsilon(), new NormalizedHypervolume());
        result = new AblationResult(indicators);
    }

    @Test
    void testRankedContributions() {
        result.addParameterContribution(
                new ParameterContribution("param1", "opt1", "def1", new double[]{0.01, 0.02}));
        result.addParameterContribution(
                new ParameterContribution("param2", "opt2", "def2", new double[]{0.05, 0.10}));
        result.addParameterContribution(
                new ParameterContribution("param3", "opt3", "def3", new double[]{0.02, 0.03}));

        var ranked = result.getRankedContributions();

        assertEquals(3, ranked.size());
        assertEquals("param2", ranked.get(0).paramName());
        assertEquals("param3", ranked.get(1).paramName());
        assertEquals("param1", ranked.get(2).paramName());
    }

    @Test
    void testTotalImprovement() {
        result.setDefaultPerformance(new double[]{0.5, 0.7});
        result.setOptimizedPerformance(new double[]{0.3, 0.5});

        double[] improvement = result.getTotalImprovement();

        assertEquals(2, improvement.length);
        assertEquals(0.2, improvement[0], 0.001);
        assertEquals(0.2, improvement[1], 0.001);
    }

    @Test
    void testTotalImprovementWithNullPerformance() {
        double[] improvement = result.getTotalImprovement();

        assertEquals(0, improvement.length);
    }

    @Test
    void testToCSV() {
        result.addParameterContribution(
                new ParameterContribution("crossover", "BLX", "SBX", new double[]{0.05, 0.03}));

        String csv = result.toCSV();

        assertTrue(csv.contains("crossover"));
        assertTrue(csv.contains("BLX"));
        assertTrue(csv.contains("SBX"));
        assertTrue(csv.contains("0.05"));
    }

    @Test
    void testGetPercentageContributions() {
        result.setDefaultPerformance(new double[]{1.0});
        result.setOptimizedPerformance(new double[]{0.5});

        result.addParameterContribution(
                new ParameterContribution("param1", "opt1", "def1", new double[]{0.25}));
        result.addParameterContribution(
                new ParameterContribution("param2", "opt2", "def2", new double[]{0.25}));

        Map<String, double[]> percentages = result.getPercentageContributions();

        assertEquals(2, percentages.size());
        assertEquals(50.0, percentages.get("param1")[0], 0.001);
        assertEquals(50.0, percentages.get("param2")[0], 0.001);
    }

    @Test
    void testAblationPath() {
        var step1 = new AblationStep("param1", "def1", "opt1",
                new double[]{0.5}, new double[]{0.4});
        var step2 = new AblationStep("param2", "def2", "opt2",
                new double[]{0.4}, new double[]{0.3});

        result.setAblationPath(List.of(step1, step2));

        assertEquals(2, result.getAblationPath().size());
        assertEquals("param1", result.getAblationPath().get(0).parameterChanged());
        assertEquals("param2", result.getAblationPath().get(1).parameterChanged());
    }

    @Test
    void testToString() {
        result.setDefaultPerformance(new double[]{0.5});
        result.setOptimizedPerformance(new double[]{0.3});
        result.addParameterContribution(
                new ParameterContribution("crossover", "BLX", "SBX", new double[]{0.1}));

        String str = result.toString();

        assertTrue(str.contains("ABLATION ANALYSIS RESULTS"));
        assertTrue(str.contains("crossover"));
        assertTrue(str.contains("BLX"));
    }
}
