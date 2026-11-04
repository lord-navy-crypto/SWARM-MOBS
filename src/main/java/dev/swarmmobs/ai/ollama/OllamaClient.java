package dev.swarmmobs.ai.ollama;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public final class OllamaClient {
    private static final Gson GSON = new Gson();
    private static final int GET_RETRY_COUNT = 1;
    private static final long GET_RETRY_DELAY_MS = 250L;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public CompletableFuture<List<String>> listModels(String baseUrl, int timeoutMs) {
        return listModelDetails(baseUrl, timeoutMs)
                .thenApply(models -> models.stream().map(ModelInfo::name).toList());
    }

    public CompletableFuture<List<ModelInfo>> listModelDetails(String baseUrl, int timeoutMs) {
        HttpRequest request = HttpRequest.newBuilder(localApiUri(baseUrl, "/api/tags"))
                .timeout(timeout(timeoutMs))
                .header("Accept", "application/json")
                .GET()
                .build();

        return sendGetWithRetry(request, GET_RETRY_COUNT)
                .thenApply(OllamaClient::requireSuccess)
                .thenApply(HttpResponse::body)
                .thenApply(OllamaClient::parseModelDetails);
    }

    public CompletableFuture<String> serverVersion(String baseUrl, int timeoutMs) {
        HttpRequest request = HttpRequest.newBuilder(localApiUri(baseUrl, "/api/version"))
                .timeout(timeout(timeoutMs))
                .header("Accept", "application/json")
                .GET()
                .build();

        return sendGetWithRetry(request, GET_RETRY_COUNT)
                .thenApply(OllamaClient::requireSuccess)
                .thenApply(HttpResponse::body)
                .thenApply(OllamaClient::parseVersion);
    }

    public CompletableFuture<Discovery> discover(String baseUrl, int timeoutMs) {
        CompletableFuture<String> version = serverVersion(baseUrl, timeoutMs);
        CompletableFuture<List<ModelInfo>> models = listModelDetails(baseUrl, timeoutMs);
        return version.thenCombine(models, Discovery::new);
    }

    public CompletableFuture<String> chatStructured(
            String baseUrl,
            String model,
            String systemPrompt,
            String userPrompt,
            JsonObject schema,
            int timeoutMs,
            String keepAlive
    ) {
        if (model == null || model.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No Ollama model selected. Run /swarmmobs ai discover, then /swarmmobs ai model <name>.")
            );
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("model", model.trim());
        payload.addProperty("stream", false);
        payload.add("format", schema);

        if (keepAlive != null && !keepAlive.isBlank()) {
            payload.addProperty("keep_alive", keepAlive.trim());
        }

        JsonObject options = new JsonObject();
        options.addProperty("temperature", 0.0);
        payload.add("options", options);

        JsonArray messages = new JsonArray();
        messages.add(message("system", systemPrompt));
        messages.add(message("user", userPrompt));
        payload.add("messages", messages);

        HttpRequest request = HttpRequest.newBuilder(localApiUri(baseUrl, "/api/chat"))
                .timeout(timeout(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload)))
                .build();

        // Do not retry inference POSTs automatically: a timed-out model may still
        // have completed the request, so replaying it could duplicate expensive work.
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(OllamaClient::requireSuccess)
                .thenApply(HttpResponse::body)
                .thenApply(OllamaClient::extractAssistantContent);
    }

    static URI localApiUri(String baseUrl, String path) {
        String raw = baseUrl == null || baseUrl.isBlank()
                ? "http://127.0.0.1:11434"
                : baseUrl.trim();

        URI base = URI.create(raw);
        String scheme = base.getScheme() == null ? "" : base.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("Ollama base URL must use http or https.");
        }

        if (!isLoopbackHost(base.getHost())) {
            throw new IllegalArgumentException(
                    "SWARM-MOBS only permits local Ollama endpoints on loopback hosts."
            );
        }

        if (base.getRawQuery() != null || base.getRawFragment() != null) {
            throw new IllegalArgumentException("Ollama base URL must not contain a query or fragment.");
        }

        String normalized = raw.endsWith("/") ? raw.substring(0, raw.length() - 1) : raw;
        return URI.create(normalized + path);
    }

    static boolean isLoopbackHost(String host) {
        if (host == null) {
            return false;
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.equals("localhost")
                || normalized.equals("127.0.0.1")
                || normalized.equals("::1")
                || normalized.equals("[::1]");
    }

    static List<ModelInfo> parseModelDetails(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        JsonArray models = root == null ? null : root.getAsJsonArray("models");
        if (models == null) {
            return List.of();
        }

        List<ModelInfo> parsed = new ArrayList<>();
        for (JsonElement element : models) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject model = element.getAsJsonObject();
            String name = stringValue(model, "name");
            if (name.isBlank()) {
                name = stringValue(model, "model");
            }
            if (name.isBlank()) {
                continue;
            }

            JsonObject details = model.has("details") && model.get("details").isJsonObject()
                    ? model.getAsJsonObject("details")
                    : new JsonObject();

            parsed.add(new ModelInfo(
                    name,
                    longValue(model, "size"),
                    stringValue(details, "family"),
                    stringValue(details, "parameter_size"),
                    stringValue(details, "quantization_level"),
                    stringValue(model, "modified_at")
            ));
        }
        return List.copyOf(parsed);
    }

    static String parseVersion(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        String version = root == null ? "" : stringValue(root, "version");
        if (version.isBlank()) {
            throw new IllegalStateException("Ollama version response did not contain a version field.");
        }
        return version;
    }

    private CompletableFuture<HttpResponse<String>> sendGetWithRetry(
            HttpRequest request,
            int retriesRemaining
    ) {
        CompletableFuture<CompletableFuture<HttpResponse<String>>> pending =
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .handle((response, error) -> {
                            if (error != null) {
                                Throwable cause = unwrap(error);
                                if (retriesRemaining > 0 && isTransient(cause)) {
                                    return retryAfterDelay(request, retriesRemaining);
                                }
                                return CompletableFuture.<HttpResponse<String>>failedFuture(cause);
                            }

                            if (retriesRemaining > 0
                                    && response.statusCode() >= 500
                                    && response.statusCode() < 600) {
                                return retryAfterDelay(request, retriesRemaining);
                            }
                            return CompletableFuture.completedFuture(response);
                        });
        return pending.thenCompose(next -> next);
    }

    private CompletableFuture<HttpResponse<String>> retryAfterDelay(
            HttpRequest request,
            int retriesRemaining
    ) {
        return CompletableFuture.runAsync(
                        () -> {},
                        CompletableFuture.delayedExecutor(GET_RETRY_DELAY_MS, TimeUnit.MILLISECONDS)
                )
                .thenCompose(ignored -> sendGetWithRetry(request, retriesRemaining - 1));
    }

    private static boolean isTransient(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof IOException) {
                return true;
            }
        }
        return false;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException
                || current instanceof java.util.concurrent.ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static long longValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return 0L;
        }
        try {
            return Math.max(0L, value.getAsLong());
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }

    private static JsonObject message(String role, String content) {
        JsonObject message = new JsonObject();
        message.addProperty("role", role);
        message.addProperty("content", content == null ? "" : content);
        return message;
    }

    private static Duration timeout(int timeoutMs) {
        return Duration.ofMillis(Math.max(250, Math.min(30000, timeoutMs)));
    }

    private static HttpResponse<String> requireSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String body = abbreviate(response.body(), 300);
            if (response.statusCode() == 404
                    && body.toLowerCase(Locale.ROOT).contains("model")
                    && body.toLowerCase(Locale.ROOT).contains("not found")) {
                throw new IllegalStateException(
                        "Selected Ollama model is not installed. Run /swarmmobs ai discover and select an installed model."
                );
            }
            throw new IllegalStateException(
                    "Ollama HTTP " + response.statusCode() + ": " + body
            );
        }
        return response;
    }

    private static String extractAssistantContent(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null || !root.has("message") || !root.get("message").isJsonObject()) {
            throw new IllegalStateException("Ollama response did not contain message.content.");
        }
        JsonElement content = root.getAsJsonObject("message").get("content");
        if (content == null || content.isJsonNull()) {
            throw new IllegalStateException("Ollama response message.content was empty.");
        }
        return content.getAsString();
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String singleLine = text.replace('\n', ' ').replace('\r', ' ');
        return singleLine.length() <= maxLength
                ? singleLine
                : singleLine.substring(0, maxLength) + "...";
    }

    public record ModelInfo(
            String name,
            long sizeBytes,
            String family,
            String parameterSize,
            String quantizationLevel,
            String modifiedAt
    ) {
        public ModelInfo {
            name = name == null ? "" : name;
            sizeBytes = Math.max(0L, sizeBytes);
            family = family == null ? "" : family;
            parameterSize = parameterSize == null ? "" : parameterSize;
            quantizationLevel = quantizationLevel == null ? "" : quantizationLevel;
            modifiedAt = modifiedAt == null ? "" : modifiedAt;
        }
    }

    public record Discovery(String version, List<ModelInfo> models) {
        public Discovery {
            version = version == null || version.isBlank() ? "unknown" : version;
            models = models == null ? List.of() : List.copyOf(models);
        }
    }
}
