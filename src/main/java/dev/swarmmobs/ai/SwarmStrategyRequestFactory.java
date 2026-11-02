package dev.swarmmobs.ai;

import dev.swarmmobs.config.SwarmConfig;
import dev.swarmmobs.experiment.SwarmExperimentManager;
import dev.swarmmobs.experiment.SwarmExperimentMetrics;
import net.minecraft.server.level.ServerLevel;

public final class SwarmStrategyRequestFactory {

    public static SwarmStrategyRequest from(ServerLevel level) {
        var metrics = SwarmExperimentMetrics.snapshot(level);
        long now = level.getGameTime();
        var attackStyle = SwarmPlayerStyleRegistry.summary(level, now);
        var movementStyle = SwarmPlayerMotionRegistry.summary(level, now);
        return new SwarmStrategyRequest(
                now,
                metrics.agentCount(),
                SwarmExperimentManager.activePreset().name(),
                metrics.active(),
                metrics.elapsedTicks(),
                metrics.communicationDropRate(),
                SwarmConfig.COMMUNICATION_PACKET_DROP_RATE.get(),
                SwarmConfig.COMMUNICATION_LATENCY_TICKS.get(),
                SwarmConfig.SENSING_DROPOUT_RATE.get(),
                SwarmConfig.SENSING_MAX_HORIZONTAL_NOISE.get(),
                metrics.obstacleDetours(),
                metrics.recoveries(),
                metrics.recoveryFailureRate(),
                metrics.pathQueries(),
                metrics.roleReassignments(),
                metrics.activeSearchEpisodes(),
                metrics.searchSuccessRate(),
                metrics.averageReacquisitionTicks(),
                attackStyle.profiles(),
                attackStyle.samples(),
                attackStyle.dominantStyle().name(),
                attackStyle.meleeShare(),
                attackStyle.projectileShare(),
                attackStyle.explosiveShare(),
                attackStyle.fireShare(),
                attackStyle.magicShare(),
                attackStyle.otherShare(),
                movementStyle.profiles(),
                movementStyle.samples(),
                movementStyle.dominantStyle().name(),
                movementStyle.stationaryShare(),
                movementStyle.walkingShare(),
                movementStyle.sprintingShare(),
                movementStyle.erraticShare()
        );
    }

    private SwarmStrategyRequestFactory() {}
}
