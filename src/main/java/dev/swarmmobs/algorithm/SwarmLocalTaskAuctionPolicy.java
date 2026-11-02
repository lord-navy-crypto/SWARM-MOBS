package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import dev.swarmmobs.agent.SwarmTaskType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.ToDoubleFunction;

/**
 * Deterministic, bounded local matching for one same-target neighborhood.
 *
 * Every nearby agent evaluates the same candidate set and demand snapshot.
 * A greedy maximum-utility matching gives each agent at most one task and
 * respects each task's desired local headcount. UUID tie-breaking makes the
 * result independent of entity iteration order. This is a local computation,
 * not a central squad controller or a persistent/global assignment service.
 */
public final class SwarmLocalTaskAuctionPolicy {

    public record Member(
            UUID entityId,
            SwarmAgentArchetype archetype,
            SwarmTaskType currentTask,
            Map<SwarmTaskType, Double> experienceByTask,
            double normalizedDistance,
            boolean combatBusy
    ) {
        public Member {
            currentTask = currentTask == null ? SwarmTaskType.RESERVE : currentTask;
            experienceByTask = experienceByTask == null
                    ? Map.of()
                    : Map.copyOf(experienceByTask);
        }

        public double experience(SwarmTaskType task) {
            if (task == null) return 0.0;
            return experienceByTask.getOrDefault(task, 0.0);
        }
    }

    public record Assignment(
            SwarmTaskType task,
            double utility,
            double demand,
            double responseThreshold
    ) {}

    public record Result(
            Map<UUID, Assignment> assignments,
            int candidateCount,
            int assignedCount
    ) {
        public Assignment assignment(UUID entityId) {
            return entityId == null ? null : assignments.get(entityId);
        }
    }

    private record Edge(
            UUID entityId,
            SwarmTaskType task,
            double utility,
            double demand,
            double responseThreshold
    ) {}

    public static Result allocate(
            List<Member> input,
            ToDoubleFunction<SwarmTaskType> demandProvider
    ) {
        if (input == null || input.isEmpty() || demandProvider == null) {
            return new Result(Map.of(), 0, 0);
        }

        // A sorted map gives every observer the same candidate ordering and
        // protects the auction from duplicate peer references.
        Map<UUID, Member> unique = new TreeMap<>();
        for (Member member : input) {
            if (member != null && member.entityId() != null
                    && member.archetype() != null) {
                unique.putIfAbsent(member.entityId(), member);
            }
        }
        if (unique.isEmpty()) return new Result(Map.of(), 0, 0);

        List<Member> members = List.copyOf(unique.values());
        int groupSize = members.size();
        EnumMap<SwarmTaskType, Integer> occupancy = new EnumMap<>(SwarmTaskType.class);
        for (Member member : members) {
            occupancy.merge(member.currentTask(), 1, Integer::sum);
        }

        Map<UUID, Assignment> assignments = new LinkedHashMap<>();
        Map<UUID, Double> reserveUtility = new HashMap<>();
        for (Member member : members) {
            SwarmTaskBidPolicy.Candidate candidate = candidate(member, SwarmTaskType.RESERVE);
            double reserveDemand = clamp01(demandProvider.applyAsDouble(SwarmTaskType.RESERVE));
            SwarmTaskBidPolicy.Bid reserveBid = SwarmTaskBidPolicy.bid(
                    candidate, SwarmTaskType.RESERVE, reserveDemand);
            double utility = Double.isFinite(reserveBid.utility())
                    ? reserveBid.utility() : 0.0;
            reserveUtility.put(member.entityId(), utility);
            assignments.put(member.entityId(), new Assignment(
                    SwarmTaskType.RESERVE,
                    utility,
                    reserveDemand,
                    SwarmTaskBidPolicy.responseThreshold(
                            member.entityId(),
                            SwarmTaskType.RESERVE,
                            SwarmTaskBidPolicy.capability(
                                    member.archetype(), SwarmTaskType.RESERVE))
            ));
        }

        List<Edge> edges = new ArrayList<>();
        EnumMap<SwarmTaskType, Integer> capacity = new EnumMap<>(SwarmTaskType.class);

        for (SwarmTaskType task : SwarmTaskType.values()) {
            if (task == SwarmTaskType.RESERVE) continue;
            double baseDemand = clamp01(demandProvider.applyAsDouble(task));
            int desired = SwarmTaskSaturationPolicy.desiredHeadcount(
                    task, baseDemand, groupSize);
            if (desired <= 0 || baseDemand <= 0.0) continue;
            capacity.put(task, Math.min(groupSize, desired));

            int currentOccupancy = occupancy.getOrDefault(task, 0);
            for (Member member : members) {
                int otherOccupancy = currentOccupancy
                        - (member.currentTask() == task ? 1 : 0);
                double adjustedDemand = SwarmTaskSaturationPolicy.adjustedDemand(
                        member.archetype(),
                        task,
                        baseDemand,
                        groupSize,
                        otherOccupancy,
                        member.currentTask()
                );
                SwarmTaskBidPolicy.Bid bid = SwarmTaskBidPolicy.bid(
                        candidate(member, task), task, adjustedDemand);
                double baseline = reserveUtility.get(member.entityId());
                if (Double.isFinite(bid.utility())
                        && bid.utility() > baseline + 1.0e-9) {
                    double capability = SwarmTaskBidPolicy.capability(
                            member.archetype(), task);
                    edges.add(new Edge(
                            member.entityId(),
                            task,
                            bid.utility(),
                            adjustedDemand,
                            SwarmTaskBidPolicy.responseThreshold(
                                    member.entityId(), task, capability)
                    ));
                }
            }
        }

        edges.sort(Comparator
                .comparingDouble(Edge::utility).reversed()
                .thenComparing(Comparator.comparingDouble(Edge::demand).reversed())
                .thenComparingInt(edge -> edge.task().ordinal())
                .thenComparing(edge -> edge.entityId().toString()));

        Set<UUID> assignedMembers = new HashSet<>();
        EnumMap<SwarmTaskType, Integer> usedCapacity = new EnumMap<>(SwarmTaskType.class);
        for (Edge edge : edges) {
            if (assignedMembers.contains(edge.entityId())) continue;
            int limit = capacity.getOrDefault(edge.task(), 0);
            int used = usedCapacity.getOrDefault(edge.task(), 0);
            if (used >= limit) continue;

            assignments.put(edge.entityId(), new Assignment(
                    edge.task(), edge.utility(), edge.demand(), edge.responseThreshold()));
            assignedMembers.add(edge.entityId());
            usedCapacity.put(edge.task(), used + 1);
        }

        int assignedCount = (int) assignments.values().stream()
                .filter(assignment -> assignment.task() != SwarmTaskType.RESERVE)
                .count();
        return new Result(Map.copyOf(assignments), members.size(), assignedCount);
    }

    private static SwarmTaskBidPolicy.Candidate candidate(
            Member member,
            SwarmTaskType task
    ) {
        return new SwarmTaskBidPolicy.Candidate(
                member.entityId(),
                member.archetype(),
                member.currentTask(),
                member.experience(task),
                member.normalizedDistance(),
                member.combatBusy()
        );
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0;
        return Math.max(0.0, Math.min(1.0, value));
    }

    private SwarmLocalTaskAuctionPolicy() {}
}
