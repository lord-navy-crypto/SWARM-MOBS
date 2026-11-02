package dev.swarmmobs.colony;

/**
 * Ant/termite-inspired mission arbitration for hostile mob colonies.
 * A pure policy: no nest blocks are edited and existing combat goals remain authoritative.
 */
public final class MonsterColonyPolicy {
    private MonsterColonyPolicy() {}

    public enum Mission { PATROL, GATHER, BUILD, REPAIR, DEFEND, EXPAND }

    public record Signals(double threat, double supplyShortage,
                          double structuralDamage, double expansionOpportunity,
                          double congestion) {
        public Signals {
            unit(threat); unit(supplyShortage); unit(structuralDamage);
            unit(expansionOpportunity); unit(congestion);
        }
    }

    public record Capabilities(double combat, double hauling,
                               double construction, double scouting) {
        public Capabilities {
            unit(combat); unit(hauling); unit(construction); unit(scouting);
        }
    }

    private static void unit(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            throw new IllegalArgumentException("Normalized inputs must be finite in [0,1]");
        }
    }

    public static Mission select(Signals signals, Capabilities mob,
                                 Mission previous, double switchingCost) {
        if (signals == null || mob == null || previous == null)
            throw new IllegalArgumentException("Mission inputs cannot be null");
        unit(switchingCost);
        Mission result = previous;
        double best = score(previous, signals, mob);
        for (Mission candidate : Mission.values()) {
            double value = score(candidate, signals, mob)
                    - (candidate == previous ? 0 : switchingCost);
            if (value > best) {
                best = value;
                result = candidate;
            }
        }
        return result;
    }

    public static double score(Mission mission, Signals s, Capabilities c) {
        return switch (mission) {
            case PATROL -> 0.15 + 0.25 * c.scouting() - 0.1 * s.congestion();
            case GATHER -> s.supplyShortage() * c.hauling() - 0.25 * s.threat();
            case BUILD -> s.expansionOpportunity() * c.construction()
                    - 0.45 * s.threat() - 0.3 * s.congestion();
            case REPAIR -> s.structuralDamage() * c.construction() - 0.15 * s.threat();
            case DEFEND -> s.threat() * (0.3 + 0.7 * c.combat());
            case EXPAND -> s.expansionOpportunity() * c.scouting()
                    - 0.6 * s.threat() - 0.4 * s.congestion();
        };
    }
}
