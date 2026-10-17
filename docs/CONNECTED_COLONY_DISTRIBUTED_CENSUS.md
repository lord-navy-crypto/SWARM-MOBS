# Connected nest: distributed census, first runtime integration

## What is implemented
Every living supported monster that has a persisted `SwarmColonyNest`
coordinate now reports its UUID to a **dimension-scoped, transient** census
on its ordinary entity tick, when optional physical monster core expansion
is enabled. Even NoAI test entities may be counted as actual residents.
The owner is taken from existing entity NBT, never inferred from proximity.

Records use `root position -> UUID -> last observed tick`, so repeated
reports cannot multiply populations. Observations from unloaded/despawned
or dead entities naturally expire 400 ticks after their last report.
Foreign dimensions are not counted. No chunk loads, scanning of a gigantic
connected block region, or permanent entity references are introduced.

The active root's 200-tick cycle now computes:
`colonyPopulation = max(existing 14-block census, recent unique
owner-tagged resident count)` while the opt-in feature is enabled.
This is used for construction's full-capacity eligibility, birth capacity
gates and the root's stored population telemetry.

The incremental connected Nest Core traversal no longer restarts in the
middle of processing a massive component just because 2400 ticks elapsed.
The saved mean population no longer clamps at 64.

## Critical limits
This is **loaded/ticking members only**, not a census of unloaded chunks.
The max(local count, owner-tagged count) avoids double counting already
registered residents, but may undercount local yet-unassigned members
alongside a much larger distant tagged group for one cycle. Those members
are assigned homes by the existing enrollment flow and report next tick.
Colony registry data is transient, so it must rebuild from tick reports
after server restart. This creates a conservative settling period.

The reproduction site remains near the root, and existing recruitment
science still samples local species composition. These aspects must be
extended to colony-wide behavior before calling it fully distributed
reproduction or military coordination.

`nestConnectedCoreGrowthEnabled=false` remains the default.
No additional blocks or automatic loading are introduced by this census.

## Validation
JUnit: UUID de-duplication, independent roots, expiry, clock handling,
10,000 resident simulation.
Runtime GameTest: two owner-tagged mobs at different nearby locations,
plus a foreign-root mob, counted precisely once and separately.
All tests must be confirmed in CI. Keep PR Draft until validated.
