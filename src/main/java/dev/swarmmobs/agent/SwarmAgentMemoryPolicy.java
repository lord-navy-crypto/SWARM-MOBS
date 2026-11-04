package dev.swarmmobs.agent;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pure-Java encoding policy for the compact per-agent experience vector. */
public final class SwarmAgentMemoryPolicy {
    public static final int SCHEMA_VERSION = 1;
    public static final int SAVE_INTERVAL_TICKS = 200;

    public static Map<String, Double> encode(Map<SwarmTaskType, Double> experience) {
        Map<String, Double> encoded = new LinkedHashMap<>();
        if (experience == null) return Map.of();

        for (SwarmTaskType task : SwarmTaskType.values()) {
            Double value = experience.get(task);
            if (value != null && Double.isFinite(value) && value > 0.0001) {
                encoded.put(task.name(), Math.max(0.0, Math.min(1.0, value)));
            }
        }
        return Map.copyOf(encoded);
    }

    public static Map<SwarmTaskType, Double> decode(
            int schemaVersion,
            Map<String, Double> encoded
    ) {
        if (schemaVersion != SCHEMA_VERSION || encoded == null || encoded.isEmpty()) {
            return Map.of();
        }

        EnumMap<SwarmTaskType, Double> decoded = new EnumMap<>(SwarmTaskType.class);
        for (SwarmTaskType task : SwarmTaskType.values()) {
            Double value = encoded.get(task.name());
            if (value != null && Double.isFinite(value)) {
                decoded.put(task, Math.max(0.0, Math.min(1.0, value)));
            }
        }
        return Map.copyOf(decoded);
    }

    /** Deterministic phase spreading avoids writing every mob's NBT on the same tick. */
    public static boolean shouldPersist(long gameTick, int entityIdHash) {
        return Math.floorMod(gameTick, SAVE_INTERVAL_TICKS)
                == Math.floorMod(entityIdHash, SAVE_INTERVAL_TICKS);
    }

    private SwarmAgentMemoryPolicy() {}
}
