package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmSkeletonTacticsPolicyTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);

    @Test void sameTargetSkeletonsReceiveOppositeStableCrossfireLanes() {
        assertEquals(-1, SwarmSkeletonTacticsPolicy.preferredSide(A, List.of(B)));
        assertEquals(1, SwarmSkeletonTacticsPolicy.preferredSide(B, List.of(A)));
    }

    @Test void duplicateOrUnorderedPeerListsDoNotChangeLaneAssignment() {
        assertEquals(SwarmSkeletonTacticsPolicy.preferredSide(A, List.of(B)),
                SwarmSkeletonTacticsPolicy.preferredSide(A, List.of(B, A, B)));
    }

    @Test void mixedSquadSkeletonUsesOverwatchAndCrossfire() {
        var operation = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 2, 1, 0), 5, false, 0.9);
        var one = SwarmSkeletonTacticsPolicy.choose(A, List.of(),
                new SwarmLocalComposition(2, 1, 1, 0), operation, false, 0.9);
        var two = SwarmSkeletonTacticsPolicy.choose(A, List.of(B),
                new SwarmLocalComposition(2, 2, 1, 0), operation, false, 0.9);
        assertEquals(SwarmSkeletonTacticsPolicy.Mode.OVERWATCH, one.mode());
        assertEquals(SwarmSkeletonTacticsPolicy.Mode.CROSS_FIRE, two.mode());
        assertTrue(two.formationRadiusMultiplier() > one.formationRadiusMultiplier());
    }

    @Test void searchRelayDoesNotKeepCombatOverwatch() {
        var decision = SwarmSkeletonTacticsPolicy.choose(A, List.of(B),
                new SwarmLocalComposition(2, 2, 1, 0), null, true, 0.2);
        assertEquals(SwarmSkeletonTacticsPolicy.Mode.SEARCH_RELAY, decision.mode());
        assertEquals(1.0, decision.formationRadiusMultiplier(), 1.0e-9);
    }
}
