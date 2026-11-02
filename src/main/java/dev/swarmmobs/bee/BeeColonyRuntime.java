package dev.swarmmobs.bee;

import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared, transient hive-local and per-worker memory for Bee event adapters. */
public final class BeeColonyRuntime {
    private static final Map<ServerLevel, BeeHiveAlarmMemory> ALARMS = new WeakHashMap<>();
    private static final Map<ServerLevel, BeeFlowerMemory> FLOWERS = new WeakHashMap<>();
    private static final Map<ServerLevel, BeeTaskMemory> TASKS = new WeakHashMap<>();
    private static final Map<ServerLevel, BeeForagingCycleMemory> CYCLES = new WeakHashMap<>();

    public static BeeHiveAlarmMemory alarms(ServerLevel level) {
        return ALARMS.computeIfAbsent(level, unused -> new BeeHiveAlarmMemory());
    }

    public static BeeFlowerMemory flowers(ServerLevel level) {
        return FLOWERS.computeIfAbsent(level, unused -> new BeeFlowerMemory());
    }

    public static BeeTaskMemory tasks(ServerLevel level) {
        return TASKS.computeIfAbsent(level, unused -> new BeeTaskMemory());
    }

    public static BeeForagingCycleMemory cycles(ServerLevel level) {
        return CYCLES.computeIfAbsent(level, unused -> new BeeForagingCycleMemory());
    }

    private BeeColonyRuntime() {}
}
