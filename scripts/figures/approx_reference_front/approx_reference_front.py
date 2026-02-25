"""
Script to generate a figure illustrating EP and HV quality indicator
behavior with estimated reference fronts for problem RE22.

This script creates a publication-quality figure showing:
- The reference front of RE22
- The estimated reference front (with 30% offset)
- The reference point for HV computation
- Three synthetic fronts (A, B, C) with their EP and HV values

Run with: conda run -n Evolver python approx_reference_front.py
"""

import sys
import os
import numpy as np
import matplotlib.pyplot as plt
import matplotlib

# Add jMetalPy source to path
sys.path.insert(0, '/Users/ajnebro/Softw/jMetal/jMetalPy/src')
from jmetal.core.quality_indicator import AdditiveEpsilonIndicator, HyperVolume

# Use non-interactive backend for script execution
matplotlib.use('Agg')

# Configure matplotlib for publication quality
plt.rcParams.update({
    'font.size': 11,
    'font.family': 'serif',
    'axes.labelsize': 13,
    'axes.titlesize': 14,
    'legend.fontsize': 9.5,
    'xtick.labelsize': 10,
    'ytick.labelsize': 10,
    'figure.dpi': 150,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight',
})

# ============================================================================
# 1. Load data
# ============================================================================
project_root = os.path.join(os.path.dirname(__file__), '..', '..', '..')

rf = np.loadtxt(
    os.path.join(project_root, 'resources/referenceFronts/RE22.csv'),
    delimiter=','
)
erf = np.loadtxt(
    os.path.join(project_root, 'resources/estimatedReferenceFronts/RE22.csv'),
    delimiter=','
)

print(f"Reference front: {rf.shape[0]} points")
print(f"Estimated reference front: {erf.shape[0]} points")

# ============================================================================
# 2. Compute reference point (element-wise maximum of estimated front)
# ============================================================================
ref_point = np.max(erf, axis=0).tolist()
print(f"Reference point: ({ref_point[0]:.2f}, {ref_point[1]:.2f})")

# ============================================================================
# 3. Generate synthetic fronts
# ============================================================================

# Sample 20 evenly-spaced points from the sorted reference front
sorted_rf = rf[np.argsort(rf[:, 0])]
indices = np.linspace(0, len(sorted_rf) - 1, 20, dtype=int)
base_points = sorted_rf[indices]

# Front A: Dominated by the reference point, HV = 0
# Scale and translate so the centroid is at (500, 300) and all points
# are worse than the reference point in both objectives
centroid = base_points.mean(axis=0)
target_centroid = np.array([600.0, 350.0])
# Compute the max scale that keeps all points dominated by ref_point
s_obj1 = (target_centroid[0] - ref_point[0]) / (centroid[0] - base_points[:, 0].min())
s_obj2 = (target_centroid[1] - ref_point[1]) / (centroid[1] - base_points[:, 1].min())
scale_a = min(s_obj1, s_obj2) * 0.95  # 5% margin for safety
front_a = target_centroid + scale_a * (base_points - centroid)

# Front B: Partially dominated by the reference point, EP < 0
# Scale by 1.30 — some points extend beyond the reference point bounds
front_b = base_points * 1.30

# Front C: Dominates Front B, EP < 0, EP_C < EP_B
# Scale by 1.15 — closer to the original front than B
front_c = base_points * 1.15

# ============================================================================
# 4. Compute quality indicators
# ============================================================================
hv_indicator = HyperVolume(reference_point=ref_point)
ep_indicator = AdditiveEpsilonIndicator(reference_front=erf)

hv_a = hv_indicator.compute(front_a)
hv_b = hv_indicator.compute(front_b)
hv_c = hv_indicator.compute(front_c)

ep_a = ep_indicator.compute(front_a)
ep_b = ep_indicator.compute(front_b)
ep_c = ep_indicator.compute(front_c)

print(f"\nQuality Indicators:")
print(f"  Front A — HV: {hv_a:>10.2f}  EP: {ep_a:>8.2f}")
print(f"  Front B — HV: {hv_b:>10.2f}  EP: {ep_b:>8.2f}")
print(f"  Front C — HV: {hv_c:>10.2f}  EP: {ep_c:>8.2f}")

# Verify constraints
assert hv_a == 0.0, f"HV(A) should be 0, got {hv_a}"
assert ep_b < 0, f"EP(B) should be < 0, got {ep_b}"
assert ep_c < 0, f"EP(C) should be < 0, got {ep_c}"
assert ep_c < ep_b, f"EP(C) should be < EP(B), got EP_C={ep_c}, EP_B={ep_b}"
print("\n✓ All constraints satisfied!")

# ============================================================================
# 5. Create figure
# ============================================================================
fig, ax = plt.subplots(figsize=(10, 7))

