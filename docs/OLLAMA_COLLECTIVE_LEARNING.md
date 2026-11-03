# Ollama-Assisted Collective Learning

## Scope

This layer evaluates saved experiment outcomes. It is an advisor, not a gameplay controller.

The evaluator receives only aggregate experiment records for:
- communication delivery and drop rates;
- navigation detours and recovery attempts/failures;
- search completion and reacquisition time;
- non-combat engineering task completion;
- run duration, preset, seed, and measured agent count.

Player-specific combat-style telemetry is not included in this evaluation request.

## Persistence

Experiment snapshots are stored in overworld SavedData under swarmmobs_ai_learning.
The ledger retains the newest 40 records and the most recent evaluation. It is world-local and survives restarts.

A snapshot can be recorded only after:
- an experiment has been started;
- at least one supported agent was measured;
- at least 200 experiment ticks have elapsed.

## Commands

~~~text
/swarmmobs experiment start
/swarmmobs experiment record
/swarmmobs ai memory
/swarmmobs ai history
/swarmmobs ai evaluate
~~~

ai evaluate requires the local Ollama provider to be enabled and at least two saved snapshots.
It submits the newest 16 records and a weighted aggregate summary.

## Output contract

The model returns a JSON-schema-constrained report:
- a short summary and confidence level;
- up to four metric findings;
- up to three follow-up experiment presets;
- up to three data-quality caveats.

The Java layer validates enum values, drops unsupported presets, limits text lengths, and persists the sanitized report.
The model cannot apply configuration changes through this interface.

## Resource and failure behavior

Only one learning evaluation may run at a time. It does not overlap an Ollama shadow request.
Inference is asynchronous and completion is marshalled back to the Minecraft server executor.
Timeouts, invalid output, and Ollama outages do not affect deterministic gameplay.

No embedding model or additional database is required. The first implementation uses bounded structured world data;
semantic/vector retrieval can be considered later if the history becomes large enough to justify it.


## Phase IV-E — Cross-Experiment Experience Transfer

The transfer layer derives auditable evidence from the existing persistent 40-run ledger. It does not create a second source of truth and does not modify runtime configuration.

### Controlled comparison pairs

Only one-factor preset pairs are compared:

- Sensing: BASELINE vs NOISY_SENSING; LOSSY_COMMS vs COMBINED_FAULTS.
- Communication: BASELINE vs LOSSY_COMMS; NOISY_SENSING vs COMBINED_FAULTS.
- Navigation: BASELINE vs NAVIGATION_STRESS.

BASELINE vs COMBINED_FAULTS is intentionally excluded because more than one experimental dimension changes.

### Matching and evidence quality

- Runs must use the same experiment seed and belong to the same world-scoped ledger.
- Population mismatch must be at most 25%; duration mismatch must be at most 20%.
- At most one best-matched pair is counted per seed, preventing repeated runs with one seed from masquerading as independent replications.
- Each metric has its own minimum event denominator before a pair contributes.
- Effects use the median of paired differences; the report retains control/treatment run IDs, paired-seed count, consistency count, and supporting sample count.
- MEDIUM confidence requires at least 3 paired seeds, at least 2/3 directional consistency, and 15 supporting events. HIGH requires at least 5 paired seeds, at least 80% consistency, and 40 supporting events. Everything else remains LOW / exploratory.

These are observational associations, not proof of causation. Transfer evidence is advisory and is supplied to Ollama as context; it cannot alter monster behavior or apply configuration changes.

### Access

- /swarmmobs ai transfer
- AI control-panel button: Review Cross-Experiment Transfer Evidence
- /swarmmobs ai evaluate now includes deterministic transfer evidence in its Ollama analysis context.
