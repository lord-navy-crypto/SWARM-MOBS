# Unbounded growth: simulation contract

No arbitrary lifetime max size for an existing connected Nest Core or vanilla
beehive colony. If resource and occupancy conditions keep being met, colony
growth can continue while nearby players keep required chunks ticking.

## Safety and performance distinction
- No **total colony size** cap.
- YES **per-server-tick** scan, path, spawn, placement and message budgets.
- Never synchronously scan every connected block on each colony tick.
- Never force-load chunks outside player-driven loaded regions.
- Only scan an existing nest/core block type; do not include other blocks.
- Use incremental frontier traversal, reevaluated when topology changes.
- Cache membership and aggregate capacity rather than allocating every tick.
- Store one controller per connected colony (future adapter), not an AI controller
  for each extension block; vanilla beehive block entities remain independent.
- Coalescence and splitting must reconcile identity, owner, cached resources,
  population, and saved-state consistency before runtime rollout.
- No auto-building in player-claimed or protected space. Air-only.
- No automatic build in combat: emergency defense overrides ordinary jobs.

## Biology-driven expansion models
Ant trails: positive/negative feedback of pheromone routes, local task response.
Termite stigmergy: new construction follows local built geometry, work clustering
and resource logistics, not a global architect.
Bees: surplus nectar/honey and wax, brood/occupancy demand, decentralized
recruitment, local comb extension and swarming as another growth path.

## Research
- Gordon, D. M. 2019. The Ecology of Collective Behavior in Ants,
  Annual Review of Entomology. doi:10.1146/annurev-ento-011118-111923
- Czaczkes, Grüter & Ratnieks. 2015. Trail Pheromones: An Integrative View,
  Annual Review of Entomology. doi:10.1146/annurev-ento-010814-020627
- Self-organized biotectonics of termite nests. PNAS 2021.
  https://pmc.ncbi.nlm.nih.gov/articles/PMC7865135/
- Architecture of the insect society: comparative analysis of collective
  construction and social function of nests. Insectes Sociaux 2025.
  https://doi.org/10.1007/s00040-025-01057-7

## Status
IncrementalColonyIndex has been committed and JUnit tests added, but runtime
integration/actual world placement, resource accounting, save/reload recovery,
full-scale benchmarking and Minecraft GameTests are still outstanding.
