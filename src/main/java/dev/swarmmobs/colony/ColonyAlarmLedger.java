package dev.swarmmobs.colony;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;

/**
 * Bounded-time evidence of a real encounter. A rally position is where a
 * colony member actually observed danger, not a remotely transmitted player
 * target or combat action. Missing coordinates retain alarm-only behavior.
 */
public final class ColonyAlarmLedger {
    private record Incident(long tick, Long witnessPosition) {}
    private final Map<Long, Incident> incidents = new HashMap<>();

    public void report(long root, long tick) {
        reportInternal(root, tick, null);
    }

    public void report(long root, long tick, long witnessPosition) {
        reportInternal(root, tick, witnessPosition);
    }

    private void reportInternal(long root, long tick, Long witnessPosition) {
        if (tick < 0) throw new IllegalArgumentException("negative tick");
        Incident previous = incidents.get(root);
        if (previous == null || tick > previous.tick()) {
            incidents.put(root, new Incident(tick, witnessPosition));
        } else if (tick == previous.tick() && previous.witnessPosition() == null
                && witnessPosition != null) {
            incidents.put(root, new Incident(tick, witnessPosition));
        }
    }

    public boolean active(long root, long now, long holdTicks) {
        if (now < 0 || holdTicks < 1)
            throw new IllegalArgumentException("invalid alarm clock");
        Incident incident = incidents.get(root);
        if (incident == null) return false;
        if (incident.tick() > now || now - incident.tick() >= holdTicks) {
            incidents.remove(root);
            return false;
        }
        return true;
    }

    public OptionalLong witnessPosition(long root, long now, long holdTicks) {
        if (!active(root, now, holdTicks)) return OptionalLong.empty();
        Incident incident = incidents.get(root);
        return incident.witnessPosition() == null
                ? OptionalLong.empty() : OptionalLong.of(incident.witnessPosition());
    }

    public int trackedRoots() { return incidents.size(); }
}
