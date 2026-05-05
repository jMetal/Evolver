#!/usr/bin/env python3
"""Postprocessing for Analysis A: compute convergence times and summary metrics.

Generates:
- outputs/convergence_times.csv
- outputs/metrics_summary.csv
- outputs/hv_series.csv (optional)

Run from scripts/analysis_A_hv_evolution:
    python3 postprocess_analysis.py
"""
from pathlib import Path
import numpy as np
import pandas as pd
import warnings
warnings.filterwarnings('ignore')

try:
    # reuse parser and config from analysis script when possible
    from analysis_A_hv_evolution import parse_var_conf_file, PROBLEMS, BUDGETS, N_RUNS, N_CHECKPOINTS, DATASETS
except Exception:
    # fallback: minimal local parser (shouldn't be needed if running inside folder)
    def parse_var_conf_file(filepath: Path):
        if not filepath.exists():
            return []
        evaluations = []
        hv_values = []
        with open(filepath, 'r') as f:
            lines = f.readlines()
        current_block = None
        block_vals = []
        for line in lines:
            line = line.strip()
            if line.startswith('# Evaluation:'):
                if current_block is not None and block_vals:
                    evaluations.append(current_block)
                    hv_values.append(max(block_vals))
                current_block = int(line.split(':',1)[1].strip())
                block_vals = []
            elif 'HVMinus=' in line:
                try:
                    parts = line.split('HVMinus=')[1].split()[0]
                    hv_minus = float(parts)
                    hv = -hv_minus
                    block_vals.append(hv)
                except Exception:
                    continue
        if current_block is not None and block_vals:
            evaluations.append(current_block)
            hv_values.append(max(block_vals))
        # running max
        if hv_values:
            rm = []
            cur = hv_values[0]
            for v in hv_values:
                cur = max(cur, v)
                rm.append(cur)
            hv_values = rm
        return list(zip(evaluations, hv_values))

    PROBLEMS = ["RE3D", "RWA3D"]
    BUDGETS = [1000, 3000, 5000, 7000]
    N_RUNS = 30
    N_CHECKPOINTS = 30
    DATASETS = {
        "referenceFronts": {"path": Path(__file__).parent.parent.parent / 'experimentation' / 'training' / 'referenceFronts'},
        "extremePointsFronts": {"path": Path(__file__).parent.parent.parent / 'experimentation' / 'training' / 'extremePoints'}
    }

def forward_fill_interpolation(data, target_evals):
    if not data:
        return [0.0] * len(target_evals)
    data_dict = dict(data)
    result = []
    for te in target_evals:
        available = [e for e in data_dict.keys() if e <= te]
        if available:
            ce = max(available)
            result.append(data_dict[ce])
        else:
            result.append(0.0)
    return result

def load_all_series(output_csv=None):
    rows = []
    base = Path(__file__).parent
    for dataset_name, dataset_conf in DATASETS.items():
        dataset_path = dataset_conf['path']
        for problem in PROBLEMS:
            for budget in BUDGETS:
                problem_dir = dataset_path / f"{problem}.{dataset_name}.{budget}"
                if not problem_dir.exists():
                    # skip with warning
                    # print(f"Warning: missing {problem_dir}")
                    continue
                for run in range(1, N_RUNS+1):
                    var_path = problem_dir / f"run{run}" / "VAR_CONF.txt"
                    data = parse_var_conf_file(var_path)
                    if not data:
                        continue
                    max_eval = max(e for e,_ in data)
                    target_evals = np.linspace(100, max_eval, N_CHECKPOINTS, dtype=int)
                    interpolated = forward_fill_interpolation(data, target_evals)
                    for idx, (ev, hv) in enumerate(zip(target_evals, interpolated)):
                        rows.append({
                            'problem': problem,
                            'dataset': dataset_name,
                            'budget': budget,
                            'run': run,
                            'checkpoint_idx': idx,
                            'eval': int(ev),
                            'hv': float(hv)
                        })
    df = pd.DataFrame(rows)
    if output_csv:
        out = Path(output_csv)
        out.parent.mkdir(parents=True, exist_ok=True)
        df.to_csv(out, index=False)
    return df

