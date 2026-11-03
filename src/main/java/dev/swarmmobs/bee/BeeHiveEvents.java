package dev.swarmmobs.bee;

import dev.swarmmobs.colony.ColonyPhysicalGrowth;
import dev.swarmmobs.config.SwarmConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Physical growth for connected vanilla hives; every block keeps native occupancy and honey. */
public final class BeeHiveEvents {
    private static final int GROWTH_INTERVAL_TICKS = 400;
    private static final int SITE_FLOWER_RADIUS = 4;
    private static final int WAX_RADIUS = BeeWaxStockpile.DEFAULT_RADIUS;
    private BeeHiveEvents() {}

    private record Candidate(BlockPos parent, BlockPos position, double score, int order) {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee)
                || !(bee.level() instanceof ServerLevel level)
                || !bee.isAlive() || !SwarmConfig.ENABLED.get()
                || !SwarmConfig.BEE_CONNECTED_HIVE_GROWTH_ENABLED.get()
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        BlockPos home = bee.getHivePos();
        if (home == null || !level.isInWorldBounds(home) || !level.hasChunkAt(home)) return;
        long now = level.getGameTime();
        if (Math.floorMod(now + home.asLong(), GROWTH_INTERVAL_TICKS) != 0) return;
        tryExpandIfEligible(level, bee, now);
    }

    public static boolean tryExpandIfEligible(ServerLevel level, Bee bee) {
        return level != null && bee != null && tryExpandIfEligible(level, bee, level.getGameTime());
    }

    static boolean tryExpandIfEligible(ServerLevel level, Bee bee, long now) {
        if (level == null || bee == null || !bee.isAlive() || now < 0
                || !SwarmConfig.ENABLED.get()
                || !SwarmConfig.BEE_CONNECTED_HIVE_GROWTH_ENABLED.get()
                || !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                || bee.hasNectar() || bee.getSavedFlowerPos() != null
                || bee.hasStung() || bee.getTarget() != null) return false;

        BlockPos home = bee.getHivePos();
        if (home == null || !level.isInWorldBounds(home) || !level.hasChunkAt(home)
                || bee.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5,
                        home.getZ() + 0.5) > 32.0 * 32.0) return false;

        int maxExtensions = SwarmConfig.BEE_HIVE_MAX_EXTENSIONS.get();
        BeeHiveColonyMemory hiveMemory = BeeColonyRuntime.hives(level);
        BeeHiveColonySnapshot.Snapshot colony = hiveMemory.snapshot(level, home, now, maxExtensions + 1);
        if (!colony.complete() || colony.overflow() || colony.hives().isEmpty()) return false;
        List<Long> colonyKeys = colony.positions().stream().map(BlockPos::asLong).toList();

        boolean observed = colony.positions().stream().anyMatch(position ->
                level.players().stream().anyMatch(player -> !player.isSpectator()
                        && player.distanceToSqr(position.getX() + 0.5, position.getY() + 0.5,
                                position.getZ() + 0.5) <= 48.0 * 48.0));
        if (!observed) return false;

        if (SwarmConfig.BEE_COLONY_SHELTER_ENABLED.get()) {
            for (BlockPos position : colony.positions()) {
                if (BeeColonyRuntime.alarms(level).active(position.asLong(), now,
                        BeeColonyEvents.ALARM_HOLD_TICKS).isPresent()) return false;
            }
        }

        BeeHiveGrowthMemory growthMemory = BeeColonyRuntime.growth(level);

        int freeSites = countAvailableSites(level, colony);
        int wax = BeeWaxStockpile.count(level, colony.positions(), WAX_RADIUS);
        int reservePerHive = SwarmConfig.BEE_HIVE_HONEY_RESERVE.get();
        int winterReserve = Math.multiplyExact(reservePerHive, colony.hives().size());
        int waxCost = SwarmConfig.BEE_HIVE_WAX_COST.get();
        BeeHiveGrowthPolicy.State state = new BeeHiveGrowthPolicy.State(
                colony.occupants(), colony.capacity(), colony.storedHoney(), winterReserve,
                wax, waxCost, freeSites, colony.extensions(), maxExtensions);
        if (!BeeHiveGrowthPolicy.canExpand(state)) return false;

        double fatigue = BeeColonyRuntime.cycles(level).fatigue(bee.getUUID(), now);
        BeeTaskPolicy.Needs needs = BeeFlowerEvents.colonyNeeds(level, home, fatigue,
                SwarmConfig.BEE_COLONY_SHELTER_ENABLED.get(), true);
        if (BeeColonyRuntime.tasks(level).assign(bee.getUUID(), home.asLong(), needs, now)
                != BeeTaskPolicy.Task.BUILD) return false;
        // Claim only after selecting a real BUILD worker; FORAGE/REST workers
        // must not suppress another worker's same-tick construction attempt.
        if (!growthMemory.claimColony(colonyKeys, now,
                SwarmConfig.BEE_HIVE_GROWTH_COOLDOWN_TICKS.get())) return false;

        List<Candidate> candidates = findCandidates(level, colony);
        if (candidates.isEmpty()) return false;
        BeeHiveColonySnapshot.Hive honeySource = colony.hives().stream()
                .filter(hive -> hive.honeyLevel() > reservePerHive)
                .sorted(Comparator.comparingInt(BeeHiveColonySnapshot.Hive::honeyLevel)
                        .reversed().thenComparingLong(hive -> hive.position().asLong()))
                .findFirst().orElse(null);
        if (honeySource == null) return false;

        for (Candidate candidate : candidates) {
            BlockPos site = candidate.position();
            if (!isAvailableSite(level, site)) continue;
            var parentState = level.getBlockState(candidate.parent());
            ColonyPhysicalGrowth.Kind kind = parentState.is(Blocks.BEE_NEST)
                    ? ColonyPhysicalGrowth.Kind.BEE_NEST : ColonyPhysicalGrowth.Kind.BEEHIVE;
            if (!ColonyPhysicalGrowth.placeOne(level, candidate.parent(), site, kind)) continue;

            BlockPos sourcePos = honeySource.position();
            var sourceState = level.getBlockState(sourcePos);
            int currentHoney = sourceState.getValue(BeehiveBlock.HONEY_LEVEL);
            if (currentHoney <= reservePerHive
                    || !level.setBlock(sourcePos,
                            sourceState.setValue(BeehiveBlock.HONEY_LEVEL, currentHoney - 1),
                            Block.UPDATE_ALL)) {
                level.removeBlock(site, false);
                return false;
            }
            if (!BeeWaxStockpile.consume(level, colony.positions(), WAX_RADIUS, waxCost)) {
                level.setBlock(sourcePos, sourceState, Block.UPDATE_ALL);
                level.removeBlock(site, false);
                return false;
            }

            growthMemory.recordExpansion(colonyKeys, now);
            hiveMemory.invalidate(colony);
            bee.setHivePos(site);
            bee.setSavedFlowerPos(null);
            BeeColonyRuntime.tasks(level).clear(bee.getUUID());
            hiveMemory.reserve(bee.getUUID(), site, 0, now);
            return true;
        }
        return false;
    }

    static boolean hasAvailableExpansionSite(ServerLevel level,
            BeeHiveColonySnapshot.Snapshot colony) {
        return colony != null && colony.complete() && countAvailableSites(level, colony) > 0;
    }

    private static int countAvailableSites(ServerLevel level, BeeHiveColonySnapshot.Snapshot colony) {
        Set<Long> sites = new HashSet<>();
        for (BlockPos parent : colony.positions())
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos site = parent.relative(direction);
                if (isAvailableSite(level, site)) sites.add(site.asLong());
            }
        return sites.size();
    }

    private static List<Candidate> findCandidates(ServerLevel level,
            BeeHiveColonySnapshot.Snapshot colony) {
        Map<Long, Candidate> unique = new HashMap<>();
        int order = 0;
        for (BlockPos parent : colony.positions())
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos site = parent.relative(direction);
                if (!isAvailableSite(level, site)) { order++; continue; }
                int flowers = countNearbyFlowers(level, site);
                int nearbyHives = countNearbyHives(level, site);
                int threats = Math.min(3, level.getEntitiesOfClass(Monster.class,
                        new AABB(site).inflate(8.0), Monster::isAlive).size());
                double score = BeeHiveGrowthPolicy.expansionSiteScore(
                        new BeeHiveGrowthPolicy.SiteSignals(flowers, nearbyHives, threats));
                Candidate candidate = new Candidate(parent.immutable(), site.immutable(), score, order++);
                Candidate previous = unique.get(site.asLong());
                if (previous == null || candidate.score() > previous.score()
                        || (candidate.score() == previous.score()
                            && candidate.parent().asLong() < previous.parent().asLong()))
                    unique.put(site.asLong(), candidate);
            }
        List<Candidate> result = new ArrayList<>(unique.values());
        result.sort(Comparator.comparingDouble(Candidate::score).reversed()
                .thenComparingInt(Candidate::order)
                .thenComparingLong(candidate -> candidate.position().asLong()));
        return result;
    }

    private static boolean isAvailableSite(ServerLevel level, BlockPos site) {
        return level.isInWorldBounds(site) && level.hasChunkAt(site)
                && level.getBlockState(site).isAir() && level.getFluidState(site).isEmpty()
                && level.getEntities((Entity) null, new AABB(site),
                        entity -> entity.isAlive()).isEmpty();
    }

    private static int countNearbyFlowers(ServerLevel level, BlockPos center) {
        int count = 0;
        for (int dx = -SITE_FLOWER_RADIUS; dx <= SITE_FLOWER_RADIUS; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -SITE_FLOWER_RADIUS; dz <= SITE_FLOWER_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.hasChunkAt(pos)
                            && level.getBlockState(pos).is(net.minecraft.tags.BlockTags.FLOWERS)) count++;
                }
        return count;
    }

    private static int countNearbyHives(ServerLevel level, BlockPos center) {
        int count = 0;
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(pos) || !level.hasChunkAt(pos)) continue;
                    var state = level.getBlockState(pos);
                    if (state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE)) count++;
                }
        return count;
    }
}
