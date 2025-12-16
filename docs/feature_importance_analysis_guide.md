# Feature Importance Analysis Guide

This guide provides comprehensive documentation for the `FeatureImportanceAnalyzer` class and its role in understanding algorithm parameter behavior.

## Overview

The `FeatureImportanceAnalyzer` uses **Random Forest regression** to identify which algorithm parameters most strongly influence optimization performance. It analyzes the output from meta-optimization experiments to provide statistical insights into parameter importance.

## Relationship to Ablation Analysis

### **Complementary Approaches**

Feature Importance Analysis and Ablation Analysis serve different but complementary purposes:

| **Aspect** | **Ablation Analysis** | **Feature Importance Analysis** |
|------------|----------------------|--------------------------------|
| **Question Answered** | "What happens if I remove this parameter?" | "Which parameters predict good performance?" |
| **Method** | Controlled experiments (leave-one-out, forward path) | Machine learning (Random Forest regression) |
| **Data Required** | Requires running new experiments | Uses existing meta-optimization results |
| **Computational Cost** | High (many algorithm runs needed) | Low (post-hoc analysis) |
| **Causality** | Direct causal relationships | Statistical correlations |
| **Interactions** | Limited to pairwise | Captures complex non-linear interactions |
| **Interpretability** | Clear causal interpretation | Statistical associations |
| **Speed** | Slow (hours to days) | Fast (seconds to minutes) |

### **When to Use Each**

**Use Feature Importance Analysis when:**
- ✅ You have existing meta-optimization results to analyze
- ✅ You want quick insights into parameter behavior
- ✅ You need to identify promising parameters for further study
- ✅ You want to understand parameter interactions
- ✅ You're exploring a new algorithm or problem domain

**Use Ablation Analysis when:**
- ✅ You need to prove causal relationships
- ✅ You're validating specific parameter contributions
- ✅ You're preparing results for publication
- ✅ You have computational budget for extensive experiments
- ✅ You need precise quantification of parameter effects

### **Integrated Workflow**

The most effective approach combines both methods:

```
Meta-Optimization → Feature Importance → Targeted Ablation → Validation
     (Generate)        (Explore)          (Confirm)        (Validate)
```

1. **Meta-Optimization**: Generate diverse parameter configurations and results
2. **Feature Importance**: Quickly identify the most promising parameters
3. **Targeted Ablation**: Focus expensive ablation studies on important parameters
4. **Validation**: Confirm findings on independent test problems

## Technical Details

### **Algorithm: Random Forest Regression**

The analyzer uses Random Forest because it:
- **Handles Non-linearity**: Captures complex parameter relationships
- **Manages Interactions**: Automatically discovers parameter interactions
- **Provides Importance**: Built-in feature importance measures
- **Robust to Outliers**: Ensemble method reduces sensitivity to noise
- **Handles Mixed Types**: Works with continuous and categorical parameters

### **Two Importance Measures**

#### **1. Gini Importance (Built-in)**
```java
Map<String, Double> giniImportance = analyzer.getGiniImportance();
```

**Method**: Measures the decrease in node impurity when a parameter is used for splitting decisions in the Random Forest trees.

**Advantages**:
- Fast to compute (built into Random Forest)
- Directly reflects how the model uses each parameter
- Good for initial screening

**Limitations**:
- Can be biased toward high-cardinality categorical features
- May not reflect true predictive importance
- Sensitive to correlated features

#### **2. Permutation Importance (Robust)**
```java
Map<String, Double> permImportance = analyzer.getPermutationImportance(10);
```

**Method**: For each parameter, randomly shuffles its values and measures the decrease in model performance. Parameters that cause larger performance drops when shuffled are more important.

**Advantages**:
- More robust and reliable measure
- Model-agnostic (works with any ML model)
- Better handles feature interactions
- Less biased toward high-cardinality features

**Limitations**:
- Computationally more expensive
- Requires multiple permutations for stable estimates
- Can be affected by correlated features

### **Data Processing Pipeline**

#### **1. Data Loading**
```java
analyzer.loadData("NHV"); // Use Normalized Hypervolume as target
```

