# Ultimate connected nests — runtime prototype (2026-10)

## Ant/termite-inspired monster colony

- Existing registered Nest Core blocks, face adjacency only, no new block types.
- Root Nest Core owns resource inventory and spawning. Physically grown
  Nest Core blocks remain passive and store the root position.
- The active root incrementally traverses *loaded* face-neighbor Nest Cores,
  checking that each extension belongs to the root. Each 200-tick root
  lifecycle scans up to 192 candidate positions. Connected extension
  count now increases `effectiveCapacity()` by 4 per loaded verified block,
  on top of the legacy chamber capacity. No configured lifetime extension cap.
- Every 2400 ticks root restarts connectivity verification so broken chains
  gradually lose capacity; a disconnected remembered growth tip cannot
  continue construction if it is not in the verified root component.
- Existing material economy still pays 8 soil + 6 timber points for each
  successful Nest Core block, and building is opt-in
  `nestConnectedCoreGrowthEnabled=false` by default.
- This is **not** a completed distributed census. The original mob census
  is still local (radius 14), and breeding/growth of sprawling colonies can
  stall or behave conservatively outside this local population window.
  Multi-anchor entity census and root re-election remain open tasks.

## Honeybee colony

- Only `Blocks.BEE_NEST` and `Blocks.BEEHIVE`, no new hive block.
- Optional `beeConnectedHiveGrowthEnabled=false` by default.
- A living real bee with a loaded native hive can initiate a build check
  once every 400 ticks (per hive position), only with an observing nearby
  player and mobGriefing enabled.
- Vanilla `BeehiveBlockEntity.getOccupantCount()` determines when the parent
  is at capacity. Parent also needs full vanilla honey level 5, no adjacent
  living entities at build site, no fire nearby, and a face-adjacent empty
  location in already loaded chunks.
- A successful build produces the same vanilla block as its parent with a
  **fresh, empty native vanilla BeehiveBlockEntity**, reduces parent honey
  level from 5 to 0 and directs the external bee to the new home.
  No cloning of stored bees or honey. As each hive fills over time, its own
  bees can initiate further growth. No arbitrary total hive count cap.
- This is a simplified game rule: honey level acts as the initial proxy for
  structural wax/comb resources. Explicit wax tracking, swarming, dancing,
  predator alarm and brood lifecycle integration still need future work.

## Verify before enabling
- New vanilla policy unit tests for full occupancy/full honey/calming.
- Connected-nest unit regression for a broken middle link.
- New registered Minecraft GameTests for real physical block placement,
  passive monster core ownership, isolation of independent roots, and new
  empty vanilla beehive occupancy.
- **All experimental world edits remain OFF by default.**
- Need latest CI status, human world test in a backup save, claims/protection
  compatibility, distributed member census, topology split under unload and
  live combat task preemption. Do not merge without these.
