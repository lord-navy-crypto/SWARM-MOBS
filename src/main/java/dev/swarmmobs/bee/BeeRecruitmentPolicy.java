package dev.swarmmobs.bee;

/**
 * Abstract waggle-dance recruitment priority.
 * Recruitment never reveals flower positions the recruiter has not observed.
 */
public final class BeeRecruitmentPolicy {
    private BeeRecruitmentPolicy() {}

    public static double priority(double patchUtility, double confidence,
                                  double receiverNeed, double congestion) {
        unit(confidence); unit(receiverNeed); unit(congestion);
        if (!Double.isFinite(patchUtility) || patchUtility < -2 || patchUtility > 1)
            throw new IllegalArgumentException("patchUtility outside supported score range");
        return Math.max(0, patchUtility) * confidence * receiverNeed
                * (1 - congestion);
    }

    private static void unit(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1)
            throw new IllegalArgumentException("Expected normalized [0,1] value");
    }
}