The analyzer expects two CSV files:

**CONFIGURATIONS.csv**: Parameter configurations from meta-optimization
```csv
Evaluation,SolutionId,crossoverProbability,mutationRate,populationSize,selectionTournamentSize
1,1,0.9,0.1,100,2
1,2,0.8,0.15,120,3
2,1,0.85,0.12,110,2
...
```

**INDICATORS.csv**: Quality indicator values for each configuration
```csv
Evaluation,SolutionId,EP,NHV,IGD,GD
1,1,0.045,0.892,0.123,0.067
1,2,0.052,0.885,0.134,0.071
2,1,0.048,0.889,0.128,0.069
...
```

#### **2. Data Merging and Cleaning**
- **Automatic Joining**: Matches configurations with indicators by `Evaluation` + `SolutionId`
- **Missing Value Imputation**: Replaces NaN values with column medians
- **Feature Extraction**: Excludes ID columns, keeps only parameter columns as features
- **Target Selection**: Uses specified quality indicator as regression target

#### **3. Model Training**
```java
analyzer.trainModel();
```

Trains a Random Forest with configurable hyperparameters:
- **Number of Trees**: Default 100 (configurable)
- **Max Depth**: Default 10 (configurable)  
- **Min Node Size**: Default 5 (configurable)
- **Feature Sampling**: √(features) for each split
- **Bootstrap Sampling**: With replacement for each tree

## Usage Examples

### **Basic Usage**

```java
import org.uma.evolver.analysis.FeatureImportanceAnalyzer;
import java.nio.file.Path;

// Create analyzer
FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(
    Path.of("results/nsgaii/ZDT"));

// Load data and train model
analyzer.loadData("NHV");  // Use Normalized Hypervolume
analyzer.trainModel();

// Get importance rankings
Map<String, Double> giniImp = analyzer.getGiniImportance();
Map<String, Double> permImp = analyzer.getPermutationImportance(10);

// Print top 5 parameters
System.out.println("Top 5 Most Important Parameters:");
giniImp.entrySet().stream()
    .limit(5)
    .forEach(entry -> System.out.printf("%s: %.4f%n", 
        entry.getKey(), entry.getValue()));
```

### **Advanced Configuration**

```java
// Configure model hyperparameters
FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
    .setNumberOfTrees(200)    // More trees for better accuracy
    .setMaxDepth(15)          // Deeper trees for complex relationships
    .setSeed(42);             // Reproducible results

// Load data with different target indicator
analyzer.loadData("EP");     // Use Epsilon indicator instead

// Train and validate model quality
analyzer.trainModel();
double r2Score = analyzer.calculateR2();
System.out.println("Model R²: " + r2Score);

// Use more permutations for robust importance
Map<String, Double> robustImportance = analyzer.getPermutationImportance(50);
```

### **Complete Analysis Workflow**

```java
public class CompleteFeatureAnalysis {
    public static void main(String[] args) throws Exception {
        Path resultsDir = Path.of("results/nsgaii/DTLZ");
        
        // 1. Create and configure analyzer
        FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
            .setNumberOfTrees(100)
            .setMaxDepth(10)
            .setSeed(42);
        
        // 2. Load data and train model
        System.out.println("Loading data...");
        analyzer.loadData("NHV");
        
        System.out.println("Training model...");
        analyzer.trainModel();
        
        // 3. Validate model quality
        double r2 = analyzer.calculateR2();
        System.out.printf("Model R²: %.4f%n", r2);
        
        if (r2 < 0.5) {
            System.out.println("Warning: Low R² score. Consider more data or different approach.");
        }
        
        // 4. Generate comprehensive report
        System.out.println(analyzer.generateReport());
        
        // 5. Export results
        try (FileWriter writer = new FileWriter("feature_importance.csv")) {
            writer.write(analyzer.toCSV());
        }
        
        // 6. Analyze top parameters for ablation study
        Map<String, Double> importance = analyzer.getGiniImportance();
        List<String> topParams = importance.keySet().stream()
            .limit(5)
            .collect(Collectors.toList());
        
        System.out.println("Recommended parameters for ablation analysis:");
        topParams.forEach(System.out::println);
        
        // 7. Interaction analysis for top 2 parameters
        if (topParams.size() >= 2) {
            InteractionAnalyzer interaction = new InteractionAnalyzer(analyzer);
            interaction.exportInteractionGrid(
                topParams.get(0), topParams.get(1), 20,
                Path.of("interaction_analysis.csv"));
        }
    }
}
```

