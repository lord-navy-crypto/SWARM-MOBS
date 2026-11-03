package dev.swarmmobs.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record SwarmAiEvaluation(
        String summary,
        Confidence confidence,
        List<Finding> findings,
        List<ExperimentSuggestion> nextExperiments,
        List<String> dataQualityNotes
) {
    private static final Set<String> METRICS = Set.of(
            "COMMUNICATION_DROP_RATE",
            "RECOVERY_FAILURE_RATE",
            "SEARCH_SUCCESS_RATE",
            "AVERAGE_REACQUISITION_TICKS",
            "PATH_QUERY_COUNT",
            "ENGINEERING_TASK_COMPLETION_RATE",
            "AGENT_COUNT",
            "DURATION_TICKS",
            "OTHER"
    );
    private static final Set<String> TRENDS = Set.of(
            "IMPROVING", "WORSENING", "MIXED", "INSUFFICIENT_DATA"
    );
    private static final Set<String> PRESETS = Set.of(
            "BASELINE", "NOISY_SENSING", "LOSSY_COMMS",
            "COMBINED_FAULTS", "NAVIGATION_STRESS"
    );

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    public record Finding(String metric, String trend, String evidence) {
        public Finding sanitized() {
            String safeMetric = normalize(metric, METRICS, "OTHER");
            String safeTrend = normalize(trend, TRENDS, "INSUFFICIENT_DATA");
            return new Finding(safeMetric, safeText(evidence, 180));
        }
    }

    public record ExperimentSuggestion(String preset, String reason) {
        public ExperimentSuggestion sanitized() {
            String safePreset = normalize(preset, PRESETS, "");
            return new ExperimentSuggestion(safePreset, safeText(reason, 180));
        }
    }

    public SwarmAiEvaluation sanitized() {
        List<Finding> safeFindings = new ArrayList<>();
        if (findings != null) {
            for (Finding finding : findings) {
                if (finding != null && safeFindings.size() < 4) {
                    safeFindings.add(finding.sanitized());
                }
            }
        }

        List<ExperimentSuggestion> safeSuggestions = new ArrayList<>();
        if (nextExperiments != null) {
            for (ExperimentSuggestion suggestion : nextExperiments) {
                if (suggestion == null || safeSuggestions.size() >= 3) {
                    continue;
                }
                ExperimentSuggestion safe = suggestion.sanitized();
                if (!safe.preset().isBlank()) {
                    safeSuggestions.add(safe);
                }
            }
        }

        List<String> safeNotes = new ArrayList<>();
        if (dataQualityNotes != null) {
            for (String note : dataQualityNotes) {
                if (safeNotes.size() >= 3) {
                    break;
                }
                safeNotes.add(safeText(note, 160));
            }
        }

        return new SwarmAiEvaluation(
                safeText(summary, 320),
                confidence == null ? Confidence.LOW : confidence,
                List.copyOf(safeFindings),
                List.copyOf(safeSuggestions),
                List.copyOf(safeNotes)
        );
    }

    private static String normalize(String value, Set<String> allowed, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT);
        return allowed.contains(normalized) ? normalized : fallback;
    }

    private static String safeText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.substring(0, Math.min(maxLength, normalized.length()));
    }
}
