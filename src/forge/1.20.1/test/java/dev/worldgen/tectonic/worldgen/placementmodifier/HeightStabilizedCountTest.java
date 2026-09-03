package dev.worldgen.tectonic.worldgen.placementmodifier;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeightStabilizedCountTest {
    @Test
    void attemptCountScalesWithTheGeneratedVerticalSpan() {
        assertEquals(7, HeightStabilizedCountMath.countForRange(-64, 16, 1.4f));
        assertEquals(13, HeightStabilizedCountMath.countForRange(-128, 16, 1.4f));
        assertEquals(30, HeightStabilizedCountMath.countForRange(-320, 16, 1.4f));
        assertEquals(1, HeightStabilizedCountMath.countForRange(-64, 16, 0.02f));
    }

    @Test
    void sampledPositionsStayInsideTheConfiguredRange() {
        RandomSource uniform = RandomSource.create(8675309L);
        RandomSource biased = RandomSource.create(8675309L);

        for (int i = 0; i < 10_000; i++) {
            assertInRange(HeightStabilizedCountMath.sampleY(uniform, -320, 16, false), -320, 16);
            assertInRange(HeightStabilizedCountMath.sampleY(biased, -320, 16, true), -320, 16);
        }
    }

    private static void assertInRange(int actual, int min, int max) {
        assertTrue(actual >= min && actual <= max, () -> actual + " outside [" + min + ", " + max + "]");
    }
}
