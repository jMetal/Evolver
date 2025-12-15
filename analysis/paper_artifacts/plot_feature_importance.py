import argparse
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

def plot_feature_importance(input_file: Path, output_dir: Path):
    """
    Generates a high-quality double bar chart comparing Gini and Permutation importance.
    """
    if not input_file.exists():
        print(f"Error: Could not find {input_file}")
        return

    # Load data
    df = pd.read_csv(input_file)
    
    # Sort by Permutation Importance (generally more reliable)
    df = df.sort_values("PermutationImportance", ascending=False).head(20) # Compare top 20
    
    # Melt for seaborn
    melted = df.melt(id_vars=["Parameter"], 
                     value_vars=["GiniImportance", "PermutationImportance"],
                     var_name="Metric", value_name="Importance")

    # Set style
    sns.set_theme(style="whitegrid", context="paper", font_scale=1.2)
    plt.figure(figsize=(10, 8))
    
    # Plot
    sns.barplot(data=melted, x="Importance", y="Parameter", hue="Metric", palette="viridis")
    
    plt.title("Feature Importance Analysis (Top 20 Parameters)")
    plt.xlabel("Importance Score")
    plt.ylabel("")
    plt.tight_layout()
    
    # Save
    output_path_pdf = output_dir / "feature_importance.pdf"
    output_path_png = output_dir / "feature_importance.png"
    plt.savefig(output_path_pdf, bbox_inches='tight')
    plt.savefig(output_path_png, dpi=300, bbox_inches='tight')
    print(f"Generated feature importance plot: {output_path_pdf}")

def main():
    parser = argparse.ArgumentParser(description="Generate feature importance plot")
    parser.add_argument("--input", type=str, required=True, help="Path to input CSV (e.g., importance.csv)")
    parser.add_argument("--output", type=str, default="output", help="Output directory")
    args = parser.parse_args()
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    plot_feature_importance(Path(args.input), output_dir)

if __name__ == "__main__":
    main()
