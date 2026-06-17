package dev.worldgen.tectonic;

import com.mojang.serialization.MapCodec;
import dev.worldgen.apollib.Apollib;
import dev.worldgen.apollib.config.ApollibConfigHolder;
import dev.worldgen.tectonic.config.state.ConfigState;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigClamp;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigConstant;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import dev.worldgen.tectonic.worldgen.densityfunction.Invert;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.msrandom.multiplatform.annotations.Actual;
import net.msrandom.multiplatform.annotations.Expect;

import java.util.function.BiConsumer;

public class TectonicActual {
    @Actual
    public static final ApollibConfigHolder<ConfigState> CONFIG = new ApollibConfigHolder<>(
        Tectonic.id("tectonic"),
        ApollibConfigHolder.CONFIG_DIRECTORY.resolve("tectonic.json"),
        ConfigState.CODEC,
        ConfigState.DEFAULT_STATE
    );
    
    @Actual
    public static int getBlendingVersion(CompoundTag tag) {
        return tag.getInt(Tectonic.BLENDING_KEY);
    }

    @Actual
    public static boolean canRunCommand(CommandSourceStack stack) {
        return stack.hasPermission(2);
    }
    
    @Actual
    public static void registerDensityFunctionTypes(BiConsumer<String, MapCodec<? extends DensityFunction>> consumer) {
        consumer.accept("config_clamp", ConfigClamp.DATA_CODEC);
        consumer.accept("config_constant", ConfigConstant.DATA_CODEC);
        consumer.accept("config_noise", ConfigNoise.DATA_CODEC);
        consumer.accept("invert", Invert.DATA_CODEC);
    }
}
