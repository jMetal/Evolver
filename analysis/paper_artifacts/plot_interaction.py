import argparse
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from pathlib import Path

def plot_interaction(input_file: Path, output_dir: Path):
    """
    Generates a heatmap for 2D Partial Dependence Plot (Interaction).
    """
    if not input_file.exists():
        print(f"Error: Could not find {input_file}")
        return

    # Load data
    df = pd.read_csv(input_file)
    cols = df.columns
    param1, param2, target = cols[0], cols[1], cols[2]
    
    # Pivot to matrix form
    matrix = df.pivot(index=param2, columns=param1, values=target)
    
    # Sort index/cols to ensure correct axes
    matrix.sort_index(axis=0, inplace=True)
    matrix.sort_index(axis=1, inplace=True)

    sns.set_theme(style="white", context="paper", font_scale=1.2)
    plt.figure(figsize=(10, 8))
    
    # Plot heatmap
    sns.heatmap(matrix, cmap="viridis", annot=False, fmt=".3f", 
                cbar_kws={'label': f'Predicted {target}'})
    
    plt.title(f"Interaction: {param1} vs {param2}")
    plt.xlabel(param1)
    plt.ylabel(param2)
    
    # Invert Y axis to match standard Cartesian coordinates if needed
    plt.gca().invert_yaxis()
    
    plt.tight_layout()
    
    # Save
    base_name = input_file.stem
    output_path_pdf = output_dir / f"{base_name}.pdf"
    output_path_png = output_dir / f"{base_name}.png"
    plt.savefig(output_path_pdf, bbox_inches='tight')
    plt.savefig(output_path_png, dpi=300, bbox_inches='tight')
    print(f"Generated interaction plot: {output_path_pdf}")

def main():
    parser = argparse.ArgumentParser(description="Generate interaction heatmap")
    parser.add_argument("--input", type=str, required=True, help="Path to interaction CSV")
    parser.add_argument("--output", type=str, default="output", help="Output directory")
    args = parser.parse_args()
    
    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)
    
    plot_interaction(Path(args.input), output_dir)

if __name__ == "__main__":
    main()
