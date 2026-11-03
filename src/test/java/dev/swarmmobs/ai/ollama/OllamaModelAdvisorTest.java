package dev.swarmmobs.ai.ollama;

import dev.swarmmobs.ai.ollama.OllamaClient.ModelInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OllamaModelAdvisorTest {

    @Test
    void prefersGeneralPurposeInstructionModelsToEmbeddingModels() {
        List<ModelInfo> models = List.of(
                new ModelInfo("nomic-embed-text:latest", 274000000L, "bert", "137M", "F16", ""),
                new ModelInfo("qwen2.5:7b-instruct", 4683075271L, "qwen2", "7.6B", "Q4_K_M", ""),
                new ModelInfo("llama3.2:3b", 2019393189L, "llama", "3.2B", "Q4_K_M", "")
        );

        var recommendations = OllamaModelAdvisor.recommend(models);

        assertEquals(2, recommendations.size());
        assertEquals("qwen2.5:7b-instruct", recommendations.getFirst().model().name());
        assertTrue(recommendations.getFirst().score() > recommendations.getLast().score());
        assertTrue(recommendations.stream().noneMatch(
                recommendation -> recommendation.model().name().contains("embed")
        ));
    }

    @Test
    void recommendationListIsEmptyWhenNoModelsAreInstalled() {
        assertTrue(OllamaModelAdvisor.recommend(List.of()).isEmpty());
    }

    @Test
    void embeddingOnlyModelsAreNotRecommendedForChat() {
        List<ModelInfo> models = List.of(
                new ModelInfo("nomic-embed-text:latest", 274000000L, "bert", "137M", "F16", ""),
                new ModelInfo("bge-m3:latest", 1200000000L, "bert", "568M", "F16", "")
        );

        assertTrue(OllamaModelAdvisor.recommend(models).isEmpty());
    }

    @Test
    void diagnosticsExplainThatNoAutomaticDownloadOrSelectionOccurs() {
        var discovery = new OllamaClient.Discovery("0.5.7", List.of());

        var lines = OllamaModelAdvisor.diagnosticLines(
                discovery,
                "http://127.0.0.1:11434",
                ""
        );

        assertTrue(lines.stream().anyMatch(line -> line.contains("No local models are installed")));
        assertTrue(lines.stream().anyMatch(line -> line.contains("No model was downloaded")));
    }
}
