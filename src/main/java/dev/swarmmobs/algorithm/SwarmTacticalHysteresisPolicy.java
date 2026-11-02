package dev.swarmmobs.algorithm;

/** Prevents noisy observations from making a squad rapidly alternate tactics. */
public final class SwarmTacticalHysteresisPolicy {
    public record State(SwarmAdaptiveTacticsPolicy.Frame activeFrame,
            SwarmAdaptiveTacticsPolicy.Pattern pendingPattern, long pendingSinceTick) {
        public static State empty() {
            return new State(null, null, Long.MIN_VALUE);
        }
    }

    public record Result(State state, SwarmAdaptiveTacticsPolicy.Frame frame,
            boolean switched) {}

    public static Result update(State current, SwarmAdaptiveTacticsPolicy.Frame candidate,
            long now, int holdTicks) {
        State state = current == null ? State.empty() : current;
        if (candidate == null || candidate.pattern() == null || now < 0) {
            return new Result(state, state.activeFrame(), false);
        }
        if (state.activeFrame() == null) {
            State next = new State(candidate, null, Long.MIN_VALUE);
            return new Result(next, candidate, true);
        }
        if (candidate.pattern() == state.activeFrame().pattern()) {
            State next = new State(candidate, null, Long.MIN_VALUE);
            return new Result(next, candidate, false);
        }
        if (holdTicks <= 0) {
            State next = new State(candidate, null, Long.MIN_VALUE);
            return new Result(next, candidate, true);
        }
        if (state.pendingPattern() != candidate.pattern()
                || state.pendingSinceTick() == Long.MIN_VALUE
                || now < state.pendingSinceTick()) {
            State next = new State(state.activeFrame(), candidate.pattern(), now);
            return new Result(next, state.activeFrame(), false);
        }
        if (now - state.pendingSinceTick() >= holdTicks) {
            State next = new State(candidate, null, Long.MIN_VALUE);
            return new Result(next, candidate, true);
        }
        return new Result(state, state.activeFrame(), false);
    }

    private SwarmTacticalHysteresisPolicy() {}
}
