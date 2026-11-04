package dev.swarmmobs.ai;

import dev.swarmmobs.algorithm.TargetObservation;
import net.minecraft.server.level.ServerLevel;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Session-local, per-dimension player movement summaries. Only decaying style
 * counts are retained; positions, paths and raw observation histories are not.
 */
public final class SwarmPlayerMotionRegistry {
    public enum MotionStyle { STATIONARY, WALKING, SPRINTING, ERRATIC, MIXED, UNKNOWN }

    public record ProfileSnapshot(double stationary, double walking, double sprinting,
            double erratic, double sampleMass, long lastObservedTick) {
        public double total() {
            return Math.max(0.0, stationary) + Math.max(0.0, walking)
                    + Math.max(0.0, sprinting) + Math.max(0.0, erratic);
        }

        public MotionStyle dominantStyle() {
            if (sampleMass < MIN_CONFIDENT_SAMPLES || total() <= 0.0) return MotionStyle.UNKNOWN;
            double[] values = {stationary, walking, sprinting, erratic};
            MotionStyle[] styles = {MotionStyle.STATIONARY, MotionStyle.WALKING,
                    MotionStyle.SPRINTING, MotionStyle.ERRATIC};
            int best = 0;
            double second = 0.0;
            for (int i = 1; i < values.length; i++) {
                if (values[i] > values[best]) {
                    second = values[best];
                    best = i;
                } else if (values[i] > second) {
                    second = values[i];
                }
            }
            double total = total();
            return values[best] / total >= DOMINANT_SHARE
                    && (values[best] - second) / total >= MIN_DOMINANCE_MARGIN
                    ? styles[best] : MotionStyle.MIXED;
        }
    }

    public record Summary(int profiles, int samples, double stationaryShare,
            double walkingShare, double sprintingShare, double erraticShare,
            MotionStyle dominantStyle) {}

    private static final int MAX_PROFILES = 64;
    private static final double MIN_CONFIDENT_SAMPLES = 5.0;
    private static final double DOMINANT_SHARE = 0.55;
    private static final double MIN_DOMINANCE_MARGIN = 0.15;
    private static final Map<ServerLevel, LinkedHashMap<UUID, SwarmPlayerMotionProfile>> LEVELS =
            new WeakHashMap<>();

    public static synchronized void observe(ServerLevel level,
            TargetObservation observation, long now) {
        if (level == null || observation == null || observation.targetId() == null
                || !observation.hasFiniteVelocity() || now < 0) return;
        LinkedHashMap<UUID, SwarmPlayerMotionProfile> profiles =
                LEVELS.computeIfAbsent(level, ignored -> new LinkedHashMap<>());
        expire(profiles, now);
        UUID playerId = observation.targetId();
        SwarmPlayerMotionProfile profile = profiles.remove(playerId);
        if (profile == null) {
            if (profiles.size() >= MAX_PROFILES) {
                Iterator<UUID> oldest = profiles.keySet().iterator();
                if (oldest.hasNext()) {
                    oldest.next();
                    oldest.remove();
                }
            }
            profile = new SwarmPlayerMotionProfile(now);
        }
        profile.observe(observation.velocityX(), observation.velocityZ(),
                observation.observationTick(), now);
        profiles.put(playerId, profile);
    }

    public static synchronized ProfileSnapshot snapshot(ServerLevel level,
            UUID playerId, long now) {
        if (level == null || playerId == null) return null;
        LinkedHashMap<UUID, SwarmPlayerMotionProfile> profiles = LEVELS.get(level);
        if (profiles == null) return null;
        expire(profiles, now);
        SwarmPlayerMotionProfile profile = profiles.get(playerId);
        return profile == null ? null : profile.snapshot(now);
    }

    public static synchronized Summary summary(ServerLevel level, long now) {
        if (level == null) return emptySummary();
        LinkedHashMap<UUID, SwarmPlayerMotionProfile> profiles = LEVELS.get(level);
        if (profiles == null || profiles.isEmpty()) return emptySummary();
        expire(profiles, now);
        double[] totals = new double[4];
        double sampleMass = 0.0;
        for (SwarmPlayerMotionProfile profile : profiles.values()) {
            ProfileSnapshot snapshot = profile.snapshot(now);
            totals[0] += snapshot.stationary();
            totals[1] += snapshot.walking();
            totals[2] += snapshot.sprinting();
            totals[3] += snapshot.erratic();
            sampleMass += snapshot.sampleMass();
        }
        double total = 0.0;
        for (double value : totals) total += value;
        if (total <= 0.0) return emptySummary();
        ProfileSnapshot aggregate = new ProfileSnapshot(
                totals[0], totals[1], totals[2], totals[3], sampleMass, now);
        return new Summary(profiles.size(), (int) Math.round(sampleMass),
                totals[0] / total, totals[1] / total, totals[2] / total,
                totals[3] / total, aggregate.dominantStyle());
    }

    public static synchronized void clear(ServerLevel level) {
        if (level != null) LEVELS.remove(level);
    }

    public static synchronized void clearAll() {
        LEVELS.clear();
    }

    private static void expire(LinkedHashMap<UUID, SwarmPlayerMotionProfile> profiles, long now) {
        profiles.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private static Summary emptySummary() {
        return new Summary(0, 0, 0.0, 0.0, 0.0, 0.0, MotionStyle.UNKNOWN);
    }

    private SwarmPlayerMotionRegistry() {}
}
