Analysis Tools
==============

This section provides detailed technical documentation for Evolver's analysis tools, including API references, configuration options, and advanced usage patterns.

Ablation Analysis Tools
-----------------------

Configuration API
~~~~~~~~~~~~~~~~~

The ``AblationConfiguration`` class provides a fluent API for configuring ablation analyses:

.. code-block:: java

   AblationConfiguration config = AblationConfiguration.forZDTProblems()
       .analysisName("Custom Analysis")
       .problemSuite("ZDT")
       .numberOfRuns(25)
       .maxEvaluations(30000)
       .numberOfThreads(8)
       .enableProgressReporting(true)
       .outputPrefix("my_results");

**Configuration Options:**

.. list-table:: AblationConfiguration Methods
   :header-rows: 1
   :widths: 30 20 50

   * - Method
     - Default
     - Description
   * - ``analysisName(String)``
     - "Ablation Analysis"
     - Display name for the analysis
   * - ``problemSuite(String)``
     - "ZDT"
     - Problem suite ("ZDT", "DTLZ", "WFG")
   * - ``numberOfRuns(int)``
     - 10
     - Independent runs per configuration per problem
   * - ``maxEvaluations(int)``
     - Varies by suite
     - Maximum evaluations per run
   * - ``numberOfThreads(int)``
     - All available
     - Thread count for parallel execution
   * - ``yamlParameterSpaceFile(String)``
     - "NSGAIIDouble.yaml"
     - YAML parameter space file
   * - ``enableProgressReporting(boolean)``
     - true
     - Enable real-time progress reporting
   * - ``outputPrefix(String)``
     - "ablation_results"
     - Prefix for CSV output files
   * - ``validateConfigurations(boolean)``
     - true
     - Enable configuration validation

Factory Methods
~~~~~~~~~~~~~~~

Pre-configured factory methods for common scenarios:

.. code-block:: java

   // ZDT problems with optimized parameters
   AblationConfiguration zdtConfig = AblationConfiguration.forZDTProblems();
   
   // DTLZ problems with optimized parameters  
   AblationConfiguration dtlzConfig = AblationConfiguration.forDTLZProblems();

Problem Suite Factory
~~~~~~~~~~~~~~~~~~~~~

The ``ProblemSuiteFactory`` creates problem suites with reference fronts:

.. code-block:: java

   // Create ZDT problem suite
   List<ProblemWithReferenceFront<DoubleSolution>> zdtProblems = 
       ProblemSuiteFactory.createProblemSuite("ZDT");
   
   // Get problem names for a suite
   List<String> problemNames = ProblemSuiteFactory.getProblemNames("DTLZ");

**Supported Problem Suites:**

.. list-table:: Problem Suites
   :header-rows: 1
   :widths: 15 30 15 40

   * - Suite
     - Problems
     - Objectives
     - Reference Fronts
   * - ZDT
     - ZDT1, ZDT2, ZDT3, ZDT4, ZDT6
     - 2
     - ``resources/referenceFronts/ZDT*.csv``
   * - DTLZ
     - DTLZ1, DTLZ2, DTLZ3, DTLZ4, DTLZ7
     - 3
     - ``resources/referenceFronts/DTLZ*.3D.csv``
   * - WFG
     - WFG1-WFG9 (planned)
     - 3
     - Not yet implemented

Progress Reporting
~~~~~~~~~~~~~~~~~~

Custom progress reporters can be implemented:

.. code-block:: java

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

Command Line Interface
~~~~~~~~~~~~~~~~~~~~~~

All example classes support command-line arguments:

.. code-block:: bash

   # Basic usage
   java SimpleZDTAblationExample
   
   # Custom thread count and runs
   java SimpleZDTAblationExample --threads 4 --runs 25
   
   # Sequential execution without progress
   java SimpleZDTAblationExample --sequential --no-progress
   
   # Custom evaluation count
   java SimpleDTLZAblationExample --evaluations 50000
   
   # Show help
   java SimpleZDTAblationExample --help

**Available Options:**

