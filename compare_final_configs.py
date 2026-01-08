import re
import pandas as pd
from collections import defaultdict

def parse_configurations(file_path):
    configurations = []
    current_eval = -1
    
    with open(file_path, 'r') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            if line.startswith('# Evaluation:'):
                try:
                    current_eval = int(line.split(':')[1].strip())
                except ValueError:
                    pass
                continue
            
            if '|' in line and current_eval == 2000:
                metrics_part, config_part = line.split('|', 1)
                
                # Parse metrics
                metrics = {}
                for m in metrics_part.strip().split():
                    if '=' in m:
                        k, v = m.split('=')
                        try:
                            metrics[k] = float(v)
                        except:
                            metrics[k] = v
                
                # Parse config parameters
                params = {}
                # Split by ' --' to separate flags
                # Add a dummy prefix to handle the first flag if needed, but splitting by ' --' works well usually
                # The format is "--key value" or "--key" or "--key value1 value2"
                
                # A robust way to parse '--key value'
                parts = config_part.strip().split(' --')
                for part in parts:
                    if not part: continue
                    tokens = part.split()
                    key = tokens[0]
                    value = " ".join(tokens[1:]) if len(tokens) > 1 else "true"
                    params[key] = value
                
                configurations.append({
                    'metrics': metrics,
                    'params': params,
                    'raw_config': config_part.strip()
                })
                
    return configurations

def compare_studies(file_re3d, file_re3d_est):
    configs_re3d = parse_configurations(file_re3d)
    configs_est = parse_configurations(file_re3d_est)
    
    print(f"Found {len(configs_re3d)} configurations at Eval 2000 for RE3D")
    print(f"Found {len(configs_est)} configurations at Eval 2000 for RE3D_estimated")
    
    all_params = set()
    for c in configs_re3d + configs_est:
        all_params.update(c['params'].keys())
        
    print("\n--- Parameter Distribution Analysis ---")
    
    common_params = ['algorithmResult', 'archiveType', 'variation', 'crossoverRepairStrategy', 'mutationRepairStrategy']
    interesting_params = [p for p in all_params if p not in common_params and 'Result' not in p]
    
    # Analyze similarities
    # We look for logic: e.g., steady state (offspring=1) vs generational (offspring ~100)
    # Mutation types, Crossover types
    
    print(f"\nComparing {len(interesting_params)} key parameters...")
    
    # Store aggregated values for summary
    summary = defaultdict(lambda: {'RE3D': [], 'RE3D_est': []})
    
    for p in interesting_params:
        vals_re3d = [c['params'].get(p, 'N/A') for c in configs_re3d]
        vals_est = [c['params'].get(p, 'N/A') for c in configs_est]
        
        summary[p]['RE3D'] = vals_re3d
        summary[p]['RE3D_est'] = vals_est

    # Print a structured comparison table for discrete/categorical choices
    categorical_keys = ['crossover', 'mutation', 'selection', 'createInitialSolutions', 'archiveType']
    
    for key in categorical_keys:
        if key not in interesting_params: continue
        print(f"\nParameter: {key}")
        print(f"{'RE3D':<20} | {'RE3D_estimated':<20}")
        print("-" * 45)
        
        # Get unique counts
        re3d_counts = pd.Series(summary[key]['RE3D']).value_counts()
        est_counts = pd.Series(summary[key]['RE3D_est']).value_counts()
        
        # Combine indices
        all_idx = sorted(set(re3d_counts.index) | set(est_counts.index))
        for val in all_idx:
            c1 = re3d_counts.get(val, 0)
            c2 = est_counts.get(val, 0)
            print(f"{str(val) + f' ({c1})':<20} | {str(val) + f' ({c2})':<20}")

    # Numerical analysis for ranges
    numerical_keys = ['populationSizeWithArchive', 'offspringPopulationSize', 
                      'crossoverProbability', 'mutationProbabilityFactor', 
                      'blxAlphaCrossoverAlpha', 'levyFlightMutationBeta']
    
    print("\n--- Numerical Parameter Ranges (Min - Max) ---")
    print(f"{'Parameter':<30} | {'RE3D':<20} | {'RE3D_estimated':<20}")
    print("-" * 75)
    
    for key in numerical_keys:
        # Check if key exists in collected params
        # Note: some params like specific crossover alpha only exist if that crossover is chosen
        
        vals_re3d = []
        for c in configs_re3d:
            if key in c['params']:
                try: vals_re3d.append(float(c['params'][key]))
                except: pass
                
        vals_est = []
        for c in configs_est:
            if key in c['params']:
                try: vals_est.append(float(c['params'][key]))
                except: pass
        
        r1 = f"{min(vals_re3d):.2f}-{max(vals_re3d):.2f}" if vals_re3d else "N/A"
        r2 = f"{min(vals_est):.2f}-{max(vals_est):.2f}" if vals_est else "N/A"
        
        print(f"{key:<30} | {r1:<20} | {r2:<20}")

    # Check for similar configurations
    print("\n--- Searching for Similar Configurations ---")
    # Definition of similar: Same crossover type, same mutation type, offspring size within 10% or both steady state (<=5) or both generational (>=50)
    
    matches = []
    for i, c1 in enumerate(configs_re3d):
        for j, c2 in enumerate(configs_est):
            p1 = c1['params']
            p2 = c2['params']
            
            # Criteria 1: Main operators match
            if p1.get('crossover') != p2.get('crossover'): continue
            if p1.get('mutation') != p2.get('mutation'): continue
            
            # Criteria 2: Generation strategy
            off1 = float(p1.get('offspringPopulationSize', 0))
            off2 = float(p2.get('offspringPopulationSize', 0))
            
            is_steady1 = off1 <= 5
            is_steady2 = off2 <= 5
            is_gen1 = off1 > 20
            is_gen2 = off2 > 20
            
            similar_strategy = (is_steady1 and is_steady2) or (is_gen1 and is_gen2) or (abs(off1 - off2) < 10)
            
            if similar_strategy:
                matches.append((c1, c2))

    if matches:
        print(f"Found {len(matches)} pairs of configurations with similar structure (Operators + Strategy).")
        print("Example match:")
        m = matches[0]
        print("RE3D Config:")
        print(m[0]['raw_config'])
        print("RE3D_est Config:")
        print(m[1]['raw_config'])
        print(f"HVMinus Difference: {abs(m[0]['metrics']['HVMinus'] - m[1]['metrics']['HVMinus']):.4f}")
    else:
        print("No configurations found with matching Crossover, Mutation, and Generation Strategy.")

if __name__ == "__main__":
    base_dir = '/Users/ajnebro/Softw/jMetal/Evolver/results/nsgaii'
    file_re3d = f"{base_dir}/RE3D/VAR_CONF.txt"
    file_re3d_est = f"{base_dir}/RE3D_estimated/VAR_CONF.txt"
    
    compare_studies(file_re3d, file_re3d_est)
