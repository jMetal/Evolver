import pandas as pd
import numpy as np
import os

base_dir = '/Users/ajnebro/Softw/jMetal/Evolver/results/2026.03.05.results/RE3D.referenceFronts.7000'

best_hv_configs = []

for run_id in range(1, 31):
    run_dir = os.path.join(base_dir, f'run{run_id}')
    ind_path = os.path.join(run_dir, 'INDICATORS.csv')
    conf_path = os.path.join(run_dir, 'CONFIGURATIONS.csv')
    
    if os.path.exists(ind_path) and os.path.exists(conf_path):
        ind_df = pd.read_csv(ind_path)
        conf_df = pd.read_csv(conf_path)
        
        merged = pd.merge(ind_df, conf_df, on=['Evaluation', 'SolutionId'])
        final_pop = merged[merged['Evaluation'] == 3000]
        
        if len(final_pop) == 0:
            continue
            
        run_best_hv_row = final_pop.loc[final_pop['HVMinus'].idxmin()]
        best_hv_configs.append(run_best_hv_row)

df = pd.DataFrame(best_hv_configs)

# Variables to analyze
print("=== Resumen de 30 configuraciones con mejor HVMinus ===")
print("Estadísticas del valor HVMinus:")
print(f"Puntuación media HVMinus: {df['HVMinus'].mean():.6f}")
print(f"Desviación estándar: {df['HVMinus'].std():.6f}")
print(f"Mínimo: {df['HVMinus'].min():.6f}, Máximo: {df['HVMinus'].max():.6f}")
print()

categorical_params = [
    'crossover', 'mutation', 'selection', 
    'crossoverRepairStrategy', 'mutationRepairStrategy',
    'archiveType', 'createInitialSolutions', 'variation'
]

numeric_params = [
    'populationSizeWithArchive', 'offspringPopulationSize', 
    'crossoverProbability', 'mutationProbabilityFactor',
    'laplaceCrossoverScale', 'levyFlightMutationBeta', 'levyFlightMutationStepSize', 
    'powerLawMutationDelta', 'blxAlphaBetaCrossoverAlpha', 'blxAlphaBetaCrossoverBeta'
]

print("--- Distribución de Parámetros Categóricos ---")
for param in categorical_params:
    if param in df.columns:
        counts = df[param].value_counts().to_dict()
        print(f"{param}: {counts}")
        
print("\n--- Estadísticas de Parámetros Numéricos ---")
for param in numeric_params:
    if param in df.columns:
        valid_vals = df[param].dropna()
        if len(valid_vals) > 0:
            mean = valid_vals.mean()
            std = valid_vals.std()
            min_v = valid_vals.min()
            max_v = valid_vals.max()
            print(f"{param:30s} -> Media: {mean:.4f}, Std: {std:.4f}, Rango: [{min_v:.4f}, {max_v:.4f}] (N={len(valid_vals)})")