- ``--threads <n>`` - Number of threads to use
- ``--sequential`` - Force sequential execution  
- ``--parallel`` - Use all available processors
- ``--no-progress`` - Disable progress reporting
- ``--runs <n>`` - Number of runs per configuration per problem
- ``--evaluations <n>`` - Maximum evaluations per run
- ``--help``, ``-h`` - Show help message

Feature Importance Analysis Tools
---------------------------------

FeatureImportanceAnalyzer API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The main analyzer class for feature importance analysis:

.. code-block:: java

   FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
       .setNumberOfTrees(200)    // More trees for better accuracy
       .setMaxDepth(15)          // Deeper trees for complex relationships  
       .setSeed(42);             // Reproducible results

**Configuration Methods:**

.. list-table:: FeatureImportanceAnalyzer Configuration
   :header-rows: 1
   :widths: 30 15 55

   * - Method
     - Default
     - Description
   * - ``setNumberOfTrees(int)``
     - 100
     - Number of trees in Random Forest
   * - ``setMaxDepth(int)``
     - 10
     - Maximum depth of trees
   * - ``setMinNodeSize(int)``
     - 5
     - Minimum samples per leaf node
   * - ``setSeed(int)``
     - 42
     - Random seed for reproducibility

Data Loading and Processing
~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: java

   // Load data with target indicator
   analyzer.loadData("NHV");  // Normalized Hypervolume
   
   // Alternative indicators
   analyzer.loadData("EP");   // Epsilon
   analyzer.loadData("IGD");  // Inverted Generational Distance

**Data Processing Pipeline:**

1. **Automatic Merging**: Joins CONFIGURATIONS.csv and INDICATORS.csv by Evaluation+SolutionId
2. **Missing Value Imputation**: Replaces NaN values with column medians  
3. **Feature Extraction**: Excludes ID columns, keeps parameter columns as features
4. **Target Selection**: Uses specified quality indicator as regression target

Model Training and Validation
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: java

   // Train Random Forest model
   analyzer.trainModel();
   
   // Check model quality
   double r2Score = analyzer.calculateR2();
   System.out.println("Model R²: " + r2Score);
   
   // R² interpretation:
   // > 0.8: Excellent model
   // > 0.6: Good model  
   // > 0.4: Acceptable model
   // < 0.4: Poor model (consider more data)

Importance Measures
~~~~~~~~~~~~~~~~~~~

.. code-block:: java

   // Get Gini importance (fast, built-in)
   Map<String, Double> giniImportance = analyzer.getGiniImportance();
   
   // Get Permutation importance (robust, slower)
   Map<String, Double> permImportance = analyzer.getPermutationImportance(20);
   
   // Both return sorted maps (most important first)

Output and Reporting
~~~~~~~~~~~~~~~~~~~~

.. code-block:: java

   // Generate formatted report
   String report = analyzer.generateReport();
   System.out.println(report);
   
   // Export to CSV
   String csvOutput = analyzer.toCSV();
   try (FileWriter writer = new FileWriter("importance.csv")) {
       writer.write(csvOutput);
   }

**Report Format:**

.. code-block:: none

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
   ...

Interaction Analysis
~~~~~~~~~~~~~~~~~~~~

The ``InteractionAnalyzer`` generates Partial Dependence Plot data:

.. code-block:: java

   // Create interaction analyzer
   InteractionAnalyzer interaction = new InteractionAnalyzer(analyzer);
   
   // Generate 2D interaction grid
   interaction.exportInteractionGrid(
       "crossoverProbability", 
       "mutationRate", 
       20,  // 20x20 grid
       Path.of("interaction.csv"));

**Output Format:**

.. code-block:: csv

   crossoverProbability,mutationRate,PredictedValue
   0.1,0.05,0.823
   0.1,0.10,0.834
   0.15,0.05,0.841
   ...

Advanced Usage Patterns
-----------------------

Integrated Analysis Workflow
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Combining both analysis methods for comprehensive insights:

