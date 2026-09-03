package dev.worldgen.tectonic.client;

import dev.worldgen.tectonic.config.state.ConfigState;
import dev.worldgen.tectonic.config.state.object.NoiseState;
import net.minecraft.client.gui.Font;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigListBuilderTest {
    @Test
    void buildExposesTheOreFixToggleAndUpdatesTheDraft() {
        ConfigState state = ConfigState.defaults();
        AtomicReference<Consumer<Boolean>> oreFixSetter = new AtomicReference<>();

        ConfigListBuilder builder = new ConfigListBuilder() {
            @Override
            public void addCategory(String name, Font font) {
            }

            @Override
            public void addBoolean(String name, Consumer<Boolean> setter, boolean getter, boolean defaultValue) {
                if (name.equals("ore_fix")) {
                    assertFalse(getter);
                    assertFalse(defaultValue);
                    oreFixSetter.set(setter);
                }
            }

            @Override
            public void addInteger(String name, double min, double max, double step, Consumer<Integer> setter, double getter, double defaultValue) {
            }

            @Override
            public void addDouble(String name, double min, double max, double step, Consumer<Double> setter, double getter, double defaultValue) {
            }

            @Override
            public void addNoise(String name, NoiseState state, NoiseState defaultState) {
            }
        };

        builder.build(null, state);
        assertNotNull(oreFixSetter.get());
        oreFixSetter.get().accept(true);
        assertTrue(state.caves.oreFix);
    }
}
