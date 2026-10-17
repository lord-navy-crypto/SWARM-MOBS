package dev.swarmmobs.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Real in-world birth locations for an active root and one verified extension.
 * Searches a fixed number of local sites per colony cycle, regardless of
 * total connected colony size. Does not spawn, load or create any blocks.
 */
public final class ColonyBirthSitePlanner {
    private static final int[] DX = {2, -2, 0, 0, 2, -2, 2, -2};
    private static final int[] DZ = {0, 0, 2, -2, 2, 2, -2, -2};
    private static final double SAFETY_RADIUS_SQUARED = 12.0 * 12.0;
    private static final double OBSERVED_RADIUS_SQUARED = 48.0 * 48.0;

    private ColonyBirthSitePlanner() {}

    public static BlockPos find(ServerLevel level, BlockPos root,
                                BlockPos verifiedTip, boolean preferTip) {
        if (level == null || root == null) return null;
        if (preferTip && verifiedTip != null) {
            BlockPos remote = findAround(level, verifiedTip, true);
            if (remote != null) return remote;
        }
        BlockPos nearRoot = findAround(level, root, false);
        if (nearRoot != null) return nearRoot;
        return !preferTip && verifiedTip != null
                ? findAround(level, verifiedTip, true) : null;
    }

    private static BlockPos findAround(ServerLevel level, BlockPos anchor,
                                      boolean requirePlayerNearby) {
        if (!level.hasChunkAt(anchor) || !level.isInWorldBounds(anchor)) return null;
        if (requirePlayerNearby && level.players().stream().noneMatch(player ->
                player.isAlive() && !player.isSpectator()
                        && player.distanceToSqr(anchor.getX() + 0.5,
                        anchor.getY() + 0.5, anchor.getZ() + 0.5)
                        <= OBSERVED_RADIUS_SQUARED)) return null;

        for (int i = 0; i < DX.length; i++) {
            BlockPos ground = anchor.offset(DX[i], -1, DZ[i]);
            BlockPos feet = ground.above();
            BlockPos head = feet.above();
            if (!level.hasChunkAt(ground) || !level.hasChunkAt(feet)
                    || !level.hasChunkAt(head) || !level.isInWorldBounds(ground)
                    || !level.isInWorldBounds(head)) continue;
            BlockState below = level.getBlockState(ground);
            if (!level.getBlockState(feet).isAir()
                    || !level.getBlockState(head).isAir()
                    || !below.isFaceSturdy(level, ground, Direction.UP)
                    || !level.getFluidState(feet).isEmpty()
                    || !level.getFluidState(ground).isEmpty()) continue;
            boolean playerClose = level.players().stream().anyMatch(player ->
                    player.isAlive() && !player.isSpectator()
                            && player.distanceToSqr(feet.getX() + 0.5,
                            feet.getY(), feet.getZ() + 0.5)
                            <= SAFETY_RADIUS_SQUARED);
            if (!playerClose) return feet;
        }
        return null;
    }
}
