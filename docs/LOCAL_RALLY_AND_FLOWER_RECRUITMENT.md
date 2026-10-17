# Swarm colonies: bounded muster and native flower knowledge

## Monster nests: optional local defensive muster
- `nestColonyRallyEnabled=false` is a new separate SERVER config flag.
- An already-owned, currently ticking monster with a real living Player target
  may report **its own current location** as a short-lived alarm witness.
  This is not the enemy's position and does not reveal remote entities.
- A fraction of nearby idle colony members within 7–40 blocks of that real
  witness may participate. New `SwarmColonyRallyGoal` uses a low-priority MOVE
  goal and a dimension-global request budget (one path attempt every 4 ticks).
  Per-mob retries also have cooldowns. Existing local swarm target handling
  and vanilla attacks retain priority; targeting is never injected.
- Rally ends when local evidence expires, the member has direct combat/search
  work, it arrives, navigation fails, or after 90 ticks.
- The original civilian work gate will honor alarms when either physical
  colony growth or this separate rally feature is enabled.
- No chunk loads, protected-world edits, or changes to enemy attacks.

## Honeybee hives: optional observed-flower communication
- `beeFlowerRecruitmentEnabled=false` is another independent SERVER flag.
- Every 100 ticks per real Bee (staggered by entity ID), a nectar-carrying bee
  can publish its vanilla `getSavedFlowerPos` only if the flower still exists
  in a loaded chunk, is tagged as a flower and lies within 64 blocks of hive.
- Bees without their own saved flower and not carrying nectar may adopt a
  fresh observation from their own hive or a directly neighboring vanilla
  BEE_NEST/BEEHIVE. Vanilla Bee AI remains responsible for flying,
  remembering, pollinating and returning.
- Memoized reports decay with time (confidence half-life 600 ticks) and expire
  after 2400 ticks. The cache remembers up to 16 flower sites per hive and at
  most 1024 currently cached hives per dimension. These are **memory/CPU
  budgets**, NOT physical hive-count or colony-growth limits.
- No synthetic flowers, wax, nectar, honey or newly spawned bees are created.

## Tests and status
- New unit cases for bounded rally selection, witness positions, flower
  memory expiry/deduplication, and caching limits.
- Runtime wiring lives in SwarmMobEvents and the mod's NeoForge event bus.
- Experimental toggles remain OFF by default. Existing Nest Core / native
  hive world expansion toggles also remain OFF by default.
- Recheck current-head GitHub Actions compile, JUnit, GameTests and JAR before
  recommending runtime activation in a backup world.
