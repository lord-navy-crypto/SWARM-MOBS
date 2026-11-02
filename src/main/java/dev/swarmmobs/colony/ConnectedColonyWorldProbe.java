package dev.swarmmobs.colony;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Read-only Minecraft bridge for connected colony layout.
 * No chunk loads, world mutation, resource deductions or new block registrations.
 */
public final class ConnectedColonyWorldProbe {
    private ConnectedColonyWorldProbe() {}

    public static ConnectedColonyLayout.Colony scanMonsterCore(
            ServerLevel level, BlockPos origin, Block existingNestCore,
            int maxBlocks, int capacityPerBlock) {
        if (existingNestCore == null) throw new IllegalArgumentException("nest core");
        return scan(level, origin, state -> state.is(existingNestCore),
                maxBlocks, capacityPerBlock);
    }

    public static ConnectedColonyLayout.Colony scanVanillaBeehives(
            ServerLevel level, BlockPos origin, int maxBlocks,
            int capacityPerBlock) {
        return scan(level, origin,
                state -> state.is(Blocks.BEEHIVE) || state.is(Blocks.BEE_NEST),
                maxBlocks, capacityPerBlock);
    }

    private static ConnectedColonyLayout.Colony scan(
            ServerLevel level, BlockPos origin, Predicate<BlockState> matches,
            int maxBlocks, int capacityPerBlock) {
        if (level == null || origin == null || matches == null)
            throw new IllegalArgumentException("null world probe inputs");
        return ConnectedColonyLayout.scan(
                new ConnectedColonyLayout.Cell(origin.getX(), origin.getY(), origin.getZ()),
                cell -> {
                    BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
                    return level.hasChunkAt(pos) && level.isInWorldBounds(pos)
                            && matches.test(level.getBlockState(pos));
                },
                maxBlocks, capacityPerBlock);
    }

    /**
     * Candidate enumeration only. Must revalidate on the server tick when a
     * construction task commits; excludes unloaded positions and non-air.
     */
    public static java.util.Optional<BlockPos> nextAirExtension(
            ServerLevel level, ConnectedColonyLayout.Colony colony) {
        if (level == null || colony == null)
            throw new IllegalArgumentException("null extension probe inputs");
        return ConnectedColonyLayout.nextExtension(colony, cell -> {
            BlockPos pos = new BlockPos(cell.x(), cell.y(), cell.z());
            return level.hasChunkAt(pos) && level.isInWorldBounds(pos)
                    && level.getBlockState(pos).isAir();
        }).map(cell -> new BlockPos(cell.x(), cell.y(), cell.z()));
    }
}
