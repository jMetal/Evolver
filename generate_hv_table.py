import pandas as pd
import numpy as np
from scipy import stats

def generate_hv_table(file_path):
    df = pd.read_csv(file_path)
    
    problems = sorted(df['Problem'].unique())
    algorithms = ['NSGAII-Standard', 'NSGAII-RE3D', 'NSGAII-RE3D-Est']
    
    # We compare against this algorithm
    control_algo = 'NSGAII-RE3D'
    
    # Storage for summary
    summary = {algo: {'W': 0, 'L': 0, 'T': 0} for algo in algorithms if algo != control_algo}
    
    print(f"{'Problem':<10} | {'NSGAII-Standard (Med)':<25} | {'NSGAII-RE3D-Est (Med)':<25} | {'NSGAII-RE3D (Med)':<20}")
    print("-" * 90)
    
    comparators = ['NSGAII-Standard', 'NSGAII-RE3D-Est']
    
    for prob in problems:
        # Get data for HV
        data = df[(df['Problem'] == prob) & (df['IndicatorName'] == 'HV')]
        
        # Get samples for Control (RE3D)
        s_control = data[data['Algorithm'] == control_algo]['IndicatorValue'].values
        m_control = np.median(s_control) if len(s_control) > 0 else 0
        
        row_str = f"{prob:<10} | "
        
        # 1. Standard and Est columns
        for algo in comparators:
            s_algo = data[data['Algorithm'] == algo]['IndicatorValue'].values
            m_algo = np.median(s_algo) if len(s_algo) > 0 else 0
            
            # Stat Test (Wilcoxon) Comparison: Algo vs Control
            symbol = "=" 
            if len(s_algo) > 0 and len(s_control) > 0:
                try:
                    _, p = stats.ranksums(s_algo, s_control)
                    if p < 0.05:
                        if m_algo > m_control: # Algo is better
                            symbol = "+"
                            summary[algo]['W'] += 1
                        else: # Algo is worse
                            symbol = "-" 
                            summary[algo]['L'] += 1
                    else:
                        symbol = "="
                        summary[algo]['T'] += 1
                except:
                    symbol = "="
                    summary[algo]['T'] += 1
            else:
                 symbol = "?"

            row_text = f"{m_algo:.4e} ({symbol})"
            row_str += f"{row_text:<25} | "
            
        # 2. Control Algorithm Column (No comparison symbol needed, it is the baseline)
        row_str += f"{m_control:.4e}".ljust(20)
            
        print(row_str)

    print("-" * 90)
    
    # Summary Row
    sum_row = f"{'Summary':<10} | "
    for algo in comparators:
        s_text = f"W:{summary[algo]['W']} L:{summary[algo]['L']} T:{summary[algo]['T']}"
        sum_row += f"{s_text:<25} | "
    
    # Empty for control column
    sum_row += f"{'':<20}"
    print(sum_row)
    print("\nLegend: (+) Better than RE3D, (-) Worse than RE3D, (=) Statistical Tie with RE3D")

if __name__ == "__main__":
    generate_hv_table('/Users/ajnebro/Softw/jMetal/Evolver/QualityIndicatorSummary.csv')