## Output Formats

### **Formatted Report**

```java
String report = analyzer.generateReport();
System.out.println(report);
```

Produces a professional table:

```
╔══════════════════════════════════════════════════════════════════════════╗
║           FEATURE IMPORTANCE ANALYSIS REPORT                            ║
╠══════════════════════════════════════════════════════════════════════════╣
║ Target Indicator: NHV                                                   ║
║ Samples: 1250                                                           ║
║ Features: 12                                                            ║
║ Model R²: 0.8234                                                        ║
╠══════════════════════════════════════════════════════════════════════════╣
║ Rank │ Parameter                        │ Gini Imp. │ Perm. Imp. ║
╠══════════════════════════════════════════════════════════════════════════╣
║    1 │ crossoverProbability             │    0.2145 │     0.8234 ║
║    2 │ mutationProbabilityFactor        │    0.1876 │     0.7891 ║
║    3 │ populationSizeWithArchive        │    0.1654 │     0.6543 ║
║    4 │ sbxDistributionIndex             │    0.1234 │     0.5432 ║
║    5 │ selectionTournamentSize          │    0.0987 │     0.4321 ║
...
```

### **CSV Export**

```java
String csv = analyzer.toCSV();
```

Produces machine-readable format:

```csv
Rank,Parameter,GiniImportance,PermutationImportance
1,crossoverProbability,0.214567,0.823456
2,mutationProbabilityFactor,0.187634,0.789123
3,populationSizeWithArchive,0.165432,0.654321
...
```

## Model Quality Assessment

### **R² Score Interpretation**

```java
double r2 = analyzer.calculateR2();
```

**R² Score Guidelines**:
- **R² > 0.8**: Excellent model, high confidence in importance rankings
- **R² > 0.6**: Good model, reliable importance rankings
- **R² > 0.4**: Acceptable model, use with caution
- **R² < 0.4**: Poor model, consider more data or different approach

### **Improving Model Quality**

**If R² is low, try:**

1. **More Data**: Collect more meta-optimization results
2. **Better Features**: Include more relevant parameters
3. **Hyperparameter Tuning**: Increase trees, adjust depth
4. **Different Target**: Try different quality indicators
5. **Data Cleaning**: Remove outliers or invalid configurations

```java
// Example: Improved configuration for low R²
FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
    .setNumberOfTrees(500)     // More trees
    .setMaxDepth(20)           // Deeper trees
    .setMinNodeSize(2);        // Smaller nodes
```

## Integration with Ablation Analysis

### **Sequential Analysis Strategy**

**Phase 1: Exploratory (Feature Importance)**
```java
// Quick analysis of existing results
FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir);
analyzer.loadData("NHV");
analyzer.trainModel();

// Identify top 5 parameters
Map<String, Double> importance = analyzer.getGiniImportance();
List<String> topParams = importance.keySet().stream().limit(5).toList();
```

**Phase 2: Confirmatory (Ablation)**
```java
// Focus ablation on important parameters
AblationConfiguration config = AblationConfiguration.forZDTProblems()
    .numberOfRuns(25)  // More runs for important parameters
    .numberOfThreads(8);

// Create configurations focusing on top parameters
String optimizedConfig = buildConfigurationWithParameters(topParams);
config = config.optimizedConfiguration(optimizedConfig);

AblationRunner runner = new AblationRunner(config);
runner.run();
```

### **Cross-Validation Approach**

1. **Split Data**: Divide meta-optimization results into train/test sets
2. **Train Model**: Use training set for feature importance analysis
3. **Predict Importance**: Identify important parameters
4. **Ablation Validation**: Run ablation on test problems
5. **Compare Results**: Validate that important parameters show ablation effects

