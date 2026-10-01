# Termination of the meta-optimizers by computing time

**Status:** proposal (2026-10-01), nothing implemented. Open for review before any code.

## Motivation

Every meta-optimizer stops after a fixed number of meta-evaluations (`TerminationByEvaluations` in
`MetaAsyncNSGAIIBuilder`, `MetaSMPSOBuilder`, `MetaAsyncGeneticAlgorithmBuilder`; the same count in
`MetaNSGAIIBuilder`, `MetaSPEA2Builder`, `TreeNSGAII` and `RandomSearch`). A time limit is more
useful in practice, for three reasons:

- **The user's real budget is time.** The cost of a meta-evaluation depends on the base-level
  algorithm and on the configuration being evaluated (SMS-EMOA with hypervolume costs far more than
  NSGA-II), so the same number of meta-evaluations means very different waits.
- **Fair comparisons.** Evolver (flat or tree encoding) and irace do not spend the same time per
  evaluation; the equitable comparison is at equal wall-clock time.
- **Components with variable cost.** The hybrid mutation with a local LLM (branch
  `experiment/llm-mutation`) adds calls whose latency is large and variable. With an evaluation
  limit they would look free; with a time limit their cost is paid with fewer meta-evaluations,
  which is the only honest way to evaluate them. The hybrid experiment should therefore use this
  condition from the start.

## Proposal

A second termination condition: **maximum computing time**, in seconds. The two conditions are
**mutually exclusive**: a run is bounded either by meta-evaluations or by time, never by both, and a
configuration that gives both is rejected with an error. Evaluations stay the default, so existing
requests, examples and tutorials do not change.

- **Mechanism.** jMetal's `TerminationByComputingTime` (jmetal-component), which takes the limit in
  milliseconds; the builders convert the seconds. The builders get a setter such as
  `setMaxComputingTime(int seconds)` as the alternative to `maxEvaluations`; `TreeNSGAII` and
  `RandomSearch`, which have their own loops, check the clock at the end of each generation.
- **Limit in seconds.** An integer number of seconds everywhere (builders, requests, `METADATA.txt`);
  the metadata also shows it formatted (`1h 0m 0s`), as the wall-clock time does today.
- **Granularity: the current generation is always completed.** When the limit is reached the
  generation in progress is not interrupted: the run waits until its evaluations finish and the
  population is updated, and only then stops. An abrupt stop would leave evaluations lost or a
  population in an inconsistent state. So the real time exceeds the limit by up to one generation
  (the time to evaluate one population in parallel). With 16 cores and long evaluations this can be
  noticeable; the report (below) states the real elapsed time and the documentation states the
  overshoot.
- **Budget strategies** (`meta.strategy`, evaluations of the base-level algorithm) are not affected:
  only the stopping rule of the meta level changes.
- **CLI.** `cli.training` requests accept `maxComputingTime` (for example in seconds or as an ISO
  duration, to decide) as an alternative or in addition to the existing meta-evaluation limit.
  Evolver-Studio exposes it in the training page.
- **Convergence plots.** `scripts/plot_training_convergence.py` uses the meta-evaluation as the
  axis; add elapsed time as an alternative axis, so runs stopped by time can be compared. The
  indicator history needs a timestamp per generation.

## Reporting the stopping condition

`METADATA.txt` (written by `ConsolidatedOutputResults` and `TreeOutputResults`) already has the
configured `Max Evaluations` of the meta-optimizer and, appended at the end, a `--- Execution ---`
section with the wall-clock time. It must also say **why the run stopped and with what budget**,
for every run, including those stopped by evaluations:

- in `--- Meta-Optimizer ---`: the stopping condition and its limit, either `Max Evaluations: 2000`
  or `Max Computing Time: 3600 s (1h 0m 0s)`, and the line `Stopping condition: evaluations` or
  `Stopping condition: computing time`;
- in `--- Execution ---`: the meta-evaluations actually performed and the wall-clock time already
  written, which with a time limit is slightly above the limit (the generation in progress is
  finished).

This makes a result stopped by time reproducible in intent (same limit) and comparable in fact (the
evaluations it reached). Studio and the analysis scripts that read `METADATA.txt` need the new
fields to be optional, so that old result directories are still readable.

A side finding in the same file: the line `Evaluation Strategy:` shows the default `toString` of the
object (`org.uma.evolver.meta.strategy.FixedEvaluationsStrategy@5e25a92e`), which says nothing about
the budget; the strategies should give a readable description. It can be fixed with this work.

## Risks

- **Reproducibility.** A run stopped by time does not give the same result on another machine or
  under another load. Mitigation: record the evaluations reached and keep the evaluation limit as the
  default for the tutorials and for validations that must be exact.
- **Termination in the asynchronous builders.** Check that the clock is read where the evaluation
  count is today and that in-flight evaluations are not lost.

## Plan (atomic commits on `develop`)

1. Maximum computing time in the builders and in `TreeNSGAII`/`RandomSearch`, with the error when
   both conditions are given, and tests (short limit, the generation in progress completes,
   evaluations only, both given).
2. `METADATA.txt`: limits and stopping condition in the two writers, readable description of the
   evaluation strategies, tests; changelog.
3. `cli.training`: the request field, documentation in `docs/utilities/cli_tools.rst`, bundled
   example request.
4. Convergence plot with elapsed time as an axis.
5. Evolver-Studio: the training page field, and the reading of the new metadata fields.

## Open questions

- Whether a time-only run should require a minimum number of generations.
- Whether the validation studies of the papers move to time budgets, and with which values.
