package dev.swarmmobs.bee;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import dev.swarmmobs.config.SwarmConfig;

/** Bounded face-connected snapshot of vanilla BEE_NEST/BEEHIVE block entities. */
public final class BeeHiveColonySnapshot {
    private BeeHiveColonySnapshot() {}

    public record Hive(BlockPos position, int occupants, int honeyLevel, boolean natural) {
        public Hive {
            position = position.immutable();
            if (occupants < 0 || honeyLevel < 0 || honeyLevel > BeehiveBlock.MAX_HONEY_LEVELS)
                throw new IllegalArgumentException("invalid hive observation");
        }
    }

    public record Snapshot(List<Hive> hives, int occupants, int capacity,
            int storedHoney, boolean complete, boolean overflow) {
        public Snapshot {
            hives = List.copyOf(hives);
            if (occupants < 0 || capacity < 0 || storedHoney < 0)
                throw new IllegalArgumentException("negative colony observation");
        }
        public List<BlockPos> positions() { return hives.stream().map(Hive::position).toList(); }
        public int extensions() { return Math.max(0, hives.size() - 1); }
        public BlockPos root() { return hives.isEmpty() ? null : hives.get(0).position(); }
        public Hive at(BlockPos position) {
            if (position == null) return null;
            return hives.stream().filter(hive -> hive.position().equals(position))
                    .findFirst().orElse(null);
        }
    }

    public static Snapshot scan(ServerLevel level, BlockPos origin, int maxHives) {
        if (level == null || origin == null || maxHives < 1 || maxHives > 33)
            throw new IllegalArgumentException("invalid hive scan request");
        if (!level.isInWorldBounds(origin) || !level.hasChunkAt(origin)
                || !isVanillaHive(level.getBlockState(origin)))
            return new Snapshot(List.of(), 0, 0, 0, false, false);

        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        List<Hive> hives = new ArrayList<>();
        pending.add(origin.immutable());
        boolean complete = true;
        boolean overflow = false;
        int totalOccupants = 0;
        int totalHoney = 0;

        while (!pending.isEmpty()) {
            BlockPos current = pending.removeFirst();
            if (!visited.add(current.asLong())) continue;
            if (!level.isInWorldBounds(current) || !level.hasChunkAt(current)) {
                complete = false;
                continue;
            }
            var state = level.getBlockState(current);
            if (!isVanillaHive(state)) continue;
            if (!(level.getBlockEntity(current) instanceof BeehiveBlockEntity be)) {
                complete = false;
                continue;
            }
            if (hives.size() >= maxHives) {
                complete = false;
                overflow = true;
                break;
            }

            int occupants = be.getOccupantCount();
            int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
            hives.add(new Hive(current, occupants, honey, state.is(Blocks.BEE_NEST)));
            totalOccupants = Math.addExact(totalOccupants, occupants);
            totalHoney = Math.addExact(totalHoney, honey);

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!level.isInWorldBounds(neighbor)) continue;
                if (!level.hasChunkAt(neighbor)) {
                    // Unknown unloaded boundaries fail closed for growth/routing.
                    complete = false;
                    continue;
                }
                if (isVanillaHive(level.getBlockState(neighbor))
                        && !visited.contains(neighbor.asLong()))
                    pending.addLast(neighbor.immutable());
            }
        }

        hives.sort(Comparator.comparingInt((Hive h) -> h.position().getY())
                .thenComparingInt(h -> h.position().getX())
                .thenComparingInt(h -> h.position().getZ()));
        int capacity = Math.multiplyExact(hives.size(), SwarmConfig.BEE_HIVE_CAPACITY.get());
        return new Snapshot(hives, totalOccupants, capacity, totalHoney, complete, overflow);
    }

    private static boolean isVanillaHive(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.BEE_NEST) || state.is(Blocks.BEEHIVE);
    }
}
