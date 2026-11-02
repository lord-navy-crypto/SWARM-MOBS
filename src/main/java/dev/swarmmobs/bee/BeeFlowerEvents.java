package dev.swarmmobs.bee;

import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Observation-bounded flower sharing and task execution for vanilla Bees.
 * Vanilla flight, pollination, hive-entry and combat goals remain authoritative.
 * This adapter only changes remembered flower destinations and bounded return-home
 * requests for REST/GUARD workers.
 */
public final class BeeFlowerEvents {
    private static final int OBSERVATION_INTERVAL = 100;
    private static final double MAX_FLOWER_DIST_SQ = 64.0 * 64.0;
    private BeeFlowerEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee)
                || !(bee.level() instanceof ServerLevel level)
                || !SwarmConfig.ENABLED.get()
                || !bee.isAlive()) return;

        boolean flowerRecruitment = SwarmConfig.BEE_FLOWER_RECRUITMENT_ENABLED.get();
        boolean hiveGrowth = SwarmConfig.BEE_CONNECTED_HIVE_GROWTH_ENABLED.get();
        if (!flowerRecruitment && !hiveGrowth) return;

        long now = level.getGameTime();
        BlockPos home = bee.getHivePos();
        if (home == null || !level.hasChunkAt(home) || !isVanillaHive(level, home)) return;

        BlockPos savedFlower = bee.getSavedFlowerPos();
        boolean nearHive = bee.distanceToSqr(
                home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5) <= 12.0 * 12.0;
        BeeForagingCycleMemory.Snapshot cycle = BeeColonyRuntime.cycles(level).observe(
                bee.getUUID(), bee.hasNectar(),
                savedFlower == null ? null : savedFlower.asLong(),
                home.asLong(), nearHive, now);

        // A patch is rewarded once, on the observed nectar-carrying -> empty
        // near-hive transition, not on every 100-tick sample while flying home.
        if (flowerRecruitment && cycle.completedTrip()
                && cycle.completedFlower() != BeeForagingCycleMemory.NO_FLOWER) {
            BeeColonyRuntime.flowers(level).report(
                    cycle.completedHive(), cycle.completedFlower(), 0.8, now);
        }

        if (!flowerRecruitment
                || Math.floorMod(now + bee.getId(), OBSERVATION_INTERVAL) != 0) return;

        if (SwarmConfig.BEE_COLONY_SHELTER_ENABLED.get()
                && BeeColonyRuntime.alarms(level)
                        .active(home.asLong(), now, BeeColonyEvents.ALARM_HOLD_TICKS).isPresent()) {
            return;
        }
        if (level.players().stream().noneMatch(player ->
                !player.isSpectator() && player.distanceToSqr(
                        home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                        <= 48.0 * 48.0)) return;

        // Do not override a loaded nectar delivery, vanilla sting response,
        // or an existing combat target. REST may cancel only the optional
        // saved flower destination after repeated completed trips.
        if (bee.hasNectar() || bee.hasStung() || bee.getTarget() != null) return;

        BeeTaskPolicy.Needs needs = colonyNeeds(level, home,
                cycle.fatigue(), SwarmConfig.BEE_COLONY_SHELTER_ENABLED.get(),
                hiveGrowth);
        BeeTaskPolicy.Task task = BeeColonyRuntime.tasks(level).assign(
                bee.getUUID(), needs, now);

        if (task == BeeTaskPolicy.Task.REST) {
            BeeColonyRuntime.cycles(level).markRestRequested(bee.getUUID(), now);
            bee.setSavedFlowerPos(null);
            bee.setStayOutOfHiveCountdown(0);
            double homeDistanceSquared = bee.distanceToSqr(
                    home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5);
            if (homeDistanceSquared > 2.5 * 2.5
                    && (bee.getNavigation().isDone()
                        || Math.floorMod(now + bee.getId(), 20L) == 0L)) {
                bee.getNavigation().moveTo(
                        home.getX() + 0.5, home.getY() + 0.5,
                        home.getZ() + 0.5, 0.85);
            }
            return;
        }

        // GUARD suppresses new flower recruitment, while BeeColonyEvents
        // independently chooses the sheltering fraction for an active alarm.
        if (task == BeeTaskPolicy.Task.GUARD || savedFlower != null) return;

        // BUILD is executed by BeeHiveEvents when the loaded hive is truly
        // full and has surplus honey. NURSE remains unassigned because vanilla
        // BeehiveBlockEntity exposes no brood sensor.
        if (task != BeeTaskPolicy.Task.FORAGE) return;

        BlockPos candidate = choose(BeeColonyRuntime.flowers(level), level,
                home, bee.blockPosition(), now, needs.nectarShortage());
        if (candidate != null) bee.setSavedFlowerPos(candidate);
    }

    static BeeTaskPolicy.Needs colonyNeeds(ServerLevel level, BlockPos home,
            double fatigue, boolean guardEnabled, boolean buildEnabled) {
        var state = level.getBlockState(home);
        int honey = state.hasProperty(BeehiveBlock.HONEY_LEVEL)
                ? state.getValue(BeehiveBlock.HONEY_LEVEL) : 0;
        int occupants = level.getBlockEntity(home) instanceof BeehiveBlockEntity hive
                ? hive.getOccupantCount() : 0;
        int threats = guardEnabled
                ? level.getEntitiesOfClass(Monster.class,
                        new AABB(home).inflate(10.0), Monster::isAlive).size()
                : 0;
        double nectarShortage = 1.0 - honey / (double) BeehiveBlock.MAX_HONEY_LEVELS;
        double spaceShortage = buildEnabled && hasAdjacentExpansionSite(level, home)
                ? Math.min(1.0, occupants / (double) BeehiveBlockEntity.MAX_OCCUPANTS)
                : 0.0;
        double threat = Math.min(1.0, threats / 3.0);
        // Brood demand is deliberately zero: vanilla exposes no reliable brood
        // count or nursing signal, so the model does not infer it from occupancy.
        return new BeeTaskPolicy.Needs(nectarShortage, 0.0, threat,
                spaceShortage, fatigue);
    }

    private static boolean hasAdjacentExpansionSite(ServerLevel level, BlockPos home) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos site = home.relative(direction);
            if (!level.isInWorldBounds(site) || !level.hasChunkAt(site)
                    || !level.getBlockState(site).isAir()
                    || !level.getFluidState(site).isEmpty()) continue;
            if (level.getEntities((Entity) null, new AABB(site),
                    entity -> entity.isAlive()).isEmpty()) return true;
        }
        return false;
    }

    private static BlockPos choose(BeeFlowerMemory shared, ServerLevel level,
            BlockPos home, BlockPos receiver, long now, double receiverNeed) {
        BlockPos result = selectFrom(shared, level, home, home, receiver, now, receiverNeed);
        if (result != null) return result;
        // Only directly adjacent vanilla hives share observations.
        for (Direction direction : Direction.values()) {
            BlockPos neighborHive = home.relative(direction);
            if (level.hasChunkAt(neighborHive) && isVanillaHive(level, neighborHive)) {
                result = selectFrom(shared, level, neighborHive, home, receiver, now,
                        receiverNeed);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static BlockPos selectFrom(BeeFlowerMemory shared, ServerLevel level,
            BlockPos rememberedBy, BlockPos home, BlockPos receiver, long now,
            double receiverNeed) {
        BlockPos bestPosition = null;
        double bestScore = 0.05;
        for (BeeFlowerMemory.FlowerObservation observation
                : shared.observations(rememberedBy.asLong(), now)) {
            BlockPos flower = BlockPos.of(observation.flower());
            if (!level.hasChunkAt(flower)) continue;
            if (!isValidFlower(level, home, flower)) {
                shared.forget(rememberedBy.asLong(), flower.asLong());
                continue;
            }
            double distance = Math.sqrt(receiver.distSqr(flower));
            if (distance * distance > MAX_FLOWER_DIST_SQ) continue;

            int nearbyBees = level.getEntitiesOfClass(Bee.class,
                    new AABB(flower).inflate(6.0), Bee::isAlive).size();
            int nearbyThreats = level.getEntitiesOfClass(Monster.class,
                    new AABB(flower).inflate(8.0), Monster::isAlive).size();
            double competition = Math.min(1.0, Math.max(0, nearbyBees - 1) / 4.0);
            double danger = Math.min(1.0, nearbyThreats / 3.0);
            var patch = new BeeForagePolicy.Patch(
                    observation.utility(), observation.utility(), distance,
                    competition, danger, observation.confidence());
            double score = BeeForagePolicy.score(patch);
            double recruitmentPriority = BeeRecruitmentPolicy.priority(
                    score, observation.confidence(), receiverNeed, competition);
            if (recruitmentPriority > bestScore || (recruitmentPriority == bestScore
                    && (bestPosition == null || flower.asLong() < bestPosition.asLong()))) {
                bestScore = recruitmentPriority;
                bestPosition = flower;
            }
        }
        return bestPosition;
    }

    private static boolean isValidFlower(ServerLevel level, BlockPos home,
            BlockPos flower) {
        return level.isInWorldBounds(flower)
                && level.hasChunkAt(flower)
                && home.distSqr(flower) <= MAX_FLOWER_DIST_SQ
                && level.getBlockState(flower).is(BlockTags.FLOWERS);
    }

    private static boolean isVanillaHive(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos)
                && (level.getBlockState(pos).is(Blocks.BEE_NEST)
                    || level.getBlockState(pos).is(Blocks.BEEHIVE));
    }
}
