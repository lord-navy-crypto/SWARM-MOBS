package dev.swarmmobs.colony;

import dev.swarmmobs.agent.SwarmAgentProfiles;
import dev.swarmmobs.config.SwarmConfig;
import java.util.Map;
import java.util.WeakHashMap;
import dev.swarmmobs.registry.SwarmNestBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;

/**
 * Tick-driven, dimension-scoped census. Only existing loaded living entities
 * report; no AABB over all connected extensions and no chunk tickets.
 * State is transient so world reload cannot resurrect phantom population.
 */
public final class ColonyPopulationRegistry {
    private static final long EXPIRY_TICKS = 400L;
    private static final long ALARM_HOLD_TICKS = 200L;
    private static final Map<ServerLevel, ColonyPopulationLedger> LEVELS = new WeakHashMap<>();
    private static final Map<ServerLevel, ColonyAlarmLedger> ALARMS = new WeakHashMap<>();

    private ColonyPopulationRegistry() {}

    public static void observe(ServerLevel level, PathfinderMob mob, long now) {
        if (level == null || mob == null || !mob.isAlive()
                || !SwarmAgentProfiles.isSupported(mob)) return;
        var tag = mob.getPersistentData();
        if (!tag.contains("SwarmColonyNest")) return;
        if (tag.contains("SwarmColonyDimension")
                && !level.dimension().location().toString()
                        .equals(tag.getString("SwarmColonyDimension"))) return;
        // The home coordinate is a persistent entity tag written by existing
        // Nest Core enrollment/spawning; it is not inferred from proximity.
        LEVELS.computeIfAbsent(level, ignored -> new ColonyPopulationLedger())
                .observe(tag.getLong("SwarmColonyNest"), mob.getUUID(), now);
        // Threat evidence is a real vanilla target, not a guessed player
        // location. This only preempts the root's civilian production.
        if (mob.getTarget() instanceof Player player && player.isAlive()) {
            ALARMS.computeIfAbsent(level, ignored -> new ColonyAlarmLedger())
                    .report(tag.getLong("SwarmColonyNest"), now,
                            mob.blockPosition().asLong());
        }
    }

    /**
     * Returns only the observed resident's own incident location, never the
     * enemy location. All world reads must be in already loaded chunks.
     */
    public static BlockPos localRallySite(ServerLevel level, PathfinderMob mob) {
        if (level == null || mob == null
                || !SwarmConfig.NEST_COLONY_RALLY_ENABLED.get()) return null;
        var tag = mob.getPersistentData();
        if (!tag.contains("SwarmColonyNest")
                || (tag.contains("SwarmColonyDimension")
                    && !level.dimension().location().toString()
                            .equals(tag.getString("SwarmColonyDimension")))) return null;
        BlockPos root = BlockPos.of(tag.getLong("SwarmColonyNest"));
        if (!level.hasChunkAt(root)
                || !level.getBlockState(root).is(SwarmNestBlocks.NEST_CORE.get())
                || !(level.getBlockEntity(root) instanceof SwarmNestBlockEntity core)
                || core.isConnectedExtension()) return null;
        ColonyAlarmLedger alarms = ALARMS.get(level);
        if (alarms == null) return null;
        var incident = alarms.witnessPosition(root.asLong(),
                level.getGameTime(), ALARM_HOLD_TICKS);
        if (incident.isEmpty()) return null;
        BlockPos at = BlockPos.of(incident.getAsLong());
        return level.isInWorldBounds(at) && level.hasChunkAt(at) ? at : null;
    }

    public static boolean hasActiveThreat(ServerLevel level, BlockPos root, long now) {
        if (level == null || root == null) return false;
        ColonyAlarmLedger ledger = ALARMS.get(level);
        return ledger != null && ledger.active(root.asLong(), now, ALARM_HOLD_TICKS);
    }

    /**
     * A live alarm preempts civilian labor across loaded colony members, but
     * never sets an enemy target or changes the vanilla attack controller.
     */
    public static boolean canPerformCivilianWork(ServerLevel level, PathfinderMob mob) {
        if (level == null || mob == null
                || !(SwarmConfig.NEST_CONNECTED_CORE_GROWTH_ENABLED.get()
                    || SwarmConfig.NEST_COLONY_RALLY_ENABLED.get())) return true;
        var data = mob.getPersistentData();
        if (!data.contains("SwarmColonyNest")) return true;
        if (data.contains("SwarmColonyDimension")
                && !level.dimension().location().toString()
                        .equals(data.getString("SwarmColonyDimension"))) return true;
        return !hasActiveThreat(level, BlockPos.of(data.getLong("SwarmColonyNest")),
                level.getGameTime());
    }

    public static int count(ServerLevel level, BlockPos root, long now) {
        if (level == null || root == null) return 0;
        ColonyPopulationLedger ledger = LEVELS.get(level);
        return ledger == null ? 0 : ledger.count(root.asLong(), now, EXPIRY_TICKS);
    }
}
