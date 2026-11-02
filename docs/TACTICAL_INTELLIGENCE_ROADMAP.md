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
