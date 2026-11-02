# Physical same-block growth bridge (integration stage)

`ColonyPhysicalGrowth.placeOne` has been added for REAL in-world placement of an existing block. It operates only on the Minecraft server, only between face-adjacent already-loaded positions, and only when the parent matches the requested colony block: registered `nest_core`, vanilla `bee_nest`, or vanilla `beehive`. The target must be actual air. Vanilla beehives create their own independent vanilla block entities.

**No autonomous scheduler calls this method yet.** This method does NOT decide colony occupancy, resources, permission/claims, queen lifecycle, or when to construct. Do not schedule it until the following are implemented:
- source colony identity and connected membership, especially for multiple adjacent Nest Core block entities;
- one elected active controller for the connected component, to prevent every core independently spawning and consuming matter;
- resource conservation, server-side claim/permission checks, work interruption during combat, and expansion cooldown;
- world-save and loaded-chunk behavior, birth/population accounting, block placement GameTests;
- propagation of bee housing decisions between adjacent vanilla hives without touching the native internal capacity.

**Existing implementation discovered**: `src/main/java/dev/swarmmobs/colony/SwarmNestBlockEntity.java` owns actual resource points, reproduction and current virtual chamber construction; `SwarmNestArchitecturePolicy.MAX_CHAMBER_LEVEL` is 7. The new unlimited physical model must be additive/migrated, not implemented by directly removing limits and causing hundreds of adjacent independent Nest Core tickers.

**Status:** code committed, compile and GameTest not executed. Keep PR draft.
