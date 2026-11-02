package dev.swarmmobs.event;

import dev.swarmmobs.agent.SwarmAgentProfiles;
import dev.swarmmobs.ai.SwarmPlayerStyleRegistry;
import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class SwarmPlayerStyleEvents {
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!SwarmConfig.PLAYER_LEARNING_ENABLED.get()
                || !(event.getEntity() instanceof PathfinderMob mob)
                || !SwarmAgentProfiles.isSupported(mob)
                || !(mob.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof Player player)
                || player.isSpectator()) {
            return;
        }

        SwarmPlayerStyleRegistry.observe(level, player.getUUID(),
                classify(event.getSource()), level.getGameTime());
    }

    private static SwarmPlayerStyleRegistry.AttackStyle classify(DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return SwarmPlayerStyleRegistry.AttackStyle.EXPLOSIVE;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return SwarmPlayerStyleRegistry.AttackStyle.FIRE;
        }
        if (source.is(DamageTypeTags.IS_MAGIC)) {
            return SwarmPlayerStyleRegistry.AttackStyle.MAGIC;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE;
        }
        return SwarmPlayerStyleRegistry.AttackStyle.MELEE;
    }

    private SwarmPlayerStyleEvents() {}
}
