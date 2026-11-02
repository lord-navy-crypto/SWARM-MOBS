# Behavior-first development scope

## Active focus

The behavior-first line keeps the core heterogeneous swarm system active while pausing the advanced Zombie colony-engineering layer. The purpose is to stabilize sensing, communication, movement, role allocation, target sharing, search, and combat cooperation before returning to environmental construction.

## Paused Zombie systems

The new SERVER config master switch is `zombieColonyEngineeringEnabled`. It defaults to **OFF in normal worlds** and gates:

- Zombie obstacle breaking and bridge placement (`SwarmZombieEngineerGoal`);
- initial Zombie Nest Core founding (`SwarmIdleNestGoal`);
- Zombie material mining, crop harvesting, berry foraging, animal hunting, and dropped-item hauling;
- Spider resource scouting that feeds Zombie nest material/prey jobs;
- nest-linked pheromone resource exploration (the code remains, but its resource-work runtime is paused);
- Zombie Nest Core lifecycle, chamber expansion, visible shell repair, connected-core growth, and colony spawning.

The source remains in this working branch for compatibility and future reactivation. Existing Nest Core blocks are not deleted; their lifecycle is paused, so they remain inert until the master switch is re-enabled. The complete pre-pause snapshot is preserved on `archive/zombie-ant-colony-2026-11-02`.

## Systems intentionally retained

- Core swarm coordination for Zombies, Skeletons, Spiders, and Creepers: local sensing, neighbor communication, target relay, ENGAGE/SEARCH, role allocation, local movement planning, and vanilla combat handoff.
- The independent local colony-rally behavior, still controlled by `nestColonyRallyEnabled`.
- Vanilla Bee flower-memory/recruitment and hive-growth hooks. They remain governed by their own independent flags, `beeFlowerRecruitmentEnabled` and `beeConnectedHiveGrowthEnabled`; the Zombie master switch does not affect them.
- Shared physical-growth helpers used by vanilla Bee hive expansion.

## Re-enabling Zombie colony engineering

Set `zombieColonyEngineeringEnabled=true`, then enable only the individual Zombie options needed for a controlled test world (for example `zombieEngineeringEnabled`, `nestConstructionEnabled`, `nestLifecycleEnabled`, and the relevant gathering/hauling options). The Bee settings are independent.

GameTest runs explicitly default the master switch ON so the existing archived-system regression fixtures can continue to exercise the Zombie code. Normal runtime defaults it OFF.
