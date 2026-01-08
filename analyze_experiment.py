import pandas as pd
import numpy as np
from scipy import stats

def analyze_results(file_path):
    # Read the CSV file
    df = pd.read_csv(file_path)
    
    # Get list of unique problems and indicators
    problems = sorted(df['Problem'].unique())
    indicators = sorted(df['IndicatorName'].unique())
    algorithms = sorted(df['Algorithm'].unique())
    
    print(f"Analyzed {len(problems)} problems and {len(indicators)} indicators.")
    print(f"Algorithms: {', '.join(algorithms)}\n")

    # We want to compare algorithms for each problem and indicator
    # Specifically, we want to know if RE3D or RE3D-Est is better than Standard (rank 1)
    
    # Store wins/losses
    # Win = Algorithm has statistically significantly better mean (Wilcoxon p < 0.05)
    # Tie = No statistical difference
    # Loss = Algorithm is worse
    
    # Since we have >2 algorithms, a simple pairwise comparison against "Standard" is best
    # or a full rank table. Let's do a median table with Friedman rankings first.
    
    # We will focus on Hypervolume (HV) and IGD+ as usually most important
    primary_indicators = ['HV', 'IGD+']
    
    for ind in primary_indicators:
        if ind not in indicators: continue
        
        print(f"--- Analysis for {ind} ---")
        print(f"{'Problem':<10} | {'Standard (Med)':<15} | {'RE3D (Med)':<15} | {'RE3D-Est (Med)':<15} | {'Best':<15}")
        print("-" * 80)
        
        for prob in problems:
            data = df[(df['Problem'] == prob) & (df['IndicatorName'] == ind)]
            
            # Get samples
            s_std = data[data['Algorithm'] == 'NSGAII-Standard']['IndicatorValue'].values
            s_re3d = data[data['Algorithm'] == 'NSGAII-RE3D']['IndicatorValue'].values
            s_est = data[data['Algorithm'] == 'NSGAII-RE3D-Est']['IndicatorValue'].values
            
            if len(s_std) == 0: continue
            
            # Calculate Medians
            m_std = np.median(s_std)
            m_re3d = np.median(s_re3d)
            m_est = np.median(s_est)
            
            # Determine direction (HV maximize, others minimize)
            minimize = ind != 'HV'
            
            medians = {'NSGAII-Standard': m_std, 'NSGAII-RE3D': m_re3d, 'NSGAII-RE3D-Est': m_est}
            
            if minimize:
                best_algo = min(medians, key=medians.get)
                # Pairwise Wilcoxon against best
                # If Best is Standard, compare RE3D vs Standard
                # Basic idea: Compare All pairs or just vs Standard?
                # Let's compare RE3D vs Standard and RE3D vs RE3D-Est
            else:
                best_algo = max(medians, key=medians.get)

            # Pairwise Tests (Wilcoxon)
            # 1. RE3D vs Standard
            try:
                _, p_re3d_vs_std = stats.ranksums(s_re3d, s_std)
            except: p_re3d_vs_std = 1.0
            
            # 2. RE3D-Est vs Standard
            try:
                _, p_est_vs_std = stats.ranksums(s_est, s_std)
            except: p_est_vs_std = 1.0
            
            # 3. RE3D vs RE3D-Est
            try:
                _, p_re3d_vs_est = stats.ranksums(s_re3d, s_est)
            except: p_re3d_vs_est = 1.0

            # Marker for significance (p < 0.05)
            sig_re3d_std = "*" if p_re3d_vs_std < 0.05 else " "
            sig_est_std = "*" if p_est_vs_std < 0.05 else " "
            sig_re3d_est = "*" if p_re3d_vs_est < 0.05 else " "
            
            # Simple formatting
            fmt = "{:.4e}"
            print(f"{prob:<10} | {fmt.format(m_std):<15} | {fmt.format(m_re3d):<15}{sig_re3d_std} | {fmt.format(m_est):<15}{sig_est_std} | {best_algo:<15}")
            print(f"{' ':10} | p(RE3D vs Std): {p_re3d_vs_std:.1e} | p(Est vs Std): {p_est_vs_std:.1e} | p(RE3D vs Est): {p_re3d_vs_est:.1e}")

        print("\n")

    # Friedman Test Ranking Summary (Average Rank across all problems)
    print("--- Average Rankings (Friedman) ---")
    
    rank_data = []

    for ind in indicators:
        current_ranks = {algo: [] for algo in algorithms}
        
        for prob in problems:
            data = df[(df['Problem'] == prob) & (df['IndicatorName'] == ind)]
            minimize = ind != 'HV'
            
            # Get median per algo
            prob_medians = {}
            for algo in algorithms:
                vals = data[data['Algorithm'] == algo]['IndicatorValue'].values
                if len(vals) > 0:
                    prob_medians[algo] = np.median(vals)
                else:
                    prob_medians[algo] = np.inf if minimize else -np.inf
            
            # Rank
            sorted_algos = sorted(prob_medians.keys(), key=lambda x: prob_medians[x], reverse=not minimize)
            
            for rank, algo in enumerate(sorted_algos):
                current_ranks[algo].append(rank + 1) # 1-based rank
        
        print(f"\nIndicator: {ind}")
        for algo in algorithms:
            avg_rank = np.mean(current_ranks[algo])
            print(f"{algo:<20}: {avg_rank:.2f}")

if __name__ == "__main__":
    analyze_results('/Users/ajnebro/Softw/jMetal/Evolver/QualityIndicatorSummary.csv')