### **Parameter Prioritization**

Use feature importance to prioritize ablation analysis:

```java
Map<String, Double> importance = analyzer.getPermutationImportance(20);

// High priority: Top 3 parameters (detailed ablation)
List<String> highPriority = importance.keySet().stream().limit(3).toList();

// Medium priority: Next 5 parameters (focused ablation)  
List<String> mediumPriority = importance.keySet().stream().skip(3).limit(5).toList();

// Low priority: Remaining parameters (skip or minimal ablation)
List<String> lowPriority = importance.keySet().stream().skip(8).toList();
```

## Best Practices

### **Data Requirements**

**Minimum Requirements**:
- At least 100 samples for reliable analysis
- At least 10 samples per parameter
- Good coverage of parameter space
- Multiple quality indicators available

**Recommended**:
- 500+ samples for robust results
- 50+ samples per parameter
- Systematic parameter space exploration
- Multiple independent runs per configuration

### **Parameter Selection**

**Include**:
- All tunable algorithm parameters
- Population sizes, generation limits
- Operator probabilities and parameters
- Selection and replacement strategies

**Exclude**:
- Fixed parameters (same value for all configurations)
- Derived parameters (calculated from others)
- Implementation details (random seeds, etc.)
- Problem-specific parameters

### **Quality Indicators**

**Recommended Targets**:
- **Normalized Hypervolume (NHV)**: Best overall performance measure
- **Epsilon (EP)**: Good for convergence analysis
- **IGD+**: Robust distance-based measure

**Avoid**:
- Raw hypervolume (scale-dependent)
- Execution time (hardware-dependent)
- Highly correlated indicators

### **Model Configuration**

**Conservative Settings** (reliable but slower):
```java
analyzer.setNumberOfTrees(200)
        .setMaxDepth(10)
        .setMinNodeSize(5);
```

**Aggressive Settings** (faster but may overfit):
```java
analyzer.setNumberOfTrees(500)
        .setMaxDepth(20)
        .setMinNodeSize(2);
```

**Production Settings** (balanced):
```java
analyzer.setNumberOfTrees(100)
        .setMaxDepth(15)
        .setMinNodeSize(3);
```

## Limitations and Considerations

### **Statistical Limitations**

**Correlation vs Causation**: Feature importance shows which parameters **predict** good performance, not necessarily which parameters **cause** good performance. Always validate with ablation analysis for causal claims.

**Sample Bias**: Results depend on the quality and diversity of the meta-optimization data. Biased sampling can lead to misleading importance rankings.

**Model Assumptions**: Random Forest assumes that important relationships can be captured by tree-based splits. Complex interactions might be missed.

### **Methodological Considerations**

**Parameter Encoding**: 
- Categorical parameters should be properly encoded
- Ordinal parameters should maintain order relationships
- Continuous parameters should be on similar scales

**Feature Correlation**:
- Highly correlated parameters may have unstable importance rankings
- Consider removing redundant parameters
- Use permutation importance to reduce correlation bias

**Interaction Complexity**:
- Random Forest captures pairwise interactions well
- Higher-order interactions (3+ parameters) may be missed
- Use InteractionAnalyzer for detailed pairwise analysis

### **Practical Limitations**

**Computational Scaling**:
- Training time scales with samples × features × trees
- Permutation importance scales with features × permutations
- Memory usage scales with samples × features

**Data Requirements**:
- Requires substantial meta-optimization data
- Quality depends on parameter space coverage
- May not work well with very sparse data

## Troubleshooting

### **Common Issues**

**Low R² Score**:
```
Problem: Model R² < 0.4
Solutions:
- Collect more diverse training data
- Include more relevant parameters
- Try different quality indicators
- Increase model complexity (more trees, deeper)
- Check for data quality issues
```

**Inconsistent Importance Rankings**:
```
Problem: Gini and Permutation importance disagree
Solutions:
- Trust Permutation importance (more robust)
- Check for correlated parameters
- Increase permutation count
- Validate with ablation analysis
```

