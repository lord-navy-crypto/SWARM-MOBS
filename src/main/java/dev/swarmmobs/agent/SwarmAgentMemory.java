package dev.swarmmobs.agent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small per-entity long-term memory.
 *
 * Only bounded task-experience values are stored in the entity's own
 * persistent NBT. Target coordinates, player identity, chat, inventory and
 * transient navigation state are deliberately excluded.
 */
public final class SwarmAgentMemory {
    public static final String DATA_KEY = "SwarmMobsAgentMemory";
    public static final int SCHEMA_VERSION = SwarmAgentMemoryPolicy.SCHEMA_VERSION;

    public static boolean restore(
            CompoundTag entityData,
            SwarmAgentState state,
            long currentGameTick
    ) {
        if (entityData == null || state == null
                || !entityData.contains(DATA_KEY, Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag memory = entityData.getCompound(DATA_KEY);
        if (memory.getInt("schemaVersion") != SCHEMA_VERSION
                || !memory.contains("taskExperience", Tag.TAG_COMPOUND)) {
            return false;
        }

        CompoundTag experienceTag = memory.getCompound("taskExperience");
        Map<String, Double> encoded = new LinkedHashMap<>();
        for (SwarmTaskType task : SwarmTaskType.values()) {
            if (experienceTag.contains(task.name(), Tag.TAG_DOUBLE)) {
                encoded.put(task.name(), experienceTag.getDouble(task.name()));
            }
        }
        Map<SwarmTaskType, Double> experience =
                SwarmAgentMemoryPolicy.decode(SCHEMA_VERSION, encoded);

        state.restoreTaskExperience(experience, currentGameTick);
        return !experience.isEmpty();
    }

    public static void save(
            CompoundTag entityData,
            SwarmAgentState state,
            String dimension,
            long gameTick
    ) {
        if (entityData == null || state == null) {
            return;
        }

        CompoundTag experienceTag = new CompoundTag();
        Map<String, Double> encoded =
                SwarmAgentMemoryPolicy.encode(state.taskExperienceSnapshot());
        for (Map.Entry<String, Double> entry : encoded.entrySet()) {
            experienceTag.putDouble(entry.getKey(), entry.getValue());
        }

        if (experienceTag.isEmpty()) {
            entityData.remove(DATA_KEY);
            return;
        }

        CompoundTag memory = new CompoundTag();
        memory.putInt("schemaVersion", SCHEMA_VERSION);
        memory.putLong("savedAt", Math.max(0L, gameTick));
        memory.putString("dimension", dimension == null ? "" : dimension);
        memory.put("taskExperience", experienceTag);
        entityData.put(DATA_KEY, memory);
    }

    /** Deterministic phase spreading avoids writing every mob's NBT on the same tick. */
    public static boolean shouldPersist(long gameTick, int entityIdHash) {
        return SwarmAgentMemoryPolicy.shouldPersist(gameTick, entityIdHash);
    }

    private SwarmAgentMemory() {}
}