.. code-block:: java

   public class IntegratedAnalysisWorkflow {
       public static void main(String[] args) throws Exception {
           Path resultsDir = Path.of("results/nsgaii/ZDT");
           
           // Phase 1: Feature Importance (Quick Screening)
           FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir);
           analyzer.loadData("NHV");
           analyzer.trainModel();
           
           // Identify top 5 parameters
           List<String> topParams = analyzer.getGiniImportance().keySet()
               .stream().limit(5).collect(Collectors.toList());
           
           System.out.println("Top parameters for ablation: " + topParams);
           
           // Phase 2: Targeted Ablation (Causal Analysis)
           AblationConfiguration config = AblationConfiguration.forZDTProblems()
               .numberOfRuns(25)  // More runs for important parameters
               .numberOfThreads(8);
           
           AblationRunner runner = new AblationRunner(config);
           runner.run();
           
           // Phase 3: Cross-validation on independent problems
           // ... additional validation logic
       }
   }

Custom Problem Suites
~~~~~~~~~~~~~~~~~~~~~

Adding new problem suites to the factory:

.. code-block:: java

   public static List<ProblemWithReferenceFront<DoubleSolution>> createCustomSuite() 
           throws IOException {
       List<ProblemWithReferenceFront<DoubleSolution>> problems = new ArrayList<>();
       
       problems.add(new ProblemWithReferenceFront<>(
           new MyCustomProblem1(), 
           VectorUtils.readVectors("resources/referenceFronts/MyProblem1.csv", ","), 
           "MyProblem1"));
       
       // Add more problems...
       return problems;
   }

Batch Analysis
~~~~~~~~~~~~~~

Processing multiple result directories:

.. code-block:: java

   public class BatchAnalysis {
       public static void main(String[] args) throws Exception {
           List<Path> resultDirs = List.of(
               Path.of("results/nsgaii/ZDT"),
               Path.of("results/nsgaii/DTLZ"),
               Path.of("results/moead/ZDT")
           );
           
           for (Path dir : resultDirs) {
               System.out.println("Analyzing: " + dir);
               
               // Feature importance analysis
               FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(dir);
               analyzer.loadData("NHV");
               analyzer.trainModel();
               
               // Export results
               String outputFile = dir.getFileName() + "_importance.csv";
               try (FileWriter writer = new FileWriter(outputFile)) {
                   writer.write(analyzer.toCSV());
               }
               
               System.out.println("Results exported to: " + outputFile);
           }
       }
   }

Performance Optimization
~~~~~~~~~~~~~~~~~~~~~~~~

For large datasets, consider these optimizations:

.. code-block:: java

   // For large datasets (>10K samples)
   FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir)
       .setNumberOfTrees(500)     // More trees for stability
       .setMaxDepth(20)           // Deeper trees for complex patterns
       .setMinNodeSize(2);        // Smaller nodes for fine-grained splits
   
   // Use fewer permutations for faster analysis
   Map<String, Double> permImportance = analyzer.getPermutationImportance(5);
   
   // Sample large interaction grids
   interaction.exportInteractionGrid(param1, param2, 10, outputPath); // 10x10 instead of 20x20

Error Handling and Debugging
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Robust error handling for production use:

.. code-block:: java

   try {
       FeatureImportanceAnalyzer analyzer = new FeatureImportanceAnalyzer(resultsDir);
       analyzer.loadData("NHV");
       
       // Validate data quality
       if (analyzer.getData().nrow() < 100) {
           System.err.println("Warning: Less than 100 samples, results may be unreliable");
       }
       
       analyzer.trainModel();
       
       // Check model quality
       double r2 = analyzer.calculateR2();
       if (r2 < 0.5) {
           System.err.println("Warning: Low R² score (" + r2 + "), consider more data");
       }
       
       // Generate results
       System.out.println(analyzer.generateReport());
       
   } catch (IOException e) {
       System.err.println("Error reading data files: " + e.getMessage());
   } catch (IllegalArgumentException e) {
       System.err.println("Invalid configuration: " + e.getMessage());
   } catch (Exception e) {
       System.err.println("Unexpected error: " + e.getMessage());
       e.printStackTrace();
   }

Robustness Analysis Tools
-------------------------

