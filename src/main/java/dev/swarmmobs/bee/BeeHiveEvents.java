package dev.swarmmobs.bee;

import dev.swarmmobs.colony.ColonyPhysicalGrowth;
import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Optional vanilla bee nest/beehive growth. Uses no custom block and
 * never modifies the original block entity occupancy directly.
 */
public final class BeeHiveEvents {
    private static final int GROWTH_INTERVAL_TICKS = 400;
    private BeeHiveEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee)
                || !(bee.level() instanceof ServerLevel level)
                || !bee.isAlive()
                || !SwarmConfig.ENABLED.get()
                || !SwarmConfig.BEE_CONNECTED_HIVE_GROWTH_ENABLED.get()
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;

        BlockPos home = bee.getHivePos();
        if (home == null || !level.hasChunkAt(home)
                || !level.isInWorldBounds(home)
                || Math.floorMod(level.getGameTime() + home.asLong(),
                        GROWTH_INTERVAL_TICKS) != 0) return;

        // Only spend simulation time near real players, without chunk tickets.
        boolean observed = level.players().stream().anyMatch(player ->
                !player.isSpectator() && player.distanceToSqr(
                        home.getX() + .5, home.getY() + .5, home.getZ() + .5)
                        <= 48.0 * 48.0);
        if (!observed) return;

        var parentState = level.getBlockState(home);
        boolean natural = parentState.is(Blocks.BEE_NEST);
        boolean crafted = parentState.is(Blocks.BEEHIVE);
        if (!natural && !crafted) return;
        if (!(level.getBlockEntity(home) instanceof BeehiveBlockEntity hive)
                || !BeeHiveGrowthPolicy.canGrowVanilla(
                        hive.getOccupantCount(), BeehiveBlockEntity.MAX_OCCUPANTS,
                        parentState.getValue(BeehiveBlock.HONEY_LEVEL),
                        BeehiveBlock.MAX_HONEY_LEVELS, !hive.isFireNearby(),
                        true)) return;

        var kind = natural ? ColonyPhysicalGrowth.Kind.BEE_NEST
                : ColonyPhysicalGrowth.Kind.BEEHIVE;
        // No total hive-count cap. One candidate at most per eligible period.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos site = home.relative(direction);
            if (!level.hasChunkAt(site) || !level.isInWorldBounds(site)
                    || !level.getBlockState(site).isAir()
                    || !level.getFluidState(site).isEmpty()
                    || !level.getEntities((net.minecraft.world.entity.Entity) null,
                            new AABB(site), e -> e.isAlive()).isEmpty()) continue;

            if (!ColonyPhysicalGrowth.placeOne(level, home, site, kind)) continue;
            // Honey is the prototype construction resource. New hive remains
            // an ordinary, EMPTY vanilla block entity, not a cloned inventory.
            if (!level.setBlock(home,
                    parentState.setValue(BeehiveBlock.HONEY_LEVEL, 0),
                    net.minecraft.world.level.block.Block.UPDATE_ALL)) {
                level.removeBlock(site, false);
                return;
            }
            // Let the external bee discover the new home immediately; stored
            // bees remain inside the original hive and are not teleported.
            bee.setHivePos(site);
            return;
        }
    }
}
