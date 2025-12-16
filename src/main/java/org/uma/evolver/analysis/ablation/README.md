# Ablation Analysis Package

This package provides a comprehensive framework for conducting ablation studies on metaheuristic algorithms, with a focus on parameter contribution analysis.

## Overview

Ablation analysis helps researchers understand which algorithm parameters contribute most to performance improvements. This package implements both leave-one-out and forward path ablation methodologies with support for multi-problem analysis.

## Core Components

### Analysis Classes

- **`AblationAnalyzer`** - Main analyzer with thread-safe parallel execution
- **`AblationResult`** - Container for analysis results with CSV export
- **`AblationStep`** - Individual step in the ablation process
- **`ParameterContribution`** - Parameter contribution measurement

### Configuration System

- **`AblationConfiguration`** - Fluent API for configuring ablation analyses
- **`AblationRunner`** - Generic runner for executing configured analyses
- **`ProblemSuiteFactory`** - Factory for creating problem suites (ZDT, DTLZ, WFG)

### Progress Reporting

- **`ProgressReporter`** - Interface for progress reporting implementations
- **`ConsoleProgressReporter`** - Console-based progress reporting with ASCII bars

## Key Features

### 🚀 **Performance**
- **Parallel Execution**: Configurable thread-based parallelism
- **Optimized Memory Usage**: ThreadLocal indicators and cached reference fronts
- **Efficient I/O**: Batch CSV export with proper encoding

### 🔧 **Flexibility**
- **Multiple Problem Suites**: ZDT, DTLZ, and extensible for custom suites
- **Configurable Parameters**: All aspects configurable via fluent API
- **Command-Line Support**: Built-in argument parsing for all examples

### 📊 **Analysis Methods**
- **Leave-One-Out Analysis**: Measures parameter importance by removal
- **Forward Path Analysis**: Builds optimal configuration step by step
- **Multi-Problem Evaluation**: Analyzes across multiple benchmark problems

### 🛡️ **Robustness**
- **Input Validation**: Comprehensive configuration validation
- **Error Handling**: Graceful handling of I/O and computation errors
- **Thread Safety**: Safe for concurrent execution

## Usage Examples

### Simple Configuration-Based Approach

```java
// ZDT problems with optimized parameters
AblationConfiguration config = AblationConfiguration.forZDTProblems();
AblationRunner runner = new AblationRunner(config);
runner.run();
```

### Custom Configuration

```java
AblationConfiguration config = new AblationConfiguration()
    .analysisName("Custom Analysis")
    .problemSuite("DTLZ")
    .numberOfRuns(25)
    .maxEvaluations(50000)
    .numberOfThreads(8)
    .enableProgressReporting(true);

AblationRunner runner = new AblationRunner(config);
runner.run();
```

### Direct Analyzer Usage

```java
// Create analyzer
AblationAnalyzer<DoubleSolution> analyzer = new AblationAnalyzer<>(
    algorithm, problems, indicators, maxEvaluations, numberOfRuns, 
    parameterSpace, numberOfThreads);

// Set progress reporter
analyzer.setProgressReporter(new ConsoleProgressReporter());

// Run analyses
AblationResult looResult = analyzer.leaveOneOutAnalysis(defaultConfig, optimizedConfig);
AblationResult pathResult = analyzer.forwardPathAnalysis(defaultConfig, optimizedConfig);

// Export results
try (FileWriter writer = new FileWriter("results.csv")) {
    writer.write(looResult.toCSV());
}
```

## Example Classes

### Simple Examples (Recommended)
- **`SimpleZDTAblationExample`** - Minimal ZDT ablation (3 lines of code)
- **`SimpleDTLZAblationExample`** - Minimal DTLZ ablation (3 lines of code)

### Full Examples (Legacy, Refactored)
- **`ZDTAblationAnalysisExample`** - ZDT ablation using configuration system
- **`DTLZ3DAblationAnalysisExample`** - DTLZ ablation using configuration system

## Command Line Options

All examples support these options:

```bash
--threads <n>      # Number of threads (default: all available)
--sequential       # Force sequential execution
--parallel         # Use all available processors  
--no-progress      # Disable progress reporting
--runs <n>         # Runs per configuration per problem
--evaluations <n>  # Maximum evaluations per run
--help, -h         # Show help message
```