RobustnessAnalyzer API
~~~~~~~~~~~~~~~~~~~~~~

The ``RobustnessAnalyzer`` evaluates configuration stability through perturbation analysis:

.. code-block:: java

   RobustnessAnalyzer<DoubleSolution> analyzer = new RobustnessAnalyzer<>(
       baseAlgorithm,        // Algorithm factory
       problem,              // Problem instance  
       parameterSpace,       // Parameter space definition
       indicators,           // Quality indicators list
       referenceFront,       // Reference front for normalization
       maxEvaluations,       // Evaluations per run
       runsPerSample);       // Runs per configuration sample

**Constructor Parameters:**

.. list-table:: RobustnessAnalyzer Constructor
   :header-rows: 1
   :widths: 25 75

   * - Parameter
     - Description
   * - ``baseAlgorithm``
     - BaseLevelAlgorithm instance used as factory for creating algorithm instances
   * - ``problem``
     - Problem instance to solve during robustness evaluation
   * - ``parameterSpace``
     - ParameterSpace defining which parameters can be perturbed
   * - ``indicators``
     - List of QualityIndicator instances for performance measurement
   * - ``referenceFront``
     - Reference front matrix for indicator normalization
   * - ``maxEvaluations``
     - Maximum evaluations per algorithm run
   * - ``runsPerSample``
     - Number of independent runs per configuration sample

Perturbation Analysis
~~~~~~~~~~~~~~~~~~~~~

.. code-block:: java

   // Define center configuration (typically optimized)
   Map<String, String> centerConfig = parseConfiguration(optimizedConfigString);
   
   // Configure perturbation analysis
   int nSamples = 20;              // Number of perturbed samples
   double perturbationSigma = 0.05; // 5% standard deviation
   
   // Run analysis
   List<Map<String, Object>> results = analyzer.analyze(
       centerConfig, nSamples, perturbationSigma);

**Analysis Parameters:**

.. list-table:: Perturbation Analysis Parameters
   :header-rows: 1
   :widths: 20 15 65

   * - Parameter
     - Typical Range
     - Description
   * - ``centerConfig``
     - N/A
     - Base configuration to perturb (usually optimized)
   * - ``nSamples``
     - 10-50
     - Number of perturbed configurations to evaluate
   * - ``perturbationSigma``
     - 0.01-0.10
     - Standard deviation of Gaussian perturbation (as fraction)

**Perturbation Method:**

The analyzer applies multiplicative Gaussian noise to numerical parameters:

.. code-block:: none

   perturbed_value = original_value × (1 + N(0, σ²))

Where N(0, σ²) is Gaussian noise with mean 0 and standard deviation σ.

Results Export and Analysis
~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. code-block:: java

   // Export results to CSV for statistical analysis
   analyzer.exportToCSV(results, Path.of("robustness_results.csv"));
   
   // Results structure
   for (Map<String, Object> result : results) {
       int sampleId = (Integer) result.get("SampleId");
       String type = (String) result.get("Type");  // "Baseline" or "Perturbed"
       
       // Quality indicator values
       double epsilon = (Double) result.get("Epsilon");
       double hypervolume = (Double) result.get("NormalizedHypervolume");
   }

**CSV Output Format:**

.. code-block:: csv

   SampleId,Type,Epsilon,NormalizedHypervolume
   0,Baseline,0.0234,0.8923
   1,Perturbed,0.0241,0.8901
   2,Perturbed,0.0238,0.8915
   3,Perturbed,0.0245,0.8887
   ...

Statistical Analysis
~~~~~~~~~~~~~~~~~~~~

The exported data enables various statistical analyses:

**Stability Metrics**
  - Coefficient of variation (CV) = std_dev / mean
  - Performance degradation = (baseline - mean_perturbed) / baseline
  - Confidence intervals for performance under perturbation

**Robustness Classification**
  - **Robust Configuration**: Low CV (<5%), small performance degradation
  - **Moderately Robust**: Medium CV (5-15%), acceptable degradation  
  - **Fragile Configuration**: High CV (>15%), significant degradation

**Example Statistical Analysis:**

