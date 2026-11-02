# Behavior-first development scope

## Active focus

The behavior-first line prioritizes heterogeneous combat coordination: sensing, communication, target relay, role allocation, target-loss search, formation selection, and species-aware movement. Advanced Zombie resource collection and nest engineering are paused.

## Paused and archived Zombie systems

The SERVER master switch `zombieColonyEngineeringEnabled` defaults to **OFF in normal worlds** and gates Zombie obstacle breaking/bridging, nest founding and lifecycle, resource mining/harvesting, berry foraging, hunting, hauling, Spider resource scouting, and connected nest growth.

The resource-oriented `SwarmNestPheromoneField` and `SwarmZombiePheromoneExploreGoal` remain available in the archive snapshot but are not registered as active goals on the tactical work line. They must not guide food, timber, soil, or other resource searches while the resource system is paused.

The complete resource/engineering snapshot is preserved on:
- `archive/zombie-ant-colony-2026-11-02`
- `archive/zombie-resource-gathering-2026-11-02`

Existing Nest Core blocks are not deleted. Their lifecycle remains paused until the master switch is deliberately re-enabled.

## Exploration that remains active

Combat SEARCH is independent of nest resource pheromones. The core system continues to use target-scoped observations, confidence decay, rotating capability sectors, same-target regrouping, and reacquisition metrics.

A separate `SwarmSearchTrailField` remembers only locations where an agent actually reached a search waypoint. It is bounded, short-lived, dimension-local, and keyed by the last-known target. It discourages repeated coverage of the same search area; it does not discover resources, scan remote chunks, or reveal a live target position.

## Bee colony retained intact

The Bee system remains separate from hostile-mob nests:
- `BeeFlowerMemory`: bounded memory of genuinely observed vanilla flower sites;
- `BeeFlowerEvents` and recruitment policy: local flower knowledge sharing;
- `BeeHiveEvents` and growth policy: vanilla hive expansion;
- independent SERVER switches `beeFlowerRecruitmentEnabled` and `beeConnectedHiveGrowthEnabled`.

These Bee features are not controlled by `zombieColonyEngineeringEnabled`. The Bee module and its tests should be validated before it is merged to `main`. Bee flower memory is an observation-sharing model, not a claim that the mod simulates literal insect pheromone chemistry.

## Re-enabling Zombie colony engineering

Set `zombieColonyEngineeringEnabled=true`, then enable only the individual Zombie options needed for a controlled test world. The Bee settings and combat-search system are independent.

GameTest runs explicitly default the master switch ON so archived-system regression fixtures can continue to exercise the Zombie code. Normal runtime defaults it OFF.
