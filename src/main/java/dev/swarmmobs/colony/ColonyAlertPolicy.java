package dev.swarmmobs.colony;

/**
 * Colony-wide emergency arbitration: jobs are suspended during combat.
 * Alert propagation is performed by existing local communication infrastructure,
 * NOT by instant world-wide target knowledge.
 */
public final class ColonyAlertPolicy {
    private ColonyAlertPolicy() {}

    public enum Mode { WORK, MOBILIZING, COMBAT, SEARCH, RECOVER }

    public record Observation(boolean playerDirectlySeen, boolean nestAttacked,
                              boolean allyAlertReceived, boolean opponentVisible,
                              boolean targetMemoryValid, long quietTicks) {
        public Observation {
            if (quietTicks < 0) throw new IllegalArgumentException("quietTicks");
        }
    }

    public static Mode next(Mode current, Observation observation, long recoverAfterTicks) {
        if (current == null || observation == null || recoverAfterTicks < 1)
            throw new IllegalArgumentException("invalid combat state inputs");
        // Local sight or damage raises an alarm; received messages can summon distant workers.
        if (observation.playerDirectlySeen() || observation.nestAttacked())
            return Mode.COMBAT;
        if (observation.opponentVisible())
            return Mode.COMBAT;
        if (observation.allyAlertReceived() && current != Mode.COMBAT)
            return Mode.MOBILIZING;
        return switch (current) {
            case WORK -> Mode.WORK;
            case MOBILIZING -> observation.targetMemoryValid() ? Mode.SEARCH : Mode.RECOVER;
            case COMBAT -> observation.targetMemoryValid() ? Mode.SEARCH : Mode.RECOVER;
            case SEARCH -> observation.targetMemoryValid() ? Mode.SEARCH : Mode.RECOVER;
            case RECOVER -> observation.quietTicks() >= recoverAfterTicks ? Mode.WORK : Mode.RECOVER;
        };
    }

    public static boolean suspendsColonyJobs(Mode mode) {
        return mode != Mode.WORK;
    }
}
