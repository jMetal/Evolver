import pandas as pd
import numpy as np
import plotly.graph_objects as go
from scipy import stats
import os

def generate_table(df, indicator, output_file):
    print(f"Generating table for {indicator}...")
    
    # Filter data
    data_ind = df[df['IndicatorName'] == indicator]
    problems = sorted(data_ind['Problem'].unique())
    
    # Algorithms and Control
    control_algo = 'NSGAII-RE3D'
    comparators = ['NSGAII-Standard', 'NSGAII-RE3D-Est']
    all_algos = comparators + [control_algo]
    
    # Determine direction
    maximize = indicator == 'HV' or indicator == 'PISAHypervolume' # Assuming 'HV' in CSV might be 'PISAHypervolume'
    # Actually CSV usually has 'PISAHypervolume' or 'HV', let's check what user used before.
    # Previous script used 'HV'. The CSV view earlier showed 'PISAHypervolume'.
    # I will handle mapping or just pass the exact string from CSV.
    
    # Mapping for user friendly name vs CSV name
    indicator_map = {
        'HV': 'PISAHypervolume', # Common mapping, adjust if needed
        'IGD+': 'InvertedGenerationalDistancePlus',
        'EP': 'EP' # Usually just EP
    }
    
    # If the exact indicator name is not found, try to use the passed csv name
    csv_indicator = indicator
    if indicator not in data_ind['IndicatorName'].unique():
         # Try to find mapped name
         if indicator in indicator_map:
             csv_indicator = indicator_map[indicator]
             if csv_indicator not in data_ind['IndicatorName'].unique():
                 # Try inverted map
                 inv_map = {v: k for k, v in indicator_map.items()}
                 if indicator in inv_map and inv_map[indicator] in data_ind['IndicatorName'].unique():
                      csv_indicator = inv_map[indicator]
    
    # Double check if we found data
    data_ind = df[df['IndicatorName'] == csv_indicator]
    if data_ind.empty:
        # Fallback: maybe specific strings
        if indicator == 'HV': csv_indicator = 'PISAHypervolume'
        elif indicator == 'IGD+': csv_indicator = 'InvertedGenerationalDistancePlus'
        elif indicator == 'EP': csv_indicator = 'Epsilon' # Epsilon is common for EP
        data_ind = df[df['IndicatorName'] == csv_indicator]
        
    if data_ind.empty:
        print(f"No data found for {indicator} (tried {csv_indicator})")
        return

    # Check maximization direction again based on resolved name
    maximize = 'Hypervolume' in csv_indicator or 'HV' in csv_indicator

    summary = {algo: {'W': 0, 'L': 0, 'T': 0} for algo in comparators}
    
    # Prepare Table Data
    # Headers
    headers = ['Problem'] + [f"{algo}<br>(Med)" for algo in comparators] + [f"{control_algo}<br>(Med)"]
    
    # Rows
    rows = []
    
    for prob in problems:
        prob_data = data_ind[data_ind['Problem'] == prob]
        
        # Control stats
        s_control = prob_data[prob_data['Algorithm'] == control_algo]['IndicatorValue'].values
        m_control = np.median(s_control) if len(s_control) > 0 else np.nan
        
        row_vals = [prob]
        
        for algo in comparators:
            s_algo = prob_data[prob_data['Algorithm'] == algo]['IndicatorValue'].values
            m_algo = np.median(s_algo) if len(s_algo) > 0 else np.nan
            
            symbol = "="
            if len(s_algo) > 0 and len(s_control) > 0:
                try:
                    _, p = stats.ranksums(s_algo, s_control)
                    if p < 0.05:
                        if maximize:
                            if m_algo > m_control: 
                                symbol = "<b>(+)</b>"
                                summary[algo]['W'] += 1
                            else: 
                                symbol = "<b>(-)</b>"
                                summary[algo]['L'] += 1
                        else: # Minimize
                            if m_algo < m_control: 
                                symbol = "<b>(+)</b>"
                                summary[algo]['W'] += 1
                            else: 
                                symbol = "<b>(-)</b>"
                                summary[algo]['L'] += 1
                    else: # p >= 0.05
                         summary[algo]['T'] += 1
                except:
                    summary[algo]['T'] += 1
            else:
                 symbol = "?"
            
            # Format cell
            if np.isnan(m_algo):
                 row_vals.append("-")
            else:
                 row_vals.append(f"{m_algo:.4e} {symbol}")
        
        # Control column (no symbol)
        if np.isnan(m_control):
             row_vals.append("-")
        else:
             row_vals.append(f"{m_control:.4e}")
            
        rows.append(row_vals)
        
    # Summary Row
    sum_vals = ["<b>Summary</b>"]
    for algo in comparators:
        s = summary[algo]
        sum_vals.append(f"<b>W:{s['W']} L:{s['L']} T:{s['T']}</b>")
    sum_vals.append("") # Empty for Control
    rows.append(sum_vals)
    
    # Transpose for Plotly (Column-based)
    # rows is list of rows (prob, val1, val2, val3)
    # plotly needs [[prob1, prob2...], [val1_1, val1_2...], ...]
    cols = list(map(list, zip(*rows)))
    
    fig = go.Figure(data=[go.Table(
        header=dict(values=headers,
                    line_color='darkslategray',
                    fill_color='lightskyblue',
                    align='center',
                    font=dict(color='black', size=12, style="normal")),
        cells=dict(values=cols,
                   line_color='darkslategray',
                   fill_color='aliceblue',
                   align='left',
                   font=dict(color='black', size=11))
    )])
    
    title_text = f"Indicator Comparison: {indicator} {'(Maximize)' if maximize else '(Minimize)'}"
    
    fig.update_layout(
        title=title_text,
        width=1000,
        height=600,
    )
    
    fig.write_html(output_file)
    print(f"Saved {output_file}")


if __name__ == "__main__":
    csv_path = '/Users/ajnebro/Softw/jMetal/Evolver/QualityIndicatorSummary.csv'
    df = pd.read_csv(csv_path)
    
    # Check what indicators are actually in the file
    print("Available indicators in CSV:", df['IndicatorName'].unique())
    
    # Define indicators to process
    # Map desired name -> CSV name if known, or just pass desired name and let logic handle it
    # Based on previous output: 'EP', 'HV' (probably PISAHypervolume), 'IGD+', 'IGD'
    # The user asked for "HV", "IGD+", "EP".
    
    # Based on the output, the CSV contains: ['EP', 'HV', 'IGD', 'IGD+']
    targets = ['HV', 'IGD+', 'EP']
    
    for ind in targets:
        # Create a nice filename
        # Clean specific characters for filename
        safe_name = ind.replace("+", "plus")
        f_name = f"results/visualization/{safe_name}_table.html"
        generate_table(df, ind, f_name)
