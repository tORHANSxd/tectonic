package dev.worldgen.tectonic.config.state;

import dev.worldgen.tectonic.config.state.object.HeightLimits;
import dev.worldgen.tectonic.config.state.object.NoiseState;
import org.apache.logging.log4j.util.TriConsumer;

public final class ConfigPresets {
    private static final ConfigState DEFAULT = new ConfigState(
        ConfigState.MINOR_VERSION,
        ConfigState.General.DEFAULT,
        ConfigState.GlobalTerrain.DEFAULT,
        ConfigState.Continents.DEFAULT,
        ConfigState.Islands.DEFAULT,
        ConfigState.Oceans.DEFAULT,
        ConfigState.Biomes.DEFAULT,
        ConfigState.Caves.DEFAULT
    );

    private static final ConfigState LARGE_BIOMES = new ConfigState(
        ConfigState.MINOR_VERSION,
        ConfigState.General.DEFAULT,
        ConfigState.GlobalTerrain.DEFAULT,
        ConfigState.Continents.DEFAULT,
        ConfigState.Islands.DEFAULT,
        ConfigState.Oceans.DEFAULT,
        new ConfigState.Biomes(
            new NoiseState(0.06, 1.1, 0),
            new NoiseState(0.06, 1.1, 0)
        ),
        ConfigState.Caves.DEFAULT
    );

    private static final ConfigState DESERTED = new ConfigState(
        ConfigState.MINOR_VERSION,
        ConfigState.General.DEFAULT,
        ConfigState.GlobalTerrain.DEFAULT,
        ConfigState.Continents.DEFAULT,
        ConfigState.Islands.DEFAULT,
        ConfigState.Oceans.DEFAULT,
        new ConfigState.Biomes(
            new NoiseState(0, 0, 1),
            NoiseState.DEFAULT
        ),
        ConfigState.Caves.DEFAULT
    );

    private static final ConfigState FROZEN_WASTELAND = new ConfigState(
        ConfigState.MINOR_VERSION,
        ConfigState.General.DEFAULT,
        new ConfigState.GlobalTerrain(
            1.2,
            0.5,
            new HeightLimits(-64, 448),
            true,
            false
        ),
        new ConfigState.Continents(
            -0.8,
            0.13,
            0.25,
            0.25,
            true,
            true,
            true,
            0,
            true,
            false
        ),
        ConfigState.Islands.DEFAULT,
        ConfigState.Oceans.DEFAULT,
        new ConfigState.Biomes(
            new NoiseState(0, 0, -0.69),
            new NoiseState(0.15, 0.1, -0.1)
        ),
        ConfigState.Caves.DEFAULT
    );

    private ConfigPresets() {
    }

    public static void acceptPresets(TriConsumer<String, ConfigState, Integer> consumer) {
        consumer.accept("default", DEFAULT.copy(), 0xffffff);
        consumer.accept("large_biomes", LARGE_BIOMES.copy(), 0x88ff99);
        consumer.accept("deserted", DESERTED.copy(), 0xe2ca76);
        consumer.accept("frozen_wasteland", FROZEN_WASTELAND.copy(), 0xc1eaff);
    }
}
