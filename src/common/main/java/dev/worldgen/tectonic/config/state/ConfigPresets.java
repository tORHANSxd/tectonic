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

    private static final ConfigState OVERKILL = new ConfigState(
        ConfigState.MINOR_VERSION,
        new ConfigState.General(
            true,
            512
        ),
        new ConfigState.GlobalTerrain(
            2.5,
            1.6,
            new HeightLimits(-64, 768),
            false,
            true
        ),
        new ConfigState.Continents(
            -0.5,
            0.1,
            0.08,
            0.2,
            false,
            false,
            false,
            0.5,
            false,
            false
        ),
        ConfigState.Islands.DEFAULT,
        ConfigState.Oceans.DEFAULT,
        new ConfigState.Biomes(
            new NoiseState(0.1, 1.1, 0),
            new NoiseState(0.1, 1.1, -0.2)
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
        consumer.accept("overkill", OVERKILL.copy(), 0xea6f6f);
    }
}
