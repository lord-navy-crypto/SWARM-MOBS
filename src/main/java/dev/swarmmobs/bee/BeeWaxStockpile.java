package dev.swarmmobs.bee;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** Uses explicitly dropped vanilla honeycomb as wax; never reads player inventories or containers. */
public final class BeeWaxStockpile {
    public static final int DEFAULT_RADIUS = 8;
    public static final int MAX_TRACKED_ITEM_ENTITIES = 256;
    public static final int MAX_COUNTED_HONEYCOMB = 4096;
    private BeeWaxStockpile() {}

    public static int count(ServerLevel level, List<BlockPos> hives, int radius) {
        return collect(level, hives, radius).available();
    }

    public static boolean consume(ServerLevel level, List<BlockPos> hives, int radius, int amount) {
        if (amount < 1) throw new IllegalArgumentException("amount must be positive");
        Stockpile stockpile = collect(level, hives, radius);
        if (stockpile.available() < amount) return false;
        int remaining = amount;
        for (ItemEntity entity : stockpile.items()) {
            if (!entity.isAlive() || !entity.getItem().is(Items.HONEYCOMB)) continue;
            ItemStack stack = entity.getItem().copy();
            int taken = Math.min(remaining, stack.getCount());
            stack.shrink(taken);
            if (stack.isEmpty()) entity.discard();
            else entity.setItem(stack);
            remaining -= taken;
            if (remaining == 0) return true;
        }
        return false;
    }

    private record Stockpile(List<ItemEntity> items, int available) {}

    private static Stockpile collect(ServerLevel level, List<BlockPos> hives, int radius) {
        if (level == null || hives == null || radius < 1 || radius > 16)
            throw new IllegalArgumentException("invalid wax stockpile query");
        Set<UUID> seen = new HashSet<>();
        List<ItemEntity> items = new ArrayList<>();
        int available = 0;
        for (BlockPos hive : hives) {
            if (hive == null || !level.isInWorldBounds(hive) || !level.hasChunkAt(hive)) continue;
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(hive).inflate(radius),
                    entity -> entity.isAlive() && entity.getItem().is(Items.HONEYCOMB))) {
                if (!seen.add(item.getUUID())) continue;
                items.add(item);
                available = Math.min(MAX_COUNTED_HONEYCOMB, available + item.getItem().getCount());
                if (items.size() >= MAX_TRACKED_ITEM_ENTITIES
                        || available >= MAX_COUNTED_HONEYCOMB) break;
            }
            if (items.size() >= MAX_TRACKED_ITEM_ENTITIES
                    || available >= MAX_COUNTED_HONEYCOMB) break;
        }
        items.sort(Comparator.comparingDouble((ItemEntity item) -> nearestHiveDistanceSq(item, hives))
                .thenComparingInt(ItemEntity::getId));
        return new Stockpile(List.copyOf(items), available);
    }

    private static double nearestHiveDistanceSq(ItemEntity item, List<BlockPos> hives) {
        double nearest = Double.POSITIVE_INFINITY;
        for (BlockPos hive : hives) {
            double dx = item.getX() - (hive.getX() + 0.5);
            double dy = item.getY() - (hive.getY() + 0.5);
            double dz = item.getZ() - (hive.getZ() + 0.5);
            nearest = Math.min(nearest, dx * dx + dy * dy + dz * dz);
        }
        return nearest;
    }
}
