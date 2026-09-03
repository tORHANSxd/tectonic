package dev.worldgen.tectonic.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSerializerMixinTest {
    @Test
    void blendingRangeUsesStoredSectionYValues() {
        ListTag sections = new ListTag();
        sections.add(section(-20));
        sections.add(section(39));

        CompoundTag blendingData = createBlendingData(sections);

        assertEquals(-20, blendingData.getInt("min_section"));
        assertEquals(40, blendingData.getInt("max_section"));
    }

    @Test
    void blendingRangeRetainsVanillaFallbackForEmptySections() {
        CompoundTag blendingData = createBlendingData(new ListTag());

        assertEquals(-4, blendingData.getInt("min_section"));
        assertEquals(20, blendingData.getInt("max_section"));
    }

    @Test
    void blendingHelperRemainsPrivateForMixinCompatibility() throws NoSuchMethodException {
        Method method = ChunkSerializerMixin.class.getDeclaredMethod("tectonic$createBlendingData", ListTag.class);

        assertTrue(Modifier.isPrivate(method.getModifiers()));
        assertTrue(Modifier.isStatic(method.getModifiers()));
    }

    private static CompoundTag createBlendingData(ListTag sections) {
        try {
            Method method = ChunkSerializerMixin.class.getDeclaredMethod("tectonic$createBlendingData", ListTag.class);
            method.setAccessible(true);
            return (CompoundTag) method.invoke(null, sections);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static CompoundTag section(int y) {
        CompoundTag section = new CompoundTag();
        section.putByte("Y", (byte) y);
        return section;
    }
}
