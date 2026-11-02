# Spider Terrain Adaptation and Bee Foraging Lifecycle v0.4

## Scope

This increment continues the species-specific navigation and bee ecology work. It preserves vanilla Spider climbing/navigation, vanilla Bee pollination and hive occupancy, and the existing four-species combat handoffs.

## Spider: native wall-climber handoff

Minecraft 1.21.1 exposes `Spider` climbing state and `WallClimberNavigation`. The mod now detects when the active Spider navigator is actually a `WallClimberNavigation` and the immediate route is blocked by a safe, bounded vertical face.

The generic horizontal obstacle detour is deferred only when all conditions hold:

- The entity is a Spider and its active navigator is `WallClimberNavigation`.
- The forward probe is blocked.
- The local collision column contains 2–5 contiguous solid blocks from the Spider's foot level.
- Two blocks of landing headroom above the wall are loaded, collision-free, and fluid-free.
- The target is not materially below the Spider.

In that narrow case the original destination is passed to native Spider navigation. The mod does not set the climbing flag manually, add vertical velocity, teleport, load chunks, or change damage/attack logic. If any condition fails, the existing generic obstacle-detour planner remains in control.

The feature is controlled by `spiderTerrainAdaptationEnabled`, default ON. Turning it OFF restores the prior generic detour behavior.

## Bee: complete observable work cycle

### 1. Trip observation and flower-memory quality

A bounded per-worker memory samples each Bee tick. A foraging trip is considered complete only when a Bee previously observed carrying nectar is later observed empty near the same loaded hive.

The last actually observed flower is credited once on that transition. The previous implementation could reward the same flower repeatedly during multiple 100-tick samples while a Bee was still flying home; the lifecycle transition prevents that overcount.

### 2. Adaptive worker tasks

The shared task memory now has operational effects:

- **FORAGE:** select a fresh, previously observed flower patch using utility, confidence, distance, competition, and danger.
- **GUARD:** observed hostile pressure suppresses new flower recruitment. The separate hive-alarm adapter chooses its deterministic sheltering fraction and returns only selected unladen Bees; it does not assign attack targets.
- **BUILD:** the hive-growth adapter may expand only when the hive is full, honey is full, mobGriefing permits it, a safe adjacent site exists, and the worker's current task is BUILD.
- **REST:** repeated completed trips accumulate a bounded fatigue heuristic. At high fatigue, the Bee clears its optional flower destination and returns to its own hive. A rest request is considered complete only after the Bee disappears from entity ticking and later reappears near that hive. REST yields when the hive is full, so the worker is not trapped trying to enter a hive that cannot accept it.

Task hysteresis remains in place to prevent rapid task switching. After a successful hive expansion, the builder's task is cleared so it can reevaluate the new hive's needs.

### 3. Biological evidence boundary

Honeybee research supports flexible division of labour, colony-demand feedback, recruitment, and interactions among communication signals. It does not justify pretending Minecraft's vanilla Bee API exposes brood counts, pollen stores, or true physiological fatigue.

Therefore:

- Nectar shortage is measured from the vanilla hive honey level.
- Space pressure is measured from actual occupant count.
- Threat is measured from locally observed hostile mobs.
- Brood demand remains zero until a reliable in-game sensor exists; NURSE is not assigned.
- Fatigue is explicitly a game heuristic based on observed completed trips and rest cycles, not a literal biological measurement.

## Performance and safety constraints

- Per-bee lifecycle observation is O(1) and uses bounded memory (4,096 workers per level).
- Flower choice and task reassessment remain on the existing 100-tick cadence.
- Hive growth retains its existing 400-tick cadence and nearby-player guard.
- No global world scans, forced chunk loads, chunk tickets, or persistent per-bee NBT.
- Spider wall sensing checks only one local column, at most six block positions, and only when the front probe is blocked.
- No changes to damage, attack targeting, projectile behavior, Creeper fuse behavior, or vanilla Bee sting behavior.

