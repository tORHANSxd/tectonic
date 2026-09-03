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

class WorldgenResourceTest {
    private static final Path RESOURCE_ROOT = Path.of("src/common/main/resources");
    private static final Path RAW_CONTINENTS = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/data/tectonic/worldgen/density_function/noise/raw_continents.json"
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
}
