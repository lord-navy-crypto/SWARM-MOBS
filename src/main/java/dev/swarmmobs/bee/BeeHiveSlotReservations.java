package dev.swarmmobs.bee;

import java.util.LinkedHashMap;
import java.util.UUID;

/** Pure-Java bounded reservation ledger, isolated for deterministic unit testing. */
public final class BeeHiveSlotReservations {
    public static final int MAX_RESERVATIONS = 4096;
    public static final long RESERVATION_TTL_TICKS = 600L;
    private record Reservation(long targetHive, long expiresAt) {}
    private final LinkedHashMap<UUID, Reservation> reservations = new LinkedHashMap<>(16, 0.75f, true);

    public boolean reserve(UUID beeId, long targetHive, int storedOccupants, long now, int maxOccupants) {
        if (beeId == null || storedOccupants < 0 || now < 0 || maxOccupants < 1 || storedOccupants > maxOccupants)
            throw new IllegalArgumentException("invalid hive slot reservation");
        prune(now);
        Reservation old = reservations.get(beeId);
        if (old != null && old.targetHive() == targetHive) return true;
        reservations.remove(beeId);
        if (storedOccupants + reservedSlots(targetHive, now) >= maxOccupants) return false;
        reservations.put(beeId, new Reservation(targetHive, Math.addExact(now, RESERVATION_TTL_TICKS)));
        while (reservations.size() > MAX_RESERVATIONS)
            reservations.remove(reservations.keySet().iterator().next());
        return true;
    }

    public int reservedSlots(long hivePosition, long now) {
        prune(now);
        int count = 0;
        for (Reservation reservation : reservations.values())
            if (reservation.targetHive() == hivePosition) count++;
        return count;
    }

    public boolean hasReservation(UUID beeId, long now) {
        prune(now);
        return beeId != null && reservations.containsKey(beeId);
    }

    public void reconcile(UUID beeId, Long currentHive, long now) {
        if (beeId == null) return;
        prune(now);
        Reservation reservation = reservations.get(beeId);
        if (reservation != null && (currentHive == null
                || reservation.targetHive() != currentHive.longValue())) reservations.remove(beeId);
    }

    public void release(UUID beeId) {
        if (beeId != null) reservations.remove(beeId);
    }

    public int reservationCount(long now) {
        prune(now);
        return reservations.size();
    }

    private void prune(long now) {
        reservations.entrySet().removeIf(entry -> now >= entry.getValue().expiresAt());
    }
}
