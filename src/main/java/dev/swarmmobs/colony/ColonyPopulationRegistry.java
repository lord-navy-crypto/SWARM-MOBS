package dev.swarmmobs.colony;

import dev.swarmmobs.agent.SwarmAgentProfiles;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Tick-driven, dimension-scoped census. Only existing loaded living entities
 * report; no AABB over all connected extensions and no chunk tickets.
 * State is transient so world reload cannot resurrect phantom population.
 */
public final class ColonyPopulationRegistry {
    private static final long EXPIRY_TICKS = 400L;
    private static final Map<ServerLevel, ColonyPopulationLedger> LEVELS = new WeakHashMap<>();

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
    }

    public static int count(ServerLevel level, BlockPos root, long now) {
        if (level == null || root == null) return 0;
        ColonyPopulationLedger ledger = LEVELS.get(level);
        return ledger == null ? 0 : ledger.count(root.asLong(), now, EXPIRY_TICKS);
    }
}
