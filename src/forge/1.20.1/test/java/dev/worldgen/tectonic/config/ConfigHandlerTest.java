package dev.worldgen.tectonic.config;

import dev.worldgen.tectonic.config.state.ConfigState;
import dev.worldgen.tectonic.config.state.object.HeightLimits;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigHandlerTest {
    @TempDir
    Path tempDirectory;

    @Test
    void malformedConfigIsPreservedAndReplacedWithDefaults() throws IOException {
        Path path = tempDirectory.resolve("tectonic.json");
        Files.writeString(path, "{ definitely-not-json", StandardCharsets.UTF_8);

        ConfigHandler.load(path);

        assertEquals(ConfigState.GlobalTerrain.VERTICAL_SCALE, ConfigHandler.getState().globalTerrain.verticalScale);
        assertTrue(Files.isRegularFile(path.resolveSibling("tectonic.json.invalid")));
        assertTrue(Files.readString(path, StandardCharsets.UTF_8).contains("\"minor_version\""));
    }

    @Test
    void saveKeepsPreviousValidFileAndNormalizesInvalidNumbers() throws IOException {
        Path path = tempDirectory.resolve("tectonic.json");
        ConfigHandler.load(path);
        String defaults = Files.readString(path, StandardCharsets.UTF_8);
        ConfigState draft = ConfigHandler.copyConfig();
        draft.globalTerrain.verticalScale = Double.NaN;
        draft.continents.oceanOffset = -1.25;

        ConfigHandler.save(draft, true);

        assertEquals(ConfigState.GlobalTerrain.VERTICAL_SCALE, ConfigHandler.getState().globalTerrain.verticalScale);
        assertEquals(-1.25, ConfigHandler.getState().continents.oceanOffset);
        assertEquals(defaults, Files.readString(path.resolveSibling("tectonic.json.bak"), StandardCharsets.UTF_8));
    }

    @Test
    void deferredSaveDoesNotReplaceActiveWorldgenSnapshot() throws IOException {
        Path path = tempDirectory.resolve("tectonic.json");
        ConfigHandler.load(path);
        ConfigState draft = ConfigHandler.copyConfig();
        draft.globalTerrain.verticalScale = 3;

        ConfigHandler.save(draft, false);

        assertEquals(ConfigState.GlobalTerrain.VERTICAL_SCALE, ConfigHandler.getState().globalTerrain.verticalScale);
        assertEquals(3, ConfigHandler.copyConfig().globalTerrain.verticalScale);
        ConfigHandler.load(path);
        assertEquals(3, ConfigHandler.getState().globalTerrain.verticalScale);
    }

    @Test
    void invalidHeightFallsBackWithoutDiscardingOtherFields() throws IOException {
        Path path = tempDirectory.resolve("tectonic.json");
        ConfigHandler.load(path);
        ConfigState draft = ConfigHandler.copyConfig();
        draft.continents.oceanOffset = -1.5;
        ConfigHandler.save(draft, true);
        String invalidHeight = Files.readString(path, StandardCharsets.UTF_8)
            .replace("\"min_y\": -64", "\"min_y\": -63");
        Files.writeString(path, invalidHeight, StandardCharsets.UTF_8);

        ConfigHandler.load(path);

        assertEquals(HeightLimits.DEFAULT.minY, ConfigHandler.getState().globalTerrain.heightLimits.minY);
        assertEquals(HeightLimits.DEFAULT.maxY, ConfigHandler.getState().globalTerrain.heightLimits.maxY);
        assertEquals(-1.5, ConfigHandler.getState().continents.oceanOffset);
        assertTrue(Files.isRegularFile(path.resolveSibling("tectonic.json.invalid")));
    }

    @Test
    void loadingNormalizedConfigTwiceDoesNotRewriteIt() throws IOException {
        Path path = tempDirectory.resolve("tectonic.json");
        ConfigHandler.load(path);
        String first = Files.readString(path, StandardCharsets.UTF_8);

        ConfigHandler.load(path);
        String second = Files.readString(path, StandardCharsets.UTF_8);

        assertEquals(first, second);
    }
}
