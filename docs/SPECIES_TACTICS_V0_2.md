# SWARM-MOBS — Species Tactics and Combined-Arms v0.2

## Design boundary

Every member derives its decisions from its own locally sampled same-target group. There is no omniscient squad commander, hidden live-player position, global entity search, or replacement of vanilla attack/fuse/projectile mechanics. Candidate movement remains subject to loaded-world footing, line-of-fire, and PathNavigation acceptance.

## Species-specific behavior

### Zombie — frontline anchor and gap coverage
- Keeps the CHASER/front anchor when the group needs a frontline.
- Takes a FRONT_SCREEN posture when Skeleton overwatch is present.
- Fills a missing flank when Spider coverage is absent.
- Uses REAR_RESERVE or secondary pressure when the current role calls for it.
- Continues to yield a blocked Skeleton bow corridor rather than forcing friendly fire.

### Skeleton — overwatch and crossfire
- A lone Skeleton keeps its ordinary ranged-support posture.
- Multiple same-target Skeletons receive opposite, deterministic lateral lane preferences based on UUID order.
- The existing support-position optimizer may retain a verified lane, but cannot ignore a blocked shot or unsafe terrain.
- Close-range spacing and native bow handoff remain unchanged.

### Creeper — staged ingress and vanilla fuse handoff
- In a mixed Zombie/Skeleton/Creeper group, a visible and sufficiently confident target may trigger a short side-entry staging waypoint.
- The staging axis is stable for the current tactical frame; the waypoint is selected to reduce local ally crowding.
- The world adapter checks local footing and active Skeleton firing lanes before accepting it.
- Once the staging waypoint is reached, the extra waypoint is released and normal Creeper approach resumes.
- Ignition or active swelling always returns control to vanilla fuse behavior.

### Spider — paired pincer and surround arc
- Same-target Spiders retain deterministic opposite flank assignment.
- When an assault frontline is present, paired Spiders widen their flank geometry.
- During a SURROUND pattern, Spider flank waypoints receive a small arc lead beyond the target's flank.
- Search mode remains a separate scout/search behavior.

## Combined-arms operations

The local composition selects one operation:
- INDEPENDENT
- FRONTLINE_OVERWATCH
- PINCER_SCREEN
- BREACH_SCREEN
- FULL_COMBINED_ARMS
- SEARCH_RELAY

The plan activates only the coordination channels supported by the observed species mix. A missing species is not fabricated, and a lost target disables combat staging.

## Bee colony intelligence

Bee behavior remains a separate ecological subsystem. It adds bounded flower observations, confidence and successful-return tracking, local task hysteresis, patch ranking by distance/competition/danger, and scored vanilla hive expansion sites. It does not share monster combat orders or modify Bee anger/attack behavior.

## Regression checks

- Species policies have deterministic JUnit coverage for posture selection, crossfire sides, staged-ingress boundaries, Spider arc behavior, and mixed-squad operations.
- Bee tests cover bounded memory, stale observations, task hysteresis, and site scoring.
- Runtime CI must verify no duplicate search-memory update, no duplicate doctrine multiplier, no Creeper fuse regression, no Skeleton friendly-fire regression, and no Bee chunk-loading side effects.
