# Colony biology split — SWARM-MOBS

## Design contract
Bee colonies and hostile-mob nests share low-level sensing, neighbor communication,
deterministic experiments, spatial indexing, and mission-selection utilities,
but must not share reproduction, storage, growth, or hive/nest block semantics.

### Bee colony (honeybee-inspired)
Foraging, flower-memory decay, recruitment, brood care, pollination, hive stores,
queen lifecycle, natural reproductive swarming and proportional defensive response.
Bees must preserve vanilla sting and return-to-hive behavior.

### Monster colony (ant/termite-inspired)
Decentralized gathering and local recruitment; defended trails and nest entrances;
division of labor among patrol, gather, construct, repair, defend, and expand;
bounded construction/hauling tasks and defense-first mission handoff.
Different mob species offer different capabilities. The colony is not a biological
honeybee hive and monster spawn rules must not use a bee queen lifecycle.

## v0.1 coding increment
`MonsterColonyPolicy` is a testable, Minecraft-independent mission-selector
with explicit normalized signals, capability affinities and reassignment cost.
It does not modify current zombie/skeleton/spider/creeper navigation, goals,
damage, nest structures, or spawn mechanics.

The bee policy and monster policy are separate classes and packages; a subsequent
integration must map existing Nest resource counters and role telemetry into
`MonsterColonyPolicy.Signals` without bypassing old combat handoff rules.

## Runtime gates before enabling
- Inspect concrete existing Nest controller, jobs, capabilities and tick scheduler.
- Preserve existing combat handoff for zombie melee, skeleton bows,
  spiders and creeper swelling.
- Gate all colony work by loaded chunks, task radius, budget, and config flag.
- Extend JUnit and Minecraft GameTests with regression coverage.
- Leave new behavior opt-in until the runtime validations pass.
