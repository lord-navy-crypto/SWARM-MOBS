package dev.swarmmobs.ai.ollama;

import dev.swarmmobs.ai.ollama.OllamaClient.Discovery;
import dev.swarmmobs.ai.ollama.OllamaClient.ModelInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OllamaModelAdvisor {
    private static final Pattern PARAMETER_COUNT = Pattern.compile("(?i)(\\d+(?:\\.\\d+)?)\\s*B\\b");

    private OllamaModelAdvisor() {}

    public static List<Recommendation> recommend(List<ModelInfo> models) {
        if (models == null || models.isEmpty()) {
            return List.of();
        }

        return models.stream()
                .filter(model -> !isEmbeddingOnly(model))
                .map(OllamaModelAdvisor::rate)
                .sorted(Comparator.comparingInt(Recommendation::score).reversed()
                        .thenComparing(value -> value.model().name(), String.CASE_INSENSITIVE_ORDER))
                .limit(3)
                .toList();
    }

    public static List<String> diagnosticLines(
            Discovery discovery,
            String endpoint,
            String selectedModel
    ) {
        List<String> lines = new ArrayList<>();
        lines.add("Ollama v" + discovery.version() + " reachable at " + endpoint + ".");
        lines.add("Discovered " + discovery.models().size() + " installed local model(s).");

        if (discovery.models().isEmpty()) {
            lines.add("No local models are installed. Install one manually with ollama pull <model-name>.");
        } else {
            int displayed = Math.min(8, discovery.models().size());
            for (int index = 0; index < displayed; index++) {
                ModelInfo model = discovery.models().get(index);
                lines.add("Installed: " + model.name() + " (" + formatSize(model.sizeBytes())
                        + ", " + label(model.family(), "family unknown")
                        + ", " + label(model.parameterSize(), "parameter size unknown")
                        + ", " + label(model.quantizationLevel(), "quantization unknown") + ").");
            }
            if (discovery.models().size() > displayed) {
                lines.add("...and " + (discovery.models().size() - displayed) + " more installed model(s).");
            }
        }

        if (selectedModel == null || selectedModel.isBlank()) {
            lines.add("No model is selected. Choose an installed name with /swarmmobs ai model <name>.");
        } else if (discovery.models().stream().noneMatch(model -> model.name().equals(selectedModel))) {
            lines.add("Selected model " + selectedModel + " is not installed; choose one from the discovered list.");
        } else {
            lines.add("Selected model " + selectedModel + " is installed.");
        }

        List<Recommendation> recommendations = recommend(discovery.models());
        if (!recommendations.isEmpty()) {
            lines.add("Recommended general-purpose local candidates (metadata heuristic, not a performance benchmark):");
            for (int index = 0; index < recommendations.size(); index++) {
                Recommendation recommendation = recommendations.get(index);
                lines.add((index + 1) + ". " + recommendation.model().name()
                        + " [score " + recommendation.score() + "]: " + recommendation.rationale() + ".");
            }
        } else if (!discovery.models().isEmpty()) {
            lines.add("No general-purpose chat candidate was identified; installed models may be embedding/reranking models.");
        }

        lines.add("No model was downloaded, started, or selected automatically; no RAM/GPU benchmark was run.");
        return List.copyOf(lines);
    }

    private static boolean isEmbeddingOnly(ModelInfo model) {
        String combined = (model.name() + " " + model.family()).toLowerCase(Locale.ROOT);
        return containsAny(combined, "embedding", "embed", "rerank", "reranker", "bge-", "e5-");
    }

    private static Recommendation rate(ModelInfo model) {
        String name = model.name().toLowerCase(Locale.ROOT);
        String family = model.family().toLowerCase(Locale.ROOT);
        String combined = name + " " + family;
        List<String> reasons = new ArrayList<>();
        int score = 35;

        if (containsAny(combined, "embedding", "embed", "rerank", "reranker")) {
            score -= 120;
            reasons.add("embedding/reranking workload rather than chat");
        } else {
            if (containsAny(name, "instruct", "chat", "assistant")) {
                score += 25;
                reasons.add("chat/instruction-oriented tag");
            }
            if (containsAny(combined, "qwen", "llama", "mistral", "gemma", "phi", "deepseek", "command-r", "aya")) {
                score += 18;
                reasons.add("general-purpose text model family");
            }
        }

        double parameters = parameterCount(model.parameterSize());
        if (parameters > 0.0) {
            if (parameters >= 3.0 && parameters <= 14.0) {
                score += 20;
                reasons.add("mid-sized parameter class");
            } else if (parameters < 3.0) {
                score += 8;
                reasons.add("small local footprint");
            } else if (parameters <= 24.0) {
                score += 8;
                reasons.add("larger reasoning capacity, with higher resource demand");
            } else {
                score -= 18;
                reasons.add("large parameter count and higher resource demand");
            }
        }

        String quantization = model.quantizationLevel().toLowerCase(Locale.ROOT);
        if (quantization.contains("q4") || quantization.contains("q5")) {
            score += 12;
            reasons.add("Q4/Q5 quantization balance");
        } else if (quantization.contains("q6")) {
            score += 8;
            reasons.add("Q6 quantization");
        } else if (quantization.contains("q8")) {
            score += 4;
            reasons.add("Q8 quantization");
        }

        long size = model.sizeBytes();
        if (size > 0L && size <= 12L * 1024L * 1024L * 1024L) {
            score += 8;
            reasons.add("moderate model file size");
        } else if (size > 24L * 1024L * 1024L * 1024L) {
            score -= 10;
            reasons.add("large model file size");
        }

        if (reasons.isEmpty()) {
            reasons.add("limited metadata; validate with a local benchmark");
        }
        return new Recommendation(model, score, String.join(", ", reasons));
    }

    private static double parameterCount(String value) {
        if (value == null || value.isBlank()) {
            return -1.0;
        }
        Matcher matcher = PARAMETER_COUNT.matcher(value);
        if (!matcher.find()) {
            return -1.0;
        }
        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1.0;
        }
    }

    private static boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private static String label(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0L) {
            return "size unknown";
        }
        double gib = bytes / (1024.0 * 1024.0 * 1024.0);
        if (gib >= 1.0) {
            return String.format(Locale.ROOT, "%.1f GiB", gib);
        }
        double mib = bytes / (1024.0 * 1024.0);
        return String.format(Locale.ROOT, "%.0f MiB", mib);
    }

    public record Recommendation(ModelInfo model, int score, String rationale) {}
}
