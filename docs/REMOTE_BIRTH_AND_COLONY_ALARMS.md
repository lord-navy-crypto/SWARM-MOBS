# Monster colony phase: emergency construction halt and remote births

## Live changes in this increment
- A real monster with a persistent Nest Core home and a living Player as its
  vanilla target reports a **root-level civilian alarm** while its loaded
  entity ticks.
- The alarm is stored transiently by level/root, refreshes on sightings,
  and expires after 200 game ticks without refreshed evidence. It reports
  *presence of danger only*, never a target coordinate or instant global
  enemy acquisition. Original vanilla melee/bow/fuse remains authoritative.
- While a connected-growth root has an active alarm, its 200-tick cycle
  pauses physical Nest Core expansion, old shell repair/virtual construction,
  and monster births. Existing colony census/telemetry still runs.
- Zombie resource worker goals already independently respect
  `SwarmEngagementPolicy.canDoNestWork`; this adds root-side emergency
  suspension, without replacing their attack goals.

## Distributed birthplace prototype
- Previously ALL births originated around the first Nest Core. An enabled
  connected colony now alternates between normal root birth sites and a
  **verified, connected, same-owner last extension**.
- Each birth site must be in loaded chunks, supported by a sturdy ground
  face, have air headroom and be fluid-free. Child collision is still
  checked by existing `level.noCollision` before spawning, and resource
  payment/cooldown are unchanged.
- Remote sites additionally require a real player within 48 blocks of that
  tip, and all candidate sites reject living players within 12 blocks.
- Existing root observer requirement (48 blocks) and root close-player
  safeguard are preserved. This means very remote growth may require a
  player near the original root and another near the extension; no chunks
  are forced to load.
- The distribution is a FIRST STEP, not a full rotating scan over all
  future extension segments. Broader balanced birth-site allocation is
  still pending, as is resident caste aggregation over all regions.

## Tests and rollout
- New pure `ColonyAlarmLedgerTest` ensures expiry/refresh/independent roots.
- New runtime `ConnectedColonyGameTests` birth planner test checks an
  actual ground site and rejects obstruction overhead.
- Java compile, JUnit and Minecraft GameTests must be checked on the
  current PR head; prior CI cannot validate this commit.
- `nestConnectedCoreGrowthEnabled` remains **false** by default.
- No claim/region protection integration yet. Keep experimental growth off
  on servers with protected building areas.
