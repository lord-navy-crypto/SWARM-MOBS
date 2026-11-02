package dev.swarmmobs.event;

import dev.swarmmobs.algorithm.SwarmSpawnWeightPolicy;
import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;

/**
 * Adjusts only the natural-spawn candidate list built by NaturalSpawner.
 * Spawner blocks, spawn eggs, commands, conversions and reinforcement logic
 * do not use this hook and remain unchanged.
 */
public final class SwarmSpawnEvents {
    public static void onPotentialSpawns(LevelEvent.PotentialSpawns event) {
        if (!(event.getLevel() instanceof ServerLevel)
                || event.getMobCategory() != MobCategory.MONSTER
                || !SwarmConfig.NATURAL_SPAWN_WEIGHTS_ENABLED.get()) {
            return;
        }

        // The event's list is mutable through add/remove methods. Iterate a
        // snapshot so each original candidate is transformed exactly once.
        List<MobSpawnSettings.SpawnerData> candidates =
                List.copyOf(event.getSpawnerDataList());
        for (MobSpawnSettings.SpawnerData candidate : candidates) {
            double multiplier = multiplierFor(candidate.type);
            if (multiplier == 1.0) continue;

            int originalWeight = candidate.getWeight().asInt();
            int adjustedWeight = SwarmSpawnWeightPolicy.adjustedWeight(
                    originalWeight, multiplier);
            if (adjustedWeight == originalWeight) continue;

            event.removeSpawnerData(candidate);
            if (adjustedWeight > 0) {
                // Keep the biome's original pack-size range unchanged.
                event.addSpawnerData(new MobSpawnSettings.SpawnerData(
                        candidate.type, adjustedWeight,
                        candidate.minCount, candidate.maxCount));
            }
        }
    }

    private static double multiplierFor(EntityType<?> type) {
        if (type == EntityType.ZOMBIE) {
            return SwarmConfig.ZOMBIE_NATURAL_SPAWN_WEIGHT.get();
        }
        if (type == EntityType.SKELETON) {
            return SwarmConfig.SKELETON_NATURAL_SPAWN_WEIGHT.get();
        }
        if (type == EntityType.CREEPER) {
            return SwarmConfig.CREEPER_NATURAL_SPAWN_WEIGHT.get();
        }
        if (type == EntityType.SPIDER) {
            return SwarmConfig.SPIDER_NATURAL_SPAWN_WEIGHT.get();
        }
        return 1.0;
    }

    private SwarmSpawnEvents() {}
}