.. code-block:: java

   // Calculate robustness metrics from results
   public class RobustnessMetrics {
       public static void analyzeResults(List<Map<String, Object>> results) {
           // Separate baseline and perturbed results
           double baseline = getBaselinePerformance(results);
           List<Double> perturbedValues = getPerturbedPerformances(results);
           
           // Calculate statistics
           double mean = perturbedValues.stream().mapToDouble(d -> d).average().orElse(0);
           double stdDev = calculateStandardDeviation(perturbedValues, mean);
           double cv = stdDev / mean;
           double degradation = (baseline - mean) / baseline;
           
           // Classify robustness
           String robustnessClass = classifyRobustness(cv, degradation);
           
           System.out.printf("Baseline: %.4f%n", baseline);
           System.out.printf("Perturbed Mean: %.4f ± %.4f%n", mean, stdDev);
           System.out.printf("Coefficient of Variation: %.2f%%%n", cv * 100);
           System.out.printf("Performance Degradation: %.2f%%%n", degradation * 100);
           System.out.printf("Robustness Class: %s%n", robustnessClass);
       }
   }

Integration with Other Analysis Methods
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Robustness analysis complements ablation and feature importance analysis:

**After Feature Importance Analysis:**

.. code-block:: java

   // 1. Identify important parameters
   FeatureImportanceAnalyzer featureAnalyzer = new FeatureImportanceAnalyzer(resultsDir);
   featureAnalyzer.loadData("NHV");
   featureAnalyzer.trainModel();
   
   List<String> topParams = featureAnalyzer.getGiniImportance().keySet()
       .stream().limit(5).collect(Collectors.toList());
   
   // 2. Get optimized configuration focusing on important parameters
   String optimizedConfig = getOptimizedConfiguration(topParams);
   
   // 3. Evaluate robustness of optimized configuration
   RobustnessAnalyzer<DoubleSolution> robustnessAnalyzer = new RobustnessAnalyzer<>(...);
   List<Map<String, Object>> robustnessResults = robustnessAnalyzer.analyze(
       parseConfiguration(optimizedConfig), 20, 0.05);

**After Ablation Analysis:**

.. code-block:: java

   // 1. Run ablation analysis to get best configuration
   AblationConfiguration config = AblationConfiguration.forZDTProblems();
   AblationRunner runner = new AblationRunner(config);
   runner.run();
   
   // 2. Extract best configuration from ablation results
   String bestConfig = extractBestConfigurationFromAblation();
   
   // 3. Validate robustness of ablation-derived configuration
   RobustnessAnalyzer<DoubleSolution> analyzer = new RobustnessAnalyzer<>(...);
   List<Map<String, Object>> results = analyzer.analyze(
       parseConfiguration(bestConfig), 30, 0.03); // Smaller perturbation for validation

Best Practices for Robustness Analysis
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

**Perturbation Size Selection:**

.. list-table:: Perturbation Guidelines
   :header-rows: 1
   :widths: 20 20 60

   * - Sigma Value
     - Perturbation Level
     - Use Case
   * - 0.01-0.03
     - Very Small (1-3%)
     - Fine-grained sensitivity analysis
   * - 0.03-0.07
     - Small (3-7%)
     - Standard robustness evaluation
   * - 0.07-0.15
     - Medium (7-15%)
     - Stress testing configuration stability
   * - >0.15
     - Large (>15%)
     - Extreme robustness evaluation

**Sample Size Guidelines:**

- **Quick Assessment**: 10-15 samples for initial robustness check
- **Standard Analysis**: 20-30 samples for reliable statistics
- **Detailed Study**: 50+ samples for publication-quality analysis
- **Statistical Power**: Use power analysis to determine required sample size

**Multiple Runs per Sample:**

- **Fast Problems**: 5-10 runs per sample for noise reduction
- **Expensive Problems**: 3-5 runs per sample (balance cost vs. reliability)
- **Stochastic Algorithms**: 10+ runs per sample to account for algorithmic variance

This comprehensive API reference provides all the technical details needed to effectively use Evolver's analysis tools in research and production environments.
