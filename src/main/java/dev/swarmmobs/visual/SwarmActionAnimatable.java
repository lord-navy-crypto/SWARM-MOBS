package dev.swarmmobs.visual;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Zombie;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Shared synchronized animation controllers, keyed by each real mob entity id. */
public final class SwarmActionAnimatable implements GeoReplacedEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.swarm_action.idle");
    private static final RawAnimation ENGINEER_INSPECT = RawAnimation.begin().thenPlay("animation.swarm_action.engineer_inspect");
    private static final RawAnimation ENGINEER_WORK = RawAnimation.begin().thenLoop("animation.swarm_action.engineer_work");
    private static final RawAnimation ENGINEER_COMPLETE = RawAnimation.begin().thenPlay("animation.swarm_action.engineer_complete");
    private static final RawAnimation ENGINEER_CANCEL = RawAnimation.begin().thenPlay("animation.swarm_action.engineer_cancel");
    private static final RawAnimation CARRIER_TRACK = RawAnimation.begin().thenPlay("animation.swarm_action.carrier_track");
    private static final RawAnimation CARRIER_ACQUIRE = RawAnimation.begin().thenPlay("animation.swarm_action.carrier_acquire");
    private static final RawAnimation CARRIER_CARRY = RawAnimation.begin().thenLoop("animation.swarm_action.carrier_carry");
    private static final RawAnimation CARRIER_DELIVER = RawAnimation.begin().thenPlay("animation.swarm_action.carrier_deliver");
    private static final RawAnimation SCOUT_OBSERVE = RawAnimation.begin().thenPlay("animation.swarm_action.scout_observe");
    private static final RawAnimation SCOUT_REPORT = RawAnimation.begin().thenPlay("animation.swarm_action.scout_report");
    private static final RawAnimation SCOUT_ACKNOWLEDGE = RawAnimation.begin().thenPlay("animation.swarm_action.scout_acknowledge");
    private static final RawAnimation FLANK_SETUP = RawAnimation.begin().thenPlay("animation.swarm_action.flank_setup");
    private static final RawAnimation REGROUP = RawAnimation.begin().thenPlay("animation.swarm_action.regroup");
    private static final RawAnimation RELIEF_ROTATION = RawAnimation.begin().thenPlay("animation.swarm_action.relief_rotation");
    private static final RawAnimation ROLE_CHANGE = RawAnimation.begin().thenPlay("animation.swarm_action.role_change");
    private static final RawAnimation SUPPORT_READY = RawAnimation.begin().thenPlay("animation.swarm_action.support_ready");
    private static final RawAnimation BREACH_READY = RawAnimation.begin().thenPlay("animation.swarm_action.breach_ready");
    private static final RawAnimation COMMUNICATION_ACKNOWLEDGED = RawAnimation.begin().thenPlay("animation.swarm_action.communication_acknowledged");

    public static final SwarmActionAnimatable ZOMBIE = new SwarmActionAnimatable(EntityType.ZOMBIE);
    public static final SwarmActionAnimatable SPIDER = new SwarmActionAnimatable(EntityType.SPIDER);
    public static final SwarmActionAnimatable SKELETON = new SwarmActionAnimatable(EntityType.SKELETON);
    public static final SwarmActionAnimatable CREEPER = new SwarmActionAnimatable(EntityType.CREEPER);

    private final EntityType<?> replacingType;
    private final AnimatableInstanceCache cache;

    private SwarmActionAnimatable(EntityType<?> replacingType) {
        this.replacingType = replacingType;
        this.cache = GeckoLibUtil.createInstanceCache(this);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public static SwarmActionAnimatable forMob(PathfinderMob mob) {
        if (mob instanceof Zombie) return ZOMBIE;
        if (mob instanceof Spider) return SPIDER;
        if (mob instanceof Skeleton) return SKELETON;
        if (mob instanceof Creeper) return CREEPER;
        return null;
    }

    @Override public EntityType<?> getReplacingEntityType() { return replacingType; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<SwarmActionAnimatable> c =
                new AnimationController<>(this, "actions", 2, state -> state.setAndContinue(IDLE));
        c.triggerableAnim(SwarmAnimationCue.ENGINEER_INSPECT.animationName(), ENGINEER_INSPECT);
        c.triggerableAnim(SwarmAnimationCue.ENGINEER_WORK.animationName(), ENGINEER_WORK);
        c.triggerableAnim(SwarmAnimationCue.ENGINEER_COMPLETE.animationName(), ENGINEER_COMPLETE);
        c.triggerableAnim(SwarmAnimationCue.ENGINEER_CANCEL.animationName(), ENGINEER_CANCEL);
        c.triggerableAnim(SwarmAnimationCue.CARRIER_TRACK.animationName(), CARRIER_TRACK);
        c.triggerableAnim(SwarmAnimationCue.CARRIER_ACQUIRE.animationName(), CARRIER_ACQUIRE);
        c.triggerableAnim(SwarmAnimationCue.CARRIER_CARRY.animationName(), CARRIER_CARRY);
        c.triggerableAnim(SwarmAnimationCue.CARRIER_DELIVER.animationName(), CARRIER_DELIVER);
        c.triggerableAnim(SwarmAnimationCue.SCOUT_OBSERVE.animationName(), SCOUT_OBSERVE);
        c.triggerableAnim(SwarmAnimationCue.SCOUT_REPORT.animationName(), SCOUT_REPORT);
        c.triggerableAnim(SwarmAnimationCue.SCOUT_ACKNOWLEDGE.animationName(), SCOUT_ACKNOWLEDGE);
        c.triggerableAnim(SwarmAnimationCue.FLANK_SETUP.animationName(), FLANK_SETUP);
        c.triggerableAnim(SwarmAnimationCue.REGROUP.animationName(), REGROUP);
        c.triggerableAnim(SwarmAnimationCue.RELIEF_ROTATION.animationName(), RELIEF_ROTATION);
        c.triggerableAnim(SwarmAnimationCue.ROLE_CHANGE.animationName(), ROLE_CHANGE);
        c.triggerableAnim(SwarmAnimationCue.SUPPORT_READY.animationName(), SUPPORT_READY);
        c.triggerableAnim(SwarmAnimationCue.BREACH_READY.animationName(), BREACH_READY);
        c.triggerableAnim(SwarmAnimationCue.COMMUNICATION_ACKNOWLEDGED.animationName(), COMMUNICATION_ACKNOWLEDGED);
        controllers.add(c);
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
