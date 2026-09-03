package dev.worldgen.tectonic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TectonicLexforgeTest {
    @Test
    void oreFixOnlyActivatesForExtendedDepth() {
        assertFalse(TectonicLexforge.shouldEnableOreFix(false, -128));
        assertFalse(TectonicLexforge.shouldEnableOreFix(true, -64));
        assertTrue(TectonicLexforge.shouldEnableOreFix(true, -128));
        assertTrue(TectonicLexforge.shouldEnableOreFix(true, -320));
    }
}
