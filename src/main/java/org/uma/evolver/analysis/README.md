# Analysis Package

This package provides advanced analysis tools for understanding algorithm parameter behavior and performance relationships in metaheuristic optimization. It complements the ablation analysis framework with machine learning-based approaches.

## Overview

The analysis package focuses on **post-hoc analysis** of meta-optimization results, using machine learning techniques to understand which parameters matter most and how they interact. This is different from but complementary to ablation analysis.

## Core Components

### 🔍 **FeatureImportanceAnalyzer**
- **Purpose**: Identifies which algorithm parameters most strongly influence performance
- **Method**: Random Forest regression on meta-optimization results
- **Input**: CONFIGURATIONS.csv + INDICATORS.csv from meta-optimization experiments
- **Output**: Ranked parameter importance scores with two different metrics

### 🔗 **InteractionAnalyzer** 
- **Purpose**: Analyzes how pairs of parameters interact to affect performance
- **Method**: Partial Dependence Plot (PDP) generation using trained Random Forest
- **Input**: Trained FeatureImportanceAnalyzer model
- **Output**: 2D interaction grids showing parameter interaction effects

### 📊 **RobustnessAnalyzer**
- **Purpose**: Evaluates parameter sensitivity and robustness across different conditions
- **Method**: Statistical analysis of parameter performance variance
- **Input**: Multi-run optimization results
- **Output**: Robustness metrics and sensitivity analysis

## Relationship to Ablation Analysis

### **Complementary Approaches**

| Aspect | **Ablation Analysis** | **Feature Importance Analysis** |
|--------|----------------------|--------------------------------|
| **Method** | Algorithmic (leave-one-out, forward path) | Machine Learning (Random Forest) |
| **Data Source** | Controlled ablation experiments | Meta-optimization results |
| **Perspective** | Causal (what happens when removed) | Correlational (what predicts performance) |
| **Granularity** | Parameter-level contributions | Feature-level importance + interactions |
| **Computational Cost** | High (requires many algorithm runs) | Low (post-hoc analysis of existing data) |
| **Interpretability** | Direct causal relationships | Statistical associations |

### **When to Use Each**

**Use Ablation Analysis when:**
- You want to understand **causal relationships** between parameters and performance
- You need to validate that parameter changes **directly cause** performance improvements
- You're designing new algorithms and need to understand parameter contributions
- You have computational budget for extensive algorithm runs

**Use Feature Importance Analysis when:**
- You have **existing meta-optimization results** to analyze
- You want to quickly identify **promising parameter regions**
- You need to understand **parameter interactions** and non-linear relationships
- You want to **guide future ablation studies** by identifying important parameters first

### **Integrated Workflow**

```
1. Meta-Optimization → 2. Feature Importance → 3. Targeted Ablation → 4. Validation
   (Generate data)      (Identify key params)   (Causal analysis)    (Confirm findings)
```

## Feature Importance Analysis

### **Core Functionality**

The `FeatureImportanceAnalyzer` provides two complementary importance measures:

#### **1. Gini Importance (Built-in)**
- **Method**: Measures decrease in node impurity when parameter is used for splitting
- **Advantages**: Fast to compute, built into Random Forest
- **Limitations**: Can be biased toward high-cardinality features

#### **2. Permutation Importance (Robust)**
- **Method**: Measures performance drop when parameter values are randomly shuffled
- **Advantages**: More robust, model-agnostic, handles feature interactions
- **Limitations**: Computationally more expensive

### **Usage Example**

```java
// Create analyzer for meta-optimization results
FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
    .setNumberOfTrees(100)
    .setMaxDepth(10)
    .setSeed(42);

// Load data (CONFIGURATIONS.csv + INDICATORS.csv)
analyzer.loadData("NHV"); // Use Normalized Hypervolume as target

// Train Random Forest model
analyzer.trainModel();

// Get importance rankings
Map<String, Double> giniImportance = analyzer.getGiniImportance();
Map<String, Double> permImportance = analyzer.getPermutationImportance(10);

// Generate formatted report
System.out.println(analyzer.generateReport());

// Export to CSV
String csvOutput = analyzer.toCSV();
```

### **Input Data Format**

The analyzer expects two CSV files from meta-optimization experiments:

**CONFIGURATIONS.csv**
```csv
Evaluation,SolutionId,crossoverProbability,mutationRate,populationSize,...
1,1,0.9,0.1,100,...
1,2,0.8,0.15,120,...
...
```

**INDICATORS.csv**
```csv
Evaluation,SolutionId,EP,NHV,IGD,...
1,1,0.045,0.892,0.123,...
1,2,0.052,0.885,0.134,...
...
```

### **Key Features**

#### **Data Processing**
- **Automatic Merging**: Joins configuration and indicator data by Evaluation+SolutionId
- **Missing Value Handling**: Imputes NaN values with column medians
- **Flexible Targets**: Can analyze any quality indicator (EP, NHV, IGD+, etc.)

