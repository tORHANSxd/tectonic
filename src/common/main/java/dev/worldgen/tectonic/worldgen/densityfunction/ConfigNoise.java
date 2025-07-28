package dev.worldgen.tectonic.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.tectonic.config.ConfigHandler;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record ConfigNoise(NoiseHolder noise, DensityFunction shiftX, DensityFunction shiftZ, double scale) implements DensityFunction {
    public static MapCodec<ConfigNoise> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("key").forGetter(df -> ""),
        NoiseHolder.CODEC.fieldOf("noise").forGetter(ConfigNoise::noise),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_x").forGetter(ConfigNoise::shiftX),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("shift_z").forGetter(ConfigNoise::shiftZ)
    ).apply(instance, ConfigNoise::new));

    public static KeyDispatchDataCodec<ConfigNoise> CODEC_HOLDER = KeyDispatchDataCodec.of(DATA_CODEC);

    public ConfigNoise(String key, NoiseHolder noise, DensityFunction shiftX, DensityFunction shiftZ) {
        this(noise, shiftX, shiftZ, ConfigHandler.getState().getValue(key));
    }

    @Override
    public double compute(FunctionContext context) {
        double x = context.blockX() * scale + shiftX.compute(context);
        double z = context.blockZ() * scale + shiftZ.compute(context);
        return noise.getValue(x, 0, z);
    }

    @Override
    public void fillArray(double[] doubles, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(doubles, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ConfigNoise(visitor.visitNoise(noise), shiftX.mapAll(visitor), shiftZ.mapAll(visitor), scale));
    }

    @Override
    public double minValue() {
        return -this.maxValue();
    }

    @Override
    public double maxValue() {
        return noise.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