# Sort reference front for line plotting
rf_sorted = rf[np.argsort(rf[:, 0])]

# Reference front (gray line)
ax.plot(
    rf_sorted[:, 0], rf_sorted[:, 1],
    color='#888888', linewidth=1.5, alpha=0.7,
    label='Reference Front', zorder=1
)

# Estimated reference front (diamond markers)
ax.scatter(
    erf[:, 0], erf[:, 1],
    marker='D', s=100, c='#2ca02c', edgecolors='black',
    linewidths=0.8, zorder=5,
    label='Estimated Reference Front'
)

# Reference point (star marker)
ax.scatter(
    ref_point[0], ref_point[1],
    marker='*', s=300, c='#d62728', edgecolors='black',
    linewidths=0.8, zorder=6,
    label=f'Reference Point ({ref_point[0]:.1f}, {ref_point[1]:.1f})'
)

# Front A (circles, red/orange)
ax.scatter(
    front_a[:, 0], front_a[:, 1],
    marker='o', s=50, c='#ff7f0e', edgecolors='#c45a00',
    linewidths=0.6, alpha=0.9, zorder=4,
    label='Front A'
)

# Front B (squares, blue)
ax.scatter(
    front_b[:, 0], front_b[:, 1],
    marker='s', s=50, c='#1f77b4', edgecolors='#0d4f8b',
    linewidths=0.6, alpha=0.9, zorder=4,
    label='Front B'
)

# Front C (triangles, purple)
ax.scatter(
    front_c[:, 0], front_c[:, 1],
    marker='^', s=55, c='#9467bd', edgecolors='#6a3d9a',
    linewidths=0.6, alpha=0.9, zorder=4,
    label='Front C'
)

# ============================================================================
# 6. Annotate quality indicator values
# ============================================================================
annotation_props = dict(
    fontsize=9.5,
    fontfamily='serif',
    bbox=dict(boxstyle='round,pad=0.3', facecolor='white',
              edgecolor='gray', alpha=0.85)
)

# Place annotations with arrows to avoid overlap
arrow_props = dict(arrowstyle='->', color='gray', lw=1.0)

# Front A annotation — place below the front
ax.annotate(
    f'Front A\nHV = {hv_a:.0f}\nε⁺ = {ep_a:.2f}',
    xy=(front_a[len(front_a)//2, 0], front_a[len(front_a)//2, 1]),
    xytext=(front_a[:, 0].mean() + 50, front_a[:, 1].min() - 50),
    arrowprops=arrow_props,
    **annotation_props, zorder=7
)

# Front B annotation — place at right side, middle height
ax.annotate(
    f'Front B\nHV = {hv_b:.2f}\nε⁺ = {ep_b:.2f}',
    xy=(front_b[len(front_b)*3//4, 0], front_b[len(front_b)*3//4, 1]),
    xytext=(420, 160),
    arrowprops=arrow_props,
    **annotation_props, zorder=7
)

# Front C annotation — place at lower-left area
ax.annotate(
    f'Front C\nHV = {hv_c:.2f}\nε⁺ = {ep_c:.2f}',
    xy=(front_c[len(front_c)*2//3, 0], front_c[len(front_c)*2//3, 1]),
    xytext=(350, 60),
    arrowprops=arrow_props,
    **annotation_props, zorder=7
)

# ============================================================================
# 7. Format the figure
# ============================================================================
ax.set_xlabel('$f_1$')
ax.set_ylabel('$f_2$')
ax.set_title('Quality Indicators with Estimated Reference Fronts (RE22)')

ax.legend(loc='upper left', framealpha=0.9, edgecolor='gray')
ax.grid(True, alpha=0.3, linestyle='--')

# Set axis limits with some padding
all_x = np.concatenate([rf[:, 0], erf[:, 0], [ref_point[0]],
                         front_a[:, 0], front_b[:, 0], front_c[:, 0]])
all_y = np.concatenate([rf[:, 1], erf[:, 1], [ref_point[1]],
                         front_a[:, 1], front_b[:, 1], front_c[:, 1]])
x_pad = (all_x.max() - all_x.min()) * 0.08
y_pad = (all_y.max() - all_y.min()) * 0.08
ax.set_xlim(all_x.min() - x_pad, all_x.max() + x_pad + 80)
ax.set_ylim(all_y.min() - y_pad, all_y.max() + y_pad + 20)

plt.tight_layout()

# Save output
output_path = os.path.join(os.path.dirname(__file__), 'approx_reference_front.pdf')
fig.savefig(output_path, format='pdf')
print(f"\nFigure saved to: {output_path}")

output_png = os.path.join(os.path.dirname(__file__), 'approx_reference_front.png')
fig.savefig(output_png, format='png')
print(f"Figure saved to: {output_png}")

plt.show()
