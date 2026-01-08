import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os

def compute_ranks(df, indicator):
    # Determine direction
    # HV is maximize, others minimize
    maximize = indicator == 'HV'
    
    # Filter data
    data = df[df['IndicatorName'] == indicator]
    problems = data['Problem'].unique()
    algos = data['Algorithm'].unique()
    
    avg_ranks = {algo: 0 for algo in algos}
    
    # Calculate ranks per problem
    # Rank 1 is best
    
    # We use median per problem/algo
    medians = data.groupby(['Problem', 'Algorithm'])['IndicatorValue'].median().unstack()
    
    # Rank
    # If maximize (HV), rank descending (method='min' or 'average'), usually 'average' for ties
    # If value is high -> Rank 1.
    if maximize:
        ranks = medians.rank(axis=1, ascending=False, method='average')
    else:
        # Minimize (IGD+, EP), value low -> Rank 1
        ranks = medians.rank(axis=1, ascending=True, method='average')
        
    return ranks.mean()

def draw_cd_plot(avg_ranks, n_datasets, alpha=0.05, title="Critical Difference Diagram", filename="cd_plot.png"):
    # Calculate CD
    k = len(avg_ranks)
    # Critical values for Nemenyi test (infinite df approx)
    # k=2: 1.960, k=3: 2.343, k=4: 2.569, k=5: 2.728
    q_alpha = {2: 1.960, 3: 2.343, 4: 2.569, 5: 2.728, 6: 2.850}
    
    if k not in q_alpha:
        print(f"Warning: Critical value for k={k} not hardcoded, using approx 2.5")
        q = 2.5
    else:
        q = q_alpha[k]
        
    cd = q * np.sqrt((k * (k + 1)) / (6 * n_datasets))
    
    # Plotting
    # Inspired by Orange/stat approaches
    # We draw a line from 1 to k
    
    fig, ax = plt.subplots(figsize=(8, 4))
    
    # Set limits
    low_lim = 1
    high_lim = k
    
    ax.set_xlim(low_lim - 0.2, high_lim + 0.2)
    ax.set_ylim(0, 3) # Arbitrary height
    
    # Draw main axis
    y_axis = 1.0
    ax.hlines(y_axis, low_lim, high_lim, color='black', linewidth=1)
    
    # Draw ticks and labels
    for i in range(low_lim, high_lim + 1):
        ax.vlines(i, y_axis - 0.05, y_axis + 0.05, color='black', linewidth=1)
        ax.text(i, y_axis + 0.1, str(i), ha='center', va='bottom', fontsize=10)
        
    ax.text(low_lim, y_axis + 0.4, "Best Rank", ha='center', va='bottom', fontsize=9, fontweight='bold')
    ax.text(high_lim, y_axis + 0.4, "Worst Rank", ha='center', va='bottom', fontsize=9, fontweight='bold')

    # Sort algorithms by rank
    sorted_algos = sorted(avg_ranks.items(), key=lambda x: x[1])
    
    # Draw Algorithm positions
    # We will stagger labels if they are too close, but with 3 algos usually fine.
    # Top algos (Rank < Median Rank?) go Top? Or Left/Right markers?
    # Standard output: lines pointing to rank on axis.
    
    # Let's put markers on the line
    # And text labels.
    
    # Determine cliques (groups connected by bar)
    # A clique is a set of algos where max_rank - min_rank < CD
    # Simple pairwise approach:
    # Sort by rank. r_1, r_2, ...
    # Connect r_i to r_j if r_j - r_i < CD
    
    # Ideally find maximal cliques.
    # For K=3 it's simple.
    
    # Draw Algos
    # We alternate labels top/down to avoid overlap? Or just left/right arrows?
    # Standard format: Text Label -----| Point on Axis
    
    left_algos = sorted_algos[:len(sorted_algos)//2 + 1]
    right_algos = sorted_algos[len(sorted_algos)//2 + 1:]
    
    def draw_labels(algos, is_top):
        for i, (name, rank) in enumerate(algos):
            # x position
            # y position of text
            y_text = y_axis - 0.5 if not is_top else y_axis + 0.5 
            # But simpler: Just list them vertically on left and right and draw lines?
            # cd-diagram usually has labels on the side.
            pass

    # Simple implementation:
    # Draw markers
    props = dict(boxstyle='round', facecolor='white', alpha=0.8)
    
    # Stagger heights to prevent overlap
    y_offsets = [0.5, 1.0, 1.5, 0.5, 1.0] 
    
    for i, (name, rank) in enumerate(sorted_algos):
        ax.plot(rank, y_axis, 'ko', markersize=5)
        # Draw line to label
        # ax.text(rank, y_axis - 0.2 - (i*0.1), f"{name}\n({rank:.2f})", ha='center', va='top', bbox=props, fontsize=9)
        
        # Better Labels:
        # Draw a line from rank to a fixed y, then text
        y_pos = y_axis + 0.3 + (i % 3) * 0.4
        ax.plot([rank, rank], [y_axis, y_pos], 'k-', linewidth=0.5, alpha=0.5)
        ax.text(rank, y_pos, f"{name}\n{rank:.2f}", ha='center', va='bottom', fontsize=9, bbox=props)

    # Draw CD Bar visual reference
    # We place it above the axis to show the length "CD"
    
    # Position: Center-ish or Top Left
    cd_x_start = low_lim
    cd_x_end = low_lim + cd
    cd_y = y_axis + 1.2 # Above markers but below title ideally
    
    # Draw line
    ax.hlines(cd_y, cd_x_start, cd_x_end, color='red', linewidth=2)
    # Caps
    ax.vlines([cd_x_start, cd_x_end], cd_y - 0.05, cd_y + 0.05, color='red', linewidth=2)
    # Label
    ax.text((cd_x_start + cd_x_end)/2, cd_y + 0.1, f"CD = {cd:.2f}", ha='center', va='bottom', color='red', fontweight='bold')

    # Connect non-significant groups (Nemenyi cliques)
    bar_y = y_axis - 0.2
    
    lines_to_draw = []
    
    # Iterate through all subsequences of sorted algos
    n = len(sorted_algos)
    for i in range(n):
        for j in range(i+1, n):
            rank_i = sorted_algos[i][1]
            rank_j = sorted_algos[j][1]
            if rank_j - rank_i < cd:
                lines_to_draw.append((rank_i, rank_j))
    
    # Filter lines: if we have (0,2), we don't need (0,1) and (1,2) usually in these plots
    # The CD plot logic is: connect maximal cliques.
    # If (rank_j - rank_i < cd), they are in a clique.
    # We want to draw bars for MAXIMAL cliques.
    # i.e., if (0, 2) is valid, we draw 0-2. We don't draw 0-1 or 1-2.
    # Simple filtering: keep (start, end) if there is no (s2, e2) such that s2<=start and e2>=end and (s2!=start or e2!=end)
    
    final_lines = []
    for i, (s1, e1) in enumerate(lines_to_draw):
        is_subsumed = False
        for j, (s2, e2) in enumerate(lines_to_draw):
            if i == j: continue
            if s2 <= s1 and e2 >= e1:
                is_subsumed = True
                break
        if not is_subsumed:
            final_lines.append((s1, e1))
            
    for i, (start, end) in enumerate(final_lines):
        offset = i * 0.2
        ax.hlines(bar_y - offset, start, end, color='black', linewidth=3)

    # Remove all spines
    ax.axis('off')
    
    ax.set_title(title, y=1.05)
    
    plt.tight_layout()
    plt.savefig(filename, dpi=150)
    print(f"Saved {filename}")
    plt.close()

if __name__ == "__main__":
    csv_path = 'QualityIndicatorSummary.csv'
    base_dir = "results/visualization"
    if not os.path.exists(base_dir):
        os.makedirs(base_dir)

    df = pd.read_csv(csv_path)
    
    # Indicators
    indicators = ['HV', 'IGD+', 'EP']
    
    for ind in indicators:
        ranks = compute_ranks(df, ind)
        print(f"--- {ind} Average Ranks ---")
        print(ranks)
        
        # Determine number of datasets (problems)
        n_datasets = len(df['Problem'].unique())
        
        safe_name = ind.replace("+", "plus")
        f_name = os.path.join(base_dir, f"{safe_name}_cd_plot.png")
        
        draw_cd_plot(ranks.to_dict(), n_datasets, title=f"Critical Difference Diagram - {ind}", filename=f_name)
