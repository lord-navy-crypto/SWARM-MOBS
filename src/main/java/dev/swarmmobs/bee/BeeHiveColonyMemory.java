package dev.swarmmobs.bee;

import java.util.LinkedHashMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;

/** Bounded transient cache and overflow-slot reservations for vanilla hive colonies. */
public final class BeeHiveColonyMemory {
    public static final int MAX_CACHED_HIVES = 1024;
    public static final int MAX_RESERVATIONS = BeeHiveSlotReservations.MAX_RESERVATIONS;
    public static final long SNAPSHOT_TTL_TICKS = 20L;
    public static final long RESERVATION_TTL_TICKS = BeeHiveSlotReservations.RESERVATION_TTL_TICKS;

    private record Cached(BeeHiveColonySnapshot.Snapshot snapshot, long observedAt, int maxHives) {}
    private final LinkedHashMap<Long, Cached> snapshots = new LinkedHashMap<>(16, 0.75f, true);
    private final BeeHiveSlotReservations reservations = new BeeHiveSlotReservations();

    public BeeHiveColonySnapshot.Snapshot snapshot(ServerLevel level, BlockPos home,
            long now, int maxHives) {
        if (level == null || home == null || now < 0)
            throw new IllegalArgumentException("invalid hive snapshot request");
        reservations.reservationCount(now);
        Cached cached = snapshots.get(home.asLong());
        if (cached != null && cached.maxHives() == maxHives && now >= cached.observedAt()
                && now - cached.observedAt() < SNAPSHOT_TTL_TICKS) return cached.snapshot();

        BeeHiveColonySnapshot.Snapshot fresh = BeeHiveColonySnapshot.scan(level, home, maxHives);
        Cached entry = new Cached(fresh, now, maxHives);
        for (BlockPos position : fresh.positions()) snapshots.put(position.asLong(), entry);
        trimSnapshots();
        return fresh;
    }

    public void invalidate(BeeHiveColonySnapshot.Snapshot snapshot) {
        if (snapshot != null)
            for (BlockPos position : snapshot.positions()) snapshots.remove(position.asLong());
    }

    public boolean reserve(UUID beeId, BlockPos targetHive, int storedOccupants, long now) {
        if (targetHive == null) throw new IllegalArgumentException("targetHive");
        return reservations.reserve(beeId, targetHive.asLong(), storedOccupants, now,
                BeehiveBlockEntity.MAX_OCCUPANTS);
    }

    public int reservedSlots(long hivePosition, long now) {
        return reservations.reservedSlots(hivePosition, now);
    }

    public boolean hasReservation(UUID beeId, long now) {
        return reservations.hasReservation(beeId, now);
    }

    public void reconcile(UUID beeId, BlockPos currentHive, long now) {
        reservations.reconcile(beeId, currentHive == null ? null : currentHive.asLong(), now);
    }

    public void release(UUID beeId) {
        reservations.release(beeId);
    }

    public int cachedHiveCount() { return snapshots.size(); }
    public int reservationCount(long now) {
        return reservations.reservationCount(now);
    }

    private void trimSnapshots() {
        while (snapshots.size() > MAX_CACHED_HIVES)
            snapshots.remove(snapshots.keySet().iterator().next());
    }
}
