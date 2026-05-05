#!/usr/bin/env python3
"""Generate summary figures and run statistical tests for Analysis A.

Saves outputs to `outputs/figures` and `outputs/stat_tests.csv`.
"""
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
from itertools import combinations
import logging

try:
    import scipy.stats as stats
except Exception:
    stats = None

BASE_DIR = Path(__file__).parent
OUT_DIR = BASE_DIR / 'outputs'
FIG_DIR = OUT_DIR / 'figures'
OUT_DIR.mkdir(parents=True, exist_ok=True)
FIG_DIR.mkdir(parents=True, exist_ok=True)

sns.set(style='whitegrid')

def load_csvs():
    hv_csv = OUT_DIR / 'hv_series.csv'
    conv_csv = OUT_DIR / 'convergence_times.csv'
    metrics_csv = OUT_DIR / 'metrics_summary.csv'
    df_hv = pd.read_csv(hv_csv) if hv_csv.exists() else pd.DataFrame()
    df_conv = pd.read_csv(conv_csv) if conv_csv.exists() else pd.DataFrame()
    df_metrics = pd.read_csv(metrics_csv) if metrics_csv.exists() else pd.DataFrame()
    return df_hv, df_conv, df_metrics

def plot_convergence_boxplots(df_conv):
    figs = {}
    for prob in sorted(df_conv['problem'].unique()):
        sub = df_conv[df_conv['problem']==prob]
        plt.figure(figsize=(6,4))
        sns.boxplot(data=sub, x='budget', y='convergence_eval')
        plt.title(f'Convergence times — {prob}')
        plt.ylabel('Evaluations to 95%')
        p = FIG_DIR / f'convergence_box_{prob}.png'
        plt.savefig(p, bbox_inches='tight', dpi=200)
        plt.close()
        figs[prob] = p
    return figs

def plot_auc_bars(df_metrics):
    figs = {}
    for prob in sorted(df_metrics['problem'].unique()):
        sub = df_metrics[df_metrics['problem']==prob]
        plt.figure(figsize=(6,4))
        sns.barplot(data=sub, x='budget', y='AUC_median')
        plt.title(f'AUC median — {prob}')
        p = FIG_DIR / f'auc_bar_{prob}.png'
        plt.savefig(p, bbox_inches='tight', dpi=200)
        plt.close()
        figs[prob] = p
    return figs

def pairwise_wilcoxon(df_conv):
    rows = []
    for prob in sorted(df_conv['problem'].unique()):
        sub = df_conv[df_conv['problem']==prob]
        budgets = sorted(sub['budget'].unique())
        for a,b in combinations(budgets,2):
            sa = sub[sub['budget']==a]['convergence_eval'].dropna()
            sb = sub[sub['budget']==b]['convergence_eval'].dropna()
            if len(sa)>=5 and len(sb)>=5 and stats is not None:
                try:
                    stat, p = stats.wilcoxon(sa.values, sb.values)
                except Exception:
                    stat, p = float('nan'), float('nan')
            else:
                stat, p = float('nan'), float('nan')
            rows.append({'problem':prob,'budget_a':a,'budget_b':b,'stat':stat,'p_value':p,'n_a':len(sa),'n_b':len(sb)})
    return pd.DataFrame(rows)

def main():
    df_hv, df_conv, df_metrics = load_csvs()
    if df_conv.empty and df_metrics.empty:
        print('No data found in outputs/. Run postprocess first.')
        return
    print('Generating figures...')
    figs1 = plot_convergence_boxplots(df_conv) if not df_conv.empty else {}
    figs2 = plot_auc_bars(df_metrics) if not df_metrics.empty else {}
    print('Figures saved to', FIG_DIR)
    if stats is None:
        print('scipy not available — skipping statistical tests; install scipy to run them.')
    else:
        print('Running statistical tests...')
        df_stats = pairwise_wilcoxon(df_conv)
        out_stats = OUT_DIR / 'stat_tests.csv'
        df_stats.to_csv(out_stats, index=False)
        print('Saved stat tests to', out_stats)

if __name__ == '__main__':
    main()
