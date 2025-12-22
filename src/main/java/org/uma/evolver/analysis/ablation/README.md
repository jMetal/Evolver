# Ablation Analysis Package

This package implements automated ablation analysis techniques to understand the performance differences between two evolutionary algorithm configurations (typically a default vs. an optimized one).

## Implemented Methodologies

### 1. Greedy Path Ablation
Based on the work of Fawcett and Hoos [1], this method constructs an "ablation path" from a starting configuration (e.g., Default) to a target configuration (e.g., Optimized).

*   **Mechanism**: At each step, the algorithm iteratively evaluates all possible single-parameter modifications that move the current configuration closer to the target. It selects the modification that yields the **best metric value** (Greedy selection).
*   **Robustness**: It automatically detects and handles parameter dependencies. If a parameter modification results in an invalid configuration (e.g., activating a child parameter without its parent), that modification is postponed.
*   **Result**: A sequence of parameter changes ranked by their contribution to performance improvement.

### 2. Leave-One-Out (LOO) Ablation
This simple heuristic evaluates the importance of individual parameter settings by treating the Optimized configuration as the baseline.

*   **Mechanism**: For each parameter in the Optimized configuration, it acts as if we "revert" it to its Default value (or remove it), while keeping all other optimized parameters fixed.
*   **Result**: A list showing the performance drop (or gain) associated with changing just that one parameter.

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

## Usage Examples

This package supports ablation on both a single problem and a set of problems (Training Set).

### Scenario 1: Single Problem Ablation
In this scenario, you evaluate the performance on a single problem instance (e.g., DTLZ3).

```java
public class SingleProblemAblationExample {
  public static void main(String[] args) throws IOException {
    // 1. Setup
    var problem = new DTLZ3();
    var parameterSpace = new YAMLParameterSpace("NSGAIIDoubleFull.yaml", new DoubleParameterFactory());
    var indicator = new NormalizedHypervolume(referenceFront);

    // 2. Define Runner
    Function<ParameterSpace, List<DoubleSolution>> algorithmRunner = space -> {
        var alg = new DoubleNSGAII(problem, 100, 25000, space).build();
        alg.run();
        return alg.result();
    };

    // 3. Create Analysis
    AblationAnalysis analysis = new AblationAnalysis(
        parameterSpace,
        algorithmRunner,
        indicator,
        5 // number of runs per configuration
    );

    // 4. Run Path Ablation
    AblationResult results = analysis.performPathAblation(defaultConfigStr, optimizedConfigStr);
    Files.writeString(Paths.get("path_ablation.csv"), results.toCSV());
  }
}
```

### Scenario 2: Training Set Ablation (Multi-Problem)
In this scenario, you evaluate the performance across a set of problems (e.g., DTLZ1-7) using a custom evaluator that computes an aggregated metric (e.g., Mean Hypervolume).

```java
public class TrainingSetAblationExample {
  public static void main(String[] args) throws Exception {
    
    // 1. Define Training Set and Parameter Space
    TrainingSet<DoubleSolution> trainingSet = new DTLZ3DTrainingSet();
    var parameterSpace = new YAMLParameterSpace("NSGAIIDoubleFull.yaml", new DoubleParameterFactory());

    // 2. Define Generic Evaluator
    Function<Map<String, String>, Double> evaluator = config -> {
      // Convert config map to args array
      List<String> argsList = new ArrayList<>();
      config.forEach((k, v) -> { argsList.add("--" + k); argsList.add(v); });
      String[] configArgs = argsList.toArray(new String[0]);

      // Run Training Set
      var runner = new TrainingSetRunner.Builder<>(
              trainingSet, 
              new DoubleNSGAII(100, parameterSpace), 
              configArgs
          )
          .outputDir("results/temp_ablation")
          .indicators(List.of(new NormalizedHypervolume()))
          .build();

      Map<String, List<DoubleSolution>> results = runner.run();

      // Compute Aggregated Metric (Mean Normalized Hypervolume)
      double sumHV = 0.0;
      int count = 0;
      var problems = trainingSet.problemList();
      var refFronts = trainingSet.referenceFronts();

      for (int i = 0; i < problems.size(); i++) {
        var result = results.get(problems.get(i).getClass().getSimpleName());
        if (result != null && !result.isEmpty()) {
            double[][] front = SolutionListUtils.getMatrixWithObjectiveValues(result);
            double[][] ref = VectorUtils.readVectors(refFronts.get(i), ",");
            sumHV += new NormalizedHypervolume(ref).compute(front);
            count++;
        }
      }
      return count > 0 ? sumHV / count : 0.0;
    };

    // 3. Run Analysis (Maximize = true for Hypervolume)
    AblationAnalysis analysis = new AblationAnalysis(evaluator, true);
    AblationResult result = analysis.performPathAblation(defaultConfigStr, optimizedConfigStr);
    // ... save results
  }
}
```

## Visualization
Use the provided Python script to generate plots and LaTeX tables.

```bash
# Path Ablation Plot
python3 analysis/ablation_visualizer.py path_ablation.csv --type path --output_dir results/ --metric_label "Normalized Hypervolume"

# LOO Bar Chart
python3 analysis/ablation_visualizer.py loo_ablation.csv --type loo --output_dir results/ --metric_label "Normalized Hypervolume"
```
