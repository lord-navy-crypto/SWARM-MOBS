package dev.swarmmobs.ai;

import dev.swarmmobs.experiment.SwarmExperimentMetrics;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SwarmAiLearningMemory extends SavedData {
    private static final String DATA_NAME = "swarmmobs_ai_learning";
    private static final int DATA_VERSION = 1;
    private static final int MAX_RUNS = 40;
    private static final SavedData.Factory<SwarmAiLearningMemory> FACTORY =
            new SavedData.Factory<>(SwarmAiLearningMemory::new, SwarmAiLearningMemory::load);

    private final Deque<RunRecord> runs = new ArrayDeque<>();
    private long nextRunId = 1L;
    private EvaluationEntry lastEvaluation;

    public static SwarmAiLearningMemory get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(FACTORY, DATA_NAME);
    }

    public synchronized RunRecord record(
            SwarmExperimentMetrics.Snapshot metrics,
            String preset,
            int seed,
            long gameTick
    ) {
        if (metrics == null || !metrics.active()) {
            throw new IllegalStateException("Start an experiment before recording its results.");
        }
        if (metrics.agentCount() <= 0) {
            throw new IllegalStateException("No supported swarm agents were measured.");
        }
        if (metrics.elapsedTicks() < 200L) {
            throw new IllegalStateException("Collect at least 200 experiment ticks before recording.");
        }
        if (!runs.isEmpty() && runs.getLast().gameTick() == gameTick) {
            throw new IllegalStateException("An experiment snapshot was already recorded at this game tick.");
        }

        RunRecord record = new RunRecord(
                nextRunId++,
                Math.max(0L, gameTick),
                clean(preset, 32),
                seed,
                Math.max(0L, metrics.elapsedTicks()),
                Math.max(0, metrics.agentCount()),
                nonNegative(metrics.communicationDelivered()),
                nonNegative(metrics.communicationDropped()),
                nonNegative(metrics.obstacleDetours()),
                nonNegative(metrics.recoveries()),
                nonNegative(metrics.recoveryPlanningAttempts()),
                nonNegative(metrics.recoveryPlanningFailures()),
                nonNegative(metrics.pathQueries()),
                nonNegative(metrics.searchEpisodesStarted()),
                nonNegative(metrics.searchEpisodesSucceeded()),
                nonNegative(metrics.searchEpisodesFailed()),
                nonNegative(metrics.searchReacquisitionTicksTotal()),
                nonNegative(metrics.engineeringRequestsPublished()),
                nonNegative(metrics.engineeringTasksClaimed()),
                nonNegative(metrics.engineeringTasksCompleted()),
                nonNegative(metrics.engineeringBlocksBroken()),
                nonNegative(metrics.engineeringBlocksPlaced())
        );

        runs.addLast(record);
        while (runs.size() > MAX_RUNS) {
            runs.removeFirst();
        }
        setDirty();
        return record;
    }

    public synchronized void storeEvaluation(
            SwarmAiEvaluation evaluation,
            String model,
            long gameTick
    ) {
        if (evaluation == null) {
            throw new IllegalArgumentException("Evaluation cannot be null.");
        }
        lastEvaluation = new EvaluationEntry(
                Math.max(0L, gameTick),
                clean(model, 96),
                evaluation.sanitized()
        );
        setDirty();
    }

    public synchronized int runCount() {
        return runs.size();
    }

    public synchronized List<RunRecord> recentRuns(int limit) {
        if (limit <= 0 || runs.isEmpty()) {
            return List.of();
        }
        List<RunRecord> all = new ArrayList<>(runs);
        int start = Math.max(0, all.size() - limit);
        return List.copyOf(all.subList(start, all.size()));
    }

    /**
     * Rebuilds auditable transfer evidence from the persisted run ledger.
     * The evidence is derived rather than separately cached, so it cannot become
     * stale when the bounded 40-run history rolls forward.
     */
    public synchronized List<SwarmAiExperienceTransfer.Insight> experienceTransfer() {
        List<SwarmAiExperienceTransfer.RunSample> samples = runs.stream()
                .map(run -> new SwarmAiExperienceTransfer.RunSample(
                        run.id(),
                        run.gameTick(),
                        run.preset(),
                        run.seed(),
                        run.elapsedTicks(),
                        run.agentCount(),
                        run.communicationDelivered(),
                        run.communicationDropped(),
                        run.recoveryAttempts(),
                        run.recoveryFailures(),
                        run.searchSucceeded(),
                        run.searchFailed(),
                        run.searchReacquisitionTicks(),
                        run.engineeringClaims(),
                        run.engineeringCompleted()
                ))
                .toList();
        return SwarmAiExperienceTransfer.analyze(samples);
    }

    public synchronized EvaluationEntry lastEvaluation() {
        return lastEvaluation;
    }

    public synchronized Summary summary() {
        long delivered = 0L;
        long dropped = 0L;
        long recoveryAttempts = 0L;
        long recoveryFailures = 0L;
        long searchSucceeded = 0L;
        long searchFailed = 0L;
        long reacquisitionTicks = 0L;
        long engineeringClaims = 0L;
        long engineeringCompleted = 0L;
        Set<String> presets = new HashSet<>();

        for (RunRecord run : runs) {
            delivered += run.communicationDelivered();
            dropped += run.communicationDropped();
            recoveryAttempts += run.recoveryAttempts();
            recoveryFailures += run.recoveryFailures();
            searchSucceeded += run.searchSucceeded();
            searchFailed += run.searchFailed();
            reacquisitionTicks += run.searchReacquisitionTicks();
            engineeringClaims += run.engineeringClaims();
            engineeringCompleted += run.engineeringCompleted();
            presets.add(run.preset());
        }

        long communicationTotal = delivered + dropped;
        long searchTotal = searchSucceeded + searchFailed;
        return new Summary(
                runs.size(),
                presets.size(),
                ratio(dropped, communicationTotal),
                ratio(recoveryFailures, recoveryAttempts),
                ratio(searchSucceeded, searchTotal),
                searchSucceeded <= 0L ? 0.0 : (double) reacquisitionTicks / searchSucceeded,
                ratio(engineeringCompleted, engineeringClaims)
        );
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", DATA_VERSION);
        tag.putLong("nextRunId", nextRunId);

        ListTag runTags = new ListTag();
        for (RunRecord run : runs) {
            runTags.add(run.toTag());
        }
        tag.put("runs", runTags);

        if (lastEvaluation != null) {
            tag.put("lastEvaluation", evaluationToTag(lastEvaluation));
        }
        return tag;
    }

    static SwarmAiLearningMemory load(CompoundTag tag, HolderLookup.Provider registries) {
        SwarmAiLearningMemory memory = new SwarmAiLearningMemory();
        ListTag runTags = tag.getList("runs", Tag.TAG_COMPOUND);
        int start = Math.max(0, runTags.size() - MAX_RUNS);
        long maxId = 0L;
        for (int i = start; i < runTags.size(); i++) {
            RunRecord run = RunRecord.fromTag(runTags.getCompound(i));
            memory.runs.addLast(run);
            maxId = Math.max(maxId, run.id());
        }
        memory.nextRunId = Math.max(Math.max(1L, tag.getLong("nextRunId")), maxId + 1L);

        if (tag.contains("lastEvaluation", Tag.TAG_COMPOUND)) {
            memory.lastEvaluation = evaluationFromTag(tag.getCompound("lastEvaluation"));
        }
        return memory;
    }

    private static CompoundTag evaluationToTag(EvaluationEntry entry) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("gameTick", entry.gameTick());
        tag.putString("model", entry.model());
        tag.putString("summary", entry.evaluation().summary());
        tag.putString("confidence", entry.evaluation().confidence().name());

        ListTag findings = new ListTag();
        for (SwarmAiEvaluation.Finding finding : entry.evaluation().findings()) {
            CompoundTag item = new CompoundTag();
            item.putString("metric", finding.metric());
            item.putString("trend", finding.trend());
            item.putString("evidence", finding.evidence());
            findings.add(item);
        }
        tag.put("findings", findings);

        ListTag suggestions = new ListTag();
        for (SwarmAiEvaluation.ExperimentSuggestion suggestion : entry.evaluation().nextExperiments()) {
            CompoundTag item = new CompoundTag();
            item.putString("preset", suggestion.preset());
            item.putString("reason", suggestion.reason());
            suggestions.add(item);
        }
        tag.put("nextExperiments", suggestions);

        ListTag notes = new ListTag();
        for (String note : entry.evaluation().dataQualityNotes()) {
            notes.add(StringTag.valueOf(note));
        }
        tag.put("dataQualityNotes", notes);
        return tag;
    }

    private static EvaluationEntry evaluationFromTag(CompoundTag tag) {
        SwarmAiEvaluation.Confidence confidence;
        try {
            confidence = SwarmAiEvaluation.Confidence.valueOf(tag.getString("confidence"));
        } catch (IllegalArgumentException exception) {
            confidence = SwarmAiEvaluation.Confidence.LOW;
        }

        List<SwarmAiEvaluation.Finding> findings = new ArrayList<>();
        ListTag findingTags = tag.getList("findings", Tag.TAG_COMPOUND);
        for (int i = 0; i < findingTags.size() && findings.size() < 4; i++) {
            CompoundTag item = findingTags.getCompound(i);
            findings.add(new SwarmAiEvaluation.Finding(
                    item.getString("metric"),
                    item.getString("trend"),
                    item.getString("evidence")
            ));
        }

        List<SwarmAiEvaluation.ExperimentSuggestion> suggestions = new ArrayList<>();
        ListTag suggestionTags = tag.getList("nextExperiments", Tag.TAG_COMPOUND);
        for (int i = 0; i < suggestionTags.size() && suggestions.size() < 3; i++) {
            CompoundTag item = suggestionTags.getCompound(i);
            suggestions.add(new SwarmAiEvaluation.ExperimentSuggestion(
                    item.getString("preset"),
                    item.getString("reason")
            ));
        }

        List<String> notes = new ArrayList<>();
        ListTag noteTags = tag.getList("dataQualityNotes", Tag.TAG_STRING);
        for (int i = 0; i < noteTags.size() && notes.size() < 3; i++) {
            notes.add(noteTags.getString(i));
        }

        SwarmAiEvaluation evaluation = new SwarmAiEvaluation(
                tag.getString("summary"),
                confidence,
                findings,
                suggestions,
                notes
        ).sanitized();
        return new EvaluationEntry(
                Math.max(0L, tag.getLong("gameTick")),
                clean(tag.getString("model"), 96),
                evaluation
        );
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }

    private static double ratio(long numerator, long denominator) {
        return denominator <= 0L ? 0.0 : (double) numerator / denominator;
    }

    private static String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.substring(0, Math.min(maxLength, normalized.length()));
    }

    public record Summary(
            int runCount,
            int presetCount,
            double communicationDropRate,
            double recoveryFailureRate,
            double searchSuccessRate,
            double averageReacquisitionTicks,
            double engineeringTaskCompletionRate
    ) {}

    public record EvaluationEntry(
            long gameTick,
            String model,
            SwarmAiEvaluation evaluation
    ) {}

    public record RunRecord(
            long id,
            long gameTick,
            String preset,
            int seed,
            long elapsedTicks,
            int agentCount,
            long communicationDelivered,
            long communicationDropped,
            long obstacleDetours,
            long recoveries,
            long recoveryAttempts,
            long recoveryFailures,
            long pathQueries,
            long searchStarted,
            long searchSucceeded,
            long searchFailed,
            long searchReacquisitionTicks,
            long engineeringRequests,
            long engineeringClaims,
            long engineeringCompleted,
            long engineeringBlocksBroken,
            long engineeringBlocksPlaced
    ) {
        public double communicationDropRate() {
            return ratio(communicationDropped, communicationDelivered + communicationDropped);
        }

        public double recoveryFailureRate() {
            return ratio(recoveryFailures, recoveryAttempts);
        }

        public double searchSuccessRate() {
            return ratio(searchSucceeded, searchSucceeded + searchFailed);
        }

        public double averageReacquisitionTicks() {
            return searchSucceeded <= 0L ? 0.0 : (double) searchReacquisitionTicks / searchSucceeded;
        }

        public double engineeringTaskCompletionRate() {
            return ratio(engineeringCompleted, engineeringClaims);
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("id", id);
            tag.putLong("gameTick", gameTick);
            tag.putString("preset", preset);
            tag.putInt("seed", seed);
            tag.putLong("elapsedTicks", elapsedTicks);
            tag.putInt("agentCount", agentCount);
            tag.putLong("communicationDelivered", communicationDelivered);
            tag.putLong("communicationDropped", communicationDropped);
            tag.putLong("obstacleDetours", obstacleDetours);
            tag.putLong("recoveries", recoveries);
            tag.putLong("recoveryAttempts", recoveryAttempts);
            tag.putLong("recoveryFailures", recoveryFailures);
            tag.putLong("pathQueries", pathQueries);
            tag.putLong("searchStarted", searchStarted);
            tag.putLong("searchSucceeded", searchSucceeded);
            tag.putLong("searchFailed", searchFailed);
            tag.putLong("searchReacquisitionTicks", searchReacquisitionTicks);
            tag.putLong("engineeringRequests", engineeringRequests);
            tag.putLong("engineeringClaims", engineeringClaims);
            tag.putLong("engineeringCompleted", engineeringCompleted);
            tag.putLong("engineeringBlocksBroken", engineeringBlocksBroken);
            tag.putLong("engineeringBlocksPlaced", engineeringBlocksPlaced);
            return tag;
        }

        private static RunRecord fromTag(CompoundTag tag) {
            return new RunRecord(
                    Math.max(1L, tag.getLong("id")),
                    Math.max(0L, tag.getLong("gameTick")),
                    clean(tag.getString("preset"), 32),
                    tag.getInt("seed"),
                    Math.max(0L, tag.getLong("elapsedTicks")),
                    Math.max(0, tag.getInt("agentCount")),
                    nonNegative(tag.getLong("communicationDelivered")),
                    nonNegative(tag.getLong("communicationDropped")),
                    nonNegative(tag.getLong("obstacleDetours")),
                    nonNegative(tag.getLong("recoveries")),
                    nonNegative(tag.getLong("recoveryAttempts")),
                    nonNegative(tag.getLong("recoveryFailures")),
                    nonNegative(tag.getLong("pathQueries")),
                    nonNegative(tag.getLong("searchStarted")),
                    nonNegative(tag.getLong("searchSucceeded")),
                    nonNegative(tag.getLong("searchFailed")),
                    nonNegative(tag.getLong("searchReacquisitionTicks")),
                    nonNegative(tag.getLong("engineeringRequests")),
                    nonNegative(tag.getLong("engineeringClaims")),
                    nonNegative(tag.getLong("engineeringCompleted")),
                    nonNegative(tag.getLong("engineeringBlocksBroken")),
                    nonNegative(tag.getLong("engineeringBlocksPlaced"))
            );
        }
    }
}
