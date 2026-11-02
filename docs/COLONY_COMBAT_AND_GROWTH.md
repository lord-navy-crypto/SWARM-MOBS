# Emergency combat and expandable colony design

## Monster nest emergency doctrine
Every monster's ordinary nest mission (patrol, gather, build, repair, defend, expand)
is PREEMPTED by combat. A locally observed player or nest damage triggers COMBAT.
Neighbor alert messages may put other members into MOBILIZING; the existing local
communication system must relay alerts across the colony. There is no instantaneous
worldwide telepathy or unobserved position tracking. SEARCH uses bounded last-known
information, followed by RECOVER and WORK after a configurable calm period.

All member jobs must yield to this emergency state at the future runtime adapter.
This explicitly includes workers/builders/haulers, but the existing vanilla combat
handoff must remain intact (Skeleton bows, Creeper swell, Zombie/Spider melee).
A target found during existing combat must not be overwritten by a weak or stale alert.

## Monster nest growth
The existing Nest Core block remains the anchor. When member capacity and
resource inventory are BOTH full, the colony may attempt bounded expansion.
The candidate must be loaded, permitted, replaceable, within the nest radius,
and not inside player protected blocks or another colony. The controller must
consume resources and place a registered extension block atomically; only
successful placement may increase capacity. Do not destroy neighboring blocks
to make room. Cooldowns, limits, ownership and persistence are required.

## Bee hive growth
Keep VANILLA bee nests/beehives as the hive anchor and preserve honey_level,
bottle/shears/campfire and ordinary bee mechanics. Expansion is OPTIONAL via
separate, explicitly registered adjacent modules (not automatic replacement of
vanilla hive blocks). A packed colony with wax and honey above a survival reserve
may attempt growth. Never write over vanilla hives or unrelated blocks.
Do not assume an expanded hive actually raises vanilla BeeHiveBlockEntity's
hard-coded internal occupancy: runtime work must either safely adapt occupancy
or use managed overflow/extension hive blocks. The pure gate does not do this yet.

## Scope of current commit
ColonyAlertPolicy, ColonyGrowthPolicy and BeeHiveGrowthPolicy are policy-only.
The Java JUnit tests cover state priority and eligibility. No existing combat
runtime, Nest block or vanilla hive runtime has been modified yet.
Build and GameTest are not executed here. PR stays draft until integration and
runtime verification.
