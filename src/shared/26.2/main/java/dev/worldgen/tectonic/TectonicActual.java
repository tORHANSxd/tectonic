package dev.worldgen.tectonic;

import com.mojang.serialization.MapCodec;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigClamp;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigConstant;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import dev.worldgen.tectonic.worldgen.densityfunction.Invert;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.msrandom.multiplatform.annotations.Actual;

import java.util.function.BiConsumer;

public class TectonicActual {
    @Actual
    public static int getBlendingVersion(CompoundTag tag) {
        return tag.getIntOr(Tectonic.BLENDING_KEY, 0);
    }

    @Actual
    public static boolean canRunCommand(CommandSourceStack stack) {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(stack);
    }
    
    @Actual
    public static void registerDensityFunctionTypes(BiConsumer<String, MapCodec<? extends DensityFunction>> consumer) {
        consumer.accept("config_clamp", ConfigClamp.DATA_CODEC);
        consumer.accept("config_constant", ConfigConstant.DATA_CODEC);
        consumer.accept("config_noise", ConfigNoise.DATA_CODEC);
        consumer.accept("invert", Invert.DATA_CODEC);
    }
}