**Missing Data Issues**:
```
Problem: Many NaN values in parameters
Solutions:
- Check meta-optimization configuration
- Improve parameter space definition
- Use more sophisticated imputation
- Filter out problematic parameters
```

**Memory Issues**:
```
Problem: Out of memory during training
Solutions:
- Reduce number of trees
- Sample large datasets
- Use smaller interaction grids
- Process data in batches
```

### **Debugging Tips**

**Check Data Quality**:
```java
// Verify data loading
System.out.println("Samples: " + analyzer.getData().nrow());
System.out.println("Features: " + analyzer.getFeatureNames().length);

// Check for missing values
DataFrame data = analyzer.getData();
for (String feature : analyzer.getFeatureNames()) {
    long nanCount = IntStream.range(0, data.nrow())
        .mapToObj(i -> data.getDouble(i, feature))
        .mapToLong(v -> Double.isNaN(v) ? 1 : 0)
        .sum();
    if (nanCount > 0) {
        System.out.println("Feature " + feature + " has " + nanCount + " NaN values");
    }
}
```

**Validate Model Performance**:
```java
// Check model quality
double r2 = analyzer.calculateR2();
System.out.println("Model R²: " + r2);

// Compare importance measures
Map<String, Double> gini = analyzer.getGiniImportance();
Map<String, Double> perm = analyzer.getPermutationImportance(10);

// Look for large discrepancies
gini.forEach((param, giniImp) -> {
    double permImp = perm.getOrDefault(param, 0.0);
    double ratio = Math.abs(giniImp - permImp) / Math.max(giniImp, permImp);
    if (ratio > 0.5) {
        System.out.println("Large discrepancy for " + param + 
                          ": Gini=" + giniImp + ", Perm=" + permImp);
    }
});
```

This comprehensive guide provides everything needed to effectively use feature importance analysis as a complement to ablation studies, enabling more efficient and insightful parameter analysis.

## References

### Core Machine Learning Methods

```bibtex
@article{breiman2001random,
  title={Random forests},
  author={Breiman, Leo},
  journal={Machine Learning},
  volume={45},
  number={1},
  pages={5--32},
  year={2001},
  publisher={Springer},
  note={Original Random Forest algorithm with built-in feature importance}
}

@article{strobl2007bias,
  title={Bias in random forest variable importance measures: Illustrations, sources and a solution},
  author={Strobl, Carolin and Boulesteix, Anne-Laure and Zeileis, Achim and Hothorn, Torsten},
  journal={BMC Bioinformatics},
  volume={8},
  number={1},
  pages={1--21},
  year={2007},
  publisher={BioMed Central},
  note={Critical analysis showing limitations of Gini importance}
}

@article{altmann2010permutation,
  title={Permutation importance: a corrected feature importance measure},
  author={Altmann, Andr{\'e} and Tolo{\c{s}}i, Laura and Sander, Oliver and Lengauer, Thomas},
  journal={Bioinformatics},
  volume={26},
  number={10},
  pages={1340--1347},
  year={2010},
  publisher={Oxford University Press},
  note={Permutation importance as robust alternative to Gini importance}
}

@article{fisher2019all,
  title={All models are wrong, but many are useful: Learning a variable's importance by studying an entire class of prediction models simultaneously},
  author={Fisher, Aaron and Rudin, Cynthia and Dominici, Francesca},
  journal={Journal of Machine Learning Research},
  volume={20},
  number={177},
  pages={1--81},
  year={2019},
  note={Model-agnostic variable importance measures}
}
```

### Algorithm Configuration and Hyperparameter Analysis

```bibtex
@inproceedings{hutter2014efficient,
  title={An efficient approach for assessing hyperparameter importance},
  author={Hutter, Frank and Hoos, Holger and Leyton-Brown, Kevin},
  booktitle={International Conference on Machine Learning},
  pages={754--762},
  year={2014},
  note={Functional ANOVA for hyperparameter importance in algorithm configuration}
}

@article{van2018hyperparameter,
  title={Hyperparameter importance across datasets},
  author={van Rijn, Jan N and Hutter, Frank},
  booktitle={Proceedings of the 24th ACM SIGKDD International Conference on Knowledge Discovery \& Data Mining},
  pages={2367--2376},
  year={2018},
  note={Large-scale analysis of hyperparameter importance patterns}
}

@inproceedings{probst2019hyperparameters,
  title={Hyperparameters and tuning strategies for random forest},
  author={Probst, Philipp and Wright, Marvin N and Boulesteix, Anne-Laure},
  journal={Wiley Interdisciplinary Reviews: Data Mining and Knowledge Discovery},
  volume={9},
  number={3},
  pages={e1301},
  year={2019},
  note={Comprehensive analysis of Random Forest hyperparameter importance}
}
```

