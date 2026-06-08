import pandas as pd
import numpy as np
import os

base_dir = '/Users/ajnebro/Softw/jMetal/Evolver/results/2026.03.05.results/RE3D.referenceFronts.7000'

best_ep_vals = []
best_hv_vals = []

best_overall_ep = float('inf')
best_overall_hv = float('inf')
best_ep_config = None
best_hv_config = None

for run_id in range(1, 31):
    run_dir = os.path.join(base_dir, f'run{run_id}')
    ind_path = os.path.join(run_dir, 'INDICATORS.csv')
    conf_path = os.path.join(run_dir, 'CONFIGURATIONS.csv')
    
    if os.path.exists(ind_path) and os.path.exists(conf_path):
        ind_df = pd.read_csv(ind_path)
        conf_df = pd.read_csv(conf_path)
        
        # Merge indicators and configurations
        merged = pd.merge(ind_df, conf_df, on=['Evaluation', 'SolutionId'])
        
        # Consider the final population (Evaluation == 3000)
        final_pop = merged[merged['Evaluation'] == 3000]
        
        if len(final_pop) == 0:
            continue
            
        # Get best values for this run
        min_ep = final_pop['EP'].min()
        min_hv = final_pop['HVMinus'].min()
        
        best_ep_vals.append(min_ep)
        best_hv_vals.append(min_hv)
        
        # Overall bests
        run_best_ep_row = final_pop.loc[final_pop['EP'].idxmin()]
        run_best_hv_row = final_pop.loc[final_pop['HVMinus'].idxmin()]
        
        if min_ep < best_overall_ep:
            best_overall_ep = min_ep
            best_ep_config = run_best_ep_row
            
        if min_hv < best_overall_hv:
            best_overall_hv = min_hv
            best_hv_config = run_best_hv_row

print("=== EP Statistics (over 30 runs) ===")
print(f"Median: {np.median(best_ep_vals)}")
print(f"Mean:   {np.mean(best_ep_vals)}")
print(f"Std:    {np.std(best_ep_vals)}")
print(f"Min:    {np.min(best_ep_vals)}")
print(f"Max:    {np.min(best_ep_vals)} (wait, {np.max(best_ep_vals)})")

print("\n=== HVMinus Statistics (over 30 runs) ===")
print(f"Median: {np.median(best_hv_vals)}")
print(f"Mean:   {np.mean(best_hv_vals)}")
print(f"Std:    {np.std(best_hv_vals)}")
print(f"Min:    {np.min(best_hv_vals)}")
print(f"Max:    {np.max(best_hv_vals)}")

print("\n=== Best Overall Config (by EP) ===")
print(best_ep_config.to_dict())

print("\n=== Best Overall Config (by HVMinus) ===")
print(best_hv_config.to_dict())
