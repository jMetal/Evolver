import pandas as pd
import numpy as np
import os
import scipy.cluster.hierarchy as sch

base_dir = '/Users/ajnebro/Softw/jMetal/Evolver/results/2026.03.05.results/RWA3D.referenceFronts.7000'

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
        run_best_hv_row['RunID'] = run_id
        best_hv_configs.append(run_best_hv_row)

df = pd.DataFrame(best_hv_configs).reset_index(drop=True)

# Variables
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

# 2. Preprocesamiento (A mano para evitar problemas de sklearn)
# One hot encoding
df_cat = df[categorical_params].astype(str)
df_cat_encoded = pd.get_dummies(df_cat)

# Numéricas con Fillna
df_num = df[numeric_params].fillna(0)

# Escalado Manual (Mean y Std)
df_num_scaled = (df_num - df_num.mean()) / df_num.std().replace(0, 1)

# Concatenamos la matriz final
X = pd.concat([df_cat_encoded, df_num_scaled], axis=1)

# 3. Clustering usando Scipy (Jerárquico)
Z = sch.linkage(X, method='ward', metric='euclidean')

# Cortamos el dendrograma para obtener 4 clusters (vamos a probar)
num_clusters = 4
clusters = sch.fcluster(Z, t=num_clusters, criterion='maxclust')

df['Cluster'] = clusters

print("=== Análisis de Clusters de Similitud - 30 mejores configs HVMinus ===")
for c in range(1, num_clusters+1):
    cluster_df = df[df['Cluster'] == c]
    if len(cluster_df) == 0: continue
    
    print(f"\n--- Cluster {c} ({len(cluster_df)} configuraciones) ---")
    print(f"HVMinus Mean: {cluster_df['HVMinus'].mean():.6f} (Std: {cluster_df['HVMinus'].std():.6f})")
    
    cross_mode = cluster_df['crossover'].mode().iloc[0] if not cluster_df['crossover'].empty else "N/A"
    mut_mode = cluster_df['mutation'].mode().iloc[0] if not cluster_df['mutation'].empty else "N/A"
    sel_mode = cluster_df['selection'].mode().iloc[0] if not cluster_df['selection'].empty else "N/A"
    cross_repair_mode = cluster_df['crossoverRepairStrategy'].mode().iloc[0] if not cluster_df['crossoverRepairStrategy'].empty else "N/A"
    
    print(f"  > Crossover Modal: {cross_mode}")
    print(f"  > Crossover Repair Modal: {cross_repair_mode}")
    print(f"  > Mutation Modal:  {mut_mode}")
    print(f"  > Selection Modal: {sel_mode}")
    
    # Check what makes this cluster unique
    cross_vals = cluster_df['crossover'].value_counts().to_dict()
    mut_vals = cluster_df['mutation'].value_counts().to_dict()
    print(f"  > Crs. Distribución: {cross_vals}")
    print(f"  > Mut. Distribución: {mut_vals}")
