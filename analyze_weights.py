import os
import pandas as pd
import numpy as np

def analyze_weight_file(filepath):
    try:
        # Determine delimiter based on extension or trial AND content
        # .csv -> comma, .dat -> whitespace
        ext = os.path.splitext(filepath)[1]
        
        if ext == '.csv':
            df = pd.read_csv(filepath, header=None)
        else:
            df = pd.read_csv(filepath, header=None, delim_whitespace=True)
            
        n_vectors, n_objectives = df.shape
        
        # Check sum
        row_sums = df.sum(axis=1)
        # Check if approx 1.0 (with tolerance)
        is_normalized = np.allclose(row_sums, 1.0, atol=1e-4)
        
        return n_vectors, n_objectives, is_normalized
    except Exception as e:
        print(f"Error reading {filepath}: {e}")
        return None, None, False

def main():
    directory = "resources/weightVectors"
    if not os.path.exists(directory):
        print(f"Directory {directory} not found.")
        return

    files = [f for f in os.listdir(directory) if f.endswith('.csv') or f.endswith('.dat')]
    files.sort()
    
    print(f"{'Filename':<20} | {'Vectors':<10} | {'Objectives':<10} | {'Normalized?':<10}")
    print("-" * 60)
    
    results = []
    
    for filename in files:
        path = os.path.join(directory, filename)
        n_vec, n_obj, norm = analyze_weight_file(path)
        
        if n_vec is not None:
             norm_str = "Yes" if norm else "No"
             print(f"{filename:<20} | {n_vec:<10} | {n_obj:<10} | {norm_str:<10}")
             results.append((filename, n_vec, n_obj))
             
    # Summary of findings
    print("\nTotal files analyzed:", len(results))

if __name__ == "__main__":
    main()
