package dev.worldgen.tectonic.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkSerializerMixinTest {
    @Test
    void blendingRangeUsesStoredSectionYValues() {
        ListTag sections = new ListTag();
        sections.add(section(-20));
        sections.add(section(39));

        CompoundTag blendingData = ChunkSerializerMixin.tectonic$createBlendingData(sections);

        assertEquals(-20, blendingData.getInt("min_section"));
        assertEquals(40, blendingData.getInt("max_section"));
    }

    @Test
    void blendingRangeRetainsVanillaFallbackForEmptySections() {
        CompoundTag blendingData = ChunkSerializerMixin.tectonic$createBlendingData(new ListTag());

        assertEquals(-4, blendingData.getInt("min_section"));
        assertEquals(20, blendingData.getInt("max_section"));
    }

    private static CompoundTag section(int y) {
        CompoundTag section = new CompoundTag();
        section.putByte("Y", (byte) y);
        return section;
    }
}
