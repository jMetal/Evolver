# ZCAT reference fronts for M ∈ {5, 7, 8, 10}

`ZCATn.{5D,7D,8D,10D}.csv` (n = 1..20, 1000 points each) were generated for the EMO2027 program
(Paper C's objective-scaling study and future many-objective work). The pre-existing
`ZCATn.{2D,3D,4D,6D}.csv` are unchanged.

## Method
1. **Optimal-solution generation** with the authors' original ZCAT C implementation
   (https://github.com/evo-mx/ZCAT): 100 000 random Pareto-optimal solutions per problem, at the
   target number of objectives, default difficulty (Level 1, no bias, no imbalance), evaluated to
   obtain their objective vectors. Fixed RNG seed = 1.
   - **Important**: the generator (`zcat_rnd_opt_sol`) places the distance variables using the
     complicated-PS optimum (`ZCAT_G`), so the C benchmark must be configured with
     `Complicated_PS = 1` for the generated solutions to satisfy g = 0 and land exactly on the
     front (objectives within the theoretical [0, i²] bound per objective i). The front in
     objective space is identical to the simple-PS front; the flag only affects the
     decision-space mapping.
2. **Distance-based subset selection** (greedy max-min / farthest-point, on per-objective-
   normalized points) down to 1000 well-spread points — matching the existing fronts' size.

## Verification
Consistency with jMetal's Java ZCAT was verified at 4D (where an existing reference front exists):
for all 20 problems, the IGD+ between a freshly generated front and jMetal's `ZCATn.4D.csv`
matched the self-consistency IGD+ between two independent generator seeds — i.e. the residual is
pure discretization noise, confirming the C generator and jMetal's Java implementation produce
statistically identical fronts. Per-objective maxima match the theoretical i² bound for all
generated dimensions.

## Caveat for M ≥ 8
At 8 and 10 objectives, 1000 points is a sparse discretization of a 7–9 dimensional manifold: the
self-consistency IGD+ noise floor is large (median ≈ 1.3 at 8D, ≈ 3.1 at 10D, vs ≈ 0.07 at 4D).
Indicators computed against these 1000-point references at high M should be interpreted with that
in mind; a denser front may be warranted if these dimensions are used for quantitative
comparison. (HV at these M also requires an approximate/Monte-Carlo implementation.)
