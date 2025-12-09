# Meta-optimization Analysis Report

**Generated:** 2025-12-09 18:09:54.732497


## Dataset Summary

- Total configurations: 55
- Evaluation range: 100 - 2000
- Number of parameters: 31

## Objective Statistics

- Best Epsilon: 0.1101
- Best NormHypervolume: 0.2302
- Final Pareto front size: 3

## Top 10 Most Important Parameters (by correlation)

*Note: Conditional parameters are only correlated using samples where the parent parameter has the appropriate value.*

| Parameter | Epsilon Corr | NormHV Corr | Avg |Abs| Corr | n | Conditional |
|-----------|--------------|-------------|-----------------|---|-------------|
| crossoverProbability | -0.800 | -0.823 | 0.811 | 55 | No |
| sbxDistributionIndex | 0.751 | 0.724 | 0.737 | 55 | Yes |
| populationSizeWithArchive | 0.466 | 0.472 | 0.469 | 16 | Yes |
| archiveType | -0.435 | -0.435 | 0.435 | 16 | Yes |
| crossoverRepairStrategy | 0.410 | 0.365 | 0.388 | 55 | No |
| createInitialSolutions | -0.339 | -0.354 | 0.347 | 55 | No |
| selectionTournamentSize | 0.315 | 0.367 | 0.341 | 55 | Yes |
| algorithmResult | -0.335 | -0.279 | 0.307 | 55 | No |
| mutationProbabilityFactor | -0.233 | -0.254 | 0.243 | 55 | No |
| mutationRepairStrategy | -0.183 | -0.238 | 0.210 | 55 | No |

## Conditional Parameters Detail

| Parameter | Parent | Valid When | Samples |
|-----------|--------|------------|---------|
| sbxDistributionIndex | crossover | 0.0 | 55 |
| populationSizeWithArchive | algorithmResult | 1.0 | 16 |
| archiveType | algorithmResult | 1.0 | 16 |
| selectionTournamentSize | selection | 0.0 | 55 |
| polynomialMutationDistributionIndex | mutation | 0.0 | 55 |

## Generated Files

- `parameter_correlations.csv`: Full correlation analysis (with conditional info)
- `pca_loadings.csv`: PCA component loadings
- `correlation_heatmap.png`: Parameter-objective correlation matrix
- `pca_variance.png`: PCA explained variance plots
- `pca_scatter.png`: Configuration space in PC1-PC2
- `parameter_convergence.png`: Evolution of top parameters
- `objective_convergence.png`: Objective convergence over evaluations
- `categorical_analysis.png`: Impact of categorical parameters