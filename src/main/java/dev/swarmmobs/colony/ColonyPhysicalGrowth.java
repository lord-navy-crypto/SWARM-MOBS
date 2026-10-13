package dev.swarmmobs.colony;

import dev.swarmmobs.registry.SwarmNestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Opt-in, server-only same-block physical growth bridge.
 * Callers handle membership, resource reservation, cooldown and ownership.
 * Never force-load a chunk or overwrite an existing block.
 */
public final class ColonyPhysicalGrowth {
    private ColonyPhysicalGrowth() {}

    public enum Kind { MONSTER, BEE_NEST, BEEHIVE }

    public static boolean placeOne(ServerLevel level, BlockPos parent, BlockPos target,
                                   Kind kind) {
        if (level == null || parent == null || target == null || kind == null)
            return false;
        int faceDistance = Math.abs(parent.getX() - target.getX())
                + Math.abs(parent.getY() - target.getY())
                + Math.abs(parent.getZ() - target.getZ());
        if (faceDistance != 1) return false; // six-face neighbor only
        if (!level.isInWorldBounds(parent) || !level.isInWorldBounds(target)
                || !level.hasChunkAt(parent) || !level.hasChunkAt(target))
            return false;

        BlockState parentState = level.getBlockState(parent);
        Block source = switch (kind) {
            case MONSTER -> SwarmNestBlocks.NEST_CORE.get();
            case BEE_NEST -> Blocks.BEE_NEST;
            case BEEHIVE -> Blocks.BEEHIVE;
        };
        if (!parentState.is(source)) return false;
        BlockPos owner = parent;
        if (kind == Kind.MONSTER) {
            if (!(level.getBlockEntity(parent) instanceof SwarmNestBlockEntity parentCore))
                return false;
            owner = parentCore.connectedRoot();
            // Do not grow from an orphaned or unloaded root.
            if (owner == null || !level.hasChunkAt(owner)
                    || !(level.getBlockEntity(owner) instanceof SwarmNestBlockEntity root)
                    || root.isConnectedExtension()) return false;
        }
        if (!level.getBlockState(target).isAir()) return false;
        if (kind == Kind.MONSTER) {
            for (var direction : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = target.relative(direction);
                if (!level.hasChunkAt(neighbor)) return false;
                if (!level.getBlockState(neighbor).is(SwarmNestBlocks.NEST_CORE.get()))
                    continue;
                if (!(level.getBlockEntity(neighbor) instanceof SwarmNestBlockEntity adjacent)
                        || !owner.equals(adjacent.connectedRoot())) return false;
            }
        }
        // The target block entity (including a vanilla hive's real inventory)
        // is created by the regular Minecraft block placement mechanism.
        BlockState newState = source.defaultBlockState();
        if (kind != Kind.MONSTER && newState.hasProperty(
                net.minecraft.world.level.block.BeehiveBlock.FACING)
                && parentState.hasProperty(net.minecraft.world.level.block.BeehiveBlock.FACING)) {
            newState = newState.setValue(net.minecraft.world.level.block.BeehiveBlock.FACING,
                    parentState.getValue(net.minecraft.world.level.block.BeehiveBlock.FACING));
        }
        if (!level.setBlock(target, newState, Block.UPDATE_ALL)) return false;
        if (kind == Kind.MONSTER) {
            if (level.getBlockEntity(target) instanceof SwarmNestBlockEntity extension) {
                extension.markConnectedExtension(owner);
            } else {
                // Never leave an unmarked, independently ticking core behind.
                level.removeBlock(target, false);
                return false;
            }
        }
        return true;
    }
}
