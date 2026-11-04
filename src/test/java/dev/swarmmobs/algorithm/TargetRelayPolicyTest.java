package dev.swarmmobs.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TargetRelayPolicyTest {

    @Test
    void freshestValidObservationWins() {
        UUID target = UUID.randomUUID();

        TargetObservation selected = TargetRelayPolicy.selectFreshest(
                100,
                30,
                List.of(
                        observation(target, 75, 1.0),
                        observation(target, 95, 2.0),
                        observation(target, 90, 3.0)
                )
        ).orElseThrow();

        assertEquals(95, selected.observationTick());
        assertEquals(2.0, selected.x(), 1.0e-9);
    }

    @Test
    void expiredObservationIsRejected() {
        UUID target = UUID.randomUUID();

        assertTrue(TargetRelayPolicy.selectFreshest(
                100,
                20,
                List.of(observation(target, 79, 1.0))
        ).isEmpty());
    }

    @Test
    void futureObservationIsRejected() {
        UUID target = UUID.randomUUID();

        assertTrue(TargetRelayPolicy.selectFreshest(
                100,
                20,
                List.of(observation(target, 101, 1.0))
        ).isEmpty());
    }

    @Test
    void relayPreservesOriginalSpatialSnapshot() {
        UUID target = UUID.randomUUID();
        TargetObservation source = new TargetObservation(
                target,
                42,
                7.5,
                64.0,
                -3.0,
                1.0,
                0.0
        );

        TargetObservation selected = TargetRelayPolicy.selectFreshest(
                50,
                100,
                List.of(source)
        ).orElseThrow();

        assertSame(source, selected);
        assertEquals(42, selected.observationTick());
        assertEquals(7.5, selected.x(), 1.0e-9);
        assertEquals(-3.0, selected.z(), 1.0e-9);
    }

    @Test
    void newerDifferentTargetCanReplaceOlderMemory() {
        UUID oldTarget = UUID.randomUUID();
        UUID newTarget = UUID.randomUUID();

        TargetObservation selected = TargetRelayPolicy.selectFreshest(
                200,
                100,
                List.of(
                        observation(oldTarget, 150, 1.0),
                        observation(newTarget, 190, 2.0)
                )
        ).orElseThrow();

        assertEquals(newTarget, selected.targetId());
        assertEquals(190, selected.observationTick());
    }

    private static TargetObservation observation(UUID target, long tick, double x) {
        return new TargetObservation(target, tick, x, 64.0, 0.0, 0.0, 1.0);
    }

    @Test
    void sameTickObservationsAreFusedAndNewestTimestampIsPreserved() {
        UUID target = UUID.randomUUID();
        var result = TargetRelayPolicy.fuseFreshest(100, 40, List.of(
                new TargetObservation(target, 100, 10.0, 64.0, 0.0, 1.0, 0.0, 0.2, 0.0),
                new TargetObservation(target, 100, 12.0, 64.0, 0.0, 1.0, 0.0, 0.2, 0.0)
        ), 4, 8.0).orElseThrow();

        assertTrue(result.fused());
        assertEquals(2, result.contributors());
        assertEquals(100, result.observation().observationTick());
        assertEquals(11.0, result.observation().x(), 1.0e-9);
    }

    @Test
    void olderObservationsAreShortHorizonAlignedUsingTheirObservedVelocity() {
        UUID target = UUID.randomUUID();
        var result = TargetRelayPolicy.fuseFreshest(100, 40, List.of(
                new TargetObservation(target, 100, 10.0, 64.0, 0.0, 1.0, 0.0, 1.0, 0.0),
                new TargetObservation(target, 98, 8.0, 64.0, 0.0, 1.0, 0.0, 1.0, 0.0)
        ), 4, 8.0).orElseThrow();

        assertEquals(2, result.contributors());
        assertEquals(10.0, result.observation().x(), 1.0e-9);
        assertEquals(100, result.observation().observationTick());
    }

    @Test
    void spatialOutliersAndOtherTargetsAreExcluded() {
        UUID target = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TargetObservation anchor = new TargetObservation(
                target, 100, 10.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        var result = TargetRelayPolicy.fuseFreshest(100, 40, List.of(
                anchor,
                new TargetObservation(target, 100, 30.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0),
                new TargetObservation(other, 100, 11.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0)
        ), 4, 8.0).orElseThrow();

        assertEquals(1, result.contributors());
        assertSame(anchor, result.observation());
        assertEquals(target, result.observation().targetId());
        assertEquals(10.0, result.observation().x(), 1.0e-9);
    }

    @Test
    void duplicateRelaysDoNotArtificiallyIncreaseContributorCount() {
        UUID target = UUID.randomUUID();
        TargetObservation source = new TargetObservation(
                target, 100, 10.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0);
        var result = TargetRelayPolicy.fuseFreshest(100, 40,
                List.of(source, source, source), 4, 8.0).orElseThrow();

        assertFalse(result.fused());
        assertEquals(1, result.contributors());
        assertSame(source, result.observation());
    }

    @Test
    void fusionWindowAndObservationAgeAreEnforced() {
        UUID target = UUID.randomUUID();
        var narrow = TargetRelayPolicy.fuseFreshest(100, 40, List.of(
                new TargetObservation(target, 100, 10.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0),
                new TargetObservation(target, 95, 10.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0)
        ), 2, 8.0).orElseThrow();
        assertEquals(1, narrow.contributors());

        var expired = TargetRelayPolicy.fuseFreshest(100, 4, List.of(
                new TargetObservation(target, 100, 10.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0),
                new TargetObservation(target, 90, 10.0, 64.0, 0.0, 1.0, 0.0, 0.0, 0.0)
        ), 20, 8.0).orElseThrow();
        assertEquals(1, expired.contributors());
    }

    @Test
    void unknownPositionsCannotParticipateInFusion() {
        UUID target = UUID.randomUUID();
        TargetObservation known = observation(target, 100, 10.0);
        var result = TargetRelayPolicy.fuseFreshest(100, 40, List.of(
                known, TargetObservation.unknownPosition(target, 100)
        ), 4, 8.0).orElseThrow();

        assertEquals(1, result.contributors());
        assertSame(known, result.observation());
    }
}
