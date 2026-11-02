package dev.swarmmobs.bee;

import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.WeakHashMap;

/** Shared, transient hive-local memory for Bee event adapters. */
public final class BeeColonyRuntime {
    private static final Map<ServerLevel, BeeHiveAlarmMemory> ALARMS = new WeakHashMap<>();

    public static BeeHiveAlarmMemory alarms(ServerLevel level) {
        return ALARMS.computeIfAbsent(level, unused -> new BeeHiveAlarmMemory());
    }

    private BeeColonyRuntime() {}
}
