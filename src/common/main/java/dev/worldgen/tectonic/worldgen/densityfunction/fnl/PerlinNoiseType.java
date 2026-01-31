package dev.worldgen.tectonic.worldgen.densityfunction.fnl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class PerlinNoiseType extends FastNoiseConfig {
    public static final MapCodec<PerlinNoiseType> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.FLOAT.fieldOf("frequency").forGetter(FastNoiseConfig::frequency),
        Codec.INT.optionalFieldOf("salt", 0).forGetter(FastNoiseConfig::salt)
    ).apply(instance, PerlinNoiseType::new));

    PerlinNoiseType(float frequency, int salt) {
        super(frequency, salt);
        fnl.SetNoiseType(FastNoiseLite.NoiseType.Perlin);
    }

    @Override
    public MapCodec<PerlinNoiseType> getCodec() {
        return CODEC;
    }
}
