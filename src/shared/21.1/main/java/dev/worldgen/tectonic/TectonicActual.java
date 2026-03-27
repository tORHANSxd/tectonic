package dev.worldgen.tectonic;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.msrandom.multiplatform.annotations.Actual;

public class TectonicActual {
    @Actual
    public static int getBlendingVersion(CompoundTag tag) {
        return tag.getInt(Tectonic.BLENDING_KEY);
    }

    @Actual
    public static boolean canRunCommand(CommandSourceStack stack) {
        return stack.hasPermission(2);
    }
}
