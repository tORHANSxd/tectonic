package dev.worldgen.tectonic.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import dev.worldgen.tectonic.Tectonic;
import dev.worldgen.tectonic.config.state.ConfigState;
import dev.worldgen.tectonic.config.state.object.HeightLimits;
import dev.worldgen.tectonic.config.state.object.NoiseState;
import net.minecraft.util.GsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ConfigHandler {
    private static final Object LOCK = new Object();

    private static ConfigState fileState = ConfigState.defaults();
    private static volatile ConfigSnapshot loadedState = ConfigSnapshot.from(fileState);
    private static Path path;

    private ConfigHandler() {
    }

    public static ConfigSnapshot getState() {
        return loadedState;
    }

    public static ConfigState copyConfig() {
        synchronized (LOCK) {
            return fileState.copy();
        }
    }

    public static void load(Path configPath) {
        synchronized (LOCK) {
            path = configPath.toAbsolutePath();
            ConfigState next;
            boolean backupCurrent = false;

            if (!Files.isRegularFile(path)) {
                next = ConfigState.defaults();
            } else {
                try {
                    next = parse(path);
                    backupCurrent = true;
                } catch (RuntimeException exception) {
                    Tectonic.LOGGER.error("Couldn't parse Tectonic config; preserved it as {} and restored defaults: {}",
                        invalidPath(), exception.getMessage());
                    preserveInvalidFile();
                    next = ConfigState.defaults();
                } catch (IOException exception) {
                    throw new RuntimeException("Couldn't read Tectonic config " + path, exception);
                }
            }

            SanitizedConfig sanitized = sanitize(next);
            logSanitizedFields(sanitized.invalidFields());
            if (!sanitized.invalidFields().isEmpty() && Files.isRegularFile(path)) {
                preserveInvalidFile();
            }
            writeAtomically(sanitized.state(), backupCurrent && sanitized.invalidFields().isEmpty());
            install(sanitized.state(), true);
        }
    }

    /**
     * Saves a validated copy. When {@code activate} is false, the current world-generation
     * snapshot remains unchanged and the saved values take effect after the next config load.
     */
    public static void save(ConfigState state, boolean activate) {
        synchronized (LOCK) {
            ensurePath();
            SanitizedConfig sanitized = sanitize(state);
            logSanitizedFields(sanitized.invalidFields());
            writeAtomically(sanitized.state(), true);
            install(sanitized.state(), activate);
        }
    }

    private static void install(ConfigState state, boolean activate) {
        fileState = state.copy();
        if (activate) {
            loadedState = ConfigSnapshot.from(fileState);
        }
    }

    private static ConfigState parse(Path source) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            return ConfigState.CODEC.parse(JsonOps.INSTANCE, json).result()
                .orElseThrow(() -> new JsonParseException("Config does not match the supported schema"));
        }
    }

    private static void writeAtomically(ConfigState state, boolean backupCurrent) {
        ensurePath();
        Path parent = path.getParent();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String contents = serialize(state);
            if (Files.isRegularFile(path)
                && Files.readString(path, StandardCharsets.UTF_8).equals(contents)) {
                return;
            }

            Files.writeString(
                temporary,
                contents,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );

            if (backupCurrent && isUsableConfig(path)) {
                Files.copy(path, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new RuntimeException("Couldn't save Tectonic config " + path, exception);
        } finally {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException exception) {
                Tectonic.LOGGER.warn("Couldn't remove temporary Tectonic config {}", temporary, exception);
            }
        }
    }

    private static String serialize(ConfigState state) throws IOException {
        JsonElement element = ConfigState.CODEC.encodeStart(JsonOps.INSTANCE, state).result()
            .orElseThrow(() -> new JsonParseException("Couldn't encode validated config"));
        StringWriter output = new StringWriter();
        try (JsonWriter writer = new JsonWriter(output)) {
            writer.setIndent("  ");
            GsonHelper.writeValue(writer, element, Comparator.naturalOrder());
        }
        return output.toString();
    }

    private static boolean isUsableConfig(Path candidate) {
        if (!Files.isRegularFile(candidate)) {
            return false;
        }
        try {
            return sanitize(parse(candidate)).invalidFields().isEmpty();
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }

    private static void preserveInvalidFile() {
        try {
            Files.copy(path, invalidPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Couldn't preserve invalid Tectonic config " + path, exception);
        }
    }

    private static Path backupPath() {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private static Path invalidPath() {
        return path.resolveSibling(path.getFileName() + ".invalid");
    }

    private static void ensurePath() {
        if (path == null) {
            throw new IllegalStateException("Tectonic config path has not been loaded");
        }
    }

    private static SanitizedConfig sanitize(ConfigState source) {
        ConfigState state = source.copy();
        List<String> invalid = new ArrayList<>();

        state.general.snowStartOffset = validInt(state.general.snowStartOffset, 0, 256, ConfigState.General.SNOW_START_OFFSET, "general.snow_start_offset", invalid);

        state.globalTerrain.verticalScale = validDouble(state.globalTerrain.verticalScale, 0.1, 15, ConfigState.GlobalTerrain.VERTICAL_SCALE, "global_terrain.vertical_scale", invalid);
        state.globalTerrain.elevationBoost = validDouble(state.globalTerrain.elevationBoost, 0, 1, ConfigState.GlobalTerrain.ELEVATION_BOOST, "global_terrain.elevation_boost", invalid);
        try {
            state.globalTerrain.heightLimits = new HeightLimits(state.globalTerrain.heightLimits.minY, state.globalTerrain.heightLimits.maxY);
        } catch (IllegalArgumentException exception) {
            invalid.add("global_terrain.height_limits");
            state.globalTerrain.heightLimits = HeightLimits.DEFAULT.copy();
        }

        state.continents.oceanOffset = validDouble(state.continents.oceanOffset, -2, 0, ConfigState.Continents.OCEAN_OFFSET, "continents.ocean_offset", invalid);
        state.continents.continentsScale = validDouble(state.continents.continentsScale, 0.01, 1, ConfigState.Continents.CONTINENTS_SCALE, "continents.continents_scale", invalid);
        state.continents.erosionScale = validDouble(state.continents.erosionScale, 0.01, 1, ConfigState.Continents.EROSION_SCALE, "continents.erosion_scale", invalid);
        state.continents.ridgeScale = validDouble(state.continents.ridgeScale, 0.01, 2, ConfigState.Continents.RIDGE_SCALE, "continents.ridge_scale", invalid);
        state.continents.flatTerrainSkew = validDouble(state.continents.flatTerrainSkew, -1, 1, ConfigState.Continents.FLAT_TERRAIN_SKEW, "continents.flat_terrain_skew", invalid);

        sanitizeNoise(state.islands.noise, ConfigState.Islands.NOISE, "islands.noise", invalid);

        state.oceans.oceanDepth = validDouble(state.oceans.oceanDepth, -10, -0.05, ConfigState.Oceans.OCEAN_DEPTH, "oceans.ocean_depth", invalid);
        state.oceans.deepOceanDepth = validDouble(state.oceans.deepOceanDepth, -10, -0.05, ConfigState.Oceans.DEEP_OCEAN_DEPTH, "oceans.deep_ocean_depth", invalid);
        state.oceans.monumentOffset = validInt(state.oceans.monumentOffset, -60, 0, ConfigState.Oceans.MONUMENT_OFFSET, "oceans.monument_offset", invalid);

        sanitizeNoise(state.biomes.temperature, NoiseState.DEFAULT, "biomes.temperature", invalid);
        sanitizeNoise(state.biomes.vegetation, NoiseState.DEFAULT, "biomes.vegetation", invalid);

        state.caves.depthCutoffStart = validDouble(state.caves.depthCutoffStart, -0.1, 1, ConfigState.Caves.DEPTH_CUTOFF_START, "caves.depth_cutoff_start", invalid);
        state.caves.depthCutoffSize = validDouble(state.caves.depthCutoffSize, 0, 1, ConfigState.Caves.DEPTH_CUTOFF_SIZE, "caves.depth_cutoff_size", invalid);
        state.caves.cheeseAdditive = validDouble(state.caves.cheeseAdditive, -0.5, 0.5, ConfigState.Caves.CHEESE_ADDITIVE, "caves.cheese_additive", invalid);
        state.caves.noodleAdditive = validDouble(state.caves.noodleAdditive, -0.25, 0.25, ConfigState.Caves.NOODLE_ADDITIVE, "caves.noodle_additive", invalid);

        return new SanitizedConfig(state, List.copyOf(invalid));
    }

    private static void sanitizeNoise(NoiseState state, NoiseState defaults, String key, List<String> invalid) {
        state.scale = validDouble(state.scale, 0, 1, defaults.scale, key + "_scale", invalid);
        state.multiplier = validDouble(state.multiplier, 0, 5, defaults.multiplier, key + "_multiplier", invalid);
        state.offset = validDouble(state.offset, -1, 1, defaults.offset, key + "_offset", invalid);
    }

    private static double validDouble(double value, double minimum, double maximum, double fallback, String key, List<String> invalid) {
        if (Double.isFinite(value) && value >= minimum && value <= maximum) {
            return value;
        }
        invalid.add(key);
        return fallback;
    }

    private static int validInt(int value, int minimum, int maximum, int fallback, String key, List<String> invalid) {
        if (value >= minimum && value <= maximum) {
            return value;
        }
        invalid.add(key);
        return fallback;
    }

    private static void logSanitizedFields(List<String> invalidFields) {
        if (!invalidFields.isEmpty()) {
            Tectonic.LOGGER.warn("Reset invalid Tectonic config values to defaults: {}", String.join(", ", invalidFields));
        }
    }

    private record SanitizedConfig(ConfigState state, List<String> invalidFields) {
    }
}
