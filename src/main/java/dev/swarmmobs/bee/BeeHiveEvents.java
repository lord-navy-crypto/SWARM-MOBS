package dev.swarmmobs.bee;

import dev.swarmmobs.colony.ColonyPhysicalGrowth;
import dev.swarmmobs.config.SwarmConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
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
    private static final int SITE_FLOWER_RADIUS = 4;
    private BeeHiveEvents() {}

    private record Candidate(BlockPos position, double score, int order) {}

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
        List<Candidate> candidates = new ArrayList<>();
        int order = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos site = home.relative(direction);
            if (!isAvailableSite(level, site)) {
                order++;
                continue;
            }
            int flowers = countNearbyFlowers(level, site);
            int hives = countNearbyHives(level, site, home);
            int threats = Math.min(3, level.getEntitiesOfClass(Monster.class,
                    new AABB(site).inflate(8.0), Monster::isAlive).size());
            double score = BeeHiveGrowthPolicy.expansionSiteScore(
                    new BeeHiveGrowthPolicy.SiteSignals(flowers, hives, threats));
            candidates.add(new Candidate(site, score, order++));
        }

        candidates.sort(Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparingInt(Candidate::order));
        for (Candidate candidate : candidates) {
            BlockPos site = candidate.position();
            if (!ColonyPhysicalGrowth.placeOne(level, home, site, kind)) continue;
            // Honey is the construction reserve. The new hive is a normal,
            // EMPTY vanilla block entity, not a cloned inventory.
            if (!level.setBlock(home,
                    parentState.setValue(BeehiveBlock.HONEY_LEVEL, 0),
                    net.minecraft.world.level.block.Block.UPDATE_ALL)) {
                level.removeBlock(site, false);
                return;
            }
            // Let the external bee discover the new home; stored bees remain
            // inside the original hive and are not teleported.
            bee.setHivePos(site);
            return;
        }
    }

    private static boolean isAvailableSite(ServerLevel level, BlockPos site) {
        return level.hasChunkAt(site) && level.isInWorldBounds(site)
                && level.getBlockState(site).isAir()
                && level.getFluidState(site).isEmpty()
                && level.getEntities((Entity) null, new AABB(site),
                        entity -> entity.isAlive()).isEmpty();
    }

    private static int countNearbyFlowers(ServerLevel level, BlockPos center) {
        int count = 0;
        for (int dx = -SITE_FLOWER_RADIUS; dx <= SITE_FLOWER_RADIUS; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -SITE_FLOWER_RADIUS; dz <= SITE_FLOWER_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.hasChunkAt(pos)
                            && level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FLOWERS)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static int countNearbyHives(ServerLevel level, BlockPos center,
            BlockPos parent) {
        int count = 0;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (pos.equals(parent) || !level.hasChunkAt(pos)) continue;
                    var state = level.getBlockState(pos);
                    if (state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE)) count++;
                }
            }
        }
        return count;
    }
}
