package dev.swarmmobs.bee;

/**
 * Honeybee nest expansion requires population pressure and food surplus,
 * unlike the monster nest's deliberately full resource reserve requirement.
 */
public final class BeeHiveGrowthPolicy {
    private BeeHiveGrowthPolicy() {}

    public record State(int bees, int capacity, int storedHoney, int winterReserve,
                        int wax, int waxCost, int adjacentFreeSites, int extensions,
                        int maxExtensions) {
        public State {
            if (bees < 0 || capacity < 1 || storedHoney < 0 || winterReserve < 0
                    || wax < 0 || waxCost < 1 || adjacentFreeSites < 0
                    || extensions < 0 || maxExtensions < 0 || extensions > maxExtensions)
                throw new IllegalArgumentException("invalid hive growth state");
        }
    }

    public record SiteSignals(int nearbyFlowers, int nearbyHives, int nearbyThreats) {
        public SiteSignals {
            if (nearbyFlowers < 0 || nearbyHives < 0 || nearbyThreats < 0)
                throw new IllegalArgumentException("negative site signal");
        }
    }

    public static boolean canExpand(State s) {
        if (s == null) throw new IllegalArgumentException("state");
        return s.bees() >= s.capacity()
                && s.storedHoney() > s.winterReserve()
                && s.wax() >= s.waxCost()
                && s.adjacentFreeSites() > 0
                && s.extensions() < s.maxExtensions();
    }

    /** Prefer connected-hive sites with nearby forage and lower observed threat/crowding. */
    public static double expansionSiteScore(SiteSignals signals) {
        if (signals == null) throw new IllegalArgumentException("signals");
        double flowers = Math.min(1.0, signals.nearbyFlowers() / 8.0);
        double hiveCrowding = Math.min(1.0, signals.nearbyHives() / 4.0);
        double threat = Math.min(1.0, signals.nearbyThreats() / 3.0);
        return 0.55 * flowers + 0.20 * (1.0 - hiveCrowding)
                + 0.25 * (1.0 - threat);
    }

    /**
     * Vanilla-only prototype: one hive may build a neighbor when full of
     * occupants and at full honey level. No lifetime extension count cap.
     */
    public static boolean canGrowVanilla(int occupants, int hiveCapacity,
                                         int honeyLevel, int fullHoneyLevel,
                                         boolean calm, boolean hasAirSite) {
        if (occupants < 0 || hiveCapacity < 1 || honeyLevel < 0
                || fullHoneyLevel < 1)
            throw new IllegalArgumentException("invalid vanilla hive readings");
        return occupants >= hiveCapacity && honeyLevel >= fullHoneyLevel
                && calm && hasAirSite;
    }
}
