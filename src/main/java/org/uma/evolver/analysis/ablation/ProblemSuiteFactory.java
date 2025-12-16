package org.uma.evolver.analysis.ablation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.uma.evolver.analysis.ablation.AblationAnalyzer.ProblemWithReferenceFront;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ1;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ2;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ3;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ4;
import org.uma.jmetal.problem.multiobjective.dtlz.DTLZ7;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT1;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT2;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT3;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT4;
import org.uma.jmetal.problem.multiobjective.zdt.ZDT6;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.VectorUtils;

/**
 * Factory for creating different problem suites for ablation analysis.
 * Supports common benchmark problem suites with their reference fronts.
 * 
 * @author Antonio J. Nebro
 */
public class ProblemSuiteFactory {
    
    private static final String REFERENCE_FRONTS_PATH = "resources/referenceFronts/";
    
    /**
     * Creates a problem suite based on the suite name.
     * 
     * @param suiteName the name of the problem suite (e.g., "ZDT", "DTLZ", "WFG")
     * @return list of problems with their reference fronts
     * @throws IOException if reference front files cannot be read
     * @throws IllegalArgumentException if the suite name is not supported
     */
    public static List<ProblemWithReferenceFront<DoubleSolution>> createProblemSuite(String suiteName) 
            throws IOException {
        
        // Validate that reference front directory exists
        java.nio.file.Path referenceFrontPath = java.nio.file.Paths.get(REFERENCE_FRONTS_PATH);
        if (!java.nio.file.Files.exists(referenceFrontPath)) {
            throw new IOException("Reference front directory not found: " + REFERENCE_FRONTS_PATH);
        }
        
        return switch (suiteName.toUpperCase()) {
            case "ZDT" -> createZDTProblemSuite();
            case "DTLZ" -> createDTLZProblemSuite();
            case "WFG" -> createWFGProblemSuite();
            default -> throw new IllegalArgumentException("Unsupported problem suite: " + suiteName + 
                                                        ". Supported suites: ZDT, DTLZ, WFG");
        };
    }
    
    /**
     * Creates the ZDT problem suite (ZDT1, ZDT2, ZDT3, ZDT4, ZDT6).
     * Note: ZDT5 is excluded as it's a binary problem.
     */
    private static List<ProblemWithReferenceFront<DoubleSolution>> createZDTProblemSuite() 
            throws IOException {
        
        List<ProblemWithReferenceFront<DoubleSolution>> problems = new ArrayList<>();
        
        problems.add(new ProblemWithReferenceFront<>(
            new ZDT1(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "ZDT1.csv", ","), "ZDT1"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new ZDT2(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "ZDT2.csv", ","), "ZDT2"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new ZDT3(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "ZDT3.csv", ","), "ZDT3"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new ZDT4(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "ZDT4.csv", ","), "ZDT4"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new ZDT6(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "ZDT6.csv", ","), "ZDT6"));
        
        return problems;
    }
    
    /**
     * Creates the DTLZ problem suite (DTLZ1, DTLZ2, DTLZ3, DTLZ4, DTLZ7).
     * Note: DTLZ5 and DTLZ6 are excluded as they have degenerate Pareto fronts.
     */
    private static List<ProblemWithReferenceFront<DoubleSolution>> createDTLZProblemSuite() 
            throws IOException {
        
        List<ProblemWithReferenceFront<DoubleSolution>> problems = new ArrayList<>();
        
        problems.add(new ProblemWithReferenceFront<>(
            new DTLZ1(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "DTLZ1.3D.csv", ","), "DTLZ1"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new DTLZ2(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "DTLZ2.3D.csv", ","), "DTLZ2"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new DTLZ3(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "DTLZ3.3D.csv", ","), "DTLZ3"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new DTLZ4(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "DTLZ4.3D.csv", ","), "DTLZ4"));
        
        problems.add(new ProblemWithReferenceFront<>(
            new DTLZ7(), VectorUtils.readVectors(REFERENCE_FRONTS_PATH + "DTLZ7.3D.csv", ","), "DTLZ7"));
        
        return problems;
    }
    
    /**
     * Creates the WFG problem suite (WFG1-WFG9 with 3 objectives).
     * Note: This is a placeholder - implement if WFG problems are needed.
     */
    private static List<ProblemWithReferenceFront<DoubleSolution>> createWFGProblemSuite() 
            throws IOException {
        
        // TODO: Implement WFG problem suite if needed
        throw new UnsupportedOperationException("WFG problem suite not yet implemented");
    }
    
    /**
     * Gets the problem names for a given suite.
     * 
     * @param suiteName the name of the problem suite
     * @return list of problem names in the suite
     */
    public static List<String> getProblemNames(String suiteName) {
        return switch (suiteName.toUpperCase()) {
            case "ZDT" -> List.of("ZDT1", "ZDT2", "ZDT3", "ZDT4", "ZDT6");
            case "DTLZ" -> List.of("DTLZ1", "DTLZ2", "DTLZ3", "DTLZ4", "DTLZ7");
            case "WFG" -> List.of("WFG1", "WFG2", "WFG3", "WFG4", "WFG5", "WFG6", "WFG7", "WFG8", "WFG9");
            default -> throw new IllegalArgumentException("Unsupported problem suite: " + suiteName);
        };
    }
    
    /**
     * Gets the supported problem suite names.
     * 
     * @return list of supported suite names
     */
    public static List<String> getSupportedSuites() {
        return List.of("ZDT", "DTLZ", "WFG");
    }
}