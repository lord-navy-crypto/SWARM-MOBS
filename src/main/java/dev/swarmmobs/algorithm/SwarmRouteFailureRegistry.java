package dev.swarmmobs.algorithm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.WeakHashMap;

/** Logical-server-thread registry; weak level keys cannot retain unloaded worlds. */
public final class SwarmRouteFailureRegistry {
    private static final Map<ServerLevel, SwarmRouteFailureMemory> MEMORY = new WeakHashMap<>();

    public static void reportFailure(ServerLevel level, BlockPos position, long now) {
        memory(level).reportFailure(position.asLong(), now);
    }

    public static void reportSuccess(ServerLevel level, BlockPos position) {
        memory(level).reportSuccess(position.asLong());
    }

    public static boolean shouldAvoid(ServerLevel level, BlockPos position, long now,
            int threshold, long memoryTicks) {
        return memory(level).shouldAvoid(position.asLong(), now, threshold, memoryTicks);
    }

    public static int size(ServerLevel level, long now) {
        return memory(level).size(now);
    }

    private static SwarmRouteFailureMemory memory(ServerLevel level) {
        return MEMORY.computeIfAbsent(level, unused -> new SwarmRouteFailureMemory());
    }

    private SwarmRouteFailureRegistry() {}
}
