import pandas as pd
import numpy as np
import os
from sklearn.preprocessing import StandardScaler
from sklearn.cluster import AgglomerativeClustering
from sklearn.decomposition import PCA
import matplotlib.pyplot as plt

base_dir = '/Users/ajnebro/Softw/jMetal/Evolver/results/2026.03.05.results/RE3D.referenceFronts.7000'

best_hv_configs = []

# 1. Leer los datos
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

df = pd.DataFrame(best_hv_configs)

# Extraer IDs y guardar para más tarde
run_ids = df['RunID'].values

# Variables predictoras (ignorando Evaluation, SolutionId, EP, HVMinus, etc)
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

# 2. Preprocesamiento
# 2.1 One-Hot-Encoding de las categóricas
df_cat = df[categorical_params].astype(str)
df_cat_encoded = pd.get_dummies(df_cat)

# 2.2 Imputación y Escalado de las numéricas
# Para los parámetros condicionales (ej: levyFlightMutationBeta sólo existe si mutation=algo en particular), 
# los rellenamos con 0 para que su ausencia/presencia marque la diferencia sin dar error.
df_num = df[numeric_params].fillna(0)

# Escalamos usando StandardScaler (resta media y divide por std dev)
scaler = StandardScaler()
df_num_scaled = pd.DataFrame(scaler.fit_transform(df_num), columns=df_num.columns, index=df_num.index)

# 2.3 Juntar todo en una matriz de características (Features)
X = pd.concat([df_cat_encoded, df_num_scaled], axis=1)

# 3. Clustering
# Usamos Agglomerative Clustering (Jerárquico) con métrica de distancia Euclidiana sobre el espacio embedding
# Proponemos 3 clusters como base para probar
n_clusters = 3
clustering = AgglomerativeClustering(n_clusters=n_clusters, metric='euclidean', linkage='ward')
cluster_labels = clustering.fit_predict(X)

df['Cluster'] = cluster_labels

# 4. Reducción de Dimensionalidad para Visualización (PCA a 2D)
pca = PCA(n_components=2)
X_pca = pca.fit_transform(X)

plt.figure(figsize=(10, 8))
scatter = plt.scatter(X_pca[:, 0], X_pca[:, 1], c=cluster_labels, cmap='viridis', s=100)
plt.title(f'Clustering de Configuraciones (HVMinus) con {n_clusters} Clusters\n(PCA projection de OHE + Scaler)')
plt.xlabel(f'PCA Component 1 ({pca.explained_variance_ratio_[0]*100:.1f}%)')
plt.ylabel(f'PCA Component 2 ({pca.explained_variance_ratio_[1]*100:.1f}%)')

# Añadir etiqueta de ejecución a cada punto
for i, txt in enumerate(run_ids):
    plt.annotate(f'R{int(txt)}', (X_pca[i, 0]+0.1, X_pca[i, 1]+0.1), fontsize=9)

legend1 = plt.legend(*scatter.legend_elements(), loc="best", title="Clusters")
plt.gca().add_artist(legend1)
plt.savefig(os.path.join(base_dir, 'clusters_hv.png'))

print("=== Análisis de Clusters ===")
for c in range(n_clusters):
    cluster_df = df[df['Cluster'] == c]
    print(f"\n--- Cluster {c} ({len(cluster_df)} configuraciones) ---")
    print(f"HVMinus Mean: {cluster_df['HVMinus'].mean():.6f} (Std: {cluster_df['HVMinus'].std():.6f})")
    print(f"Crossover dominante:   {cluster_df['crossover'].mode().iloc[0]}")
    print(f"Mutación dominante:    {cluster_df['mutation'].mode().iloc[0]}")
    print(f"Selección dominante:   {cluster_df['selection'].mode().iloc[0]}")
