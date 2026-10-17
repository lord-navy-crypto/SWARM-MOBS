# Connected colony controller safety contract (phase 2)

Existing source confirms `SwarmNestBlockEntity` runs a 200-tick lifecycle with
resources, production and spawning. An extension Nest Core must NOT run this
cycle or accept a second independent economy.

## Root decisions
- Exactly one original active Nest Core plus zero or more flagged passive
  extension Nest Core blocks constitutes an eligible controlled colony.
- Two separately created active cores that physically touch are a CONFLICT.
  Do not silently merge their inventories or retroactively convert either
  into an extension; prevent automatic growth across that boundary.
- No active root (after root destruction) is an orphan. Passive extensions
  remain inert until a deliberate, safe recovery/migration path is designed.
- Capacity is root's existing effective capacity plus
  `extensions * capacityPerExtension`, calculated with checked long arithmetic.
  There is no arbitrary extension count ceiling.
- The root-election policy requires an accurately resolved, face-connected
  snapshot; unloaded boundaries make ownership unresolved, not safe to merge.

## Why no automatic spawner activation yet
The old controller assumes local entity census radius 14 and resource capacity
128. Blindly switching to unlimited population would allow an enormous
remote colony to generate monsters without the correct census and ownership
rules. Growth must first address loaded-world census and duplicate membership,
combat interruption, scheduling and resource conservation.

## Status
`ColonyRootPolicy` and JUnit tests commit these constraints as pure Java.
Runtime wiring, GameTest, world save/restore and live testing remain required.
