# Adaptive Player Model & Natural Spawn Control

Status: first implementation slice, in review.

Tracking issue: https://github.com/lord-navy-crypto/SWARM-MOBS/issues/89

## Natural spawn weights

The Command Center exposes one relative weight multiplier per combat species:

- Zombie
- Skeleton
- Creeper
- Spider

Values are bounded from 0.0 to 4.0:
- 0.0 removes that species' entry from the natural spawn candidate list.
- 1.0 preserves the biome's vanilla weight.
- Values above 1.0 scale the existing weight, changing the species' relative selection probability.

The runtime hook is NeoForge LevelEvent.PotentialSpawns, which is called while NaturalSpawner builds the candidate list. The adapter replaces only matching entries, preserving their original min/max pack size and all unrelated entries. It does not create extra spawn attempts, change the hostile mob cap, or modify spawner blocks, spawn eggs, commands, conversions, or reinforcement logic.

A multiplier is not an absolute mobs-per-minute guarantee. Actual spawn counts still depend on biome entries, light, local spawn rules, player distance, mob cap and available spawn positions.

## Session-local player style model

Learning is opt-in and OFF by default.

The first model observes only successful damage events where a player damages one of the four supported swarm species. It increments one of five broad categories:
- melee
- projectile
- explosion
- fire
- other

The registry stores decaying category counts, not event replays. It is session-local, capped at 64 player profiles, expires inactive profiles after 12,000 ticks, and uses a 2,400-tick half-life. No player name, coordinates, chat, inventory contents, item identifiers or raw event history are stored. Disabling learning clears the current world's profiles; the Command Center also has an explicit reset action.

The deterministic policy requires at least five decayed samples and a dominant category share of at least 55% before adapting. It can adjust only bounded formation radius, separation and cohesion multipliers. It cannot set targets, choose exact coordinates, issue per-mob commands, change damage, or override vanilla combat handoffs.

When local Ollama shadow analysis is explicitly enabled, the request includes only aggregate profile counts and category shares. Ollama remains loopback-only in the existing architecture; the learned profile itself is never sent to a remote service.

## Homogeneous Zombie group geometry

During a fresh, high-confidence stationary-target SURROUND pattern, existing FLANK_LEFT and FLANK_RIGHT Zombie slots now receive distinct, bounded lateral offsets. This makes an all-Zombie group visibly less likely to converge on the same lane even when no Spider archetype is present.

The change is a local Minecraft waypoint refinement only. Existing collision checks, navigation acceptance, terrain validation and native combat handoffs remain authoritative. Skeleton ranged lanes and Creeper ingress/fuse behavior are not modified.

## Research basis

1. **A Survey of Planning and Learning in Games** (Applied Sciences, 2020) describes opponent modeling from observed actions, online player modeling, and dynamic scripting in which rule weights are updated after encounter outcomes. This motivates a lightweight, interpretable first model rather than attempting unconstrained deep learning during every server tick.
   - https://www.mdpi.com/2076-3417/10/13/4529

2. **Deep Multiagent Reinforcement Learning: Challenges and Directions** (Artificial Intelligence Review, 2022) surveys non-stationarity, partial observability, coordination and scalability challenges. These are reasons to keep SWARM-MOBS' low-level control decentralized, bounded and deterministic, while treating more expensive learning as an optional higher-level layer.
   - https://link.springer.com/article/10.1007/s10462-022-10299-x

3. **NeoForge 1.21.1 LevelEvent.PotentialSpawns API** documents that the event is called from NaturalSpawner.mobsAt while building natural spawn candidates, and provides explicit add/remove operations.
   - https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/event/level/levelevent.potentialspawns

4. **NeoForge 1.21.1 DamageContainer API** documents the damage pipeline and the post-damage event stage used to observe successful damage against supported swarm entities.
   - https://aldak.netlify.app/javadoc/1.21.1-21.1.x/net/neoforged/neoforge/common/damagesource/damagecontainer

## Known limitations / next learning steps

This is online opponent profiling, not a trained neural network or full reinforcement-learning system. It currently learns only broad damage-source categories; it does not yet model movement style, defensive timing, vertical movement, line-of-sight breaks, healing or block placement. It also does not yet update tactic-rule weights from encounter outcomes.

Next steps should add:
- movement and line-of-sight behavior summaries using bounded sampling;
- per-species survival/casualty and successful-navigation metrics;
- encounter-level outcome attribution before any rule-weight learning;
- a replayable offline evaluation harness to compare deterministic baselines against learned policies;
- mixed and homogeneous squad GameTests plus disposable-world playtests.

Do not add neural inference or a per-mob LLM call to the server tick loop. Keep the deterministic fallback playable on its own.
