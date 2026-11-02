package dev.swarmmobs.bee;

import dev.swarmmobs.config.SwarmConfig;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Observation-bounded flower sharing for vanilla Bees.
 * The memory can rank known patches, but vanilla flight/pollination goals
 * remain authoritative. No remote chunk scans or forced path navigation.
 */
public final class BeeFlowerEvents {
    private static final int OBSERVATION_INTERVAL = 100;
    private static final double MAX_FLOWER_DIST_SQ = 64.0 * 64.0;
    private static final Map<ServerLevel, BeeFlowerMemory> MEMORY = new WeakHashMap<>();
    private static final Map<ServerLevel, BeeTaskMemory> TASKS = new WeakHashMap<>();

    private BeeFlowerEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee)
                || !(bee.level() instanceof ServerLevel level)
                || !SwarmConfig.ENABLED.get()
                || !SwarmConfig.BEE_FLOWER_RECRUITMENT_ENABLED.get()
                || !bee.isAlive()) return;
        long now = level.getGameTime();
        if (Math.floorMod(now + bee.getId(), OBSERVATION_INTERVAL) != 0) return;

        BlockPos home = bee.getHivePos();
        if (home == null || !level.hasChunkAt(home) || !isVanillaHive(level, home)) return;
        if (SwarmConfig.BEE_COLONY_SHELTER_ENABLED.get()
                && BeeColonyRuntime.alarms(level)
                        .active(home.asLong(), now, BeeColonyEvents.ALARM_HOLD_TICKS).isPresent()) return;
        if (level.players().stream().noneMatch(player ->
                !player.isSpectator() && player.distanceToSqr(
                        home.getX() + .5, home.getY() + .5, home.getZ() + .5)
                        <= 48.0 * 48.0)) return;

        BeeFlowerMemory shared = MEMORY.computeIfAbsent(
                level, unused -> new BeeFlowerMemory());
        BeeTaskMemory tasks = TASKS.computeIfAbsent(
                level, unused -> new BeeTaskMemory());

        BlockPos observedFlower = bee.getSavedFlowerPos();
        if (bee.hasNectar() && observedFlower != null
                && isValidFlower(level, home, observedFlower)) {
            // Repeated successful returns improve confidence in a real patch.
            shared.report(home.asLong(), observedFlower.asLong(), 0.8, now);
            return;
        }
        if (bee.hasNectar() || observedFlower != null) return;

        BeeTaskPolicy.Needs needs = colonyNeeds(level, home);
        BeeTaskPolicy.Task task = tasks.assign(bee.getUUID(), needs, now);
        // GUARD/BUILD/NURSE/REST assignments suppress recruitment only;
        // they do not replace or cancel vanilla Bee goals.
        if (task != BeeTaskPolicy.Task.FORAGE) return;

        BlockPos candidate = choose(shared, level, home, bee.blockPosition(), now,
                needs.nectarShortage());
        if (candidate != null) bee.setSavedFlowerPos(candidate);
    }

    private static BeeTaskPolicy.Needs colonyNeeds(ServerLevel level, BlockPos home) {
        var state = level.getBlockState(home);
        int honey = state.hasProperty(BeehiveBlock.HONEY_LEVEL)
                ? state.getValue(BeehiveBlock.HONEY_LEVEL) : 0;
        int occupants = level.getBlockEntity(home) instanceof BeehiveBlockEntity hive
                ? hive.getOccupantCount() : 0;
        int threats = level.getEntitiesOfClass(Monster.class,
                new AABB(home).inflate(10.0), Monster::isAlive).size();
        double nectarShortage = 1.0 - honey / (double) BeehiveBlock.MAX_HONEY_LEVELS;
        double spaceShortage = Math.min(1.0,
                occupants / (double) BeehiveBlockEntity.MAX_OCCUPANTS);
        double threat = Math.min(1.0, threats / 3.0);
        // Vanilla Bee exposes no reliable brood or fatigue sensor here.
        // Those signals remain zero rather than being guessed from occupancy.
        return new BeeTaskPolicy.Needs(nectarShortage, 0.0, threat,
                spaceShortage, 0.0);
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
