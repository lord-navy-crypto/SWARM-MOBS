package dev.swarmmobs.ai.ollama;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OllamaClientTest {

    @Test
    void acceptsLoopbackHostsOnly() {
        assertTrue(OllamaClient.isLoopbackHost("localhost"));
        assertTrue(OllamaClient.isLoopbackHost("127.0.0.1"));
        assertTrue(OllamaClient.isLoopbackHost("::1"));

        assertFalse(OllamaClient.isLoopbackHost("example.com"));
        assertFalse(OllamaClient.isLoopbackHost("192.168.1.20"));
        assertFalse(OllamaClient.isLoopbackHost(null));
    }

    @Test
    void rejectsRemoteBaseUrl() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OllamaClient.localApiUri("https://example.com", "/api/chat")
        );
    }

    @Test
    void buildsExpectedLocalApiPath() {
        var uri = OllamaClient.localApiUri(
                "http://127.0.0.1:11434/",
                "/api/chat"
        );

        assertEquals("http://127.0.0.1:11434/api/chat", uri.toString());
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(
                IllegalArgumentException.class,
                () -> OllamaClient.localApiUri("file://localhost/tmp", "/api/chat")
        );
    }

    @Test
    void parsesOllamaVersion() {
        assertEquals("0.5.7", OllamaClient.parseVersion("""
                {"version":"0.5.7"}
                """));
    }

    @Test
    void parsesModelMetadataFromTagsResponse() {
        var models = OllamaClient.parseModelDetails("""
                {"models":[
                  {
                    "name":"qwen2.5:7b-instruct",
                    "size":4683075271,
                    "modified_at":"2026-11-03T10:00:00Z",
                    "details":{
                      "family":"qwen2",
                      "parameter_size":"7.6B",
                      "quantization_level":"Q4_K_M"
                    }
                  }
                ]}
                """);

        assertEquals(1, models.size());
        assertEquals("qwen2.5:7b-instruct", models.getFirst().name());
        assertEquals(4683075271L, models.getFirst().sizeBytes());
        assertEquals("qwen2", models.getFirst().family());
        assertEquals("7.6B", models.getFirst().parameterSize());
        assertEquals("Q4_K_M", models.getFirst().quantizationLevel());
    }

    @Test
    void returnsEmptyModelListForEmptyTagsResponse() {
        assertTrue(OllamaClient.parseModelDetails("{\"models\":[]}").isEmpty());
    }

}