## Problem Suites

### ZDT Suite
- **Problems**: ZDT1, ZDT2, ZDT3, ZDT4, ZDT6
- **Objectives**: 2
- **Reference Fronts**: `resources/referenceFronts/ZDT*.csv`

### DTLZ Suite
- **Problems**: DTLZ1, DTLZ2, DTLZ3, DTLZ4, DTLZ7
- **Objectives**: 3 (default)
- **Reference Fronts**: `resources/referenceFronts/DTLZ*.3D.csv`

### WFG Suite (Planned)
- **Problems**: WFG1-WFG9
- **Status**: Factory placeholder implemented

## Architecture Improvements

### Before (Legacy Approach)
- ❌ 200+ lines of boilerplate per example
- ❌ Code duplication across examples
- ❌ Manual configuration parsing
- ❌ Inconsistent error handling
- ❌ Limited command-line support

### After (Configuration-Based Approach)
- ✅ 3 lines of code for simple cases
- ✅ Zero code duplication
- ✅ Fluent configuration API
- ✅ Comprehensive error handling
- ✅ Built-in command-line parsing
- ✅ Consistent interface across all analyses

## Performance Characteristics

### Thread Scaling
- **Sequential** (`numberOfThreads=1`): Predictable, lower memory usage
- **Parallel** (`numberOfThreads>1`): Scales well up to available cores
- **Memory Usage**: ~50MB per thread for typical problem suites

### Execution Time (Typical)
- **ZDT Suite** (10 runs, 20K evaluations): ~5-15 minutes (8 threads)
- **DTLZ Suite** (10 runs, 25K evaluations): ~10-25 minutes (8 threads)
- **Progress Reporting**: <1% overhead

## Extension Points

### Custom Problem Suites
```java
public static List<ProblemWithReferenceFront<DoubleSolution>> createCustomSuite() {
    // Implement custom problem suite
}
```

### Custom Progress Reporters
```java
public class CustomProgressReporter implements ProgressReporter {
    // Implement custom progress tracking
}
```

### Custom Quality Indicators
```java
AblationConfiguration config = new AblationConfiguration()
    .indicators(List.of(new MyCustomIndicator(), new Epsilon()));
```

## Best Practices

### Configuration
1. Use factory methods (`forZDTProblems()`, `forDTLZProblems()`) for standard analyses
2. Enable validation during development: `.validateConfigurations(true)`
3. Use appropriate thread counts: typically 1-2x CPU cores

### Performance
1. Start with fewer runs for testing: `.numberOfRuns(5)`
2. Use progress reporting for long analyses: `.enableProgressReporting(true)`
3. Monitor memory usage with high thread counts

### Results
1. Always export results: automatic CSV export included
2. Use descriptive output prefixes: `.outputPrefix("experiment_name")`
3. Keep both leave-one-out and forward path results

## Migration Guide

### From Legacy Examples
Replace 200+ lines of boilerplate:

```java
// OLD: Manual setup, configuration parsing, execution, export
List<ProblemWithReferenceFront<DoubleSolution>> problems = createProblemSet();
// ... 200+ lines of setup code ...

// NEW: Configuration-based approach  
AblationConfiguration config = AblationConfiguration.forZDTProblems();
AblationRunner runner = new AblationRunner(config);
runner.run();
```

### Benefits of Migration
- **95% code reduction** for simple cases
- **Consistent interface** across all analyses
- **Built-in validation** and error handling
- **Command-line support** out of the box
- **Better maintainability** and extensibility

## Dependencies

- **JMetal Framework**: Core optimization algorithms and problems
- **Java 17+**: Modern language features (switch expressions, text blocks)
- **Maven**: Build and dependency management

## Related Documentation

- [Ablation Configuration Guide](../../../../../../../docs/ablation_configuration_guide.md) - Comprehensive usage guide
- [JMetal Documentation](http://jmetal.github.io/jMetal/) - Core framework documentation
- [Evolver Project Structure](../../../../../../../docs/project_structure.rst) - Overall project organization