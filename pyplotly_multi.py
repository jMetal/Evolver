#!/usr/bin/env python3
"""Pareto front plotter for comparing multiple CSV files using Plotly.

Each file is displayed with a different color and labeled in the legend.
"""

import sys
import os
import pandas as pd
import plotly.express as px
import plotly.graph_objects as go

if len(sys.argv) < 2:
    print("Usage: python pyplotly_multi.py <file1.csv> [file2.csv] [file3.csv] ...")
    sys.exit(1)

# List of CSV files to plot
csv_files = sys.argv[1:]

# Color palette for multiple files
colors = px.colors.qualitative.Plotly

# Read first file to determine number of objectives
first_data = pd.read_csv(csv_files[0], header=None)
n_obj = first_data.shape[1]

if n_obj == 2:
    # 2D Scatter Plot
    fig = go.Figure()
    for i, csv_file in enumerate(csv_files):
        data = pd.read_csv(csv_file, header=None)
        label = os.path.basename(csv_file)
        fig.add_trace(go.Scatter(
            x=data[0], 
            y=data[1], 
            mode='markers',
            name=label,
            marker=dict(color=colors[i % len(colors)])
        ))
    fig.update_layout(
        title='Pareto Fronts Comparison',
        xaxis_title='f1',
        yaxis_title='f2',
        legend_title='Files'
    )

elif n_obj == 3:
    # 3D Scatter Plot
    fig = go.Figure()
    for i, csv_file in enumerate(csv_files):
        data = pd.read_csv(csv_file, header=None)
        label = os.path.basename(csv_file)
        fig.add_trace(go.Scatter3d(
            x=data[0], 
            y=data[1], 
            z=data[2],
            mode='markers',
            name=label,
            marker=dict(color=colors[i % len(colors)], size=4)
        ))
    fig.update_layout(
        title='Pareto Fronts Comparison',
        scene=dict(
            xaxis_title='f1',
            yaxis_title='f2',
            zaxis_title='f3'
        ),
        legend_title='Files'
    )

else:
    # Parallel Coordinates Plot for many objectives
    # Combine all data with a 'source' column for coloring
    all_data = []
    for i, csv_file in enumerate(csv_files):
        data = pd.read_csv(csv_file, header=None)
        data.columns = [f'f{j+1}' for j in range(n_obj)]
        data['source'] = os.path.basename(csv_file)
        data['source_id'] = i
        all_data.append(data)
    
    combined_data = pd.concat(all_data, ignore_index=True)
    
    fig = px.parallel_coordinates(
        combined_data,
        dimensions=[f'f{j+1}' for j in range(n_obj)],
        color='source_id',
        color_continuous_scale=px.colors.qualitative.Plotly,
        title='Pareto Fronts Comparison'
    )
    
    # Update colorbar to show file names
    file_labels = [os.path.basename(f) for f in csv_files]
    fig.update_layout(
        coloraxis_colorbar=dict(
            title='Files',
            tickvals=list(range(len(csv_files))),
            ticktext=file_labels
        )
    )

fig.show()
