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
