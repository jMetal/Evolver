import pandas as pd
import numpy as np
import os
import scipy.cluster.hierarchy as sch
import matplotlib.pyplot as plt

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
            
        run_best_hv_row = final_pop.loc[final_pop['HVMinus'].idxmin()].copy()
        run_best_hv_row['RunID'] = run_id
        best_hv_configs.append(run_best_hv_row)

df = pd.DataFrame(best_hv_configs).reset_index(drop=True)
run_ids = df['RunID'].values

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

# 2. Preprocesamiento 
df_cat = df[categorical_params].astype(str)
df_cat_encoded = pd.get_dummies(df_cat)

df_num = df[numeric_params].fillna(0)
df_num_scaled = (df_num - df_num.mean()) / df_num.std().replace(0, 1)

X = pd.concat([df_cat_encoded, df_num_scaled], axis=1)

# 3. Clustering usando Scipy (Jerárquico)
Z = sch.linkage(X, method='ward', metric='euclidean')

num_clusters = 4
clusters = sch.fcluster(Z, t=num_clusters, criterion='maxclust')
df['Cluster'] = clusters

# 4. Custom PCA para 2D usando Numpy puro (evitando problemas de binarios de scikit-learn)
X_mat = X.to_numpy(dtype=np.float64)
# Añadir ruido pequeñísimo para evitar problemas de covarianza en columnas constantes
X_mat = X_mat + np.random.normal(0, 1e-8, X_mat.shape)
X_centered = X_mat - np.mean(X_mat, axis=0)

cov_matrix = np.cov(X_centered, rowvar=False)
eigenvalues, eigenvectors = np.linalg.eigh(cov_matrix)

# Ordenar eigenvalues descendente
sorted_idx = np.argsort(eigenvalues)[::-1]
sorted_eigenvectors = eigenvectors[:, sorted_idx]

# Tomar los 2 primeros componentes (PCA)
eigenvector_subset = sorted_eigenvectors[:, 0:2]
X_pca = np.dot(X_centered, eigenvector_subset)

# 5. Dibujar
plt.figure(figsize=(10, 8))
colors = ['red', 'blue', 'green', 'orange', 'purple', 'cyan']

for i in range(len(X_pca)):
    c_idx = clusters[i] - 1
    plt.scatter(X_pca[i, 0], X_pca[i, 1], c=colors[c_idx % len(colors)], s=120, edgecolors='black', alpha=0.7)
    plt.annotate(f"R{int(run_ids[i])}", (X_pca[i, 0]+0.1, X_pca[i, 1]+0.1), fontsize=9)

# Crear leyenda fantasma
import matplotlib.patches as mpatches
legend_handles = []
for c in range(1, num_clusters+1):
    c_idx = c - 1
    cluster_df = df[df['Cluster'] == c]
    if len(cluster_df) == 0: continue
    
    cross_mode = cluster_df['crossover'].mode().iloc[0]
    mut_mode = cluster_df['mutation'].mode().iloc[0]
    
    label_name = f"C{c} (Crs:{int(float(cross_mode))}, Mut:{int(float(mut_mode))}) [N={len(cluster_df)}]"
    patch = mpatches.Patch(color=colors[c_idx % len(colors)], label=label_name)
    legend_handles.append(patch)

plt.legend(handles=legend_handles, title="Clústeres (Operadores moda)", loc="best")
plt.title(f'Clustering de Configuraciones Mapeadas con PCA\n(Las 30 corridas Meta-Optimizando HVMinus)')
plt.xlabel('Componente Principal 1')
plt.ylabel('Componente Principal 2')
plt.grid(True, linestyle='--', alpha=0.5)

plot_path = os.path.join(base_dir, 'clusters_pca_plot.png')
plt.savefig(plot_path, dpi=200, bbox_inches='tight')
print(f"Plot guardado en: {plot_path}")
