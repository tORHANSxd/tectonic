package dev.worldgen.tectonic;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.msrandom.multiplatform.annotations.Actual;

public class TectonicActual {
    @Actual
    public static int getBlendingVersion(CompoundTag tag) {
        return tag.getIntOr(Tectonic.BLENDING_KEY, 0);
    }

    @Actual
    public static boolean canRunCommand(CommandSourceStack stack) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(stack);
    }
}
