# Same-block connected colonies — long-term implementation plan

## Locked-in simple prototype design

**Monster nests**: the existing registered Nest Core block is both anchor and
growth material. When the connected colony is full of members and its resource
inventory is full, attempt to place *the same registered Nest Core block* at
a vacant face-neighbor coordinate. The component is all six-face-connected
Nest Core blocks and capacity scales by the number of valid connected blocks.
Combat overrides every worker role; colony alerts spread through the existing
local communication system.

**Bees**: use ONLY vanilla Minecraft BEEHIVE / BEE_NEST blocks. Grow a
face-adjacent beehive block into a truly vacant permitted position when bee
occupancy and food/wax surplus thresholds are met. Connected beehive/nest
blocks form a single logical bee colony but keep separate vanilla block
entities for actual bee occupancy, honey levels, and native interactions.
The adapter must avoid manipulating vanilla occupancy internals.

## Pure model now committed
- ConnectedColonyLayout.scan: six-neighbor flood fill, bounded count,
  disconnected blocks excluded, capacity summed.
- ConnectedColonyLayout.nextExtension: deterministic valid-air candidate.
- ColonyGrowthPolicy and BeeHiveGrowthPolicy: independent eligibility.
- ColonyAlertPolicy: combat work preemption.
- Tests for connectivity, bounds, expansion and mode switching.

## Game runtime integration (NOT YET IMPLEMENTED)
1. Inspect existing Nest Core registration and ownership/state classes, then
   add a server-level adapter mapping its registered block to connected cells.
2. Keep scanning inside a single loaded chunk region or already-loaded adjacent
   chunks; NEVER force chunk loading. Strict radius/cell caps.
3. Persist connected component ID and ownership consistently; neighboring enemy
   colonies must not accidentally merge just because blocks touch. Use anchor
   identity and prevent ambiguous merges.
4. Reserve/remove resources only if placement validates immediately before
   the server's actual block placement; restore on failure. Do not overwrite
   non-air blocks. Honor world border, height, permissions, protection and cooldown.
5. Map logical colony occupancy to real mob membership; distinguish capacity
   per block from actual entity count; use an immutable tick snapshot.
6. For bees use actual vanilla BEEHIVE/BEE_NEST placed by a verified runtime
   adapter. Each expanded hive block has its own natural BeeHiveBlockEntity.
   Never claim capacity increases unless bees can choose these homes.
7. Add config flags (both systems off by default during rollout), real
   GameTests for placement, save/reload, chunk unload and existing combat handoff.
8. Benchmark large formations and cap scanning/build frequency per server tick.
9. Only merge the draft PR after tests and performance validation.

## Suggested staged acceptance criteria
M1: Connectivity and deterministic growth gates — Java policies/tests.
M2: Real Nest Core adapter and in-world extension with reversible resource costs.
M3: Real vanilla beehive adapter and colony housing/flower-goal compatibility.
M4: Emergency combat broadcast to ALL connected colony members through
    bounded local communications, without telepathy or bypassing vanilla damage.
M5: GameTest regression, world-save stress tests and performance approval.

Long-term work lives in this branch and this plan. This document is NOT
an automation or a claim that development continues while ChatGPT is offline.
