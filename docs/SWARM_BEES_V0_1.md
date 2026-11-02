# Swarm Bees — coordinated vanilla colony behavior

## Runtime behavior

The Bee module integrates with vanilla Minecraft Bees, Bee Nests, and Beehives without replacing their native flight, pollination, anger, breeding, or return-to-hive goals.

### Flower memory and recruitment

- A successful nectar-carrying Bee reports only its actual saved flower position.
- Memory is hive-scoped, bounded to 16 flowers per hive and 1,024 cached hives, and expires after 2,400 ticks.
- Repeated successful returns increase observation reliability; stale observations decay.
- A receiver can learn from its own hive or a directly adjacent vanilla hive only.
- Candidate patches are ranked using observed utility, freshness, distance, local Bee competition, and nearby hostile-mob pressure.
- Invalid flowers are forgotten only when their chunks are loaded; unloaded locations are not force-loaded.
- Vanilla movement remains authoritative. Shared memory only supplies a remembered flower position when the Bee has no current flower assignment.

### Local task coordination

`BeeTaskMemory` gives each Bee a deterministic, short-lived task preference with switching hysteresis. It uses only available signals:
- nectar shortage from the hive honey level;
- space pressure from observed hive occupancy;
- local hostile-mob presence.

The runtime does not invent brood or fatigue readings when vanilla exposes no reliable sensor for them. Task preferences influence flower recruitment priority only; they do not replace vanilla Bee goals or falsely claim to implement nursing/guarding animations.

### Connected hive growth

- A full vanilla hive with full honey, calm conditions, an available air site, and mobGriefing enabled may expand.
- Candidate sites are ranked by nearby flowers, existing hive crowding, and observed hostile-mob pressure.
- Only loaded blocks are inspected; no chunk tickets are requested.
- One adjacent vanilla hive is placed per eligible growth interval.
- The parent honey reserve is consumed; the new hive is an ordinary empty vanilla block entity.
- There is no artificial lifetime hive-count cap.

## Configuration

Both runtime integrations remain independently opt-in:
- `beeFlowerRecruitmentEnabled`
- `beeConnectedHiveGrowthEnabled`

Existing worlds retain their persisted server-config choices. Disabling either switch leaves vanilla Bee behavior untouched.

## Validation

Pure-policy tests cover task hysteresis and expiry, observation reliability and decay, flower ranking inputs, and expansion-site scoring. Runtime CI must additionally validate vanilla Bee return-to-hive, pollination, anger, and hive placement behavior.
