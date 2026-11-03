package dev.swarmmobs.visual;

import dev.swarmmobs.agent.SwarmTaskType;
import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-synchronized, lightweight action cues built on vanilla entity
 * animations. No renderer replacement or mandatory animation library.
 */
public final class SwarmActionVisuals {
    public static void taskChanged(ServerLevel level, PathfinderMob mob, SwarmTaskType task) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get() || task == null) return;
        ParticleOptions cue = switch (task) {
            case SEARCH -> ParticleTypes.END_ROD;
            case FLANK -> ParticleTypes.PORTAL;
            case BREACH -> ParticleTypes.CRIT;
            case RANGED_SUPPORT -> ParticleTypes.ENCHANT;
            case ENGINEERING -> ParticleTypes.ELECTRIC_SPARK;
            case MATERIAL -> ParticleTypes.HAPPY_VILLAGER;
            case RESERVE -> ParticleTypes.CLOUD;
        };
        level.sendParticles(cue, mob.getX(), mob.getY() + 0.9, mob.getZ(),
                4, 0.22, 0.22, 0.22, 0.01);
    }

    public static void workProgress(
            ServerLevel level,
            Zombie zombie,
            BlockPos position,
            BlockState material,
            int progressTicks
    ) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get()) return;
        if (progressTicks % 5 == 0) {
            zombie.swing(InteractionHand.MAIN_HAND);
        }
        if (progressTicks % 10 == 0 && position != null && material != null) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, material),
                    position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5,
                    3, 0.16, 0.16, 0.16, 0.02);
        }
    }

    public static void workCompleted(
            ServerLevel level,
            Zombie zombie,
            BlockPos position,
            BlockState material,
            boolean succeeded
    ) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get() || !succeeded
                || position == null || material == null) return;
        zombie.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, material),
                position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5,
                7, 0.22, 0.22, 0.22, 0.035);
    }

    public static void carrierPickup(ServerLevel level, Zombie zombie) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get()) return;
        zombie.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                zombie.getX(), zombie.getY() + 0.7, zombie.getZ(),
                2, 0.18, 0.12, 0.18, 0.01);
    }

    public static void carrierDelivery(
            ServerLevel level,
            Zombie zombie,
            BlockPos nest,
            int acceptedItems
    ) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get() || nest == null || acceptedItems <= 0) return;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                nest.getX() + 0.5, nest.getY() + 1.0, nest.getZ() + 0.5,
                Math.min(8, 2 + acceptedItems), 0.28, 0.25, 0.28, 0.015);
    }

    public static void scoutSignal(ServerLevel level, Spider spider, int reports) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get() || reports <= 0) return;
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                spider.getX(), spider.getY() + 0.65, spider.getZ(),
                Math.min(8, reports), 0.22, 0.2, 0.22, 0.015);
    }

    private SwarmActionVisuals() {}
}
