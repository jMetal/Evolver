import pandas as pd
import numpy as np
import os

base_dir = '/Users/ajnebro/Softw/jMetal/Evolver/results/2026.03.05.results'
re3d_dir = os.path.join(base_dir, 'RE3D.referenceFronts.7000')
rwa3d_dir = os.path.join(base_dir, 'RWA3D.referenceFronts.7000')

def get_best_configs(dataset_dir):
    best_configs = []
    for run_id in range(1, 31):
        run_dir = os.path.join(dataset_dir, f'run{run_id}')
        ind_path = os.path.join(run_dir, 'INDICATORS.csv')
        conf_path = os.path.join(run_dir, 'CONFIGURATIONS.csv')
        
        if os.path.exists(ind_path) and os.path.exists(conf_path):
            ind_df = pd.read_csv(ind_path)
            conf_df = pd.read_csv(conf_path)
            
            merged = pd.merge(ind_df, conf_df, on=['Evaluation', 'SolutionId'])
            final_pop = merged[merged['Evaluation'] == 3000]
            if len(final_pop) == 0: continue
                
            run_best_hv_row = final_pop.loc[final_pop['HVMinus'].idxmin()].copy()
            run_best_hv_row['RunID'] = run_id
            best_configs.append(run_best_hv_row)
    return pd.DataFrame(best_configs).reset_index(drop=True)

df_re3d = get_best_configs(re3d_dir)
df_rwa3d = get_best_configs(rwa3d_dir)

print(f"=== Comparativa Básica ===")
print(f"RE3D  -> Media HVMinus = {df_re3d['HVMinus'].mean():.6f} (Mejor: {df_re3d['HVMinus'].min():.6f})")
print(f"RWA3D -> Media HVMinus = {df_rwa3d['HVMinus'].mean():.6f} (Mejor: {df_rwa3d['HVMinus'].min():.6f})")

# Comparar solapamiento exacto de configuraciones categóricas principales
categorical_params = [
    'crossover', 'mutation', 'selection', 
    'crossoverRepairStrategy', 'mutationRepairStrategy',
    'archiveType'
]

# Crear strings (hashes) de las configs categóricas
df_re3d['cat_hash'] = df_re3d[categorical_params].astype(str).apply(lambda x: '|'.join(x), axis=1)
df_rwa3d['cat_hash'] = df_rwa3d[categorical_params].astype(str).apply(lambda x: '|'.join(x), axis=1)

re3d_hashes = set(df_re3d['cat_hash'].unique())
rwa3d_hashes = set(df_rwa3d['cat_hash'].unique())

common_hashes = re3d_hashes.intersection(rwa3d_hashes)

print("\n=== Solapamiento Categórico (Sub-espacio exacto) ===")
print(f"Configuraciones Categóricas Únicas en RE3D: {len(re3d_hashes)}")
print(f"Configuraciones Categóricas Únicas en RWA3D: {len(rwa3d_hashes)}")
print(f"Configuraciones Categóricas en Común (Exactas): {len(common_hashes)}")

for i, h in enumerate(common_hashes):
    print(f"\nConfiguración Común #{i+1}:")
    vals = h.split('|')
    for p, v in zip(categorical_params, vals):
        print(f"  {p}: {v}")
    
    # Cuántas veces aparece en cada uno
    c1 = (df_re3d['cat_hash'] == h).sum()
    c2 = (df_rwa3d['cat_hash'] == h).sum()
    print(f"  -> Usada {c1} veces en RE3D")
    print(f"  -> Usada {c2} veces en RWA3D")

