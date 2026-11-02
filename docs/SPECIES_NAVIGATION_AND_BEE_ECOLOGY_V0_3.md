# Species Navigation and Bee Ecology v0.3

## Scope

This increment improves locomotion reliability and non-combat colony behavior. It does not change Zombie/Skeleton/Creeper/Spider damage, attack targets, projectile behavior, Creeper fuse timing, or vanilla attack goals.

## Four-species locomotion

A shared, bounded route-failure memory now records only positions that the local Minecraft navigator or path-evidence check has already rejected.

- One failed observation is not enough to avoid a position.
- Repeated failures within a short window temporarily suppress the same candidate.
- A successfully accepted route clears the old failure evidence.
- Memory is local to a ServerLevel, bounded to 2,048 cells, and expires.
- It never scans new terrain, reveals hidden entities, persists to disk, or requests chunks.

Each species receives a different locomotion profile:

| Species | Navigation distinction |
|---|---|
| Zombie | Baseline detour and congestion costs; balanced recovery memory |
| Skeleton | More conservative spacing and a stronger congestion penalty |
| Creeper | Smaller lateral detours and longer memory to reduce route oscillation |
| Spider | Wider local detours and shorter failure memory so native movement can retry sooner |

The profile affects only obstacle/recovery candidate geometry and congestion scoring. Native species goals remain authoritative.

## Bee colony ecology

### Flower recruitment

Flower recruitment now uses the hive's measured nectar shortage, the observation confidence, and local flower competition. A patch with good historical returns is not automatically recruited when the receiving hive has little need or the patch is already crowded.

Only flowers actually observed by a Bee and confirmed on successful nectar returns enter the memory. Neighboring hives share only directly adjacent, loaded-hive observations.

### Local alarm and shelter

When enabled, a Bee close to its own loaded vanilla hive may observe a hostile Monster within eight blocks and publish a short-lived, hive-local alarm.

- The alert is refreshed only by a nearby Bee with direct line of sight.
- Repeated observations raise the alert level; it expires after 120 ticks without new evidence.
- A deterministic fraction of unladen Bees return toward their own hive: 35%, 70%, or 100% according to alert severity.
- Bees carrying nectar retain vanilla return-to-hive behavior.
- Bees that have already stung or already have a target are not overridden.
- The feature never assigns an attack target or adds a sting, damage, speed, or health bonus.
- Flower recruitment and optional hive expansion pause while the alert is active.

This is a game-specific shelter response inspired by honeybee alarm communication; it is not a literal simulation of guard-bee stinging behavior.

### Hive expansion

The existing connected-hive expansion continues to use nearby flower availability, hive crowding, local threat pressure, full occupancy, full honey, and mobGriefing. Expansion is deferred during a local alarm. The new hive remains a normal empty vanilla Beehive/Bee Nest block entity.

## Configuration

The new setting is independent and OFF by default:

- `beeColonyShelterEnabled`

Existing switches remain independent:

- `beeFlowerRecruitmentEnabled`
- `beeConnectedHiveGrowthEnabled`

No existing world configuration is silently enabled or rewritten.

## Performance and safety constraints

- No forced chunk loads or chunk tickets.
- Hive threat sensing is local and throttled to once per 20 ticks per hive.
- Route memory is transient, bounded, and server-level scoped.
- No global entity scans.
- No new attack targets, damage, projectile behavior, or fuse changes.

## Research basis and limits

1. Bossens, Ramchurn & Tarapore (2022), *Resilient Robot Teams: a Review Integrating Decentralised Control, Change-Detection, and Learning*, Current Robotics Reports. DOI: https://doi.org/10.1007/s43154-022-00079-4. Inspiration: local sensing, fault detection, bounded adaptation, and explicit recovery.
2. Albani et al. (2021), *Hierarchical Task Assignment and Path Finding with Limited Communication for Robot Swarms*, Applied Sciences 11(7):3115. DOI: https://doi.org/10.3390/app11073115. Inspiration: task/path coupling under limited communication and agent failures.
3. Dyer (2002), *The Biology of the Dance Language*, Annual Review of Entomology 47:917-949. DOI: https://doi.org/10.1146/annurev.ento.47.091201.145306. Inspiration: confidence-weighted, locally observed flower recruitment.
4. Nouvian, Reinhard & Giurfa (2016), *The defensive response of the honeybee Apis mellifera*, Journal of Experimental Biology. DOI: https://doi.org/10.1242/jeb.143016. Inspiration: context-dependent alarm and recruitment while preserving foraging capacity.
5. Ramírez-Moreno, Galizia & Nouvian (2025), *Division of labour during honeybee colony defence: poetic and scientific views*, Philosophical Transactions of the Royal Society B. https://pmc.ncbi.nlm.nih.gov/articles/PMC11923620/. Important caveat: the authors note that the determinants of guard/soldier specialization remain incompletely understood; the mod therefore uses dynamic shelter proportions rather than fixed castes.
6. Isla (2005), *Handling Complexity in the Halo 2 AI*, Game Developer / GDC proceedings. https://www.gamedeveloper.com/programming/gdc-2005-proceeding-handling-complexity-in-the-i-halo-2-i-ai. Inspiration: coherent state transitions, transparency, and explicit behavior arbitration. The present increment applies these principles to navigation and ecological behavior only.
7. NeoForge 1.21.1 documentation: https://docs.neoforged.net/docs/1.21.1/blockentities/. Inspiration: keep world access bounded and avoid expensive work in high-frequency tick paths.

## Validation targets

- Unit tests for species locomotion profiles and route-failure memory.
- Unit tests for hive alarm expiry, scan throttling, shelter proportions, and nectar-need recruitment.
- Pull-request CI: Java 21 compile, JUnit, Runtime GameTests, Gradle build, and JAR packaging.
- In-world playtest: verify shelter behavior around a hive, no target assignment, normal Bee pollination resumption after alarm expiry, and no repeated failed-route loops.
