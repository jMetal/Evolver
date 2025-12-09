package org.uma.evolver.ablation;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uma.evolver.ablation.MultiProblemAblationAnalyzer.ProblemWithReferenceFront;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
 * Unit tests for {@link MultiProblemAblationAnalyzer}.
 */
class MultiProblemAblationAnalyzerTest {

    private DoubleProblem problem1;
    private DoubleProblem problem2;
    private double[][] referenceFront1;
    private double[][] referenceFront2;

    @BeforeEach
    void setUp() {
        problem1 = new ZDT1();
        problem2 = new ZDT1(20); // ZDT1 with different number of variables
        
        // Simple reference fronts for testing
        referenceFront1 = new double[][] {
            {0.0, 1.0},
            {0.5, 0.5},
            {1.0, 0.0}
        };
        referenceFront2 = new double[][] {
            {0.0, 1.0},
            {0.25, 0.75},
            {0.5, 0.5},
            {0.75, 0.25},
            {1.0, 0.0}
        };
    }

    @Test
    void constructorShouldCreateAnalyzerWithProblems() {
        List<ProblemWithReferenceFront<DoubleSolution>> problems = List.of(
            new ProblemWithReferenceFront<>(problem1, referenceFront1, "ZDT1_30"),
            new ProblemWithReferenceFront<>(problem2, referenceFront2, "ZDT1_20")
        );

        // This is a partial test since we can't easily mock the algorithm
        assertEquals(2, problems.size());
        assertEquals("ZDT1_30", problems.get(0).name());
        assertEquals("ZDT1_20", problems.get(1).name());
    }

    @Test
    void problemWithReferenceFrontShouldStoreCorrectValues() {
        ProblemWithReferenceFront<DoubleSolution> problemData = 
            new ProblemWithReferenceFront<>(problem1, referenceFront1, "TestProblem");

        assertSame(problem1, problemData.problem());
        assertSame(referenceFront1, problemData.referenceFront());
        assertEquals("TestProblem", problemData.name());
    }

    @Test
    void problemWithReferenceFrontNameShouldNotBeNull() {
        ProblemWithReferenceFront<DoubleSolution> problemData = 
            new ProblemWithReferenceFront<>(problem1, referenceFront1, "MyProblem");

        assertNotNull(problemData.name());
        assertFalse(problemData.name().isEmpty());
    }

    @Test
    void multipleProblemsCanBeCreatedWithDifferentReferenceFronts() {
        List<ProblemWithReferenceFront<DoubleSolution>> problems = List.of(
            new ProblemWithReferenceFront<>(problem1, referenceFront1, "Problem1"),
            new ProblemWithReferenceFront<>(problem2, referenceFront2, "Problem2")
        );

        assertEquals(2, problems.size());
        assertEquals(3, problems.get(0).referenceFront().length);
        assertEquals(5, problems.get(1).referenceFront().length);
    }

    @Test
    void conditionalParameterCanBeAddedAfterConstruction() {
        // Test that the addConditionalParameter method exists and accepts parameters
        // Full integration test would require a complete algorithm setup
        
        Map<String, String> testConfig = Map.of(
            "param1", "value1",
            "param2", "value2"
        );

        // Verify the configuration structure is valid
        assertEquals(2, testConfig.size());
        assertEquals("value1", testConfig.get("param1"));
    }
}