## Research sources

### Spider locomotion

1. Poerschke, Bastian; Gorb, Stanislav N.; Schaber, Clemens F. (2021). *Adhesion of Individual Attachment Setae of the Spider Cupiennius salei to Substrates With Different Roughness and Surface Energy*. Frontiers in Mechanical Engineering. https://doi.org/10.3389/fmech.2021.702297. The study demonstrates that attachment performance depends on seta morphology and substrate properties; the game design uses this only as inspiration for treating climbing surfaces as a distinct locomotion affordance, not as a literal force model.
2. Wolff, Jonas O.; Gorb, Stanislav N. (2013). *Radial arrangement of Janus-like setae permits friction control in spiders*. Scientific Reports. https://www.nature.com/articles/srep01101. The study describes directional friction and controlled attachment/detachment; the mod's abstraction is a native wall-climb handoff rather than a simulated leg/adhesion model.
3. *Tarantulas use different adhesive pads complementarily during climbing on smooth surfaces* (2016). Peer-reviewed open-access study. https://pmc.ncbi.nlm.nih.gov/articles/PMC4736027/. Evidence supports multiple attachment structures contributing during climbing; this cautions against assuming one simplistic universal climbing mechanism.
4. Minecraft 1.21.1 mapped API: Spider, including `onClimbable`, `isClimbing`, and `setClimbing`: https://mappings.dev/1.21.1/net/minecraft/world/entity/monster/Spider.html.
5. Minecraft 1.21.1 `WallClimberNavigation` API: https://lexxie.dev/neoforge/1.21.1/net/minecraft/world/entity/ai/navigation/WallClimberNavigation.html.

### Bee colony work cycles

6. Hasenjager, Matthew J.; Franks, Victoria R.; Leadbeater, Ellouise (2022). *From dyads to collectives: a review of honeybee signalling*. Behavioral Ecology and Sociobiology. https://doi.org/10.1007/s00265-022-03218-1. The review describes multiple interacting signals and positive/negative feedback loops in colony-level workforce allocation; this supports combining flower recruitment with local demand and threat signals rather than treating the waggle dance as the only input.
7. Schilcher, Felix; Scheiner, Ricarda (2023). *New insight into molecular mechanisms underlying division of labor in honeybees*. Current Opinion in Insect Science. https://doi.org/10.1016/j.cois.2023.101080. Review evidence supports flexible task specialization and behavioral plasticity; the mod uses persistent worker tendencies plus task hysteresis as a coarse game abstraction.
8. De Marco, Rodrigo J. (2006). *How bees tune their dancing according to their colony's nectar influx: re-examining the role of the food-receivers' eagerness*. Journal of Experimental Biology. https://doi.org/10.1242/jeb.02025. Experimental work supports colony nectar influx affecting recruitment thresholds; the mod uses stored honey as an observable demand signal, not as a direct copy of the biological mechanism.
9. *Resilience of honeybee colonies via common stomach: A model of self-regulation of foraging* (2017). Open-access model paper. https://pmc.ncbi.nlm.nih.gov/articles/PMC5697885/. The model links stored resources, recruitment, task switching, and resilience; the game design uses bounded feedback among honey level, worker task, and hive expansion.

## Validation targets

- Unit tests: safe Spider wall-height and headroom gates; successful-return transition; no false trip when nectar is lost away from hive; natural and explicit rest recovery; task selection under fatigue; worker task reset after expansion.
- CI: Java 21 compile, JUnit, Runtime GameTests, Gradle build, custom asset verification, and JAR packaging.
- In-world tests: low wall, 2–5 block wall, wall with blocked headroom, wall taller than limit, target below Spider, Bee completes a flower-to-hive trip, flower confidence increments once, fatigue triggers REST, rest completes on re-emergence, hostile pressure triggers GUARD, and full hive expansion requires BUILD.
