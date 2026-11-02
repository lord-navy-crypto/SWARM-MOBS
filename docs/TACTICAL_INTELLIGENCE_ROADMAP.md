# Tactical Intelligence Roadmap

## Current baseline

SWARM-MOBS already has a substantial decentralized tactical foundation:
- four capability profiles: Zombie assault, Skeleton ranged support, Spider flanker, Creeper breacher;
- local same-target groups, target relay, observation confidence, bounded prediction, and reacquisition metrics;
- capability-aware formation slots, role hysteresis, specialization bidding, task saturation, and local role-coverage repair;
- target-loss SEARCH with rotating sectors, separation/cohesion/alignment, and same-target regrouping;
- species-aware flank geometry, ranged spacing, bow-lane yielding, breacher ingress clearance, and pack decongestion.

These systems are local and observation-bound. No global squad commander or hidden live-target location is required.

## Phase 1 — Search intelligence and adaptive doctrine

- Keep combat SEARCH independent of resource-gathering pheromones.
- Record only actually reached search waypoints in a bounded, decaying, target-specific field.
- Prefer under-visited sectors while preserving stable capability spacing.
- Select a combined-arms doctrine from the current local composition and target confidence.
- Scale formation geometry by doctrine and archetype, while retaining role hysteresis and native game behavior.

## Phase 2 — Dynamic team organization

- Re-evaluate composition as members join, leave, die, lose a target, or regain one.
- Reassign roles only when the capability gap is meaningful; keep a short hold period to avoid role thrashing.
- Explicitly account for missing frontline, flank, ranged-support, and breacher coverage.
- Preserve useful task experience without allowing paused engineering/material tasks to remain assigned.

## Phase 3 — Multi-stage encounter behavior

- Add bounded state transitions such as approach, screen, flank, surround, recover, and search.
- Coordinate transitions through fresh local observations and teammate state, not a central omniscient controller.
- Give each species a distinct, complementary contribution; never force a species into an incompatible task.
- Validate candidate waypoints against loaded terrain, friendly-fire lanes, navigation acceptance, and native attack behavior.

## Phase 4 — Challenge tuning and validation

- Measure reacquisition time, duplicate search coverage, role-switch frequency, formation occupancy, navigation rejection, and species contribution.
- Add deterministic unit tests for policy boundaries and mixed compositions.
- Add Minecraft GameTests for target loss/reacquisition, changing group size, mixed-species formations, and config isolation.
- Tune difficulty through bounded perception, coordination delay, and tactical complexity rather than unfair omniscience or inflated damage.

## Phase 5 — Species intelligence refinement

The next implementation cycle strengthens each species without flattening them into one generic combat controller.

- **Zombie / ASSAULT:** improve frontline cohesion, lane discipline, pressure distribution, and bounded support-role handoffs.
- **Skeleton / RANGED_SUPPORT:** improve firing-position selection, ally-lane clearance, spacing, and repositioning while preserving vanilla bow behavior.
- **Creeper / BREACHER:** improve lead/reserve coordination and approach spacing while preserving the vanilla fuse handoff. No fuse, damage, or explosion-radius buffs.
- **Spider / FLANKER:** first validate the native wall-climbing handoff from PR #86 in a disposable world; then improve three-dimensional route choice, flank persistence, and mixed-terrain recovery.
- **Bee colony:** continue as a separate non-combat ecology track: flower quality/competition, completed foraging cycles, worker task balance, alarm/shelter recovery, and safe hive expansion. Do not invent brood or pollen sensors that vanilla APIs do not expose.

## Phase 6 — Adaptive collective organization

- Recompute local composition when members join, leave, die, lose sight, or reacquire a target.
- Repair missing role coverage without rapid role thrashing.
- Scale formation geometry and task allocation to local group size.
- Preserve task experience only while its task remains valid; clear stale responsibilities after handoffs.
- Keep decisions based on fresh local observations and bounded neighbor communication.

## Phase 7 — Multi-stage encounter and search coordination

- Improve target-loss SEARCH with sector coverage, duplicate-work reduction, confidence decay, and local regrouping.
- Add bounded transitions among APPROACH, SCREEN, FLANK, SURROUND, RECOVER, SEARCH, and RETURN-TO-FORMATION.
- Validate every candidate against loaded terrain, navigation acceptance, ally firing lanes, and native combat handoffs.
- Fall back to the last feasible behavior when observations are stale or a route is rejected.

## Phase 8 — Evaluation, fairness, and performance

Measure role coverage/switch rate, reacquisition latency, duplicate search coverage, accepted navigation versus actual progress, formation congestion, per-species contribution, task completion, path-query count, and tick cost.

Use deterministic JUnit tests for policy boundaries, Runtime GameTests for state transitions and combat handoffs, and disposable-world playtests at multiple group sizes (4, 8, 16, 24) with mixed compositions. CI is necessary but does not replace real Minecraft playtesting.

## Phase constraints

- Keep vanilla damage, projectile behavior, Creeper fuse, and Bee sting behavior unchanged.
- No hidden player tracking, omniscient squad commander, forced chunk loads, global per-tick entity scans, or unbounded persistent memory.
- External AI remains optional and above deterministic low-level safety/navigation.
- Zombie block breaking, material gathering, resource collection, and Zombie nest construction remain archived/paused. The exploration-oriented nest information tree may remain available but must not restart resource collection.
- Bee hive expansion remains an active, separate ecology feature.

Tracking issue: https://github.com/lord-navy-crypto/SWARM-MOBS/issues/87
