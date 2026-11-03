package dev.swarmmobs.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwarmAiEvaluationTest {

    @Test
    void sanitizesModelOutputToKnownMetricsAndExperimentPresets() {
        SwarmAiEvaluation evaluation = new SwarmAiEvaluation(
                "  Useful summary\nwith a line break.  ",
                SwarmAiEvaluation.Confidence.HIGH,
                java.util.List.of(
                        new SwarmAiEvaluation.Finding("UNKNOWN_METRIC", "ATTACK", " evidence "),
                        new SwarmAiEvaluation.Finding("SEARCH_SUCCESS_RATE", "IMPROVING", "second")
                ),
                java.util.List.of(
                        new SwarmAiEvaluation.ExperimentSuggestion("UNSUPPORTED_PRESET", "not allowed"),
                        new SwarmAiEvaluation.ExperimentSuggestion("BASELINE", "repeat with same seed")
                ),
                java.util.List.of("  small sample  ", "second note", "third note", "fourth note")
        ).sanitized();

        assertEquals("Useful summary with a line break.", evaluation.summary());
        assertEquals("OTHER", evaluation.findings().get(0).metric());
        assertEquals("INSUFFICIENT_DATA", evaluation.findings().get(0).trend());
        assertEquals(1, evaluation.nextExperiments().size());
        assertEquals("BASELINE", evaluation.nextExperiments().get(0).preset());
        assertEquals(3, evaluation.dataQualityNotes().size());
    }

    @Test
    void defaultsMissingConfidenceAndBoundsText() {
        SwarmAiEvaluation evaluation = new SwarmAiEvaluation(
                "x".repeat(500), null, null, null, null
        ).sanitized();

        assertEquals(SwarmAiEvaluation.Confidence.LOW, evaluation.confidence());
        assertEquals(320, evaluation.summary().length());
        assertTrue(evaluation.findings().isEmpty());
        assertTrue(evaluation.nextExperiments().isEmpty());
    }
}
