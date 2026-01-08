import pandas as pd
import matplotlib.pyplot as plt
import os

def plot_evolution(file_paths, labels, output_dir):
    # Create a figure with 2 rows (High/Low metrics) and 2 columns (Study 1, Study 2)
    # Actually, simpler: Rows = Indicators (HVMinus, EP), Columns = Studies (RE3D, RE3D_est)
    fig, axs = plt.subplots(2, 2, figsize=(14, 10))
    
    for i, (file_path, label) in enumerate(zip(file_paths, labels)):
        try:
            df = pd.read_csv(file_path)
            
            if 'Evaluation' not in df.columns or 'HVMinus' not in df.columns or 'EP' not in df.columns:
                print(f"Skipping {label}: Missing columns in {file_path}")
                continue
            
            # Group by Evaluation and get min
            best_hv = df.groupby('Evaluation')['HVMinus'].min().reset_index()
            best_ep = df.groupby('Evaluation')['EP'].min().reset_index()
            
            # Row 0: HVMinus
            ax_hv = axs[0, i]
            ax_hv.plot(best_hv['Evaluation'], best_hv['HVMinus'], marker='o', markersize=3, color='tab:blue')
            ax_hv.set_title(f'HVMinus Evolution - {label}')
            ax_hv.set_xlabel('Evaluation')
            ax_hv.set_ylabel('HVMinus')
            ax_hv.grid(True)
            
            # Row 1: EP
            ax_ep = axs[1, i]
            ax_ep.plot(best_ep['Evaluation'], best_ep['EP'], marker='o', markersize=3, color='tab:orange')
            ax_ep.set_title(f'EP Evolution - {label}')
            ax_ep.set_xlabel('Evaluation')
            ax_ep.set_ylabel('EP')
            ax_ep.grid(True)
            
        except Exception as e:
            print(f"Error processing {file_path}: {e}")

    plt.tight_layout()
    output_path = os.path.join(output_dir, 'indicators_evolution_separated.png')
    plt.savefig(output_path)
    print(f"Chart saved to {output_path}")

if __name__ == "__main__":
    base_dir = '/Users/ajnebro/Softw/jMetal/Evolver/results/nsgaii'
    files = [
        os.path.join(base_dir, 'RE3D/INDICATORS.csv'),
        os.path.join(base_dir, 'RE3D_estimated/INDICATORS.csv')
    ]
    labels = ['RE3D', 'RE3D_estimated']
    
    # Save in the base directory
    output_dir = '/Users/ajnebro/Softw/jMetal/Evolver'
    
    plot_evolution(files, labels, output_dir)
