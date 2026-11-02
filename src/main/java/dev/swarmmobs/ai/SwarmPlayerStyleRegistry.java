package dev.swarmmobs.ai;

import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Session-local online opponent model. It stores only decaying counts of broad
 * damage-source categories against supported SWARM-MOBS entities. No names,
 * coordinates, chat, inventory contents or raw event history are retained.
 */
public final class SwarmPlayerStyleRegistry {
    public enum AttackStyle {
        MELEE,
        PROJECTILE,
        EXPLOSIVE,
        FIRE,
        MAGIC,
        OTHER,
        MIXED,
        UNKNOWN
    }

    public record ProfileSnapshot(
            double melee,
            double projectile,
            double explosive,
            double fire,
            double magic,
            double other,
            double sampleMass,
            long lastObservedTick
    ) {
        public double total() {
            return Math.max(0.0, melee) + Math.max(0.0, projectile)
                    + Math.max(0.0, explosive) + Math.max(0.0, fire)
                    + Math.max(0.0, magic) + Math.max(0.0, other);
        }

        public AttackStyle dominantStyle() {
            if (sampleMass < MIN_CONFIDENT_SAMPLES || total() <= 0.0) {
                return AttackStyle.UNKNOWN;
            }
            double[] values = {melee, projectile, explosive, fire, magic, other};
            AttackStyle[] styles = {
                    AttackStyle.MELEE, AttackStyle.PROJECTILE,
                    AttackStyle.EXPLOSIVE, AttackStyle.FIRE,
                    AttackStyle.MAGIC, AttackStyle.OTHER
            };
            int best = 0;
            double secondBest = 0.0;
            for (int i = 1; i < values.length; i++) {
                if (values[i] > values[best]) {
                    secondBest = values[best];
                    best = i;
                } else if (values[i] > secondBest) {
                    secondBest = values[i];
                }
            }
            double total = total();
            double dominantShare = values[best] / total;
            double dominanceMargin = (values[best] - secondBest) / total;
            return dominantShare >= DOMINANT_SHARE
                    && dominanceMargin >= MIN_DOMINANCE_MARGIN
                    ? styles[best] : AttackStyle.MIXED;
        }
    }

    public record Summary(
            int profiles,
            int samples,
            double meleeShare,
            double projectileShare,
            double explosiveShare,
            double fireShare,
            double magicShare,
            double otherShare,
            AttackStyle dominantStyle
    ) {}

    private static final int MAX_PROFILES = 64;
    private static final long HALF_LIFE_TICKS = 2400L;
    private static final long PROFILE_TTL_TICKS = 12000L;
    private static final double MIN_CONFIDENT_SAMPLES = 5.0;
    private static final double DOMINANT_SHARE = 0.55;
    private static final double MIN_DOMINANCE_MARGIN = 0.15;
    private static final Map<ServerLevel, LinkedHashMap<UUID, MutableProfile>> LEVELS =
            new WeakHashMap<>();

    private static final class MutableProfile {
        private final double[] counts = new double[6];
        private double sampleMass;
        private long lastDecayTick;
        private long lastObservedTick;

        private MutableProfile(long now) {
            lastDecayTick = now;
            lastObservedTick = now;
        }

        private void observe(AttackStyle style, long now) {
            decay(now);
            counts[index(style)] += 1.0;
            sampleMass += 1.0;
            lastObservedTick = now;
        }

        private void decay(long now) {
            if (now <= lastDecayTick) return;
            double factor = Math.pow(0.5,
                    (double) (now - lastDecayTick) / HALF_LIFE_TICKS);
            for (int i = 0; i < counts.length; i++) counts[i] *= factor;
            sampleMass *= factor;
            lastDecayTick = now;
        }

        private ProfileSnapshot snapshot(long now) {
            decay(now);
            return new ProfileSnapshot(counts[0], counts[1], counts[2],
                    counts[3], counts[4], counts[5], sampleMass, lastObservedTick);
        }
    }

    public static synchronized void observe(ServerLevel level, UUID playerId,
            AttackStyle style, long now) {
        if (level == null || playerId == null || style == null
                || style == AttackStyle.MIXED || style == AttackStyle.UNKNOWN) return;
        LinkedHashMap<UUID, MutableProfile> profiles =
                LEVELS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        expire(profiles, now);
        MutableProfile profile = profiles.remove(playerId);
        if (profile == null) {
            if (profiles.size() >= MAX_PROFILES) {
                Iterator<UUID> oldest = profiles.keySet().iterator();
                if (oldest.hasNext()) {
                    oldest.next();
                    oldest.remove();
                }
            }
            profile = new MutableProfile(now);
        }
        profile.observe(style, now);
        profiles.put(playerId, profile);
    }

    public static synchronized ProfileSnapshot snapshot(ServerLevel level,
            UUID playerId, long now) {
        if (level == null || playerId == null) return null;
        LinkedHashMap<UUID, MutableProfile> profiles = LEVELS.get(level);
        if (profiles == null) return null;
        expire(profiles, now);
        MutableProfile profile = profiles.get(playerId);
        return profile == null ? null : profile.snapshot(now);
    }

    public static synchronized Summary summary(ServerLevel level, long now) {
        if (level == null) return emptySummary();
        LinkedHashMap<UUID, MutableProfile> profiles = LEVELS.get(level);
        if (profiles == null || profiles.isEmpty()) return emptySummary();
        expire(profiles, now);

        double[] totals = new double[6];
        double sampleMass = 0.0;
        for (MutableProfile profile : profiles.values()) {
            ProfileSnapshot snapshot = profile.snapshot(now);
            totals[0] += snapshot.melee();
            totals[1] += snapshot.projectile();
            totals[2] += snapshot.explosive();
            totals[3] += snapshot.fire();
            totals[4] += snapshot.magic();
            totals[5] += snapshot.other();
            sampleMass += snapshot.sampleMass();
        }
        double total = 0.0;
        for (double value : totals) total += value;
        if (total <= 0.0) return emptySummary();
        ProfileSnapshot aggregate = new ProfileSnapshot(
                totals[0], totals[1], totals[2], totals[3], totals[4], totals[5],
                sampleMass, now);
        return new Summary(
                profiles.size(), (int) Math.round(sampleMass),
                totals[0] / total, totals[1] / total, totals[2] / total,
                totals[3] / total, totals[4] / total, totals[5] / total,
                aggregate.dominantStyle());
    }

    public static synchronized void clear(ServerLevel level) {
        if (level != null) LEVELS.remove(level);
    }

    public static synchronized void clearAll() {
        LEVELS.clear();
    }

    private static void expire(LinkedHashMap<UUID, MutableProfile> profiles, long now) {
        profiles.entrySet().removeIf(entry ->
                now - entry.getValue().lastObservedTick > PROFILE_TTL_TICKS);
    }

    private static int index(AttackStyle style) {
        return switch (style) {
            case MELEE -> 0;
            case PROJECTILE -> 1;
            case EXPLOSIVE -> 2;
            case FIRE -> 3;
            case MAGIC -> 4;
            case OTHER -> 5;
            case MIXED, UNKNOWN -> throw new IllegalArgumentException("Non-observation style");
        };
    }

    private static Summary emptySummary() {
        return new Summary(0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                AttackStyle.UNKNOWN);
    }

    private SwarmPlayerStyleRegistry() {}
}
