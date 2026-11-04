package dev.swarmmobs.bee;

import dev.swarmmobs.config.SwarmConfig;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Server-authoritative bee maturation, colony recruitment and nectar-scaled honey bonus. */
public final class BeeEcologyEvents {
    private static final int VANILLA_BABY_AGE_TICKS = 24000;
    private static final int HONEY_BASE_INTERVAL_TICKS = 4800;
    private static final int FLOWER_RADIUS = 8;
    private BeeEcologyEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee)
                || !(bee.level() instanceof ServerLevel level)
                || !bee.isAlive() || !SwarmConfig.ENABLED.get()) return;

        int ageMultiplier = SwarmConfig.BEE_AGE_SPEED_MULTIPLIER.get();
        if (ageMultiplier > 1 && bee.getAge() != 0)
            bee.setAge(BeeEcologyPolicy.acceleratedAge(bee.getAge(), ageMultiplier));

        long now = level.getGameTime();
        BlockPos home = bee.getHivePos();
        if (home == null || !level.isInWorldBounds(home) || !level.hasChunkAt(home)
                || !isVanillaHive(level, home)) return;

        tryBonusHoney(level, bee, home, now);

        if (!SwarmConfig.BEE_COLONY_REPRODUCTION_ENABLED.get()
                || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || bee.hasNectar() || bee.hasStung() || bee.getTarget() != null
                || bee.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5,
                        home.getZ() + 0.5) > 24.0 * 24.0) return;

        boolean observed = level.players().stream().anyMatch(player ->
                !player.isSpectator() && player.distanceToSqr(
                        home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                        <= 48.0 * 48.0);
        if (!observed) return;

        int maxHives = SwarmConfig.BEE_HIVE_MAX_EXTENSIONS.get() + 1;
        BeeHiveColonySnapshot.Snapshot colony = BeeColonyRuntime.hives(level)
                .snapshot(level, home, now, maxHives);
        if (!colony.complete() || colony.overflow() || colony.hives().isEmpty()) return;

        int flowers = countNearbyFlowers(level, colony.root());
        int livePopulation = countLoadedColonyBees(level, colony.positions(), colony.root());
        int population = colony.occupants() + livePopulation;
        int reserve = SwarmConfig.BEE_HIVE_HONEY_RESERVE.get() * colony.hives().size();
        if (!BeeEcologyPolicy.shouldRecruit(population,
                BeeEcologyPolicy.populationLimit(SwarmConfig.BEE_COLONY_MAX_POPULATION.get()),
                colony.storedHoney(), reserve, flowers, true, true)) return;

        int reservePerHive = SwarmConfig.BEE_HIVE_HONEY_RESERVE.get();
        BeeHiveColonySnapshot.Hive source = colony.hives().stream()
                .filter(hive -> hive.honeyLevel() > reservePerHive)
                .max(Comparator.comparingInt(BeeHiveColonySnapshot.Hive::honeyLevel)
                        .thenComparingLong(hive -> -hive.position().asLong()))
                .orElse(null);
        if (source == null) return;

        BlockPos birthSite = findBirthSite(level, source.position());
        if (birthSite == null) return;
        long interval = SwarmConfig.BEE_REPRODUCTION_INTERVAL_TICKS.get();
        if (!BeeColonyRuntime.economy(level).claimBirth(colony.root().asLong(), now, interval)) return;

        Bee newborn = EntityType.BEE.create(level);
        if (newborn == null) return;
        newborn.setPos(birthSite.getX() + 0.5, birthSite.getY(), birthSite.getZ() + 0.5);
        newborn.setAge(-VANILLA_BABY_AGE_TICKS);
        newborn.setHivePos(source.position());
        if (!level.noCollision(newborn)) {
            newborn.discard();
            return;
        }

        var sourceState = level.getBlockState(source.position());
        int honey = sourceState.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honey <= reservePerHive
                || !level.setBlock(source.position(),
                        sourceState.setValue(BeehiveBlock.HONEY_LEVEL, honey - 1),
                        Block.UPDATE_ALL)) {
            newborn.discard();
            return;
        }
        if (!level.addFreshEntity(newborn)) {
            level.setBlock(source.position(), sourceState, Block.UPDATE_ALL);
            newborn.discard();
        }
    }

    private static void tryBonusHoney(ServerLevel level, Bee bee, BlockPos home, long now) {
        if (!SwarmConfig.BEE_HONEY_BONUS_ENABLED.get() || !bee.hasNectar()
                || bee.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5,
                        home.getZ() + 0.5) > 4.5 * 4.5
                || !(level.getBlockEntity(home) instanceof BeehiveBlockEntity hive)) return;
        var state = level.getBlockState(home);
        int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honey >= BeehiveBlock.MAX_HONEY_LEVELS || hive.getOccupantCount() < 1) return;
        int interval = BeeEcologyPolicy.honeyBonusInterval(HONEY_BASE_INTERVAL_TICKS,
                SwarmConfig.BEE_HONEY_PRODUCTION_MULTIPLIER.get(), hive.getOccupantCount());
        if (!BeeColonyRuntime.economy(level).claimHoney(home.asLong(), now, interval)) return;
        level.setBlock(home, state.setValue(BeehiveBlock.HONEY_LEVEL, honey + 1), Block.UPDATE_ALL);
    }

    private static int countLoadedColonyBees(ServerLevel level,
            java.util.List<BlockPos> hives, BlockPos root) {
        Set<Long> positions = new HashSet<>();
        for (BlockPos hive : hives) positions.add(hive.asLong());
        return (int) level.getEntitiesOfClass(Bee.class, new AABB(root).inflate(48.0),
                Bee::isAlive).stream().filter(bee -> bee.getHivePos() != null
                        && positions.contains(bee.getHivePos().asLong())).count();
    }

    private static int countNearbyFlowers(ServerLevel level, BlockPos center) {
        int count = 0;
        for (int dx = -FLOWER_RADIUS; dx <= FLOWER_RADIUS; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -FLOWER_RADIUS; dz <= FLOWER_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (level.hasChunkAt(pos) && level.getBlockState(pos).is(BlockTags.FLOWERS)
                            && ++count >= 8) return count;
                }
        return count;
    }

    private static BlockPos findBirthSite(ServerLevel level, BlockPos hive) {
        for (int radius = 2; radius <= 6; radius++)
            for (int dx = -radius; dx <= radius; dx++)
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    BlockPos site = hive.offset(dx, 1, dz);
                    BlockPos floor = site.below();
                    if (!level.isInWorldBounds(site) || !level.hasChunkAt(site)
                            || !level.getBlockState(site).isAir()
                            || !level.getFluidState(site).isEmpty()
                            || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                            || !level.getEntitiesOfClass(Bee.class, new AABB(site).inflate(1.5),
                                    Bee::isAlive).isEmpty()) continue;
                    return site;
                }
        return null;
    }

    private static boolean isVanillaHive(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.is(net.minecraft.world.level.block.Blocks.BEE_NEST)
                || state.is(net.minecraft.world.level.block.Blocks.BEEHIVE);
    }
}
