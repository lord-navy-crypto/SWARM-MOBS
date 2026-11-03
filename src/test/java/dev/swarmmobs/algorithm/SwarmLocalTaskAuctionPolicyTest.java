package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import dev.swarmmobs.agent.SwarmTaskType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SwarmLocalTaskAuctionPolicyTest {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID D = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private static SwarmLocalTaskAuctionPolicy.Member member(
            UUID id,
            SwarmAgentArchetype archetype,
            SwarmTaskType currentTask,
            double distance
    ) {
        return new SwarmLocalTaskAuctionPolicy.Member(
                id, archetype, currentTask, Map.of(), distance, false);
    }

    @Test
    void eachMemberReceivesAtMostOneTaskAndTaskCapacityIsRespected() {
        List<SwarmLocalTaskAuctionPolicy.Member> members = List.of(
                member(A, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.1),
                member(B, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.2),
                member(C, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.3),
                member(D, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.4)
        );

        var result = SwarmLocalTaskAuctionPolicy.allocate(
                members,
                task -> switch (task) {
                    case ENGINEERING -> 1.0;
                    case MATERIAL -> 0.9;
                    case FLANK -> 0.8;
                    case RESERVE -> 0.30;
                    default -> 0.0;
                });

        assertEquals(4, result.candidateCount());
        assertEquals(4, result.assignments().size());
        assertTrue(result.assignedCount() <= 4);
        assertTrue(result.assignments().values().stream()
                .filter(a -> a.task() == SwarmTaskType.ENGINEERING).count() <= 1);
        assertTrue(result.assignments().values().stream()
                .filter(a -> a.task() == SwarmTaskType.MATERIAL).count() <= 1);
        assertTrue(result.assignments().values().stream()
                .filter(a -> a.task() == SwarmTaskType.FLANK).count() <= 1);
    }

    @Test
    void specialistWinsItsNativeTaskOverPoorCapabilityPeers() {
        var result = SwarmLocalTaskAuctionPolicy.allocate(List.of(
                member(A, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.0),
                member(B, SwarmAgentArchetype.FLANKER, SwarmTaskType.RESERVE, 0.5),
                member(C, SwarmAgentArchetype.RANGED_SUPPORT, SwarmTaskType.RESERVE, 0.0)
        ), task -> task == SwarmTaskType.FLANK ? 1.0 : 0.0);

        assertEquals(SwarmTaskType.FLANK, result.assignment(B).task());
        assertNotEquals(SwarmTaskType.FLANK, result.assignment(A).task());
        assertNotEquals(SwarmTaskType.FLANK, result.assignment(C).task());
    }

    @Test
    void experienceAndDistanceInfluenceWhichEligibleAgentWins() {
        EnumMap<SwarmTaskType, Double> experienced = new EnumMap<>(SwarmTaskType.class);
        experienced.put(SwarmTaskType.ENGINEERING, 1.0);
        var result = SwarmLocalTaskAuctionPolicy.allocate(List.of(
                new SwarmLocalTaskAuctionPolicy.Member(
                        A, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE,
                        experienced, 0.0, false),
                member(B, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 1.0)
        ), task -> task == SwarmTaskType.ENGINEERING ? 1.0 : 0.0);

        assertEquals(SwarmTaskType.ENGINEERING, result.assignment(A).task());
        assertEquals(SwarmTaskType.RESERVE, result.assignment(B).task());
    }

    @Test
    void allocationIsIndependentOfCandidateIterationOrder() {
        List<SwarmLocalTaskAuctionPolicy.Member> members = new ArrayList<>(List.of(
                member(A, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.1),
                member(B, SwarmAgentArchetype.FLANKER, SwarmTaskType.RESERVE, 0.2),
                member(C, SwarmAgentArchetype.BREACHER, SwarmTaskType.RESERVE, 0.3),
                member(D, SwarmAgentArchetype.RANGED_SUPPORT, SwarmTaskType.RESERVE, 0.4)
        ));
        var first = SwarmLocalTaskAuctionPolicy.allocate(members,
                task -> task == SwarmTaskType.SEARCH || task == SwarmTaskType.FLANK
                        || task == SwarmTaskType.BREACH
                        || task == SwarmTaskType.RANGED_SUPPORT ? 1.0 : 0.0);
        java.util.Collections.reverse(members);
        var second = SwarmLocalTaskAuctionPolicy.allocate(members,
                task -> task == SwarmTaskType.SEARCH || task == SwarmTaskType.FLANK
                        || task == SwarmTaskType.BREACH
                        || task == SwarmTaskType.RANGED_SUPPORT ? 1.0 : 0.0);

        assertEquals(first.assignments(), second.assignments());
    }

    @Test
    void duplicatePeerReferencesDoNotCreateExtraCandidatesOrSlots() {
        var a = member(A, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.0);
        var b = member(B, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.0);
        var result = SwarmLocalTaskAuctionPolicy.allocate(List.of(a, b, a, b),
                task -> task == SwarmTaskType.ENGINEERING ? 1.0 : 0.0);

        assertEquals(2, result.candidateCount());
        assertEquals(2, result.assignments().size());
    }

    @Test
    void nativeCoreMembersAreNotCrowdedOutByGenericTaskHeadcount() {
        var result = SwarmLocalTaskAuctionPolicy.allocate(List.of(
                member(A, SwarmAgentArchetype.RANGED_SUPPORT, SwarmTaskType.RANGED_SUPPORT, 0.0),
                member(B, SwarmAgentArchetype.RANGED_SUPPORT, SwarmTaskType.RANGED_SUPPORT, 0.0),
                member(C, SwarmAgentArchetype.RANGED_SUPPORT, SwarmTaskType.RANGED_SUPPORT, 0.0),
                member(D, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.0)
        ), task -> task == SwarmTaskType.RANGED_SUPPORT ? 0.95 : 0.0);

        assertEquals(3, result.assignments().values().stream()
                .filter(a -> a.task() == SwarmTaskType.RANGED_SUPPORT).count());
    }

    @Test
    void noEligibleSpecializedBidFallsBackToReserve() {
        var result = SwarmLocalTaskAuctionPolicy.allocate(List.of(
                member(A, SwarmAgentArchetype.ASSAULT, SwarmTaskType.RESERVE, 0.0)
        ), task -> task == SwarmTaskType.SEARCH ? 0.01
                : task == SwarmTaskType.RESERVE ? 0.30 : 0.0);

        assertEquals(SwarmTaskType.RESERVE, result.assignment(A).task());
    }
}
