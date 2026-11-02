package dev.swarmmobs.bee;

public final class BeeEcologyPolicy {
    public static final int MIN_HIVE_CAPACITY = 3;
    public static final int MAX_HIVE_CAPACITY = 16;
    public static final int MAX_COLONY_POPULATION = 128;
    private BeeEcologyPolicy() {}

    public static int hiveCapacity(int configured) {
        return clamp(configured, MIN_HIVE_CAPACITY, MAX_HIVE_CAPACITY);
    }

    public static int populationLimit(int configured) {
        return clamp(configured, 3, MAX_COLONY_POPULATION);
    }

    /** Minecraft advances age by one tick itself; this returns the extra bounded advance. */
    public static int acceleratedAge(int currentAge, int multiplier) {
        if (multiplier < 1 || multiplier > 8)
            throw new IllegalArgumentException("age multiplier must be in [1,8]");
        int extra = multiplier - 1;
        if (currentAge < 0) return Math.min(0, currentAge + extra);
        if (currentAge > 0) return Math.max(0, currentAge - extra);
        return 0;
    }

    public static int honeyBonusInterval(int baseInterval, int multiplier, int storedBees) {
        if (baseInterval < 100 || multiplier < 1 || multiplier > 5 || storedBees < 0)
            throw new IllegalArgumentException("invalid honey production settings");
        int workerFactor = Math.max(1, (storedBees + 2) / 3);
        return Math.max(100, baseInterval / (multiplier * workerFactor));
    }

    public static boolean shouldRecruit(int population, int populationLimit,
            int storedHoney, int honeyReserve, int nearbyFlowers,
            boolean enabled, boolean mobSpawning) {
        if (population < 0 || populationLimit < 3 || storedHoney < 0
                || honeyReserve < 0 || nearbyFlowers < 0)
            throw new IllegalArgumentException("invalid colony recruitment state");
        return enabled && mobSpawning && population < populationLimit
                && storedHoney > honeyReserve && nearbyFlowers > 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
