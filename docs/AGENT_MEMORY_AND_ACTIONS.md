# Persistent Agent Memory and Action Cues

## Memory layers

Swarm Mobs now distinguishes three memory scopes rather than merging them into one global store:

1. **Agent memory** — a compact task-experience vector stored in the supported entity's own persistent NBT. It survives chunk unload/reload and world restart with the entity.
2. **Colony memory** — existing Nest Core work boards, pheromone fields, scout opportunities, resource ledgers and Bee colony systems remain owned by their current colony/hive structures.
3. **Experiment memory** — existing `SwarmAiLearningMemory` remains the bounded 40-run SavedData ledger used for cross-experiment evaluation and Ollama-assisted analysis.

Agent memory stores only the seven bounded task-experience values (SEARCH, FLANK, BREACH, RANGED_SUPPORT, ENGINEERING, MATERIAL, RESERVE), a schema version, save tick and dimension label. It does not persist player identity, target coordinates, chat, inventory, or transient navigation state. Restoring memory resets the experience clock to the current game tick, so unloaded time does not create artificial decay.

Periodic NBT writes are deterministically spread over a 200-tick window. The per-agent state remains the runtime decision cache; persistent NBT is its compact long-term experience record.

## Action-linked visuals

The supported mobs use vanilla Minecraft renderers, so this phase deliberately avoids replacing those renderers or adding a mandatory animation framework.

- **Zombie engineer:** repeated arm swings and source-block particles while breaking or placing a bridge block.
- **Zombie gatherer:** a visible work swing and block particles when harvesting a real resource block.
- **Zombie carrier:** a pickup gesture when the real dropped item transitions to the return trip; delivery particles appear at the Nest Core only after the item is physically accepted.
- **Spider scout:** a short electric-spark pulse only when it publishes at least one valid scout report.
- **Skeleton:** native bow draw/release pose remains authoritative.
- **Creeper:** native swell/fuse animation remains authoritative.
- **All supported species:** a small task-specific particle cue appears when the deterministic task assignment actually changes.

These are client-visible server events using vanilla entity swing synchronization and particles. They do not change pathfinding, attack cadence, fuse timing, task allocation, or world mutation.

The visuals can be disabled with the server config option `actionVisualsEnabled`.

## AI boundary

The deterministic swarm planner remains the gameplay authority. Ollama Shadow remains observational. Existing Active mode may only apply its already bounded high-level strategy multipliers; it does not receive entity-level memory writes or direct action control.

## Verification

Pure-Java unit tests cover experience-vector encode/decode, offline-time handling, schema rejection, value bounds and deterministic save staggering. The Minecraft NBT adapter is compiled in the mod runtime and still needs runtime integration verification. Runtime GameTests and a real-client inspection remain required before release.
