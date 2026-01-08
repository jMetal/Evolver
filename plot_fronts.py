import plotly.graph_objects as go
from plotly.subplots import make_subplots
import pandas as pd
import os

def read_front(file_path, delimiter=','):
    try:
        if not os.path.exists(file_path):
             return None
        # Try comma first (CSV), then whitespace
        try:
            df = pd.read_csv(file_path, header=None)
            if df.shape[1] < 2:
                 df = pd.read_csv(file_path, header=None,  delim_whitespace=True)
            return df.values
        except:
             return None
    except Exception as e:
        print(f"Error reading {file_path}: {e}")
        return None

def generate_plots(base_dir, ref_dir):
    problems = ['RE31', 'RE32', 'RE33', 'RE34', 'RE35', 'RE36', 'RE37',
                'RWA1', 'RWA2', 'RWA3', 'RWA6', 'RWA7', 'RWA8']
    
    data_dir = os.path.join(base_dir, "RE3DVisualization", "data")
    
    html_parts = []
    
    print(f"Starting generation of combined report...")
    
    for i, prob in enumerate(problems):
        print(f"Processing {prob}...")
        
        f_std = read_front(os.path.join(data_dir, "NSGAII-Standard", prob, "FUN0.csv"))
        f_re3d = read_front(os.path.join(data_dir, "NSGAII-RE3D", prob, "FUN0.csv"))
        f_est = read_front(os.path.join(data_dir, "NSGAII-RE3D-Est", prob, "FUN0.csv"))
        f_ref = read_front(os.path.join(ref_dir, f"{prob}.csv"))
        
        fig = make_subplots(
            rows=1, cols=4,
            specs=[[{'type': 'scene'}, {'type': 'scene'}, {'type': 'scene'}, {'type': 'scene'}]],
            subplot_titles=("NSGAII-Standard", "NSGAII-RE3D", "NSGAII-RE3D-Est", "Reference Front"),
            horizontal_spacing=0.01
        )
        
        def add_trace(data, color, name, col):
            if data is None: return
            fig.add_trace(
                go.Scatter3d(
                    x=data[:,0], y=data[:,1], z=data[:,2],
                    mode='markers',
                    marker=dict(size=2, color=color),
                    name=name,
                    showlegend=False
                ),
                row=1, col=col
            )
            
        add_trace(f_std, 'red', 'Standard', 1)
        add_trace(f_re3d, 'blue', 'RE3D', 2)
        add_trace(f_est, 'green', 'RE3D-Est', 3)
        add_trace(f_ref, 'black', 'Reference', 4)
        
        fig.update_layout(
            title_text=f"Pareto Front Comparison - {prob}",
            height=500,
            width=1600,
            margin=dict(l=10, r=10, b=10, t=50)
        )
        
        # Include plotly.js only in the first figure to avoid duplication/overhead
        include_js = 'cdn' if i == 0 else False
        
        html_parts.append(fig.to_html(full_html=False, include_plotlyjs=include_js))
        html_parts.append("<hr>")

    full_html = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <title>RE3D & RWA3D Result Comparison</title>
        <style>
            body {{ font-family: Arial, sans-serif; margin: 20px; }}
            h1 {{ text-align: center; }}
        </style>
    </head>
    <body>
        <h1>Comparison of NSGA-II Variants on all Problems</h1>
        {''.join(html_parts)}
    </body>
    </html>
    """
    
    output_file = os.path.join(base_dir, "all_problems_comparison.html")
    with open(output_file, "w", encoding="utf-8") as f:
        f.write(full_html)
        
    print(f"Saved combined plot to {output_file}")

if __name__ == "__main__":
    base_dir = "results/visualization"
    ref_dir = "resources/referenceFronts"
    generate_plots(base_dir, ref_dir)
