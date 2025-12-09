package org.uma.evolver.ablation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the AblationStep record.
 *
 * @author Antonio J. Nebro
 */
class AblationStepTest {

    @Test
    void testImprovement() {
        var step = new AblationStep(
                "crossoverProbability",
                "0.9",
                "0.97",
                new double[]{0.5, 0.7},
                new double[]{0.3, 0.6}
        );

        double[] improvement = step.improvement();

        assertEquals(2, improvement.length);
        assertEquals(0.2, improvement[0], 0.001);
        assertEquals(0.1, improvement[1], 0.001);
    }

    @Test
    void testImprovementNegative() {
        var step = new AblationStep(
                "mutation",
                "polynomial",
                "uniform",
                new double[]{0.3},
                new double[]{0.5}
        );

        double[] improvement = step.improvement();

        assertEquals(-0.2, improvement[0], 0.001);
    }

    @Test
    void testRecordAccessors() {
        var step = new AblationStep(
                "selection",
                "random",
                "tournament",
                new double[]{0.5},
                new double[]{0.4}
        );

        assertEquals("selection", step.parameterChanged());
        assertEquals("random", step.fromValue());
        assertEquals("tournament", step.toValue());
        assertArrayEquals(new double[]{0.5}, step.performanceBefore());
        assertArrayEquals(new double[]{0.4}, step.performanceAfter());
    }
}
