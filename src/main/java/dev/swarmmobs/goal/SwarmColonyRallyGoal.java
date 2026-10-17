package dev.swarmmobs.goal;

import dev.swarmmobs.agent.SwarmAgentState;
import dev.swarmmobs.algorithm.SwarmEngagementPolicy;
import dev.swarmmobs.algorithm.SwarmNestSurveyBudget;
import dev.swarmmobs.colony.ColonyPopulationRegistry;
import dev.swarmmobs.colony.ColonyRallyPolicy;
import dev.swarmmobs.config.SwarmConfig;
import dev.swarmmobs.data.SwarmAttachments;
import java.util.EnumSet;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Low-priority, opt-in local muster around a real witness position.
 * Does not grant enemy knowledge, attack, alter vanilla combat goals, or
 * ask the server to load chunks. One new path request per dimension per
 * four ticks at most, with a per-mob retry cooldown.
 */
public final class SwarmColonyRallyGoal extends Goal {
    private static final Map<ServerLevel, SwarmNestSurveyBudget> BUDGETS =
            new WeakHashMap<>();
    private static final int MAX_DURATION_TICKS = 90;
    private final PathfinderMob mob;
    private BlockPos site;
    private long nextAttemptTick = Long.MIN_VALUE;
    private long startedTick;

    public SwarmColonyRallyGoal(PathfinderMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    private boolean idle(ServerLevel level) {
        if (!SwarmConfig.ENABLED.get()
                || !SwarmConfig.NEST_COLONY_RALLY_ENABLED.get()
                || !mob.isAlive() || mob.isNoAi()
                || mob.getTarget() != null) return false;
        SwarmAgentState state = mob.getData(SwarmAttachments.AGENT_STATE.get());
        return state.targetId() == null && !state.hasDestination()
                && state.engagementMode() == SwarmEngagementPolicy.Mode.WORK;
    }

    @Override
    public boolean canUse() {
        if (!(mob.level() instanceof ServerLevel level) || !idle(level)) return false;
        long now = level.getGameTime();
        if (nextAttemptTick != Long.MIN_VALUE && now < nextAttemptTick) return false;
        nextAttemptTick = now + 60L + Math.floorMod(mob.getId(), 41);
        BlockPos witness = ColonyPopulationRegistry.localRallySite(level, mob);
        if (witness == null || !ColonyRallyPolicy.mayRally(
                mob.getUUID(), true, true, level.hasChunkAt(witness),
                mob.distanceToSqr(witness.getX()+.5, witness.getY()+.5,
                        witness.getZ()+.5))) return false;
        if (!BUDGETS.computeIfAbsent(level, key -> new SwarmNestSurveyBudget())
                .trySurvey(now, 4)) return false;
        site = witness.immutable();
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(mob.level() instanceof ServerLevel level)
                || site == null || !idle(level)
                || level.getGameTime() - startedTick >= MAX_DURATION_TICKS
                || !level.hasChunkAt(site)
                || !site.equals(ColonyPopulationRegistry.localRallySite(level, mob))
                || mob.getNavigation().isDone()) return false;
        return mob.distanceToSqr(site.getX()+.5, site.getY()+.5,
                site.getZ()+.5) > 6.0 * 6.0;
    }

    @Override
    public void start() {
        if (!(mob.level() instanceof ServerLevel level) || site == null
                || !idle(level) || !level.hasChunkAt(site)) return;
        startedTick = level.getGameTime();
        mob.getNavigation().moveTo(site.getX() + .5,
                site.getY(), site.getZ() + .5, 1.0);
    }

    @Override
    public void stop() {
        if (mob.getTarget() == null) {
            SwarmAgentState state = mob.getData(SwarmAttachments.AGENT_STATE.get());
            if (state.targetId() == null) mob.getNavigation().stop();
        }
        site = null;
    }
}