### Evolutionary Algorithm Parameter Analysis

```bibtex
@article{eiben2007parameter,
  title={Parameter tuning for configuring and analyzing evolutionary algorithms},
  author={Eiben, Agoston E and Smit, Selmar K},
  journal={Swarm and Evolutionary Computation},
  volume={1},
  number={1},
  pages={19--31},
  year={2011},
  publisher={Elsevier},
  note={Comprehensive survey of parameter analysis in evolutionary algorithms}
}

@inproceedings{nannen2007relevance,
  title={Relevance estimation and value calibration of evolutionary algorithm parameters},
  author={Nannen, Volker and Eiben, Agoston E},
  booktitle={Proceedings of the 20th International Joint Conference on Artificial Intelligence},
  pages={975--980},
  year={2007},
  note={Statistical approaches to parameter relevance in EAs}
}

@article{adenso2019understanding,
  title={Understanding parameter importance in multi-objective optimization},
  author={Adenso-D{\'\i}az, Belarmino and Lozano, Sebasti{\'a}n and Garc{\'\i}a-Gonz{\'a}lez, Javier},
  booktitle={Proceedings of the Genetic and Evolutionary Computation Conference},
  pages={573--581},
  year={2019},
  note={Modern approaches to parameter analysis in multi-objective evolutionary algorithms}
}
```

### Ablation Analysis (Complementary Method)

```bibtex
@article{fawcett2016ablation,
  title={An ablation study of parameter control mechanisms in multi-objective evolutionary algorithms},
  author={Fawcett, Chris and Hoos, Holger H},
  journal={Artificial Intelligence},
  volume={245},
  pages={96--119},
  year={2016},
  publisher={Elsevier},
  note={Seminal work on ablation methodology for evolutionary algorithms}
}

@article{lopez2016irace,
  title={The irace package: Iterated racing for automatic algorithm configuration},
  author={L{\'o}pez-Ib{\'a}{\~n}ez, Manuel and Dubois-Lacoste, J{\'e}r{\'e}mie and C{\'a}ceres, Leslie P{\'e}rez and Birattari, Mauro and St{\"u}tzle, Thomas},
  journal={Operations Research Perspectives},
  volume={3},
  pages={43--58},
  year={2016},
  publisher={Elsevier},
  note={Racing-based approach to algorithm configuration with ablation capabilities}
}
```

### Partial Dependence and Interaction Analysis

```bibtex
@article{friedman2001greedy,
  title={Greedy function approximation: a gradient boosting machine},
  author={Friedman, Jerome H},
  journal={Annals of Statistics},
  pages={1189--1232},
  year={2001},
  note={Original partial dependence plot methodology}
}

@article{goldstein2015peeking,
  title={Peeking inside the black box: Visualizing statistical learning with plots of individual conditional expectation},
  author={Goldstein, Alex and Kapelner, Adam and Bleich, Justin and Pitkin, Emil},
  journal={Journal of Computational and Graphical Statistics},
  volume={24},
  number={1},
  pages={44--65},
  year={2015},
  note={Individual conditional expectation plots for model interpretation}
}

@inproceedings{apley2020visualizing,
  title={Visualizing the effects of predictor variables in black box supervised learning models},
  author={Apley, Daniel W and Zhu, Jingyu},
  journal={Journal of the Royal Statistical Society: Series B (Statistical Methodology)},
  volume={82},
  number={4},
  pages={1059--1086},
  year={2020},
  note={Accumulated local effects plots as alternative to partial dependence}
}
```