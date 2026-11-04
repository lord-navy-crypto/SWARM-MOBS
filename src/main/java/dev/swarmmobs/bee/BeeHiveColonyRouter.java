package dev.swarmmobs.bee;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import dev.swarmmobs.config.SwarmConfig;

/** Routes idle, unladen overflow Bees to connected vanilla hives with reserved free slots. */
public final class BeeHiveColonyRouter {
    private BeeHiveColonyRouter() {}

    public static boolean routeOverflowBee(ServerLevel level, Bee bee, long now) {
        if (level == null || bee == null || !bee.isAlive() || now < 0
                || !SwarmConfig.ENABLED.get()
                || !SwarmConfig.BEE_CONNECTED_HIVE_GROWTH_ENABLED.get()
                || bee.hasNectar() || bee.getSavedFlowerPos() != null
                || bee.hasStung() || bee.getTarget() != null) return false;

        BlockPos home = bee.getHivePos();
        if (home == null || !level.isInWorldBounds(home) || !level.hasChunkAt(home)
                || bee.distanceToSqr(home.getX() + 0.5, home.getY() + 0.5,
                        home.getZ() + 0.5) > 24.0 * 24.0) return false;

        BeeHiveColonyMemory memory = BeeColonyRuntime.hives(level);
        memory.reconcile(bee.getUUID(), home, now);
        if (memory.hasReservation(bee.getUUID(), now)) return false;

        int maxHives = SwarmConfig.BEE_HIVE_MAX_EXTENSIONS.get() + 1;
        BeeHiveColonySnapshot.Snapshot colony = memory.snapshot(level, home, now, maxHives);
        if (!colony.complete() || colony.overflow() || colony.hives().size() < 2
                || liveOccupants(level, home) < SwarmConfig.BEE_HIVE_CAPACITY.get()) return false;

        var candidates = colony.hives().stream()
                .filter(hive -> !hive.position().equals(home))
                .map(hive -> new Candidate(hive.position(),
                        liveOccupants(level, hive.position())
                                + memory.reservedSlots(hive.position().asLong(), now),
                        bee.distanceToSqr(hive.position().getX() + 0.5,
                                hive.position().getY() + 0.5, hive.position().getZ() + 0.5)))
                .filter(candidate -> candidate.load() >= 0
                        && candidate.load() < SwarmConfig.BEE_HIVE_CAPACITY.get())
                .sorted(Comparator.comparingInt(Candidate::load)
                        .thenComparingDouble(Candidate::distance)
                        .thenComparingLong(candidate -> candidate.position().asLong()))
                .toList();

        for (Candidate candidate : candidates) {
            int occupants = liveOccupants(level, candidate.position());
            if (occupants < 0) continue;
            if (!memory.reserve(bee.getUUID(), candidate.position(), occupants, now)) continue;
            bee.setHivePos(candidate.position());
            return true;
        }
        return false;
    }

    private record Candidate(BlockPos position, int load, double distance) {}

    private static int liveOccupants(ServerLevel level, BlockPos position) {
        return level.getBlockEntity(position) instanceof BeehiveBlockEntity hive
                ? hive.getOccupantCount() : -1;
    }
}
