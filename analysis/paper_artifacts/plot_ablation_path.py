import argparse
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

def plot_ablation_path(input_file: Path, output_dir: Path):
    """
    Generates a step chart showing performance evolution along the ablation path.
    """
    if not input_file.exists():
        print(f"Error: Could not find {input_file}")
        return

    # Load data
    df = pd.read_csv(input_file)
    
    # Identify indicators
    indicators = [col for col in df.columns if col.startswith("Performance_")]
    
    sns.set_theme(style="whitegrid", context="paper", font_scale=1.2)
    
    for ind_col in indicators:
        ind_name = ind_col.replace("Performance_", "")
        
        plt.figure(figsize=(12, 6))
        
        # Plot line
        sns.lineplot(data=df, x="Step", y=ind_col, marker="o", linewidth=2.5, markersize=8)
        
        # Add labels
        for idx, row in df.iterrows():
            if idx == 0: continue # Skip baseline label if it's too crowded
            
            # Label with parameter name
            param = row["Parameter"]
            val_change = f"{row['FromValue']} -> {row['ToValue']}"
            
            # Simple alternating annotation to avoid overlap (even/odd)
            y_offset = 15 if idx % 2 == 0 else -25
            va = 'bottom' if y_offset > 0 else 'top'
            
            plt.annotate(f"{idx}. {param}", 
                         (row["Step"], row[ind_col]),
                         xytext=(0, y_offset), textcoords='offset points',
                         ha='center', va=va, fontsize=9,
                         bbox=dict(boxstyle='round', facecolor='white', alpha=0.8),
                         arrowprops=dict(arrowstyle='-', color='gray'))

        plt.title(f"Ablation Path: Performance Improvement ({ind_name})")
        plt.xlabel("Step (Forward Scan)")
        plt.ylabel(ind_name)
        plt.xticks(df["Step"])
        plt.tight_layout()
        
        # Save
        output_path_pdf = output_dir / f"ablation_path_{ind_name}.pdf"
        output_path_png = output_dir / f"ablation_path_{ind_name}.png"
        plt.savefig(output_path_pdf, bbox_inches='tight')
        plt.savefig(output_path_png, dpi=300, bbox_inches='tight')
        print(f"Generated ablation path plot: {output_path_pdf}")

def main():
    parser = argparse.ArgumentParser(description="Generate ablation path plot")
    parser.add_argument("--input", type=str, required=True, help="Path to ablation path CSV")
    parser.add_argument("--output", type=str, default="output", help="Output directory")
    args = parser.parse_args()
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    plot_ablation_path(Path(args.input), output_dir)

if __name__ == "__main__":
    main()
