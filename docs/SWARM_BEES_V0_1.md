# Swarm Bees — v0.1 decision core

This branch is intentionally the first non-invasive step toward integrating vanilla Minecraft bees with the existing SWARM-MOBS engine.

## Implemented
- Pure Java, deterministic `BeeTaskPolicy` with FORAGE / NURSE / GUARD / BUILD / REST tasks.
- Local-demand signals and individual response biases.
- Switching penalty to avoid role thrashing.
- Pure Java `BeeForagePolicy` for nectar/pollen reward, distance, competition and danger.
- Time-based confidence decay for remembered flower patches.
- JUnit policy tests.

## Not implemented yet
No runtime hook into vanilla `Bee`, bee nest block entities, networking, HUD, population simulation, resource storage, reproduction, or in-game commands. Existing combat AI is untouched.

## Integration requirements
1. Inspect the existing entity controllers, sensing/communication APIs, role hysteresis, config, telemetry, and GameTest patterns.
2. Adapt existing swarm primitives rather than duplicating generic communication and perception.
3. Add vanilla Bee-specific attachment/state and lifecycle management with server authority.
4. Define flower-patch observations from loaded chunks only, bounded sensing radius, finite cache size and message expiry.
5. Make a bee's own forage goal yield to suitable vanilla behaviors (return-to-hive, anger, pollination, breeding) rather than overriding them.
6. Add GameTests for ordinary pollination, nest return, stinging/anger behavior, persistence, and mob combat regressions.
7. Benchmark bee counts and prevent chunk-loading side effects.

## Proposed experiment metrics
Gathered nectar per bee per 1,000 ticks, exploratory trips, failed flower visits, recruitment utility, profession reassignments, path failure ratio, and average tick time.

## Verification
The GitHub connector saved the files; neither Gradle nor Minecraft GameTests were run in this environment. All behavior tests must be executed before merging.
