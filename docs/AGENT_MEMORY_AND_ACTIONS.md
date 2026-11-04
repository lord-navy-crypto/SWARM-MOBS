# Persistent Agent Memory and Action Cues

## Memory layers

Swarm Mobs now distinguishes three memory scopes rather than merging them into one global store:

1. **Agent memory** — a compact task-experience vector stored in the supported entity's own persistent NBT. It survives chunk unload/reload and world restart with the entity.
2. **Colony memory** — existing Nest Core work boards, pheromone fields, scout opportunities, resource ledgers and Bee colony systems remain owned by their current colony/hive structures.
3. **Experiment memory** — existing `SwarmAiLearningMemory` remains the bounded 40-run SavedData ledger used for cross-experiment evaluation and Ollama-assisted analysis.

Agent memory stores only the seven bounded task-experience values (SEARCH, FLANK, BREACH, RANGED_SUPPORT, ENGINEERING, MATERIAL, RESERVE), a schema version, save tick and dimension label. It does not persist player identity, target coordinates, chat, inventory, or transient navigation state. Restoring memory resets the experience clock to the current game tick, so unloaded time does not create artificial decay.

Periodic NBT writes are deterministically spread over a 200-tick window. The per-agent state remains the runtime decision cache; persistent NBT is its compact long-term experience record.

## Action-linked visuals

The current Phase IV-H implementation uses vanilla entity swing synchronization and event-linked particles:

- **Zombie engineer/gatherer:** visible work swings and source-block particles only during real work.
- **Zombie carrier:** pickup and accepted-delivery cues only after the corresponding inventory transition succeeds.
- **Spider scout:** a signal pulse only after publishing at least one valid report.
- **Skeleton and Creeper:** their native animation systems remain authoritative.
- **All supported species:** a small task-specific cue appears when the deterministic assignment actually changes.

These cues do not change pathfinding, attack cadence, fuse timing, task allocation, or world mutation. They can be disabled with the server config option `actionVisualsEnabled`.

## GeckoLib animated colony signal

The Nest Core now has a small floating signal orb rendered by GeckoLib. The original block model is not replaced.

- **Idle:** a slow rotation and gentle bob make the colony core visibly active.
- **Scout report:** a pulse is triggered only after the Nest Core accepts a valid resource report.
- **Delivery:** a pulse is triggered only after a worker's physical haul is accepted and counted.
- **Rally:** a short signal is triggered when a new visible colony leader is appointed.

The server triggers these animations from completed state transitions. A short per-core cooldown bounds animation packets when several events arrive together. The animation state is transient presentation data: it is not added to agent memory, colony ledgers, or experiment history.


GeckoLib **4.9.3** is pinned for Minecraft 1.21.1 / NeoForge and declared as a required runtime dependency. This foundation commit adds the dependency and metadata; it does not claim that GeckoLib entity renderers or animation assets are already active.

The next renderer stage will translate real deterministic state transitions into synchronized, short-lived animation triggers:

- task assignment/change: role-specific transition cue;
- engineer work: inspect → work loop → completion;
- carrier: acquire → carry loop → accepted delivery;
- scout: observe → report pulse → return to patrol;
- squad coordination: flank setup, regroup, relief rotation and report acknowledgement.

The server-side planner remains the source of truth. GeckoLib only renders an event that the planner has already produced; it does not choose tasks, targets, paths, attacks, or world mutations. Ollama Shadow remains advisory, and bounded Active mode cannot trigger arbitrary per-tick animation or gameplay commands.

Vanilla renderer behavior should be preserved wherever possible. Any renderer replacement must first account for equipment, armor, held items, invisibility, name tags, native movement, and species-specific vanilla states, then pass client playtesting before it becomes the default.

## AI boundary

The deterministic swarm planner remains the gameplay authority. Ollama Shadow remains observational. Existing Active mode may only apply its already bounded high-level strategy multipliers; it does not receive entity-level memory writes or direct action control.

## Verification

Pure-Java unit tests cover experience-vector encode/decode, offline-time handling, schema rejection, value bounds and deterministic save staggering. The Minecraft NBT adapter is compiled in the mod runtime and still needs runtime integration verification. Runtime GameTests and a real-client inspection remain required before release.
