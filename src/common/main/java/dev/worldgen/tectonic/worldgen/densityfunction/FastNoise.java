package dev.worldgen.tectonic.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.tectonic.TectonicRegistries;
import dev.worldgen.tectonic.worldgen.densityfunction.fnl.FastNoiseConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record FastNoise(Holder<FastNoiseConfig> config, double xzScale, double yScale) implements DensityFunction {
    public static final MapCodec<FastNoise> DATA_CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(
        RegistryFileCodec.create(TectonicRegistries.FAST_NOISE_CONFIG, FastNoiseConfig.CODEC, false).fieldOf("config").forGetter(FastNoise::config),
        Codec.DOUBLE.optionalFieldOf("xz_scale", 1.0).forGetter(FastNoise::xzScale),
        Codec.DOUBLE.optionalFieldOf("y_scale", 1.0).forGetter(FastNoise::yScale)
    ).apply(instance, FastNoise::new));
    public static final KeyDispatchDataCodec<FastNoise> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return config.value().sample(context.blockX() * xzScale, context.blockY() * yScale, context.blockZ() * xzScale);
    }

    @Override
    public void fillArray(double[] doubles, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(doubles, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return this;
    }

    @Override
    public double minValue() {
        return -1;
    }

    @Override
    public double maxValue() {
        return 1;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
