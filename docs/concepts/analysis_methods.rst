Analysis Methods
================

Evolver provides comprehensive analysis tools for understanding algorithm parameter behavior and performance relationships. These tools help researchers identify which parameters matter most and how they interact to influence optimization performance.

Overview
--------

The analysis framework consists of two complementary approaches:

1. **Ablation Analysis** - Controlled experiments to establish causal relationships between parameters and performance
2. **Feature Importance Analysis** - Machine learning-based analysis to identify statistical patterns in parameter behavior

Both methods serve different purposes and are most effective when used together in an integrated workflow.

Ablation Analysis
-----------------

Ablation analysis determines parameter contributions through controlled experiments that systematically remove or modify parameters to measure their causal impact on performance.

Methodology
~~~~~~~~~~~

The framework implements two primary ablation methods:

**Leave-One-Out Analysis**
  Measures the performance impact of removing each parameter from an optimized configuration. Parameters that cause the largest performance drops when removed are considered most important.

**Forward Path Analysis**
  Builds an optimal configuration step by step, starting from a baseline and adding the parameter that provides the largest improvement at each step.

Key Features
~~~~~~~~~~~~

- **Multi-Problem Analysis**: Evaluates parameter contributions across multiple benchmark problems
- **Thread-Safe Parallel Execution**: Configurable parallelism for faster analysis
- **Real-Time Progress Reporting**: ASCII progress bars and detailed timing information
- **Comprehensive Validation**: Built-in configuration validation and error handling
- **Flexible Configuration**: Fluent API for easy customization of all analysis aspects

Usage Example
~~~~~~~~~~~~~

.. code-block:: java

   // Simple configuration-based approach
   AblationConfiguration config = AblationConfiguration.forZDTProblems()
       .numberOfRuns(25)
       .numberOfThreads(8)
       .enableProgressReporting(true);
   
   AblationRunner runner = new AblationRunner(config);
   runner.run();

**Command Line Usage:**

.. code-block:: bash

   java SimpleZDTAblationExample --threads 4 --runs 25 --no-progress

Configuration System
~~~~~~~~~~~~~~~~~~~~

The ablation framework uses a configuration-based approach that eliminates code duplication:

- **AblationConfiguration**: Fluent API for configuring all analysis aspects
- **AblationRunner**: Generic execution engine for configured analyses  
- **ProblemSuiteFactory**: Factory for creating ZDT, DTLZ, and WFG problem suites
- **Progress Reporting**: Real-time progress tracking with customizable reporters

This approach reduces boilerplate code by 95% compared to manual implementation.

Feature Importance Analysis
---------------------------

Feature importance analysis uses Random Forest regression to identify which algorithm parameters most strongly predict optimization performance, based on existing meta-optimization results.

Methodology
~~~~~~~~~~~

The analyzer processes meta-optimization results (CONFIGURATIONS.csv + INDICATORS.csv) and provides two complementary importance measures:

**Gini Importance**
  Built-in Random Forest measure based on decrease in node impurity when a parameter is used for splitting decisions. Fast to compute but can be biased toward high-cardinality features.

**Permutation Importance**
  More robust measure that calculates performance decrease when parameter values are randomly shuffled. Model-agnostic and handles feature interactions better.

Key Features
~~~~~~~~~~~~

- **Automatic Data Processing**: Merges configuration and indicator data, handles missing values
- **Model Quality Assessment**: Built-in R² scoring for reliability evaluation
- **Professional Reporting**: Formatted tables and CSV export for further analysis
- **Interaction Analysis**: Works with InteractionAnalyzer for parameter interaction discovery
- **Configurable Models**: Tunable Random Forest hyperparameters for different scenarios

Usage Example
~~~~~~~~~~~~~

.. code-block:: java

   // Create analyzer for meta-optimization results
   FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
       .setNumberOfTrees(100)
       .setMaxDepth(10);
   
   // Load data and train model
   analyzer.loadData("NHV"); // Use Normalized Hypervolume as target
   analyzer.trainModel();
   
   // Get importance rankings
   Map<String, Double> giniImportance = analyzer.getGiniImportance();
   Map<String, Double> permImportance = analyzer.getPermutationImportance(10);
   
   // Generate report
   System.out.println(analyzer.generateReport());

Data Requirements
~~~~~~~~~~~~~~~~~

The analyzer expects two CSV files from meta-optimization experiments:

**CONFIGURATIONS.csv**: Parameter configurations
  Contains columns for Evaluation, SolutionId, and all algorithm parameters

