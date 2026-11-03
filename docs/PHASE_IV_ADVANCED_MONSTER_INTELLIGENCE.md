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

## Phase IV-G — Bee workforce saturation and casualty-aware frontline rotation

### Bee colony labor
- Count only task assignments refreshed within 400 ticks and scoped to the Bee's current native hive.
- Estimate local workforce size from those recent assignments; do not add a world entity scan or a global colony manager.
- Derive desired headcounts from observed nectar shortage, brood demand, threat, build readiness, and worker fatigue.
- Strongly reduce new recruitment into a filled task while retaining a softer incumbent preference; materially overstaffed tasks can release workers.
- Preserve stable caste bias and task-switch hysteresis. Vanilla flight, pollination, hive entry, anger, and breeding remain authoritative.

### Monster frontline rotation
- Read health fractions only from the already sampled same-target local squad.
- When the current Zombie CHASER is at or below 35% health, promote a healthy REAR_PRESSURE Zombie at or above 70% health if its health advantage is at least 35 percentage points.
- Rotate only uncommitted assault members after both local flanks are covered; ENGINEERING/MATERIAL workers and Spider flank assignments are not stolen.
- The injured member moves to REAR_PRESSURE; the relief member takes CHASER. Existing role hysteresis still applies, with the normal urgent-vacancy hold.
- The policy changes formation roles only. Vanilla target selection, damage, attacks, projectiles, Creeper fuse, and navigation remain authoritative.

Validation: deterministic unit tests cover task saturation, hive scoping, stale census expiry, frontline relief thresholds, committed workers, species isolation, and roster-order independence.

## Controls and privacy

The Player Learning page exposes the learning toggle, aggregate profile summaries, reset action, and tactical-pattern hold duration. Profiles are session-local and in-memory only. The mod does not persist raw combat events, player coordinates, movement paths, chat, or inventory, and it does not upload these summaries.

## Validation targets

- Unit tests cover decay, expiry, sample confidence, movement classification, duplicate-sample rejection, and tactical hysteresis.
- Runtime GameTests continue to verify the existing native combat and movement handoffs.
- Manual playtesting should compare homogeneous Zombie groups and mixed-species groups against stationary, moving, and direction-changing players.

## Phase IV-B — Collective Observation Fusion

The existing neighbor-to-neighbor message system now fuses fresh, spatially consistent observations before the deterministic planner consumes them.

- The newest actual observation anchors the fused estimate and its timestamp is preserved.
- Older observations may be projected forward only within a short configurable window, using their own observed horizontal velocity and a three-block correction cap.
- Duplicate relays are deduplicated; observations for other targets, expired records, vertical outliers, and spatial outliers are excluded.
- Same-tick observations receive equal weight. Older observations receive lower weight according to their age and existing confidence.
- Fusion never reads hidden live target state, changes the source observation timestamp, or bypasses vanilla target/attack/navigation ownership.
- The Communication page exposes the fusion toggle, time window, cumulative fusion count, and current contributor summary.

Default fusion window: 4 ticks. Maximum accepted horizontal disagreement: 8 blocks. Maximum temporal correction: 3 blocks. Fusion is enabled by default and can be disabled independently of message communication.

## Phase IV-C — Collective Experience and Local Task Auctions

Task experience is now measured against elapsed game time rather than the number of planner updates. The existing gain and retention settings are calibrated to the original six-tick planning interval; fractional intervals compose consistently, so changing the planner cadence does not accelerate or slow learning. Catch-up after long inactive gaps is capped at 40 ticks. Inactive skills continue to decay when an agent has no target or dynamic division of labor is switched off.

The same-target neighborhood now runs a deterministic local matching pass:

- Each member bids for tasks using its archetype capability, task experience, observed-target distance, combat-busy penalty, and current assignment switching cost.
- A utility-ordered greedy matching gives each member at most one task and respects per-task desired headcounts.
- Native species-core assignments and incumbents still inside their specialization hold window retain their required capacity.
- Task occupancy, task experience, and specialization-hold state use same-tick snapshots, reducing entity-tick-order effects when nearby mobs update sequentially.
- UUID tie-breaking and duplicate-member filtering keep the result stable for an identical local candidate set.
- The auction reuses the existing same-target neighbor list; it does not scan all loaded entities or issue additional pathfinding queries.

The Labor page exposes an independent local-auction toggle and aggregate round/assignment counters. The feature is enabled by default, can be disabled without disabling the existing per-agent division-of-labor policy, and remains local to the observed neighborhood rather than introducing a central squad commander.

Experience and auction telemetry remain in-memory agent state. No raw combat history, player coordinates, movement paths, chat, inventory, or external service data are persisted by this phase.

Validation includes cadence-invariance tests, deterministic-order tests, capacity and native-core tests, locked-incumbent tests, same-tick snapshot tests, and the existing runtime GameTests.