#### **Model Configuration**
- **Configurable Hyperparameters**: Trees, depth, node size, random seed
- **Model Validation**: Built-in R² calculation for model quality assessment
- **Robust Training**: Handles various data sizes and parameter types

#### **Output Formats**
- **Formatted Report**: Professional table with rankings and statistics
- **CSV Export**: Machine-readable format for further analysis
- **Programmatic Access**: Direct access to importance maps

## Interaction Analysis

### **Purpose**
Understanding how parameter pairs interact to influence performance, beyond individual parameter effects.

### **Method: Partial Dependence Plots (PDP)**
1. **Grid Generation**: Creates a grid of values for two selected parameters
2. **Prediction**: For each grid point, predicts performance while averaging over all other parameters
3. **Visualization Data**: Exports 2D grid suitable for heatmap visualization

### **Usage Example**

```java
// After training FeatureImportanceAnalyzer
InteractionAnalyzer interaction = new InteractionAnalyzer(analyzer);

// Analyze interaction between top 2 parameters
String param1 = "crossoverProbability";
String param2 = "mutationRate";

// Export 20x20 interaction grid
interaction.exportInteractionGrid(param1, param2, 20, 
    Path.of("interaction_crossover_mutation.csv"));
```

### **Output Format**
```csv
crossoverProbability,mutationRate,PredictedValue
0.1,0.05,0.823
0.1,0.10,0.834
...
```

## Practical Applications

### **1. Algorithm Design**
- **Parameter Prioritization**: Focus tuning efforts on high-importance parameters
- **Interaction Discovery**: Identify parameter pairs that work synergistically
- **Simplification**: Remove low-importance parameters to reduce complexity

### **2. Meta-Optimization Guidance**
- **Search Space Reduction**: Focus on important parameter regions
- **Interaction-Aware Tuning**: Consider parameter interactions in optimization
- **Transfer Learning**: Apply importance insights across similar problems

### **3. Research Insights**
- **Parameter Understanding**: Gain insights into algorithm behavior
- **Comparative Analysis**: Compare parameter importance across different problems
- **Publication Support**: Provide statistical evidence for parameter choices

## Advanced Features

### **Model Quality Assessment**
```java
// Check model quality
double r2Score = analyzer.calculateR2();
System.out.println("Model R²: " + r2Score);

// Good model: R² > 0.7
// Acceptable: R² > 0.5  
// Poor: R² < 0.3 (consider more data or different approach)
```

### **Hyperparameter Tuning**
```java
FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
    .setNumberOfTrees(200)    // More trees for better accuracy
    .setMaxDepth(15)          // Deeper trees for complex relationships
    .setMinNodeSize(3)        // Smaller nodes for fine-grained splits
    .setSeed(42);             // Reproducible results
```

### **Robust Permutation Importance**
```java
// Use more permutations for more stable estimates
Map<String, Double> robustImportance = analyzer.getPermutationImportance(50);

// Compare with Gini importance to identify potential biases
Map<String, Double> giniImportance = analyzer.getGiniImportance();
```

## Integration with Ablation Analysis

### **Sequential Workflow**

**Phase 1: Exploratory Analysis**
```java
// 1. Analyze existing meta-optimization results
FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir);
analyzer.loadData("NHV");
analyzer.trainModel();

// 2. Identify top parameters
Map<String, Double> importance = analyzer.getGiniImportance();
List<String> topParams = importance.keySet().stream().limit(5).toList();
```

**Phase 2: Targeted Ablation**
```java
// 3. Focus ablation analysis on important parameters
AblationConfiguration config = new AblationConfiguration()
    .problemSuite("ZDT")
    .numberOfRuns(25)
    // Focus on parameters identified by feature importance
    .optimizedConfiguration(buildConfigWithTopParams(topParams));

AblationRunner runner = new AblationRunner(config);
runner.run();
```

### **Validation Workflow**

**Cross-Validation Approach**
1. **Feature Importance**: Identify important parameters from meta-optimization
2. **Ablation Validation**: Confirm importance through controlled ablation
3. **Interaction Analysis**: Understand how important parameters interact
4. **Final Validation**: Test final configuration on independent problems

## Performance Considerations

### **Computational Complexity**
- **Training**: O(n × log(n) × trees × features) - typically seconds to minutes
- **Prediction**: O(trees) per prediction - very fast
- **Permutation Importance**: O(features × permutations × n) - moderate cost
- **Interaction Analysis**: O(grid² × samples) - can be expensive for large grids

### **Memory Usage**
- **Data Loading**: ~8 bytes per numeric value in dataset
- **Model Storage**: ~MB per 100 trees (depends on depth)
- **Interaction Grids**: ~8 bytes per grid point

