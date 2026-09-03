package dev.worldgen.tectonic.config.state;

import dev.worldgen.tectonic.config.ConfigSnapshot;
import dev.worldgen.tectonic.config.state.object.HeightLimits;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStateTest {
    @Test
    void copyDoesNotShareMutableNestedState() {
        ConfigState original = ConfigState.defaults();
        ConfigState copy = original.copy();

        copy.globalTerrain.verticalScale = 2.5;
        copy.globalTerrain.heightLimits = new HeightLimits(-128, 640);
        copy.islands.noise.offset = 0.75;
        copy.biomes.temperature.scale = 0.5;

        assertEquals(ConfigState.GlobalTerrain.VERTICAL_SCALE, original.globalTerrain.verticalScale);
        assertEquals(HeightLimits.DEFAULT.minY, original.globalTerrain.heightLimits.minY);
        assertEquals(HeightLimits.DEFAULT.maxY, original.globalTerrain.heightLimits.maxY);
        assertEquals(ConfigState.Islands.NOISE.offset, original.islands.noise.offset);
        assertEquals(0.25, original.biomes.temperature.scale);
        assertNotSame(original.globalTerrain, copy.globalTerrain);
        assertNotSame(original.islands.noise, copy.islands.noise);
    }

    @Test
    void presetConsumersReceiveFreshCopies() {
        AtomicReference<ConfigState> first = new AtomicReference<>();
        ConfigPresets.acceptPresets((name, state, color) -> {
            if (name.equals("default")) first.set(state);
        });
        first.get().globalTerrain.verticalScale = 9;

        AtomicReference<ConfigState> second = new AtomicReference<>();
        ConfigPresets.acceptPresets((name, state, color) -> {
            if (name.equals("default")) second.set(state);
        });

        assertEquals(ConfigState.GlobalTerrain.VERTICAL_SCALE, second.get().globalTerrain.verticalScale);
        assertNotSame(first.get(), second.get());
    }

    @Test
    void runtimeSnapshotIsUnaffectedByDraftMutation() {
        ConfigState draft = ConfigState.defaults();
        ConfigSnapshot snapshot = ConfigSnapshot.from(draft);

        draft.globalTerrain.verticalScale = 8;
        draft.islands.noise.scale = 0.9;

        assertEquals(ConfigState.GlobalTerrain.VERTICAL_SCALE, snapshot.globalTerrain.verticalScale);
        assertEquals(ConfigState.Islands.NOISE.scale, snapshot.getNoiseState("island").scale);
    }

    @Test
    void riverIceDefaultsOffAndIsCopiedIntoRuntimeSnapshot() {
        ConfigState state = ConfigState.defaults();
        assertFalse(state.continents.riverIce);

        state.continents.riverIce = true;
        ConfigSnapshot snapshot = ConfigSnapshot.from(state);

        state.continents.riverIce = false;
        assertTrue(snapshot.test("river_ice"));
    }
}
