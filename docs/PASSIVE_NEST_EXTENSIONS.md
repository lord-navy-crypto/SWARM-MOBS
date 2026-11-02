# Passive extension core migration and verification

The real `SwarmNestBlockEntity` now persists a `ConnectedExtension` bit.
Automatically placed monster Nest Core blocks are explicitly marked as passive:
they skip their own 200-tick economy/spawn cycle, preventing a replicated
Nest Core block from becoming an independent free spawner.
Pre-existing cores default to false and preserve their legacy operation.

`ColonyPhysicalGrowth.placeOne` now requires Manhattan distance exactly 1,
only loaded air in world bounds, and marks a freshly placed monster core passive.
If a proper Nest Core block entity cannot be obtained after placement, placement
is reverted.

**Still needed:** 
- Choose and persist authoritative colony root and adjacency membership.
- Attach counted capacity to the active controller without hard-coded cap.
- Guarantee that material is debited exactly once before the block appears.
- Coordinate physical block changes with player protection and saved state.
- Implement bee hive housing sharing using unmodified vanilla block entities.
- Add GameTests for placement/reload, root destruction, orphaned extensions,
  block removal and colony splits.
- Run `./gradlew test` and gameTestServer before merging.

No forced chunk loads; no background automation.
