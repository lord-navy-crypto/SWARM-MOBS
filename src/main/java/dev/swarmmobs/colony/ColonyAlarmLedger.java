package dev.swarmmobs.colony;

import java.util.HashMap;
import java.util.Map;

/**
 * One local sighting suspends a root's civilian construction and births.
 * A signal is NOT global target acquisition or a command to teleport workers.
 */
public final class ColonyAlarmLedger {
    private final Map<Long, Long> lastThreatTick = new HashMap<>();

    public void report(long root, long tick) {
        if (tick < 0) throw new IllegalArgumentException("negative tick");
        lastThreatTick.merge(root, tick, Math::max);
    }

    public boolean active(long root, long now, long holdTicks) {
        if (now < 0 || holdTicks < 1)
            throw new IllegalArgumentException("invalid alarm clock");
        Long at = lastThreatTick.get(root);
        if (at == null) return false;
        if (at > now || now - at >= holdTicks) {
            lastThreatTick.remove(root);
            return false;
        }
        return true;
    }

    public int trackedRoots() { return lastThreatTick.size(); }
}
