import argparse
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

def plot_convergence(results_dir: Path, output_dir: Path):
    """
    Generates a high-quality convergence plot for all indicators.
    """
    input_file = results_dir / "INDICATORS.csv"
    if not input_file.exists():
        print(f"Error: Could not find {input_file}")
        return

    # Load data
    df = pd.read_csv(input_file)
    indicators = [col for col in df.columns if col not in ["Evaluation", "SolutionId"]]
    
    # Set paper-quality style
    sns.set_theme(style="whitegrid", context="paper", font_scale=1.2)
    
    for ind in indicators:
        plt.figure(figsize=(8, 6))
        
        # Calculate statistics per evaluation
        # Aggregated mean and credible interval (95%)
        sns.lineplot(data=df, x="Evaluation", y=ind, errorbar=("ci", 95), linewidth=2)
        
        plt.title(f"Convergence of {ind}")
        plt.xlabel("Evaluations")
        plt.ylabel(ind)
        plt.tight_layout()
        
        # Save as PDF (vector) for papers and PNG for preview
        plt.savefig(output_dir / f"convergence_{ind}.pdf", bbox_inches='tight')
        plt.savefig(output_dir / f"convergence_{ind}.png", dpi=300, bbox_inches='tight')
        plt.close()
        print(f"Generated convergence plot for {ind}")

def main():
    parser = argparse.ArgumentParser(description="Generate paper-quality convergence plots")
    parser.add_argument("--input", type=str, required=True, help="Path to results directory")
    parser.add_argument("--output", type=str, default="output", help="Output directory")
    args = parser.parse_args()
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    plot_convergence(Path(args.input), output_dir)

if __name__ == "__main__":
    main()
