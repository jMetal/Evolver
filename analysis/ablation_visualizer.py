import pandas as pd
import matplotlib.pyplot as plt
import argparse
import os

def generate_path_plot(df, output_file, metric_label="Metric"):
    plt.figure(figsize=(10, 6))
    plt.plot(df['Step'], df['Metric'], marker='o', linestyle='-')
    plt.xticks(rotation=45, ha='right')
    plt.title('Ablation Path Analysis')
    plt.xlabel('Step')
    plt.ylabel(metric_label)
    # Use logarithmic scale if metric varies widely?
    # plt.yscale('log') 
    plt.grid(True)
    plt.tight_layout()
    plt.savefig(output_file)
    print(f"Plot saved to {output_file}")

def generate_loo_plot(df, output_file, metric_label="Metric"):
    optimized_rows = df[df['Step'] == 'Optimized']
    if not optimized_rows.empty:
        optimized_score = optimized_rows['Metric'].values[0]
    else:
        # Fallback if no specific Optimized row, maybe first row?
        optimized_score = df['Metric'].iloc[0]

    plt.figure(figsize=(10, 6))
    plt.bar(df['Step'], df['Metric'], color='skyblue', label=metric_label)
    plt.axhline(y=optimized_score, color='r', linestyle='--', label='Optimized Baseline')
    plt.xticks(rotation=45, ha='right')
    plt.title('Leave-One-Out Ablation Analysis')
    plt.xlabel('Parameter Removed')
    plt.ylabel(metric_label)
    plt.legend()
    plt.grid(axis='y')
    plt.tight_layout()
    plt.savefig(output_file)
    print(f"Plot saved to {output_file}")

def generate_latex_table(df, caption, label):
    # Select columns
    latex_df = df[['Step', 'ModifiedParameter', 'Metric']]
    table = latex_df.to_latex(index=False, caption=caption, label=label, float_format="%.4e")
    return table

def main():
    parser = argparse.ArgumentParser(description='Generate plots and tables from ablation CSV results.')
    parser.add_argument('csv_file', help='Path to the input CSV file')
    parser.add_argument('--type', choices=['path', 'loo'], required=True, help='Type of ablation analysis (path or loo)')
    parser.add_argument('--output_dir', default='.', help='Directory to save outputs')
    parser.add_argument('--metric_label', default='Metric', help='Label for the metric axis (e.g. "Hypervolume")')
    
    args = parser.parse_args()
    
    if not os.path.exists(args.csv_file):
        print(f"Error: File {args.csv_file} not found.")
        return

    df = pd.read_csv(args.csv_file)
    
    base_name = os.path.splitext(os.path.basename(args.csv_file))[0]
    plot_file = os.path.join(args.output_dir, f"{base_name}.png")
    tex_file = os.path.join(args.output_dir, f"{base_name}.tex")
    
    if args.type == 'path':
        generate_path_plot(df, plot_file, args.metric_label)
        caption = "Ablation Path Analysis Results"
        label = "tab:ablation_path"
    else:
        generate_loo_plot(df, plot_file, args.metric_label)
        caption = "Leave-One-Out Ablation Analysis Results"
        label = "tab:ablation_loo"
        
    latex_table = generate_latex_table(df, caption, label)
    
    with open(tex_file, 'w') as f:
        f.write(latex_table)
    print(f"LaTeX table saved to {tex_file}")

if __name__ == "__main__":
    main()
