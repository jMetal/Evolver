import re
import sys

def parse_var_conf(file_path):
    configurations = []
    current_eval = -1
    
    with open(file_path, 'r') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            
            # Check for evaluation marker
            # Format: # Evaluation: 100
            eval_match = re.match(r'^# Evaluation:\s*(\d+)', line)
            if eval_match:
                current_eval = int(eval_match.group(1))
                continue
            
            # Parse configuration line
            # Format: EP=... HVMinus=... | --algorithmResult ...
            # Regex to capture HVMinus and the rest
            # Example: EP=0.017... HVMinus=-0.85... | --algorithmResult ...
            
            # We assume the line starts with metrics part
            if '|' in line:
                metrics_part, config_part = line.split('|', 1)
                
                # Extract HVMinus
                hv_match = re.search(r'HVMinus=([-\d.E]+)', metrics_part)
                if hv_match:
                    hv_minus = float(hv_match.group(1))
                    configurations.append({
                        'eval': current_eval,
                        'hv_minus': hv_minus,
                        'config': config_part.strip(),
                        'full_metrics': metrics_part.strip()
                    })

    return configurations

def analyze_file(file_path, label):
    print(f"--- Analysis for {label} ---")
    data = parse_var_conf(file_path)
    
    if not data:
        print("No data found.")
        return

    # Find max evaluation
    max_eval = max(d['eval'] for d in data)
    print(f"Max Evaluation: {max_eval}")
    
    # Filter for max evaluation
    final_configs = [d for d in data if d['eval'] == max_eval]
    print(f"Number of configurations in final evaluation: {len(final_configs)}")
    
    # Find best HVMinus (minimum value)
    best_config = min(final_configs, key=lambda x: x['hv_minus'])
    
    print(f"Best HVMinus: {best_config['hv_minus']}")
    print(f"Metrics: {best_config['full_metrics']}")
    print(f"Configuration:\n{best_config['config']}")
    print("\n")

if __name__ == "__main__":
    analyze_file('/Users/ajnebro/Softw/jMetal/Evolver/results/nsgaii/RE3D/VAR_CONF.txt', 'RE3D')
    analyze_file('/Users/ajnebro/Softw/jMetal/Evolver/results/nsgaii/RE3D_estimated/VAR_CONF.txt', 'RE3D_estimated')
