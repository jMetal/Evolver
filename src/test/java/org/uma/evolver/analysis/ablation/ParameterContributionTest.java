package org.uma.evolver.analysis.ablation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ParameterContribution record.
 *
 * @author Antonio J. Nebro
 */
class ParameterContributionTest {

    @Test
    void testTotalAbsoluteContribution() {
        var contribution = new ParameterContribution(
                "crossover",
                "SBX",
                "BLX",
                new double[]{0.05, -0.03, 0.02}
        );

        double total = contribution.totalAbsoluteContribution();

        assertEquals(0.10, total, 0.001);
    }

    @Test
    void testTotalAbsoluteContributionSingleIndicator() {
        var contribution = new ParameterContribution(
                "mutation",
                "uniform",
                "polynomial",
                new double[]{0.15}
        );

        double total = contribution.totalAbsoluteContribution();

        assertEquals(0.15, total, 0.001);
    }

    @Test
    void testRecordAccessors() {
        var contribution = new ParameterContribution(
                "selection",
                "tournament",
                "random",
                new double[]{0.1, 0.2}
        );

        assertEquals("selection", contribution.paramName());
        assertEquals("tournament", contribution.optimizedValue());
        assertEquals("random", contribution.defaultValue());
        assertArrayEquals(new double[]{0.1, 0.2}, contribution.contribution());
    }

    @Test
    void testZeroContribution() {
        var contribution = new ParameterContribution(
                "param",
                "value1",
                "value2",
                new double[]{0.0, 0.0}
        );

        assertEquals(0.0, contribution.totalAbsoluteContribution(), 0.001);
    }
}
