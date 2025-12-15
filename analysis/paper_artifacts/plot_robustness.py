import argparse
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

def plot_robustness(input_file: Path, output_dir: Path):
    """
    Generates a robustness plot (violin plot of perturbed samples vs baseline).
    """
    if not input_file.exists():
        print(f"Error: Could not find {input_file}")
        return

    # Load data
    df = pd.read_csv(input_file)
    indicators = [col for col in df.columns if col not in ["SampleId", "Type"]]
    
    sns.set_theme(style="whitegrid", context="paper", font_scale=1.2)
    
    for ind in indicators:
        plt.figure(figsize=(10, 6))
        
        # Filter data
        baseline = df[df["Type"] == "Baseline"][ind].mean()
        perturbed = df[df["Type"] == "Perturbed"]
        
        # Plot distribution of perturbed samples
        sns.violinplot(data=perturbed, y=ind, color="skyblue", alpha=0.5, inner="stick")
        sns.stripplot(data=perturbed, y=ind, color="blue", alpha=0.3, jitter=True, size=4)
        
        # Add baseline line
        plt.axhline(y=baseline, color='r', linestyle='--', linewidth=2, label=f'Baseline ({baseline:.4f})')
        
        plt.title(f"Local Robustness Analysis: {ind}")
        plt.ylabel(ind)
        plt.legend()
        plt.tight_layout()
        
        # Save
        output_path_pdf = output_dir / f"robustness_{ind}.pdf"
        output_path_png = output_dir / f"robustness_{ind}.png"
        plt.savefig(output_path_pdf, bbox_inches='tight')
        plt.savefig(output_path_png, dpi=300, bbox_inches='tight')
        print(f"Generated robustness plot: {output_path_pdf}")

def main():
    parser = argparse.ArgumentParser(description="Generate robustness plot")
    parser.add_argument("--input", type=str, required=True, help="Path to robustness CSV")
    parser.add_argument("--output", type=str, default="output", help="Output directory")
    args = parser.parse_args()
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    plot_robustness(Path(args.input), output_dir)

if __name__ == "__main__":
    main()
