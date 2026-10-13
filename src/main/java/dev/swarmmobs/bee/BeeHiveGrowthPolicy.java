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

    public static boolean canExpand(State s) {
        if (s == null) throw new IllegalArgumentException("state");
        return s.bees() >= s.capacity()
                && s.storedHoney() > s.winterReserve()
                && s.wax() >= s.waxCost()
                && s.adjacentFreeSites() > 0
                && s.extensions() < s.maxExtensions();
    }
}