def compute_convergence_times(df, threshold=0.95):
    records = []
    grouped = df.groupby(['problem','dataset','budget','run'])
    for (problem,dataset,budget,run), g in grouped:
        g_sorted = g.sort_values('eval')
        evals = g_sorted['eval'].to_numpy()
        hvs = g_sorted['hv'].to_numpy()
        if len(hvs) == 0:
            continue
        hv_min = hvs[0]
        hv_max = hvs[-1]
        hv_thr = hv_min + threshold * (hv_max - hv_min)
        conv_eval = int(evals[-1])
        for e,h in zip(evals, hvs):
            if h >= hv_thr:
                conv_eval = int(e)
                break
        records.append({'problem':problem,'dataset':dataset,'budget':budget,'run':run,'convergence_eval':conv_eval})
    dfc = pd.DataFrame(records)
    return dfc

def compute_metrics(df):
    import math
    metrics = []
    grouped = df.groupby(['problem','dataset','budget'])
    for (problem,dataset,budget), g in grouped:
        # compute per-run AUC and slope, then aggregate
        aucs = []
        slopes = []
        plateaus = []
        runs = g['run'].unique()
        for run in runs:
            gr = g[g['run']==run].sort_values('eval')
            evals = gr['eval'].to_numpy()
            hvs = gr['hv'].to_numpy()
            if len(hvs) < 2:
                continue
            if len(evals) >= 2:
                auc = float(np.sum((hvs[:-1] + hvs[1:]) * (evals[1:] - evals[:-1]) / 2.0))
            else:
                auc = float('nan')
            aucs.append(auc)
            # slope on first 10% of checkpoints (use numpy.polyfit to avoid sklearn)
            n0 = max(2, int(len(evals)*0.1))
            try:
                p = np.polyfit(evals[:n0], hvs[:n0], 1)
                slope = float(p[0])
            except Exception:
                slope = float('nan')
            slopes.append(slope)
            # plateau detection: small incremental gains
            diffs = np.diff(hvs)
            rel_thr = 0.01 * (hvs[-1] - hvs[0])
            plateau_idx = len(evals)-1
            # look for first run of 3 consecutive diffs below threshold
            consec = 0
            for i,d in enumerate(diffs):
                if d < rel_thr:
                    consec += 1
                    if consec >= 3:
                        plateau_idx = i - 1
                        break
                else:
                    consec = 0
            plateaus.append(int(evals[min(plateau_idx, len(evals)-1)]))
        if len(aucs)==0:
            continue
        metrics.append({
            'problem':problem,
            'dataset':dataset,
            'budget':budget,
            'AUC_median': float(np.median(aucs)),
            'AUC_mean': float(np.mean(aucs)),
            'AUC_std': float(np.std(aucs, ddof=1)) if len(aucs)>1 else 0.0,
            'slope_median': float(np.nanmedian(slopes)),
            'plateau_median': int(np.median(plateaus)) if plateaus else None,
            'n_runs': int(len(aucs))
        })
    return pd.DataFrame(metrics)

def save_outputs(hv_df, conv_df, metrics_df):
    out_dir = Path(__file__).parent / 'outputs'
    figs_dir = out_dir / 'figures'
    out_dir.mkdir(parents=True, exist_ok=True)
    figs_dir.mkdir(parents=True, exist_ok=True)
    hv_df.to_csv(out_dir / 'hv_series.csv', index=False)
    conv_df.to_csv(out_dir / 'convergence_times.csv', index=False)
    metrics_df.to_csv(out_dir / 'metrics_summary.csv', index=False)
    print(f"Wrote outputs to {out_dir}")

def main():
    print('Loading HV series...')
    hv_df = load_all_series()
    print('Computing convergence times...')
    conv_df = compute_convergence_times(hv_df)
    print('Computing aggregated metrics...')
    metrics_df = compute_metrics(hv_df)
    save_outputs(hv_df, conv_df, metrics_df)

if __name__ == '__main__':
    main()
