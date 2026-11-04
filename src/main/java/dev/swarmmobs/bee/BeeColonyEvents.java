package dev.swarmmobs.bee;

import dev.swarmmobs.config.SwarmConfig;
import dev.swarmmobs.visual.SwarmActionVisuals;
import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Local hazard warning for vanilla Bees. Selected unladen workers return to
 * their own loaded hive; no Bee is assigned a target and no stinging behavior
 * is added. Vanilla anger, attack, pollination and hive-entry goals remain
 * authoritative.
 */
public final class BeeColonyEvents {
    public static final long ALARM_HOLD_TICKS = BeeHiveAlarmMemory.DEFAULT_HOLD_TICKS;
    private static final long SCAN_INTERVAL_TICKS = 20L;
    private static final double THREAT_RADIUS = 8.0;
    private static final double SHELTER_RADIUS_SQUARED = 20.0 * 20.0;

    private BeeColonyEvents() {}

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bee bee)
                || !(bee.level() instanceof ServerLevel level)
                || !bee.isAlive()
                || !SwarmConfig.ENABLED.get()
                || !SwarmConfig.BEE_COLONY_SHELTER_ENABLED.get()) return;

        BlockPos home = bee.getHivePos();
        if (home == null || !level.isInWorldBounds(home) || !level.hasChunkAt(home)) return;
        var hiveState = level.getBlockState(home);
        if (!hiveState.is(Blocks.BEE_NEST) && !hiveState.is(Blocks.BEEHIVE)) return;

        double hiveDistanceSquared = bee.distanceToSqr(
                home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5);
        if (hiveDistanceSquared > SHELTER_RADIUS_SQUARED) return;

        BeeHiveAlarmMemory alarms = BeeColonyRuntime.alarms(level);
        long now = level.getGameTime();

        // Only Bees already near their own hive may witness and publish an alarm.
        if (hiveDistanceSquared <= 12.0 * 12.0
                && alarms.claimScan(home.asLong(), now, SCAN_INTERVAL_TICKS)) {
            Monster threat = level.getEntitiesOfClass(Monster.class,
                            new net.minecraft.world.phys.AABB(home).inflate(THREAT_RADIUS),
                            Monster::isAlive)
                    .stream()
                    .filter(candidate -> candidate.distanceToSqr(
                            home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5)
                            <= THREAT_RADIUS * THREAT_RADIUS)
                    .filter(bee::hasLineOfSight)
                    .min(Comparator.comparingDouble(candidate ->
                            candidate.distanceToSqr(home.getX() + 0.5,
                                    home.getY() + 0.5, home.getZ() + 0.5)))
                    .orElse(null);
            if (threat != null) {
                alarms.report(home.asLong(), threat.getUUID(),
                        threat.blockPosition().asLong(), now);
            }
        }

        var alert = alarms.active(home.asLong(), now, ALARM_HOLD_TICKS);
        if (alert.isEmpty()) return;

        var mode = BeeShelterPolicy.choose(alert.get(), bee.getUUID(),
                bee.hasHive(), bee.hasNectar(), bee.hasStung(),
                bee.getTarget() != null, Math.sqrt(hiveDistanceSquared));
        if (mode != BeeShelterPolicy.Mode.RETURN_TO_HIVE) return;

        if (BeeCastePolicy.caste(bee.getUUID()) == BeeCastePolicy.Caste.GUARD) {
            SwarmActionVisuals.beeGuardAlert(level, bee);
        }
        SwarmActionVisuals.beeReturnHome(level, bee);

        // Cancel only the optional remembered flower destination. The bee's
        // own hive is loaded and within a bounded radius; no chunk is requested.
        bee.setSavedFlowerPos(null);
        bee.setStayOutOfHiveCountdown(0);
        if (hiveDistanceSquared > 2.5 * 2.5
                && (bee.getNavigation().isDone()
                    || Math.floorMod(now + bee.getId(), 20L) == 0L)) {
            bee.getNavigation().moveTo(
                    home.getX() + 0.5, home.getY() + 0.5,
                    home.getZ() + 0.5, 0.9);
        }
    }
}
