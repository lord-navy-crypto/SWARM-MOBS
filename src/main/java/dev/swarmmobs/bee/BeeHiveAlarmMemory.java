package dev.swarmmobs.bee;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived local alarm evidence for a vanilla Bee hive.
 * The alarm is a shelter/foraging signal only: it never assigns attack targets.
 */
public final class BeeHiveAlarmMemory {
    public static final int MAX_HIVES = 1024;
    public static final long DEFAULT_HOLD_TICKS = 120L;

    public record Alert(UUID observedThreat, long observedPosition,
            long lastObservedTick, int confirmations) {
        public int severity() { return Math.min(3, confirmations); }
    }

    private record Scan(long lastScanTick) {}
    private final LinkedHashMap<Long, Alert> alerts =
            new LinkedHashMap<>(16, 0.75f, true);
    private final LinkedHashMap<Long, Scan> scans =
            new LinkedHashMap<>(16, 0.75f, true);

    public boolean claimScan(long hive, long now, long intervalTicks) {
        if (now < 0 || intervalTicks < 1) {
            throw new IllegalArgumentException("invalid scan clock");
        }
        Scan previous = scans.get(hive);
        if (previous != null && (now < previous.lastScanTick()
                || now - previous.lastScanTick() < intervalTicks)) return false;
        scans.put(hive, new Scan(now));
        trim(scans, MAX_HIVES);
        return true;
    }

    public void report(long hive, UUID threat, long position, long now) {
        if (threat == null || now < 0) {
            throw new IllegalArgumentException("invalid alarm report");
        }
        Alert previous = alerts.get(hive);
        if (previous != null && now < previous.lastObservedTick()) return;
        int confirmations = previous != null
                && previous.observedThreat().equals(threat)
                && now - previous.lastObservedTick() <= 80L
                ? Math.min(3, previous.confirmations() + 1) : 1;
        alerts.put(hive, new Alert(threat, position, now, confirmations));
        trim(alerts, MAX_HIVES);
    }

    public Optional<Alert> active(long hive, long now, long holdTicks) {
        if (now < 0 || holdTicks < 1) {
            throw new IllegalArgumentException("invalid alarm clock");
        }
        Alert alert = alerts.get(hive);
        if (alert == null) return Optional.empty();
        if (now < alert.lastObservedTick()
                || now - alert.lastObservedTick() >= holdTicks) {
            alerts.remove(hive);
            return Optional.empty();
        }
        return Optional.of(alert);
    }

    public void clear(long hive) {
        alerts.remove(hive);
    }

    public int trackedHives() { return alerts.size(); }

    private static <K, V> void trim(LinkedHashMap<K, V> map, int limit) {
        while (map.size() > limit) {
            map.remove(map.keySet().iterator().next());
        }
    }
}
