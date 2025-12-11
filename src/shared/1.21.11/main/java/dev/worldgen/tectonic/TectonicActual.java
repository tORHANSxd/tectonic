package dev.worldgen.tectonic;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.msrandom.multiplatform.annotations.Actual;
import net.msrandom.multiplatform.annotations.Expect;

public class TectonicActual {
    @Actual
    public static Identifier idVanilla(String name) {
        return Identifier.withDefaultNamespace(name);
    }

    @Actual
    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(Tectonic.MOD_ID, name);
    }

    @Actual
    public static int getBlendingVersion(CompoundTag tag) {
        return tag.getIntOr(Tectonic.BLENDING_KEY, 0);
    }

    @Actual
    public static boolean canRunCommand(CommandSourceStack stack) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(stack);
    }
}
