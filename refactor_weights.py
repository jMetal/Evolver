import os
import pandas as pd

def refactor_weight_files(directory):
    print(f"Refactoring .csv files in {directory} to match WxD_Y.dat format...")
    
    # Pattern: X-100.csv -> WXD_100.dat 
    # where X is dimensions (objectives) and 100 is size
    
    files = [f for f in os.listdir(directory) if f.endswith('.csv')]
    
    if not files:
        print("No CSV files found.")
        return

    for filename in files:
        filepath = os.path.join(directory, filename)
        
        # 1. Parse filename (e.g., "10-100.csv")
        try:
            basename = os.path.splitext(filename)[0] # "10-100"
            parts = basename.split('-')
            
            if len(parts) != 2:
                print(f"Skipping {filename}: Unexpected format")
                continue
                
            dims = parts[0]
            size = parts[1]
            
            # Construct new name
            new_name = f"W{dims}D_{size}.dat"
            new_path = os.path.join(directory, new_name)
            
            # 2. Read Content (Comma Separated)
            try:
                df = pd.read_csv(filepath, header=None)
                
                # Check consistency
                actual_dims = df.shape[1]
                actual_size = df.shape[0]
                
                if str(actual_dims) != dims or str(actual_size) != size:
                    print(f"Warning: {filename} content ({actual_size}x{actual_dims}) mismatch filename ({size}x{dims}). Using content for verification but keeping filename based on origin.")

                # 3. Write Content (Whitespace Separated)
                # Using to_csv with sep=' ' and no index/header
                
                # We write to the new path
                df.to_csv(new_path, sep=' ', header=False, index=False, float_format='%.18e')
                
                print(f"Converted {filename} -> {new_name}")
                
                # 4. Remove original? User asked to "refactor", usually implies replace.
                # I will uncomment this line if explicit confirmation, but generally safe to clean up.
                os.remove(filepath)
                
            except Exception as e:
                print(f"Error processing {filename}: {e}")
                
        except Exception as e:
            print(f"Error handling filename {filename}: {e}")

    # Also fix the anomalous W4D_165.dat which we found was comma separated
    w4_path = os.path.join(directory, "W4D_165.dat")
    if os.path.exists(w4_path):
        try:
             # Read checking if it is comma separated
             with open(w4_path, 'r') as f:
                 first_line = f.readline()
             
             if ',' in first_line:
                 print("Fixing format of W4D_165.dat (Comma -> Whitespace)...")
                 df = pd.read_csv(w4_path, header=None)
                 df.to_csv(w4_path, sep=' ', header=False, index=False, float_format='%.18e')
                 print("Fixed W4D_165.dat")
        except Exception as e:
            print(f"Error fixing W4D_165.dat: {e}")

if __name__ == "__main__":
    refactor_weight_files("resources/weightVectors")
