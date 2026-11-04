package dev.swarmmobs.visual;

import dev.swarmmobs.agent.SwarmRole;
import dev.swarmmobs.agent.SwarmTaskType;
import dev.swarmmobs.config.SwarmConfig;
import dev.swarmmobs.bee.BeeActionMemory;
import dev.swarmmobs.bee.BeeCastePolicy;
import dev.swarmmobs.bee.BeeColonyRuntime;
import dev.swarmmobs.bee.BeeTaskPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-authoritative bridge from real swarm state transitions to GeckoLib.
 * Presentation only: this class never assigns tasks, targets, paths, attacks,
 * or world mutations.
 */
public final class SwarmActionVisuals {
    private static void trigger(PathfinderMob mob, SwarmAnimationCue cue) {
        if (mob == null || cue == null || !mob.isAlive()
                || !SwarmConfig.ACTION_VISUALS_ENABLED.get()
                || !(mob.level() instanceof ServerLevel)) return;
        SwarmActionAnimatable animatable = SwarmActionAnimatable.forMob(mob);
        if (animatable != null)
            animatable.triggerAnim(mob, mob.getId(), "actions", cue.animationName());
    }

    private static void stop(PathfinderMob mob, SwarmAnimationCue cue) {
        if (mob == null || cue == null || !(mob.level() instanceof ServerLevel)) return;
        SwarmActionAnimatable animatable = SwarmActionAnimatable.forMob(mob);
        if (animatable != null)
            animatable.stopTriggeredAnim(mob, mob.getId(), "actions", cue.animationName());
    }

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
        if (mob instanceof Spider && task == SwarmTaskType.SEARCH)
            trigger(mob, SwarmAnimationCue.SCOUT_OBSERVE);
        else if (task == SwarmTaskType.FLANK)
            trigger(mob, SwarmAnimationCue.FLANK_SETUP);
        else if (mob instanceof Skeleton && task == SwarmTaskType.RANGED_SUPPORT)
            trigger(mob, SwarmAnimationCue.SUPPORT_READY);
        else if (mob instanceof Creeper && task == SwarmTaskType.BREACH)
            trigger(mob, SwarmAnimationCue.BREACH_READY);
        else
            trigger(mob, SwarmAnimationCue.ROLE_CHANGE);
    }

    public static void engineerInspect(ServerLevel level, Zombie zombie) {
        trigger(zombie, SwarmAnimationCue.ENGINEER_INSPECT);
    }

    public static void engineerInterrupted(ServerLevel level, Zombie zombie) {
        stop(zombie, SwarmAnimationCue.ENGINEER_INSPECT);
        stop(zombie, SwarmAnimationCue.ENGINEER_WORK);
    }

    public static void workProgress(ServerLevel level, Zombie zombie,
                                    BlockPos position, BlockState material, int progressTicks) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get()) return;
        if (progressTicks == 5) trigger(zombie, SwarmAnimationCue.ENGINEER_WORK);
        if (progressTicks % 5 == 0) zombie.swing(InteractionHand.MAIN_HAND);
        if (progressTicks % 10 == 0 && position != null && material != null) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, material),
                    position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5,
                    3, 0.16, 0.16, 0.16, 0.02);
        }
    }

    public static void workCompleted(ServerLevel level, Zombie zombie, BlockPos position,
                                     BlockState material, boolean succeeded) {
        stop(zombie, SwarmAnimationCue.ENGINEER_WORK);
        stop(zombie, SwarmAnimationCue.ENGINEER_INSPECT);
        if (succeeded) trigger(zombie, SwarmAnimationCue.ENGINEER_COMPLETE);
        else trigger(zombie, SwarmAnimationCue.ENGINEER_CANCEL);
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get() || !succeeded
                || position == null || material == null) return;
        zombie.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, material),
                position.getX() + 0.5, position.getY() + 0.5, position.getZ() + 0.5,
                7, 0.22, 0.22, 0.22, 0.035);
    }

    public static void carrierTrack(ServerLevel level, Zombie zombie) {
        trigger(zombie, SwarmAnimationCue.CARRIER_TRACK);
    }

    public static void carrierPickup(ServerLevel level, Zombie zombie) {
        trigger(zombie, SwarmAnimationCue.CARRIER_ACQUIRE);
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get()) return;
        zombie.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                zombie.getX(), zombie.getY() + 0.7, zombie.getZ(),
                2, 0.18, 0.12, 0.18, 0.01);
    }

    public static void carrierCarry(ServerLevel level, Zombie zombie) {
        trigger(zombie, SwarmAnimationCue.CARRIER_CARRY);
    }

    public static void carrierInterrupted(ServerLevel level, Zombie zombie) {
        stop(zombie, SwarmAnimationCue.CARRIER_CARRY);
    }

    public static void carrierDelivery(ServerLevel level, Zombie zombie,
                                       BlockPos nest, int acceptedItems) {
        if (nest == null || acceptedItems <= 0) return;
        stop(zombie, SwarmAnimationCue.CARRIER_CARRY);
        trigger(zombie, SwarmAnimationCue.CARRIER_DELIVER);
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get()) return;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                nest.getX() + 0.5, nest.getY() + 1.0, nest.getZ() + 0.5,
                Math.min(8, 2 + acceptedItems), 0.28, 0.25, 0.28, 0.015);
    }

    public static void scoutObserve(ServerLevel level, Spider spider) {
        trigger(spider, SwarmAnimationCue.SCOUT_OBSERVE);
    }

    public static void scoutSignal(ServerLevel level, Spider spider, int reports) {
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get() || reports <= 0) return;
        trigger(spider, SwarmAnimationCue.SCOUT_REPORT);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                spider.getX(), spider.getY() + 0.65, spider.getZ(),
                Math.min(8, reports), 0.22, 0.2, 0.22, 0.015);
    }

    public static void roleChanged(ServerLevel level, PathfinderMob mob, SwarmRole role) {
        if (role == SwarmRole.FLANK_LEFT || role == SwarmRole.FLANK_RIGHT)
            trigger(mob, SwarmAnimationCue.FLANK_SETUP);
        else if (role == SwarmRole.RANGED_SUPPORT)
            trigger(mob, SwarmAnimationCue.SUPPORT_READY);
        else
            trigger(mob, SwarmAnimationCue.ROLE_CHANGE);
    }

    public static void regroup(ServerLevel level, PathfinderMob mob) {
        trigger(mob, SwarmAnimationCue.REGROUP);
    }

    public static void reliefRotation(ServerLevel level, PathfinderMob mob) {
        trigger(mob, SwarmAnimationCue.RELIEF_ROTATION);
    }

    public static void communicationAcknowledged(ServerLevel level, PathfinderMob mob,
                                                  int deliveredMessages) {
        if (deliveredMessages > 0)
            trigger(mob, SwarmAnimationCue.COMMUNICATION_ACKNOWLEDGED);
    }

    private static void triggerBee(
            ServerLevel level, Bee bee, SwarmAnimationCue cue, long cooldownTicks) {
        if (level == null || bee == null || cue == null
                || !SwarmConfig.ACTION_VISUALS_ENABLED.get()) return;
        BeeActionMemory memory = BeeColonyRuntime.actions(level);
        if (!memory.claim(bee.getUUID(), cue, level.getGameTime(), cooldownTicks)) return;
        trigger(bee, cue);
    }

    /** A task badge marks the worker's actual current colony assignment. */
    public static void beeTaskChanged(ServerLevel level, Bee bee, BeeTaskPolicy.Task task) {
        SwarmAnimationCue cue = switch (task) {
            case FORAGE -> SwarmAnimationCue.BEE_FORAGE_DUTY;
            case NURSE -> SwarmAnimationCue.BEE_NURSE_DUTY;
            case GUARD -> SwarmAnimationCue.BEE_GUARD_DUTY;
            case BUILD -> SwarmAnimationCue.BEE_BUILD_DUTY;
            case REST -> SwarmAnimationCue.BEE_REST;
        };
        triggerBee(level, bee, cue, 20L);
    }

    public static void beeForageDispatch(ServerLevel level, Bee bee) {
        triggerBee(level, bee, SwarmAnimationCue.BEE_FORAGE_DISPATCH, 40L);
    }

    public static void beePollinate(ServerLevel level, Bee bee) {
        triggerBee(level, bee, SwarmAnimationCue.BEE_POLLINATE, 30L);
    }

    public static void beeReturnDelivery(ServerLevel level, Bee bee) {
        triggerBee(level, bee, SwarmAnimationCue.BEE_HONEY_DELIVERY, 40L);
    }

    public static void beeReturnHome(ServerLevel level, Bee bee) {
        triggerBee(level, bee, SwarmAnimationCue.BEE_RETURN_HOME, 40L);
    }

    public static void beeGuardAlert(ServerLevel level, Bee bee) {
        if (BeeCastePolicy.caste(bee.getUUID()) != BeeCastePolicy.Caste.GUARD) return;
        triggerBee(level, bee, SwarmAnimationCue.BEE_GUARD_ALERT, 80L);
    }

    public static void beeBuildCompleted(ServerLevel level, Bee bee, BlockPos hivePosition) {
        if (hivePosition == null) return;
        triggerBee(level, bee, SwarmAnimationCue.BEE_BUILD_COMPLETE, 100L);
        if (!SwarmConfig.ACTION_VISUALS_ENABLED.get()) return;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                hivePosition.getX() + 0.5, hivePosition.getY() + 0.7, hivePosition.getZ() + 0.5,
                8, 0.35, 0.25, 0.35, 0.015);
    }

    public static void beeScoutReport(ServerLevel level, Bee bee) {
        if (BeeCastePolicy.caste(bee.getUUID()) != BeeCastePolicy.Caste.SCOUT) return;
        triggerBee(level, bee, SwarmAnimationCue.BEE_SCOUT_REPORT, 100L);
    }

    private SwarmActionVisuals() {}
}
