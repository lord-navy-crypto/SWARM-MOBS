package dev.swarmmobs.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.swarmmobs.ai.ollama.OllamaClient;
import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SwarmAiLearningService {
    private static final Gson GSON = new Gson();
    private static final OllamaClient CLIENT = new OllamaClient();
    private static final AtomicBoolean REQUEST_IN_FLIGHT = new AtomicBoolean(false);
    private static final JsonObject EVALUATION_SCHEMA = buildEvaluationSchema();

    private static final String SYSTEM_PROMPT = """
            You are an experiment evaluator for a Minecraft swarm simulation.
            Analyze only aggregate system reliability, communication, sensing, navigation recovery,
            search reacquisition, and non-combat engineering task metrics.
            This is a post-run evaluator, not a controller. Do not provide combat, attack,
            player-targeting, or real-time action advice. Do not propose direct runtime behavior changes.
            Recommend only controlled experiment presets for additional measurement.
            Compare like-for-like presets and seeds when possible. Distinguish correlation from causation.
            Explicitly note small sample sizes, missing denominators, and incomparable runs.
            Keep findings concise, evidence-based, and technically cautious.
            """;

    public static CompletableFuture<SwarmAiEvaluation> evaluate(ServerLevel level) {
        if (!SwarmConfig.EXTERNAL_AI_ENABLED.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Enable the local Ollama provider first: /swarmmobs ai on.")
            );
        }
        if (SwarmAiShadowService.requestInFlight()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("A shadow strategy request is already in flight.")
            );
        }

        SwarmAiLearningMemory memory = SwarmAiLearningMemory.get(level);
        List<SwarmAiLearningMemory.RunRecord> runs = memory.recentRuns(16);
        if (runs.size() < 2) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Record at least two experiment snapshots before evaluation.")
            );
        }
        if (!REQUEST_IN_FLIGHT.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("An experiment evaluation is already in flight.")
            );
        }

        String model = SwarmConfig.OLLAMA_MODEL.get();
        String userPrompt = """
                Evaluate the following saved experiment history.
                Do not infer causation from different presets or seeds. Identify the strongest
                supported trend, note uncertainty, and suggest up to three useful follow-up
                measurement presets from the allowed list only.

                Allowed presets: BASELINE, NOISY_SENSING, LOSSY_COMMS, COMBINED_FAULTS, NAVIGATION_STRESS.

                Aggregate summary:
                %s

                Recent experiment records (oldest to newest):
                %s

                Return only the JSON object matching the supplied schema.
                """.formatted(GSON.toJson(memory.summary()), GSON.toJson(runs));

        CompletableFuture<SwarmAiEvaluation> result = new CompletableFuture<>();
        try {
            CLIENT.chatStructured(
                    SwarmConfig.OLLAMA_BASE_URL.get(),
                    model,
                    SYSTEM_PROMPT,
                    userPrompt,
                    EVALUATION_SCHEMA,
                    SwarmConfig.OLLAMA_TIMEOUT_MS.get(),
                    SwarmConfig.OLLAMA_KEEP_ALIVE.get()
            ).whenComplete((content, error) -> {
                try {
                    level.getServer().execute(() -> {
                        try {
                            if (error != null) {
                                result.completeExceptionally(rootCause(error));
                                return;
                            }
                            SwarmAiEvaluation evaluation = parseEvaluation(content);
                            memory.storeEvaluation(evaluation, model, level.getGameTime());
                            result.complete(evaluation);
                        } catch (Throwable failure) {
                            result.completeExceptionally(failure);
                        } finally {
                            REQUEST_IN_FLIGHT.set(false);
                        }
                    });
                } catch (Throwable schedulingFailure) {
                    REQUEST_IN_FLIGHT.set(false);
                    result.completeExceptionally(schedulingFailure);
                }
            });
        } catch (Throwable failure) {
            REQUEST_IN_FLIGHT.set(false);
            result.completeExceptionally(failure);
        }
        return result;
    }

    public static boolean requestInFlight() {
        return REQUEST_IN_FLIGHT.get();
    }

    private static SwarmAiEvaluation parseEvaluation(String content) {
        EvaluationPayload payload = GSON.fromJson(content, EvaluationPayload.class);
        if (payload == null) {
            throw new IllegalStateException("Ollama returned an empty experiment evaluation.");
        }
        SwarmAiEvaluation.Confidence confidence;
        try {
            confidence = SwarmAiEvaluation.Confidence.valueOf(
                    payload.confidence == null ? "LOW" : payload.confidence
            );
        } catch (IllegalArgumentException exception) {
            confidence = SwarmAiEvaluation.Confidence.LOW;
        }

        return new SwarmAiEvaluation(
                payload.summary,
                confidence,
                payload.findings,
                payload.nextExperiments,
                payload.dataQualityNotes
        ).sanitized();
    }

    private static JsonObject buildEvaluationSchema() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "object");
        root.addProperty("additionalProperties", false);

        JsonObject properties = new JsonObject();

        JsonObject summary = new JsonObject();
        summary.addProperty("type", "string");
        summary.addProperty("maxLength", 320);
        properties.add("summary", summary);

        JsonObject confidence = new JsonObject();
        confidence.addProperty("type", "string");
        JsonArray confidenceValues = new JsonArray();
        confidenceValues.add("LOW");
        confidenceValues.add("MEDIUM");
        confidenceValues.add("HIGH");
        confidence.add("enum", confidenceValues);
        properties.add("confidence", confidence);

        JsonObject finding = new JsonObject();
        finding.addProperty("type", "object");
        finding.addProperty("additionalProperties", false);
        JsonObject findingProperties = new JsonObject();
        findingProperties.add("metric", enumSchema(
                "COMMUNICATION_DROP_RATE", "RECOVERY_FAILURE_RATE", "SEARCH_SUCCESS_RATE",
                "AVERAGE_REACQUISITION_TICKS", "PATH_QUERY_COUNT",
                "ENGINEERING_TASK_COMPLETION_RATE", "AGENT_COUNT", "DURATION_TICKS", "OTHER"
        ));
        findingProperties.add("trend", enumSchema(
                "IMPROVING", "WORSENING", "MIXED", "INSUFFICIENT_DATA"
        ));
        JsonObject evidence = new JsonObject();
        evidence.addProperty("type", "string");
        evidence.addProperty("maxLength", 180);
        findingProperties.add("evidence", evidence);
        finding.add("properties", findingProperties);
        finding.add("required", strings("metric", "trend", "evidence"));
        properties.add("findings", arraySchema(finding, 4));

        JsonObject suggestion = new JsonObject();
        suggestion.addProperty("type", "object");
        suggestion.addProperty("additionalProperties", false);
        JsonObject suggestionProperties = new JsonObject();
        suggestionProperties.add("preset", enumSchema(
                "BASELINE", "NOISY_SENSING", "LOSSY_COMMS", "COMBINED_FAULTS", "NAVIGATION_STRESS"
        ));
        JsonObject reason = new JsonObject();
        reason.addProperty("type", "string");
        reason.addProperty("maxLength", 180);
        suggestionProperties.add("reason", reason);
        suggestion.add("properties", suggestionProperties);
        suggestion.add("required", strings("preset", "reason"));
        properties.add("nextExperiments", arraySchema(suggestion, 3));

        JsonObject note = new JsonObject();
        note.addProperty("type", "string");
        note.addProperty("maxLength", 160);
        properties.add("dataQualityNotes", arraySchema(note, 3));

        root.add("properties", properties);
        root.add("required", strings(
                "summary", "confidence", "findings", "nextExperiments", "dataQualityNotes"
        ));
        return root;
    }

    private static JsonObject enumSchema(String... values) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "string");
        schema.add("enum", strings(values));
        return schema;
    }

    private static JsonObject arraySchema(JsonObject item, int maxItems) {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "array");
        schema.add("items", item);
        schema.addProperty("maxItems", maxItems);
        return schema;
    }

    private static JsonArray strings(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static Throwable rootCause(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

    private static final class EvaluationPayload {
        String summary;
        String confidence;
        List<SwarmAiEvaluation.Finding> findings;
        List<SwarmAiEvaluation.ExperimentSuggestion> nextExperiments;
        List<String> dataQualityNotes;
    }

    private SwarmAiLearningService() {}
}
