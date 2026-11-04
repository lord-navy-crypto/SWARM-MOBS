package dev.swarmmobs.visual;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwarmAnimationAssetTest {
    private static JsonObject readJson(String path) throws Exception {
        InputStream stream = SwarmAnimationAssetTest.class.getClassLoader()
                .getResourceAsStream(path);
        assertNotNull(stream, "Missing resource: " + path);
        try (stream) {
            return JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void everyAnimationCueHasARealGeckoLibClip() throws Exception {
        JsonObject root = readJson("assets/swarmmobs/animations/swarm_action.animation.json");
        JsonObject clips = root.getAsJsonObject("animations");
        for (SwarmAnimationCue cue : SwarmAnimationCue.values()) {
            assertTrue(clips.has("animation.swarm_action." + cue.animationName()),
                    "Missing animation clip for " + cue);
        }
    }

    @Test
    void everyAnimatedBoneExistsInTheGeckoModel() throws Exception {
        JsonObject animationRoot = readJson("assets/swarmmobs/animations/swarm_action.animation.json");
        JsonObject geometryRoot = readJson("assets/swarmmobs/geo/swarm_action.geo.json");
        Set<String> modelBones = new HashSet<>();
        JsonArray bones = geometryRoot.getAsJsonArray("minecraft:geometry")
                .get(0).getAsJsonObject().getAsJsonArray("bones");
        bones.forEach(bone -> modelBones.add(bone.getAsJsonObject().get("name").getAsString()));

        animationRoot.getAsJsonObject("animations").entrySet().forEach(entry -> {
            JsonObject animatedBones = entry.getValue().getAsJsonObject()
                    .getAsJsonObject("bones");
            if (animatedBones != null) {
                animatedBones.keySet().forEach(bone ->
                        assertTrue(modelBones.contains(bone),
                                entry.getKey() + " references missing model bone " + bone));
            }
        });
    }
}
