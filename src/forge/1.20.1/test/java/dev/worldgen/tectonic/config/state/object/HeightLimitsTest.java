package dev.worldgen.tectonic.config.state.object;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeightLimitsTest {
    @Test
    void acceptsMinecraft1201SectionAlignedBounds() {
        HeightLimits limits = new HeightLimits(-2032, 2032);

        assertEquals(4064, limits.getHeight());
    }

    @Test
    void rejectsMisalignedOrOutOfRangeBounds() {
        assertThrows(IllegalArgumentException.class, () -> new HeightLimits(-63, 320));
        assertThrows(IllegalArgumentException.class, () -> new HeightLimits(-2048, 320));
        assertThrows(IllegalArgumentException.class, () -> new HeightLimits(-64, 2048));
        assertThrows(IllegalArgumentException.class, () -> new HeightLimits(-64, 240));
    }

    @Test
    void recognizesOnlyVanillaOverworldLimits() {
        assertTrue(new HeightLimits(-64, 320).isVanilla());
        assertFalse(new HeightLimits(-64, 640).isVanilla());
        assertFalse(new HeightLimits(-128, 320).isVanilla());
    }
}
