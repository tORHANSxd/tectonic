package dev.worldgen.tectonic.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.tectonic.config.ConfigHandler;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Arrays;

public record ConfigConstant(double value, double min, double max) implements DensityFunction {
    public static MapCodec<ConfigConstant> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.STRING.fieldOf("key").forGetter(df -> ""),
        Codec.BOOL.fieldOf("min_max_hack").orElse(false).forGetter(df -> df.min == Double.NEGATIVE_INFINITY)
    ).apply(instance, ConfigConstant::create));

    public static KeyDispatchDataCodec<ConfigConstant> CODEC_HOLDER = KeyDispatchDataCodec.of(DATA_CODEC);

    public static ConfigConstant create(String key, boolean minMaxHack) {
        double value = ConfigHandler.getState().getValue(key);
        return new ConfigConstant(value, minMaxHack ? Double.NEGATIVE_INFINITY : value, minMaxHack ? Double.POSITIVE_INFINITY : value);
    }

    @Override
    public double compute(FunctionContext context) {
        return value;
    }

    @Override
    public void fillArray(double[] doubles, ContextProvider contextProvider) {
        Arrays.fill(doubles, value);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(this);
    }

    @Override
    public double minValue() {
        return min;
    }

    @Override
    public double maxValue() {
        return max;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
