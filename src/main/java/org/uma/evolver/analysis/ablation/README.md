# Ablation Analysis Package

This package implements configuration-driven ablation analysis to quantify how individual
parameter changes impact algorithm performance. It supports both greedy path ablation and
leave-one-out (LOO) analysis across problem suites such as ZDT and DTLZ.

## Implemented Methodologies

### 1. Greedy Path Ablation
Builds an ablation path from a baseline configuration to an optimized configuration by greedily
selecting the single parameter change that yields the best metric value at each step.

### 2. Leave-One-Out (LOO) Ablation
Evaluates the contribution of each optimized parameter by reverting it to the baseline value
while keeping all other parameters fixed.

## Quick Start

```java
// Minimal configuration-based approach
AblationConfiguration config = AblationConfiguration.forZDTProblems()
    .numberOfRuns(25)
    .numberOfThreads(8)
    .enableProgressReporting(true);

AblationRunner runner = new AblationRunner(config);
runner.run();
```

## Example Entry Points

- `SimpleZDTAblationExample` - Minimal ZDT ablation with CLI options
- `SimpleDTLZAblationExample` - Minimal DTLZ ablation with CLI options
- `ZDTAblationAnalysisExample` - Full configuration example for ZDT
- `DTLZ3DAblationAnalysisExample` - Full configuration example for DTLZ 3D

### Command-Line Options (Simple Examples)

```
--threads <n>       Number of threads to use
--runs <n>          Number of runs per configuration
--evaluations <n>   Max evaluations per run
--sequential        Force sequential execution
--parallel          Use all available processors
--no-progress       Disable progress reporting
--help, -h          Show help
```

## Outputs

Results are written to CSV files under the configured output directory (default:
`results/ablation/<suite>`). The filenames are:

- `<outputPrefix>_path.csv`
- `<outputPrefix>_loo.csv`

## Reference

The greedy ablation strategy is based on:

```bibtex
@inproceedings{FawcettH16,
  author    = {Chris Fawcett and Holger H. Hoos},
  title     = {Analysing differences between algorithm configurations through ablation},
  booktitle = {Proceedings of the 10th Learning and Intelligent Optimization Conference (LION 10)},
  year      = {2016},
  pages     = {123--137},
  publisher = {Springer},
  doi       = {10.1007/978-3-319-44953-4_11}
}
```
