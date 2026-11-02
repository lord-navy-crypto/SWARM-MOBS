# Phase IV — Advanced Monster Intelligence

## Scope

Phase IV improves the existing Minecraft swarm controller without replacing vanilla attacks, damage, entity goals, or path navigation. The deterministic planner remains authoritative. The optional Ollama integration receives aggregate telemetry only and remains in shadow mode unless an operator explicitly enables its bounded overlay.

## New decision inputs

- **Player combat style:** successful damage against supported swarm mobs is classified into melee, projectile, explosive, fire, magic, or other.
- **Player movement style:** fresh direct observations are summarized as stationary, walking, sprinting, or erratic movement. Direction changes are detected from consecutive observed velocity vectors, not camera direction.
- **Confidence gates:** a style must have at least five effective samples, a dominant share of at least 55%, and a 15-point margin over the next category before it influences the controller.
- **Time decay:** evidence uses a 2,400-tick half-life and expires after 12,000 ticks without new observations.
- **Bounded memory:** at most 64 player profiles are retained per loaded dimension; repeated observations from the same tick are deduplicated.

## How the swarm adapts

- Repeated melee pressure encourages a slightly tighter formation and stronger cohesion.
- Projectile and explosive pressure increase spacing and reduce crowding.
- Fast or erratic movement broadens search coverage and applies bounded formation adjustments.
- The existing species planners still determine Zombie, Skeleton, Spider, and Creeper behavior.
- Tactical geometry changes pass through a configurable hysteresis window, preventing rapid pattern switching when observations fluctuate.

## Controls and privacy

The Player Learning page exposes the learning toggle, aggregate profile summaries, reset action, and tactical-pattern hold duration. Profiles are session-local and in-memory only. The mod does not persist raw combat events, player coordinates, movement paths, chat, or inventory, and it does not upload these summaries.

## Validation targets

- Unit tests cover decay, expiry, sample confidence, movement classification, duplicate-sample rejection, and tactical hysteresis.
- Runtime GameTests continue to verify the existing native combat and movement handoffs.
- Manual playtesting should compare homogeneous Zombie groups and mixed-species groups against stationary, moving, and direction-changing players.
