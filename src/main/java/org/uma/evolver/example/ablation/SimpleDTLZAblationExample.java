package org.uma.evolver.example.ablation;

import java.io.IOException;
import org.uma.evolver.analysis.ablation.AblationConfiguration;
import org.uma.evolver.analysis.ablation.AblationRunner;

/**
 * Simplified example demonstrating DTLZ ablation analysis using the configuration-based approach.
 * 
 * <p>This example shows how to run ablation analysis with minimal code using the 
 * {@link AblationConfiguration} and {@link AblationRunner} classes.
 * 
 * <p>The DTLZ configuration uses optimized parameters obtained from AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ
 * with the lowest IGD+ value after 2000 evaluations.
 * 
 * <p>Usage examples:
 * <ul>
 *   <li>{@code java SimpleDTLZAblationExample} - Run with default settings</li>
 *   <li>{@code java SimpleDTLZAblationExample --threads 8} - Use 8 threads</li>
 *   <li>{@code java SimpleDTLZAblationExample --sequential} - Force sequential execution</li>
 *   <li>{@code java SimpleDTLZAblationExample --no-progress} - Disable progress reporting</li>
 *   <li>{@code java SimpleDTLZAblationExample --runs 15} - Use 15 runs per configuration</li>
 * </ul>
 *
 * @author Antonio J. Nebro
 */
public class SimpleDTLZAblationExample {

    public static void main(String[] args) throws IOException {
        // Create DTLZ configuration with optimized parameters
        AblationConfiguration config = AblationConfiguration.forDTLZProblems();
        
        // Override configuration based on command line arguments
        config = parseCommandLineArguments(config, args);
        
        // Create and run the ablation analysis
        AblationRunner runner = new AblationRunner(config);
        runner.run();
    }
    
    /**
     * Parses command line arguments and updates the configuration accordingly.
     * 
     * @param config the base configuration to modify
     * @param args command line arguments
     * @return updated configuration
     */
    private static AblationConfiguration parseCommandLineArguments(AblationConfiguration config, String[] args) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--threads":
                    if (i + 1 < args.length) {
                        try {
                            int threads = Integer.parseInt(args[i + 1]);
                            config = config.numberOfThreads(Math.max(1, threads));
                            i++; // Skip next argument
                        } catch (NumberFormatException e) {
                            System.err.println("Warning: Invalid thread count, using default");
                        }
                    }
                    break;
                    
                case "--sequential":
                    config = config.numberOfThreads(1);
                    break;
                    
                case "--parallel":
                    config = config.numberOfThreads(Runtime.getRuntime().availableProcessors());
                    break;
                    
                case "--no-progress":
                    config = config.enableProgressReporting(false);
                    break;
                    
                case "--runs":
                    if (i + 1 < args.length) {
                        try {
                            int runs = Integer.parseInt(args[i + 1]);
                            config = config.numberOfRuns(Math.max(1, runs));
                            i++; // Skip next argument
                        } catch (NumberFormatException e) {
                            System.err.println("Warning: Invalid run count, using default");
                        }
                    }
                    break;
                    
                case "--evaluations":
                    if (i + 1 < args.length) {
                        try {
                            int evaluations = Integer.parseInt(args[i + 1]);
                            config = config.maxEvaluations(Math.max(1000, evaluations));
                            i++; // Skip next argument
                        } catch (NumberFormatException e) {
                            System.err.println("Warning: Invalid evaluation count, using default");
                        }
                    }
                    break;
                    
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                    
                default:
                    if (args[i].startsWith("--")) {
                        System.err.println("Warning: Unknown argument " + args[i]);
                    }
                    break;
            }
        }
        return config;
    }
    
    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: java SimpleDTLZAblationExample [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --threads <n>      Number of threads to use (default: all available)");
        System.out.println("  --sequential       Force sequential execution (equivalent to --threads 1)");
        System.out.println("  --parallel         Use all available processors");
        System.out.println("  --no-progress      Disable real-time progress reporting");
        System.out.println("  --runs <n>         Number of runs per configuration per problem (default: 10)");
        System.out.println("  --evaluations <n>  Maximum evaluations per run (default: 25000)");
        System.out.println("  --help, -h         Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java SimpleDTLZAblationExample");
        System.out.println("  java SimpleDTLZAblationExample --threads 8 --runs 15");
        System.out.println("  java SimpleDTLZAblationExample --sequential --no-progress");
        System.out.println();
        System.out.println("Note: This example uses optimized DTLZ parameters from AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ");
    }
}