### **Scalability Guidelines**
- **Small datasets** (<1K samples): Use default settings
- **Medium datasets** (1K-10K samples): Increase trees to 200-500
- **Large datasets** (>10K samples): Consider sampling for interaction analysis

## Best Practices

### **Data Quality**
1. **Sufficient Samples**: Aim for >100 samples per parameter for reliable results
2. **Parameter Diversity**: Ensure good coverage of parameter space
3. **Quality Indicators**: Use normalized indicators (NHV preferred over raw values)

### **Model Configuration**
1. **Tree Count**: Start with 100, increase to 500 for better accuracy
2. **Depth Limit**: Use 10-15 to prevent overfitting
3. **Validation**: Always check R² score before interpreting results

### **Interpretation**
1. **Compare Metrics**: Use both Gini and Permutation importance
2. **Statistical Significance**: Higher importance differences are more reliable
3. **Domain Knowledge**: Validate results against algorithmic understanding

### **Integration Strategy**
1. **Start Broad**: Use feature importance to identify candidate parameters
2. **Focus Deep**: Use ablation analysis for detailed causal analysis
3. **Validate Thoroughly**: Test findings on independent problem sets

## Limitations and Considerations

### **Statistical Limitations**
- **Correlation vs Causation**: Feature importance shows correlation, not causation
- **Model Assumptions**: Random Forest assumes feature interactions are learnable
- **Sample Bias**: Results depend on quality and diversity of training data

### **Methodological Considerations**
- **Parameter Encoding**: Categorical parameters need proper encoding
- **Scale Sensitivity**: Normalize parameters if they have very different scales
- **Interaction Complexity**: Higher-order interactions (>2 parameters) not directly captured

### **Complementary Nature**
Feature importance analysis is **not a replacement** for ablation analysis, but rather a **complementary tool** that:
- Provides **quick insights** from existing data
- **Guides** more expensive ablation studies
- **Validates** ablation findings from a different perspective
- **Discovers** parameter interactions that ablation might miss

## Future Extensions

### **Planned Enhancements**
- **Multi-Objective Analysis**: Handle multiple indicators simultaneously
- **Temporal Analysis**: Track parameter importance over optimization progress
- **Ensemble Methods**: Combine multiple ML models for more robust importance
- **Automated Reporting**: Generate publication-ready figures and tables

### **Integration Opportunities**
- **Real-Time Analysis**: Analyze importance during meta-optimization
- **Adaptive Sampling**: Use importance to guide parameter space exploration
- **Transfer Learning**: Apply importance patterns across problem domains
- **Visualization Tools**: Interactive parameter importance and interaction plots

This analysis framework provides a powerful complement to ablation studies, enabling researchers to efficiently understand parameter behavior and make informed decisions about algorithm design and tuning.

## References

### Ablation Analysis

```bibtex
@article{fawcett2016ablation,
  title={An ablation study of parameter control mechanisms in multi-objective evolutionary algorithms},
  author={Fawcett, Chris and Hoos, Holger H},
  journal={Artificial Intelligence},
  volume={245},
  pages={96--119},
  year={2016},
  publisher={Elsevier}
}

@article{lopez2016irace,
  title={The irace package: Iterated racing for automatic algorithm configuration},
  author={L{\'o}pez-Ib{\'a}{\~n}ez, Manuel and Dubois-Lacoste, J{\'e}r{\'e}mie and C{\'a}ceres, Leslie P{\'e}rez and Birattari, Mauro and St{\"u}tzle, Thomas},
  journal={Operations Research Perspectives},
  volume={3},
  pages={43--58},
  year={2016},
  publisher={Elsevier}
}
```

### Feature Importance Analysis

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
  note={Original Random Forest paper with feature importance}
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
  note={Critical analysis of Random Forest importance measures}
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
  note={Permutation importance methodology}
}

@inproceedings{hutter2014efficient,
  title={An efficient approach for assessing hyperparameter importance},
  author={Hutter, Frank and Hoos, Holger and Leyton-Brown, Kevin},
  booktitle={International Conference on Machine Learning},
  pages={754--762},
  year={2014},
  note={Functional ANOVA for hyperparameter importance}
}
```

### Algorithm Configuration and Meta-Optimization

```bibtex
@article{hutter2011sequential,
  title={Sequential model-based optimization for general algorithm configuration},
  author={Hutter, Frank and Hoos, Holger H and Leyton-Brown, Kevin},
  journal={Learning and Intelligent Optimization},
  pages={507--523},
  year={2011},
  publisher={Springer}
}

@article{eiben2007parameter,
  title={Parameter tuning for configuring and analyzing evolutionary algorithms},
  author={Eiben, Agoston E and Smit, Selmar K},
  journal={Swarm and Evolutionary Computation},
  volume={1},
  number={1},
  pages={19--31},
  year={2011},
  publisher={Elsevier}
}
```