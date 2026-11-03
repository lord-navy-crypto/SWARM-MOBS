package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import dev.swarmmobs.agent.SwarmRole;
import dev.swarmmobs.agent.SwarmTaskType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SwarmFrontlineRotationPolicyTest {
    private static final UUID FRONT = new UUID(0L, 1L);
    private static final UUID RESERVE = new UUID(0L, 2L);
    private static final UUID OTHER = new UUID(0L, 3L);

    private static SwarmFrontlineRotationPolicy.Member member(
            UUID id, SwarmRole role, double health) {
        return new SwarmFrontlineRotationPolicy.Member(
                id, SwarmAgentArchetype.ASSAULT, role,
                SwarmTaskType.RESERVE, health);
    }

    private static List<SwarmFrontlineRotationPolicy.Member> roster() {
        return List.of(
                member(FRONT, SwarmRole.CHASER, 0.20),
                member(RESERVE, SwarmRole.REAR_PRESSURE, 0.90),
                member(OTHER, SwarmRole.FLANK_LEFT, 0.65)
        );
    }

    @Test
    void injuredFrontlinerRotatesToRearPressure() {
        var decision = SwarmFrontlineRotationPolicy.choose(
                FRONT, SwarmRole.CHASER, roster(), true);

        assertTrue(decision.rotated());
        assertTrue(decision.rotatingOut());
        assertFalse(decision.rotatingIn());
        assertEquals(SwarmRole.REAR_PRESSURE, decision.role());
    }

    @Test
    void healthiestReserveTakesTheFrontline() {
        var decision = SwarmFrontlineRotationPolicy.choose(
                RESERVE, SwarmRole.REAR_PRESSURE, roster(), true);

        assertTrue(decision.rotated());
        assertFalse(decision.rotatingOut());
        assertTrue(decision.rotatingIn());
        assertEquals(SwarmRole.CHASER, decision.role());
    }

    @Test
    void noRotationOccursWithoutAHealthyReserveOrMeaningfulHealthGap() {
        var weakReserve = List.of(
                member(FRONT, SwarmRole.CHASER, 0.20),
                member(RESERVE, SwarmRole.REAR_PRESSURE, 0.55)
        );
        var closeReserve = List.of(
                member(FRONT, SwarmRole.CHASER, 0.40),
                member(RESERVE, SwarmRole.REAR_PRESSURE, 0.75)
        );

        assertFalse(SwarmFrontlineRotationPolicy.choose(
                FRONT, SwarmRole.CHASER, weakReserve, true).rotated());
        assertFalse(SwarmFrontlineRotationPolicy.choose(
                FRONT, SwarmRole.CHASER, closeReserve, true).rotated());
    }

    @Test
    void committedWorkersAndOtherSpeciesAreNeverUsedAsRelief() {
        var committedReserve = List.of(
                member(FRONT, SwarmRole.CHASER, 0.20),
                new SwarmFrontlineRotationPolicy.Member(
                        RESERVE, SwarmAgentArchetype.ASSAULT,
                        SwarmRole.REAR_PRESSURE, SwarmTaskType.ENGINEERING, 0.95)
        );
        var spiderReserve = List.of(
                member(FRONT, SwarmRole.CHASER, 0.20),
                new SwarmFrontlineRotationPolicy.Member(
                        RESERVE, SwarmAgentArchetype.FLANKER,
                        SwarmRole.REAR_PRESSURE, SwarmTaskType.RESERVE, 0.95)
        );

        assertFalse(SwarmFrontlineRotationPolicy.choose(
                FRONT, SwarmRole.CHASER, committedReserve, true).rotated());
        assertFalse(SwarmFrontlineRotationPolicy.choose(
                FRONT, SwarmRole.CHASER, spiderReserve, true).rotated());
    }

    @Test
    void missingFlankAssignmentIsNotOverriddenByReservePromotion() {
        var decision = SwarmFrontlineRotationPolicy.choose(
                RESERVE, SwarmRole.FLANK_RIGHT, roster(), true);

        assertFalse(decision.rotated());
        assertEquals(SwarmRole.FLANK_RIGHT, decision.role());
    }

    @Test
    void rosterOrderDoesNotChangeTheDecisionAndDisabledModeIsBaseline() {
        var original = roster();
        var reversed = List.of(original.get(2), original.get(1), original.get(0));

        assertEquals(
                SwarmFrontlineRotationPolicy.choose(FRONT, SwarmRole.CHASER, original, true),
                SwarmFrontlineRotationPolicy.choose(FRONT, SwarmRole.CHASER, reversed, true));
        var disabled = SwarmFrontlineRotationPolicy.choose(
                RESERVE, SwarmRole.REAR_PRESSURE, original, false);
        assertFalse(disabled.rotated());
        assertEquals(SwarmRole.REAR_PRESSURE, disabled.role());
    }

    @Test
    void invalidHealthObservationIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                member(FRONT, SwarmRole.CHASER, Double.NaN));
    }
}