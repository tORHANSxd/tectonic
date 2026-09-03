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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldgenResourceTest {
    private static final Path RESOURCE_ROOT = Path.of("src/common/main/resources");
    private static final Path EN_US = RESOURCE_ROOT.resolve("assets/tectonic/lang/en_us.json");
    private static final Path RAW_CONTINENTS = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/data/tectonic/worldgen/density_function/noise/raw_continents.json"
    );
    private static final Path JAGGEDNESS_CONTINENTS = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/data/tectonic/worldgen/density_function/terrain_spline/jaggedness/continents.json"
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
    private static final Path REGION_ROOT = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/data/tectonic/worldgen/density_function/region"
    );
    private static final Path ORE_FIX_ROOT = RESOURCE_ROOT.resolve(
        "resourcepacks/tectonic/overlay.ore_fix"
    );
    private static final List<String> HORIZONTAL_CACHE_REGIONS = List.of(
        "club.json",
        "club_weak.json",
        "heart.json",
        "spade.json",
        "spade_weak.json",
        "diamond.json"
    );
    private static final Map<String, OreFixExpectation> ORE_FIX_FEATURES = Map.ofEntries(
        Map.entry("ore_diamond.json", new OreFixExpectation("minecraft:ore_diamond_small", "absolute", 16, 1.4, true)),
        Map.entry("ore_diamond_buried.json", new OreFixExpectation("minecraft:ore_diamond_buried", "absolute", 16, 0.8, true)),
        Map.entry("ore_diamond_large.json", new OreFixExpectation("minecraft:ore_diamond_large", "absolute", 16, 0.02, true)),
        Map.entry("ore_gold.json", new OreFixExpectation("minecraft:ore_gold_buried", "absolute", 32, 0.66, false)),
        Map.entry("ore_gold_extra.json", new OreFixExpectation("minecraft:ore_gold_buried", "absolute", -48, 0.5, false)),
        Map.entry("ore_gravel.json", new OreFixExpectation("minecraft:ore_gravel", "below_top", 0, 0.7, false)),
        Map.entry("ore_lapis.json", new OreFixExpectation("minecraft:ore_lapis", "absolute", 32, 0.5, true)),
        Map.entry("ore_lapis_buried.json", new OreFixExpectation("minecraft:ore_lapis_buried", "absolute", 64, 0.5, false)),
        Map.entry("ore_redstone.json", new OreFixExpectation("minecraft:ore_redstone", "absolute", 16, 0.66, false)),
        Map.entry("ore_redstone_lower.json", new OreFixExpectation("minecraft:ore_redstone", "absolute", -32, 4, false)),
        Map.entry("ore_tuff.json", new OreFixExpectation("minecraft:ore_tuff", "absolute", 0, 0.5, false))
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
    void mountainJaggednessKeepsTheValueRestoredByUpstream3026() throws IOException {
        try (Reader reader = Files.newBufferedReader(JAGGEDNESS_CONTINENTS, StandardCharsets.UTF_8)) {
            JsonObject spline = JsonParser.parseReader(reader).getAsJsonObject()
                .getAsJsonObject("argument")
                .getAsJsonObject("argument")
                .getAsJsonObject("spline");
            JsonObject erosionSpline = spline.getAsJsonArray("points").get(1).getAsJsonObject()
                .getAsJsonObject("value");

            assertEquals(
                0.65,
                erosionSpline.getAsJsonArray("points").get(0).getAsJsonObject().get("value").getAsDouble()
            );
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

    @Test
    void regionSplinesUseOneHorizontalCacheLayer() throws IOException {
        for (String fileName : HORIZONTAL_CACHE_REGIONS) {
            try (Reader reader = Files.newBufferedReader(REGION_ROOT.resolve(fileName), StandardCharsets.UTF_8)) {
                JsonObject flatCache = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject cache2d = flatCache.getAsJsonObject("argument");

                assertEquals("minecraft:flat_cache", flatCache.get("type").getAsString(), fileName);
                assertEquals("minecraft:cache_2d", cache2d.get("type").getAsString(), fileName);
                assertEquals("minecraft:spline", cache2d.getAsJsonObject("argument").get("type").getAsString(), fileName);
            }
        }
    }

    @Test
    void overkillPresetLabelWarnsAboutWorldgenCost() throws IOException {
        try (Reader reader = Files.newBufferedReader(EN_US, StandardCharsets.UTF_8)) {
            JsonObject language = JsonParser.parseReader(reader).getAsJsonObject();

            assertEquals(
                "Overkill - Very High Worldgen Cost",
                language.get("preset.tectonic.overkill").getAsString()
            );
        }
    }

    @Test
    void oreFixOverlayMatchesTheCompatibleUpstreamFeatureSet() throws IOException {
        Path placedFeatures = ORE_FIX_ROOT.resolve("data/minecraft/worldgen/placed_feature");
        try (var paths = Files.list(placedFeatures)) {
            assertEquals(
                ORE_FIX_FEATURES.keySet().stream().sorted().toList(),
                paths.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).sorted().toList()
            );
        }

        for (Map.Entry<String, OreFixExpectation> entry : ORE_FIX_FEATURES.entrySet()) {
            try (Reader reader = Files.newBufferedReader(placedFeatures.resolve(entry.getKey()), StandardCharsets.UTF_8)) {
                JsonObject feature = JsonParser.parseReader(reader).getAsJsonObject();
                OreFixExpectation expected = entry.getValue();
                JsonObject count = feature.getAsJsonArray("placement").get(0).getAsJsonObject();
                JsonObject maxY = count.getAsJsonObject("max_y");

                assertEquals(expected.feature(), feature.get("feature").getAsString(), entry.getKey());
                assertEquals("tectonic:height_stabilized_count", count.get("type").getAsString(), entry.getKey());
                assertEquals(1, maxY.size(), entry.getKey());
                assertEquals(expected.maxY(), maxY.get(expected.anchor()).getAsInt(), entry.getKey());
                assertEquals(expected.countPerSection(), count.get("count_per_section").getAsDouble(), 0.000_001, entry.getKey());
                assertEquals(expected.biasedToBottom(), count.get("biased_to_bottom").getAsBoolean(), entry.getKey());
                assertEquals("minecraft:in_square", feature.getAsJsonArray("placement").get(1).getAsJsonObject().get("type").getAsString(), entry.getKey());
                assertEquals("minecraft:biome", feature.getAsJsonArray("placement").get(2).getAsJsonObject().get("type").getAsString(), entry.getKey());
            }
        }
    }

    @Test
    void oreFixOverlayUsesTheForge1201PackFormat() throws IOException {
        try (Reader reader = Files.newBufferedReader(ORE_FIX_ROOT.resolve("pack.mcmeta"), StandardCharsets.UTF_8)) {
            JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals(15, metadata.getAsJsonObject("pack").get("pack_format").getAsInt());
        }
    }

    @Test
    void oreFixHasVisibleRestartAwareTranslations() throws IOException {
        try (Reader reader = Files.newBufferedReader(EN_US, StandardCharsets.UTF_8)) {
            JsonObject language = JsonParser.parseReader(reader).getAsJsonObject();
            assertEquals("Ore Fix", language.get("config.tectonic.option.ore_fix").getAsString());
            assertTrue(language.get("config.tectonic.option.ore_fix.tooltip").getAsString().contains("restart"));
        }
    }

    private record OreFixExpectation(
        String feature,
        String anchor,
        int maxY,
        double countPerSection,
        boolean biasedToBottom
    ) {
    }
}
