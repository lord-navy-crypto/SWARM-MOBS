package dev.swarmmobs.bee;

import java.util.LinkedHashMap;

/** Bounded per-colony cooldown memory for births and bonus honey credits. */
public final class BeeColonyEconomyMemory {
    public static final int MAX_TRACKED_HIVES = 1024;
    public static final long MEMORY_TICKS = 48000L;
    private final LinkedHashMap<Long, Long> lastBirthByRoot = new LinkedHashMap<>(16, 0.75f, true);
    private final LinkedHashMap<Long, Long> lastHoneyByHive = new LinkedHashMap<>(16, 0.75f, true);

    public boolean claimBirth(long colonyRoot, long now, long intervalTicks) {
        return claim(lastBirthByRoot, colonyRoot, now, intervalTicks);
    }

    public boolean claimHoney(long hivePosition, long now, long intervalTicks) {
        return claim(lastHoneyByHive, hivePosition, now, intervalTicks);
    }

    public int trackedColonies(long now) {
        prune(lastBirthByRoot, now);
        return lastBirthByRoot.size();
    }

    public int trackedHoneyHives(long now) {
        prune(lastHoneyByHive, now);
        return lastHoneyByHive.size();
    }

    private static boolean claim(LinkedHashMap<Long, Long> ledger,
            long key, long now, long intervalTicks) {
        if (now < 0 || intervalTicks < 1)
            throw new IllegalArgumentException("invalid colony economy claim");
        prune(ledger, now);
        Long previous = ledger.get(key);
        if (previous == null || now < previous) {
            ledger.put(key, now);
            trim(ledger);
            return false;
        }
        if (now - previous < intervalTicks) return false;
        ledger.put(key, now);
        trim(ledger);
        return true;
    }

    private static void prune(LinkedHashMap<Long, Long> ledger, long now) {
        if (now < 0) return;
        ledger.entrySet().removeIf(entry -> now >= entry.getValue()
                && now - entry.getValue() > MEMORY_TICKS);
    }

    private static void trim(LinkedHashMap<Long, Long> ledger) {
        while (ledger.size() > MAX_TRACKED_HIVES)
            ledger.remove(ledger.keySet().iterator().next());
    }
}
