# Ablation Analysis Configuration Guide

This guide explains how to use the new configuration-based approach for ablation analysis in Evolver, which eliminates code duplication and provides a flexible, reusable system.

## Overview

The configuration system consists of three main components:

1. **AblationConfiguration** - Fluent API for configuring all aspects of an ablation analysis
2. **ProblemSuiteFactory** - Factory for creating different problem suites (ZDT, DTLZ, WFG)
3. **AblationRunner** - Generic runner that executes ablation analyses based on configuration

## Quick Start

### Simple Usage

For the simplest approach, use the pre-configured factory methods:

```java
// ZDT problems with optimized parameters
AblationConfiguration config = AblationConfiguration.forZDTProblems();
AblationRunner runner = new AblationRunner(config);
runner.run();

// DTLZ problems with optimized parameters  
AblationConfiguration config = AblationConfiguration.forDTLZProblems();
AblationRunner runner = new AblationRunner(config);
runner.run();
```

### Custom Configuration

For more control, use the fluent API:

```java
AblationConfiguration config = new AblationConfiguration()
    .analysisName("Custom Analysis")
    .problemSuite("ZDT")
    .numberOfRuns(25)
    .maxEvaluations(30000)
    .numberOfThreads(8)
    .enableProgressReporting(true)
    .outputPrefix("my_results");

AblationRunner runner = new AblationRunner(config);
runner.run();
```

## Configuration Options

### Analysis Parameters

- `analysisName(String)` - Display name for the analysis
- `problemSuite(String)` - Problem suite to use ("ZDT", "DTLZ", "WFG")
- `numberOfRuns(int)` - Independent runs per configuration per problem (default: 10)
- `maxEvaluations(int)` - Maximum evaluations per run (default: varies by suite)
- `numberOfThreads(int)` - Thread count for parallel execution (default: all available)

### Algorithm Configuration

- `yamlParameterSpaceFile(String)` - YAML parameter space file (default: "NSGAIIDouble.yaml")
- `defaultConfiguration(String)` - Default algorithm configuration string
- `optimizedConfiguration(String)` - Optimized algorithm configuration string
- `populationSize(int)` - Population size for the algorithm (default: 100)

### Quality Indicators

- `indicators(List<QualityIndicator>)` - Quality indicators to use (default: Epsilon, NormalizedHypervolume)

### Progress and Output

- `enableProgressReporting(boolean)` - Enable real-time progress reporting (default: true)
- `showProgressBars(boolean)` - Show ASCII progress bars (default: true)
- `showTimestamps(boolean)` - Show timestamps in output (default: true)
- `outputPrefix(String)` - Prefix for CSV output files (default: "ablation_results")

### Validation

- `validateConfigurations(boolean)` - Enable configuration validation (default: true)

## Problem Suites

### ZDT Suite
- **Problems**: ZDT1, ZDT2, ZDT3, ZDT4, ZDT6
- **Objectives**: 2
- **Default evaluations**: 20,000
- **Optimized config**: From meta-optimization on ZDT training set

### DTLZ Suite  
- **Problems**: DTLZ1, DTLZ2, DTLZ3, DTLZ4, DTLZ7
- **Objectives**: 3
- **Default evaluations**: 25,000
- **Optimized config**: From AsyncNSGAIIOptimizingNSGAIIForBenchmarkDTLZ (best IGD+ after 2000 evaluations)

### WFG Suite
- **Status**: Placeholder (not yet implemented)
- **Problems**: WFG1-WFG9 (planned)

## Example Classes

### Simple Examples
- `SimpleZDTAblationExample` - Minimal ZDT ablation with command-line options
- `SimpleDTLZAblationExample` - Minimal DTLZ ablation with command-line options

### Full Examples (Refactored)
- `ZDTAblationAnalysisExample` - ZDT ablation using configuration system
- `DTLZ3DAblationAnalysisExample` - DTLZ ablation using configuration system

## Command Line Options

All example classes support these command-line options:

- `--threads <n>` - Number of threads to use
- `--sequential` - Force sequential execution (equivalent to `--threads 1`)
- `--parallel` - Use all available processors
- `--no-progress` - Disable real-time progress reporting
- `--runs <n>` - Number of runs per configuration per problem
- `--evaluations <n>` - Maximum evaluations per run
- `--help`, `-h` - Show help message

