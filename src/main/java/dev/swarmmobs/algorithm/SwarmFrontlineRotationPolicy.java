package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import dev.swarmmobs.agent.SwarmRole;
import dev.swarmmobs.agent.SwarmTaskType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Local casualty-aware Zombie frontline rotation.
 *
 * A badly injured CHASER may fall back to REAR_PRESSURE when a healthy,
 * uncommitted assault reserve is already present in the same observed squad.
 * The reserve can take the front slot. This changes only formation roles:
 * vanilla target selection, attacks, damage and navigation remain authoritative.
 */
public final class SwarmFrontlineRotationPolicy {
    public static final double ROTATE_OUT_HEALTH = 0.35;
    public static final double RELIEF_HEALTH = 0.70;
    public static final double MIN_HEALTH_ADVANTAGE = 0.35;

    public record Member(UUID id, SwarmAgentArchetype archetype,
                         SwarmRole role, SwarmTaskType task,
                         double healthFraction) {
        public Member {
            if (id == null || archetype == null
                    || !Double.isFinite(healthFraction)
                    || healthFraction < 0.0 || healthFraction > 1.0) {
                throw new IllegalArgumentException("invalid frontline member");
            }
            role = role == null ? SwarmRole.REAR_PRESSURE : role;
            task = task == null ? SwarmTaskType.RESERVE : task;
        }
    }

    public record Decision(SwarmRole role, boolean rotated,
                           boolean rotatingOut, boolean rotatingIn) {}

    public static Decision choose(UUID selfId, SwarmRole baseline,
                                  List<Member> roster, boolean enabled) {
        SwarmRole fallback = baseline == null ? SwarmRole.CHASER : baseline;
        Decision unchanged = new Decision(fallback, false, false, false);
        if (!enabled || selfId == null || roster == null || roster.size() < 2) {
            return unchanged;
        }

        Map<UUID, Member> unique = new TreeMap<>();
        for (Member member : roster) {
            if (member != null) unique.putIfAbsent(member.id(), member);
        }
        Member self = unique.get(selfId);
        if (self == null || self.archetype() != SwarmAgentArchetype.ASSAULT
                || committed(self.task())) {
            return unchanged;
        }

        boolean leftCovered = unique.values().stream()
                .anyMatch(member -> member.role() == SwarmRole.FLANK_LEFT
                        && !committed(member.task()));
        boolean rightCovered = unique.values().stream()
                .anyMatch(member -> member.role() == SwarmRole.FLANK_RIGHT
                        && !committed(member.task()));
        // A reserve is eligible only after both local flank duties are covered.
        // Otherwise it may be the only member available to fill a missing side.
        if (!leftCovered || !rightCovered) return unchanged;

        Member frontliner = unique.values().stream()
                .filter(member -> member.archetype() == SwarmAgentArchetype.ASSAULT
                        && member.role() == SwarmRole.CHASER
                        && !committed(member.task()))
                .min(Comparator.comparingDouble(Member::healthFraction)
                        .thenComparing(Member::id))
                .orElse(null);
        if (frontliner == null || frontliner.healthFraction() > ROTATE_OUT_HEALTH) {
            return unchanged;
        }

        Member relief = unique.values().stream()
                .filter(member -> member.archetype() == SwarmAgentArchetype.ASSAULT
                        && member.role() == SwarmRole.REAR_PRESSURE
                        && !committed(member.task())
                        && !member.id().equals(frontliner.id()))
                .sorted(Comparator.comparingDouble(Member::healthFraction).reversed()
                        .thenComparing(Member::id))
                .findFirst()
                .orElse(null);
        if (relief == null || relief.healthFraction() < RELIEF_HEALTH
                || relief.healthFraction() - frontliner.healthFraction()
                        < MIN_HEALTH_ADVANTAGE) {
            return unchanged;
        }

        if (selfId.equals(frontliner.id())) {
            return new Decision(SwarmRole.REAR_PRESSURE, true, true, false);
        }
        // Do not override a role-coverage assignment that is filling a missing
        // flank; only an actual rear reserve may be promoted to the front.
        if (selfId.equals(relief.id()) && fallback == SwarmRole.REAR_PRESSURE) {
            return new Decision(SwarmRole.CHASER, true, false, true);
        }
        return unchanged;
    }

    private static boolean committed(SwarmTaskType task) {
        return task == SwarmTaskType.ENGINEERING || task == SwarmTaskType.MATERIAL;
    }

    private SwarmFrontlineRotationPolicy() {}
}