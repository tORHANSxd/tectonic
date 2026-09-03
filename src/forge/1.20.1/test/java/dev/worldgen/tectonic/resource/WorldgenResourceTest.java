package dev.worldgen.tectonic.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorldgenResourceTest {
    private static final Path RESOURCE_ROOT = Path.of("src/common/main/resources");
    private static final Path RAW_CONTINENTS = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/data/tectonic/worldgen/density_function/noise/raw_continents.json"
    );
    private static final Path RIVER_ICE_MODIFIER = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/overlay.mod/data/tectonic/lithostitched/worldgen_modifier/underground_river/ice.json"
    );
    private static final Path RIVER_LANTERN_NOISE = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/data/tectonic/worldgen/noise/river_lanterns.json"
    );
    private static final Path RIVER_LANTERN_MODIFIER = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/overlay.mod/data/tectonic/lithostitched/worldgen_modifier/river_lanterns.json"
    );

    static List<Path> jsonResources() throws IOException {
        try (var paths = Files.walk(RESOURCE_ROOT)) {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".json"))
                .sorted()
                .toList();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("jsonResources")
    void everyJsonResourceUsesStrictSyntax(Path path) throws IOException {
        try (Reader source = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonReader reader = new JsonReader(source);
            reader.setLenient(false);
            Streams.parse(reader);
            assertEquals(JsonToken.END_DOCUMENT, reader.peek());
        }
    }

    @Test
    void rawContinentsClampsLowOceanOffsetsWithoutChangingConfiguredRange() throws IOException {
        try (Reader reader = Files.newBufferedReader(RAW_CONTINENTS, StandardCharsets.UTF_8)) {
            JsonObject clamp = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("argument");

            assertEquals("minecraft:clamp", clamp.get("type").getAsString());
            assertEquals(-1, clamp.get("min").getAsInt());
            assertEquals(2, clamp.get("max").getAsInt());
            assertEquals("minecraft:add", clamp.getAsJsonObject("input").get("type").getAsString());
        }
    }

    @Test
    void riverIceUsesForge1201SnowyBiomeTagAndConfigPredicate() throws IOException {
        try (Reader reader = Files.newBufferedReader(RIVER_ICE_MODIFIER, StandardCharsets.UTF_8)) {
            JsonObject modifier = JsonParser.parseReader(reader).getAsJsonObject();

            assertEquals("#forge:is_snowy", modifier.get("biomes").getAsString());
            assertEquals("river_ice", modifier.getAsJsonObject("predicate").get("key").getAsString());
            assertEquals("tectonic:underground_river/ice", modifier.get("features").getAsString());
        }
    }

    @Test
    void riverLanternFrequencyAndLoadPredicateMatchUpstreamFixes() throws IOException {
        try (Reader noiseReader = Files.newBufferedReader(RIVER_LANTERN_NOISE, StandardCharsets.UTF_8);
             Reader modifierReader = Files.newBufferedReader(RIVER_LANTERN_MODIFIER, StandardCharsets.UTF_8)) {
            JsonObject noise = JsonParser.parseReader(noiseReader).getAsJsonObject();
            JsonObject modifier = JsonParser.parseReader(modifierReader).getAsJsonObject();

            assertEquals(1, noise.getAsJsonArray("amplitudes").size());
            assertEquals(2, noise.getAsJsonArray("amplitudes").get(0).getAsInt());
            assertEquals("river_lanterns", modifier.getAsJsonObject("predicate").get("key").getAsString());
            assertFalse(modifier.has("fabric:load_conditions"));
            assertFalse(modifier.has("neoforge:conditions"));
        }
    }
}