**INDICATORS.csv**: Quality indicator values  
  Contains columns for Evaluation, SolutionId, and quality indicators (EP, NHV, IGD+, etc.)

Complementary Approaches
------------------------

Comparison
~~~~~~~~~~

.. list-table:: Ablation vs Feature Importance Analysis
   :header-rows: 1
   :widths: 20 40 40

   * - Aspect
     - Ablation Analysis
     - Feature Importance Analysis
   * - Purpose
     - Proves causal relationships
     - Discovers statistical patterns
   * - Method
     - Controlled experiments
     - Machine learning analysis
   * - Data Source
     - Requires new algorithm runs
     - Uses existing meta-optimization results
   * - Speed
     - Slow (hours/days)
     - Fast (seconds/minutes)
   * - Cost
     - High computational cost
     - Low computational cost
   * - Causality
     - Direct causal evidence
     - Correlational evidence
   * - Interactions
     - Limited to pairwise
     - Captures complex non-linear interactions

Integrated Workflow
~~~~~~~~~~~~~~~~~~~

The most effective approach combines both methods:

.. code-block:: none

   Meta-Optimization → Feature Importance → Targeted Ablation → Validation
      (Generate data)    (Quick screening)   (Causal analysis)   (Confirm findings)

**Phase 1: Exploratory Analysis**
  Use feature importance to quickly identify promising parameters from existing meta-optimization results.

**Phase 2: Confirmatory Analysis**  
  Focus expensive ablation studies on the most important parameters identified in Phase 1.

**Phase 3: Validation**
  Cross-validate findings on independent test problems to ensure generalizability.

When to Use Each Method
~~~~~~~~~~~~~~~~~~~~~~~

**Use Feature Importance Analysis when:**

- You have existing meta-optimization results to analyze
- You want quick insights (minutes vs hours)  
- You need to prioritize parameters for further study
- You want to understand parameter interactions
- You're exploring new algorithms or problems

**Use Ablation Analysis when:**

- You need causal proof of parameter effects
- You're preparing results for publication
- You want precise quantification of contributions  
- You have computational budget for extensive experiments
- You need to validate feature importance findings

Implementation Details
----------------------

Package Structure
~~~~~~~~~~~~~~~~~

The analysis framework is organized into two main packages:

**org.uma.evolver.analysis.ablation**
  Contains all ablation analysis components including configuration system, analyzers, and example classes.

**org.uma.evolver.analysis**  
  Contains feature importance analysis, interaction analysis, and robustness analysis components.

Example Classes
~~~~~~~~~~~~~~~

**Simple Examples (Recommended)**

- ``SimpleZDTAblationExample`` - Minimal ZDT ablation (3 lines of code)
- ``SimpleDTLZAblationExample`` - Minimal DTLZ ablation (3 lines of code)  
- ``FeatureImportanceExample`` - Complete feature importance analysis workflow

**Full Examples (Legacy, Refactored)**

- ``ZDTAblationAnalysisExample`` - ZDT ablation using configuration system
- ``DTLZ3DAblationAnalysisExample`` - DTLZ ablation using configuration system

Performance Considerations
~~~~~~~~~~~~~~~~~~~~~~~~~~

**Ablation Analysis**
  - Scales with: problems × runs × parameters × evaluations
  - Typical runtime: Hours to days for comprehensive analysis
  - Memory usage: Moderate (thread-local indicators)
  - Parallelization: Excellent scaling up to available cores

**Feature Importance Analysis**  
  - Scales with: samples × features × trees
  - Typical runtime: Seconds to minutes
  - Memory usage: Low to moderate depending on dataset size
  - Parallelization: Built into Random Forest training

Best Practices
--------------

Data Quality
~~~~~~~~~~~~

**For Ablation Analysis:**
- Use sufficient runs (≥10) for statistical reliability
- Ensure proper reference front availability
- Validate configurations before analysis
- Use normalized quality indicators

**For Feature Importance Analysis:**  
- Aim for ≥100 samples per parameter for reliable results
- Ensure good parameter space coverage
- Check model R² score (≥0.5 recommended)
- Use both Gini and Permutation importance

Integration Strategy
~~~~~~~~~~~~~~~~~~~~

1. **Start Broad**: Use feature importance for initial parameter screening
2. **Focus Deep**: Use ablation analysis for detailed causal analysis of important parameters  
3. **Validate Thoroughly**: Test findings on independent problem sets
4. **Document Results**: Use both methods to provide comprehensive evidence

The combination of ablation and feature importance analysis provides researchers with powerful tools for understanding algorithm behavior, making informed design decisions, and providing robust evidence for parameter choices in publications.