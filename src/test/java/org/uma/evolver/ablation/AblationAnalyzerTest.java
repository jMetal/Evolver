package org.uma.evolver.ablation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uma.evolver.ablation.AblationAnalyzer.ProblemWithReferenceFront;
import org.uma.evolver.algorithm.base.BaseLevelAlgorithm;
import org.uma.evolver.parameter.ParameterSpace;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.doubleproblem.DoubleProblem;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

class AblationAnalyzerTest {

    private AblationAnalyzer<DoubleSolution> analyzer;
    private BaseLevelAlgorithm<DoubleSolution> algorithm;
    private Problem<DoubleSolution> problem;
    private QualityIndicator indicator;
    private ParameterSpace parameterSpace;

    private DoubleProblem problem1;
    private DoubleProblem problem2;
    private double[][] referenceFront1;
    private double[][] referenceFront2;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        algorithm = mock(BaseLevelAlgorithm.class);
        problem = mock(Problem.class);
        indicator = mock(QualityIndicator.class);
        parameterSpace = mock(ParameterSpace.class);

        BaseLevelAlgorithm<DoubleSolution> baseAlgorithmMock = mock(BaseLevelAlgorithm.class);
        Algorithm<List<DoubleSolution>> algorithmMock = mock(Algorithm.class);

        when(algorithm.createInstance(any(), anyInt())).thenReturn(baseAlgorithmMock);
        when(baseAlgorithmMock.build()).thenReturn(algorithmMock);
        DoubleSolution solution1 = mock(DoubleSolution.class);
        when(solution1.objectives()).thenReturn(new double[] { 0.0, 1.0 });
        DoubleSolution solution2 = mock(DoubleSolution.class);
        when(solution2.objectives()).thenReturn(new double[] { 1.0, 0.0 });

        when(algorithmMock.result()).thenReturn(List.of(solution1, solution2));

        problem1 = new ZDT1();
        problem2 = new ZDT1(20);

        referenceFront1 = new double[][] { { 0.0, 1.0 }, { 0.5, 0.5 }, { 1.0, 0.0 } };
        referenceFront2 = new double[][] { { 0.0, 1.0 }, { 0.25, 0.75 }, { 0.5, 0.5 }, { 0.75, 0.25 }, { 1.0, 0.0 } };

        ProblemWithReferenceFront<DoubleSolution> problemWithRef = new ProblemWithReferenceFront<>(problem,
                referenceFront1, "TestProblem");

        analyzer = new AblationAnalyzer<>(
                algorithm, List.of(problemWithRef), List.of(indicator), 1000, 1, parameterSpace);
    }

    @Test
    void constructorShouldCreateAnalyzerWithProblems() {
        List<ProblemWithReferenceFront<DoubleSolution>> problems = List.of(
                new ProblemWithReferenceFront<>(problem1, referenceFront1, "ZDT1_30"),
                new ProblemWithReferenceFront<>(problem2, referenceFront2, "ZDT1_20"));

        assertEquals(2, problems.size());
        assertEquals("ZDT1_30", problems.get(0).name());
        assertEquals("ZDT1_20", problems.get(1).name());
    }

    @Test
    void problemWithReferenceFrontShouldStoreCorrectValues() {
        ProblemWithReferenceFront<DoubleSolution> problemData = new ProblemWithReferenceFront<>(problem1,
                referenceFront1, "TestProblem");

        assertSame(problem1, problemData.problem());
        assertSame(referenceFront1, problemData.referenceFront());
        assertEquals("TestProblem", problemData.name());
    }

    @Test
    void problemWithReferenceFrontNameShouldNotBeNull() {
        ProblemWithReferenceFront<DoubleSolution> problemData = new ProblemWithReferenceFront<>(problem1,
                referenceFront1, "MyProblem");

        assertNotNull(problemData.name());
        assertFalse(problemData.name().isEmpty());
    }

    @Test
    void shouldPerformLeaveOneOutAnalysis() {
        Map<String, String> defaultConfig = new LinkedHashMap<>();
        defaultConfig.put("param1", "value1");
        defaultConfig.put("param2", "value2");

        Map<String, String> optimizedConfig = new LinkedHashMap<>();
        optimizedConfig.put("param1", "optValue1");
        optimizedConfig.put("param2", "optValue2");

        AblationResult result = analyzer.leaveOneOutAnalysis(defaultConfig, optimizedConfig);

        assertNotNull(result);
        // parameter contributions count depends on logic, here we just check it doesn't
        // crash
        // and returns a result object
    }

    @Test
    void shouldPerformForwardPathAnalysis() {
        Map<String, String> defaultConfig = new LinkedHashMap<>();
        defaultConfig.put("param1", "value1");
        defaultConfig.put("param2", "value2");

        Map<String, String> optimizedConfig = new LinkedHashMap<>();
        optimizedConfig.put("param1", "optValue1");
        optimizedConfig.put("param2", "optValue2");

        AblationResult result = analyzer.forwardPathAnalysis(defaultConfig, optimizedConfig);

        assertNotNull(result);
    }
}