### Usage Examples

```bash
# Run with default settings
java SimpleZDTAblationExample

# Use 4 threads and 25 runs
java SimpleZDTAblationExample --threads 4 --runs 25

# Sequential execution without progress reporting
java SimpleZDTAblationExample --sequential --no-progress

# Custom evaluation count
java SimpleDTLZAblationExample --evaluations 50000
```

## Advanced Usage

### Custom Problem Suite

To add a new problem suite, extend `ProblemSuiteFactory`:

```java
public static List<ProblemWithReferenceFront<DoubleSolution>> createCustomSuite() 
        throws IOException {
    List<ProblemWithReferenceFront<DoubleSolution>> problems = new ArrayList<>();
    
    problems.add(new ProblemWithReferenceFront<>(
        new MyProblem1(), 
        VectorUtils.readVectors("resources/referenceFronts/MyProblem1.csv", ","), 
        "MyProblem1"));
    
    return problems;
}
```

### Custom Configuration Factory

Create factory methods for specific research scenarios:

```java
public static AblationConfiguration forMyResearch() {
    return new AblationConfiguration()
        .analysisName("My Research Analysis")
        .problemSuite("CUSTOM")
        .numberOfRuns(50)
        .maxEvaluations(100000)
        .numberOfThreads(16)
        .outputPrefix("research_results")
        .optimizedConfiguration("--my --custom --parameters");
}
```

### Custom Progress Reporter

Implement `ProgressReporter` for custom progress tracking:

```java
public class FileProgressReporter implements ProgressReporter {
    private final PrintWriter writer;
    
    public FileProgressReporter(String filename) throws IOException {
        this.writer = new PrintWriter(new FileWriter(filename));
    }
    
    @Override
    public void reportProgress(String phase, int current, int total, String details) {
        writer.printf("[%s] %d/%d - %s%n", phase, current, total, details);
        writer.flush();
    }
    
    // ... implement other methods
}
```

## Migration Guide

### From Old Examples to New System

**Before (old approach):**
```java
// 200+ lines of boilerplate code
List<ProblemWithReferenceFront<DoubleSolution>> problems = createProblemSet();
AblationAnalyzer<DoubleSolution> analyzer = new AblationAnalyzer<>(...);
// Manual configuration parsing, validation, execution, export...
```

**After (new approach):**
```java
// 3 lines of code
AblationConfiguration config = AblationConfiguration.forZDTProblems();
AblationRunner runner = new AblationRunner(config);
runner.run();
```

### Benefits

1. **Reduced Code Duplication**: ~95% reduction in boilerplate code
2. **Consistent Interface**: All ablation analyses use the same configuration API
3. **Flexible Configuration**: Easy to customize any aspect without code changes
4. **Command-Line Support**: Built-in argument parsing for all examples
5. **Better Maintainability**: Changes to ablation logic only need to be made in one place
6. **Type Safety**: Fluent API prevents configuration errors
7. **Validation**: Built-in configuration validation with helpful error messages

## Performance Considerations

- **Thread Count**: Use `numberOfThreads(1)` for sequential execution, or specify exact count
- **Memory Usage**: Higher thread counts increase memory usage due to parallel problem instances
- **I/O Performance**: Progress reporting adds minimal overhead but can be disabled
- **Validation**: Configuration validation adds startup time but catches errors early

## Troubleshooting

### Common Issues

1. **Reference Front Not Found**: Ensure `resources/referenceFronts/` directory exists with CSV files
2. **Invalid Configuration**: Enable validation to catch parameter errors early
3. **Memory Issues**: Reduce thread count or number of runs for large problem suites
4. **File Export Errors**: Check write permissions for output directory

### Debug Mode

Enable detailed logging by setting validation and progress reporting:

```java
AblationConfiguration config = AblationConfiguration.forZDTProblems()
    .validateConfigurations(true)
    .enableProgressReporting(true)
    .showTimestamps(true);
```

## Future Extensions

The configuration system is designed for extensibility:

- **New Problem Suites**: Add WFG, CEC, or custom problem sets
- **New Algorithms**: Support for other metaheuristics beyond NSGA-II
- **New Indicators**: Easy integration of additional quality indicators
- **Export Formats**: Support for JSON, XML, or database export
- **Distributed Execution**: Potential for cluster-based parallel execution