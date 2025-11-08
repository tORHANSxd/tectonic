package dev.worldgen.tectonic.worldgen.densityfunction;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record ConfigClamp(DensityFunction input, DensityFunction min, DensityFunction max) implements DensityFunction {
    public static final MapCodec<ConfigClamp> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(ConfigClamp::input),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("min").forGetter(ConfigClamp::min),
        DensityFunction.HOLDER_HELPER_CODEC.fieldOf("max").forGetter(ConfigClamp::max)
    ).apply(instance, ConfigClamp::new));
    public static KeyDispatchDataCodec<ConfigClamp> CODEC_HOLDER = KeyDispatchDataCodec.of(DATA_CODEC);

    @Override
    public double compute(FunctionContext context) {
        return Math.min(Math.max(this.input.compute(context), this.min.compute(context)), this.max.compute(context));
    }

    @Override
    public void fillArray(double[] densities, ContextProvider context) {
        context.fillAllDirectly(densities, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ConfigClamp(input.mapAll(visitor), min.mapAll(visitor), max.mapAll(visitor)));
    }

    @Override
    public double minValue() {
        return this.min.minValue();
    }

    @Override
    public double maxValue() {
        return this.max.maxValue();
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC_HOLDER;
    }
}
