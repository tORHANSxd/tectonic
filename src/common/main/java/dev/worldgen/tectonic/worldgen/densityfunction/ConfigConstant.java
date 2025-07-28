package dev.worldgen.tectonic.worldgen.densityfunction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.worldgen.tectonic.config.ConfigHandler;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

import java.util.Arrays;

public record ConfigConstant(double value) implements DensityFunction {
    public static MapCodec<ConfigConstant> DATA_CODEC = Codec.STRING.fieldOf("key").xmap(ConfigConstant::new, df -> "");

    public static KeyDispatchDataCodec<ConfigConstant> CODEC_HOLDER = KeyDispatchDataCodec.of(DATA_CODEC);

    public ConfigConstant(String key) {
        this(ConfigHandler.getState().getValue(key));
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
        return value;
    }

    @Override
    public double maxValue() {
        return value;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
