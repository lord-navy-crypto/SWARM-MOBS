package dev.swarmmobs.bee;

import dev.swarmmobs.config.SwarmConfig;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Opt-in, no-new-goal version of honeybee flower communication.
 * Successful nectar foragers publish ONLY the real vanilla remembered flower.
 * Receivers without their own flower memory may adopt a fresh local sighting;
 * after that, vanilla Bee movement/pollination AI remains in charge.
 */
public final class BeeFlowerEvents {
    private static final int OBSERVATION_INTERVAL = 100;
    private static final double MAX_FLOWER_DIST_SQ = 64.0 * 64.0;
    private static final Map<ServerLevel, BeeFlowerMemory> MEMORY = new WeakHashMap<>();

    private BeeFlowerEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee)
                || !(bee.level() instanceof ServerLevel level)
                || !SwarmConfig.ENABLED.get()
                || !SwarmConfig.BEE_FLOWER_RECRUITMENT_ENABLED.get()
                || !bee.isAlive()) return;
        long now = level.getGameTime();
        if (Math.floorMod(now + bee.getId(), OBSERVATION_INTERVAL) != 0)
            return;
        BlockPos home = bee.getHivePos();
        if (home == null || !level.hasChunkAt(home)
                || !isVanillaHive(level, home)) return;
        if (level.players().stream().noneMatch(player ->
                !player.isSpectator() && player.distanceToSqr(
                        home.getX() + .5, home.getY() + .5, home.getZ() + .5)
                        <= 48.0 * 48.0)) return;

        BeeFlowerMemory shared = MEMORY.computeIfAbsent(
                level, unused -> new BeeFlowerMemory());
        BlockPos observedFlower = bee.getSavedFlowerPos();
        if (bee.hasNectar() && observedFlower != null
                && isValidFlower(level, home, observedFlower)) {
            // This is a real observed patch, not an artificial reward source.
            shared.report(home.asLong(), observedFlower.asLong(), 0.8, now);
            return;
        }
        // Never overwrite a Bee's own remembered flower or forcibly route it.
        if (bee.hasNectar() || observedFlower != null) return;

        BlockPos candidate = choose(shared, level, home, bee.blockPosition(), now);
        if (candidate != null) bee.setSavedFlowerPos(candidate);
    }

    private static BlockPos choose(BeeFlowerMemory shared, ServerLevel level,
                                   BlockPos home, BlockPos receiver, long now) {
        BlockPos result = selectFrom(shared, level, home, home, receiver, now);
        if (result != null) return result;
        // Only directly connected vanilla hives share flower observations.
        for (Direction direction : Direction.values()) {
            BlockPos neighborHive = home.relative(direction);
            if (level.hasChunkAt(neighborHive)
                    && isVanillaHive(level, neighborHive)) {
                result = selectFrom(shared, level, neighborHive, home, receiver, now);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static BlockPos selectFrom(BeeFlowerMemory shared, ServerLevel level,
                                       BlockPos rememberedBy, BlockPos home,
                                       BlockPos receiver, long now) {
        var suggestion = shared.suggest(rememberedBy.asLong(), now);
        if (suggestion.isEmpty()) return null;
        BlockPos pos = BlockPos.of(suggestion.getAsLong());
        if (receiver.distSqr(pos) > MAX_FLOWER_DIST_SQ
                || !isValidFlower(level, home, pos)) {
            // Out-of-range patches might still help another nearby worker:
            // discard only when the flower is actually invalid, not distant.
            if (level.hasChunkAt(pos)
                    && !level.getBlockState(pos).is(BlockTags.FLOWERS))
                shared.forget(rememberedBy.asLong(), pos.asLong());
            return null;
        }
        return pos;
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
