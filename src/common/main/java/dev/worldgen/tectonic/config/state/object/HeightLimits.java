package dev.worldgen.tectonic.config.state.object;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public final class HeightLimits {
    public static final HeightLimits DEFAULT = new HeightLimits(-64, 320);
    public static final HeightLimits INCREASED_HEIGHT = new HeightLimits(-64, 640);

    private static final MapCodec<HeightLimits> OLD_CODEC = Codec.BOOL.fieldOf("increased_height").xmap(HeightLimits::defaultLimits, limits -> limits.maxY > 320);
    private static final MapCodec<HeightLimits> NEW_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("min_y").forGetter(limits -> limits.minY),
        Codec.INT.fieldOf("max_y").forGetter(limits -> limits.maxY)
    ).apply(instance, HeightLimits::decoded));
    public static final MapCodec<HeightLimits> FULL_CODEC = Codec.mapEither(NEW_CODEC, OLD_CODEC).xmap(either -> either.map(t -> t, t -> t), Either::left);

    public final int minY;
    public final int maxY;

    public HeightLimits(int minY, int maxY) {
        this(minY, maxY, true);
    }

    private HeightLimits(int minY, int maxY, boolean validate) {
        if (validate) {
            validate(minY, maxY);
        }
        this.minY = minY;
        this.maxY = maxY;
    }

    private static void validate(int minY, int maxY) {
        if (minY % 16 != 0) {
            throw new IllegalArgumentException("min_y should be a multiple of 16!");
        } else if (minY < -2032 || minY > -64) {
            throw new IllegalArgumentException("min_y should be between -2032 and -64!");
        } else if (maxY % 16 != 0) {
            throw new IllegalArgumentException("max_y should be a multiple of 16!");
        } else if (maxY < 256 || maxY > 2032) {
            throw new IllegalArgumentException("max_y should be between 256 and 2032!");
        } else if (maxY <= minY) {
            throw new IllegalArgumentException("max_y should be greater than min_y!");
        }
    }

    private static HeightLimits decoded(int minY, int maxY) {
        return new HeightLimits(minY, maxY, false);
    }

    public static HeightLimits defaultLimits(boolean increasedHeight) {
        return (increasedHeight ? INCREASED_HEIGHT : DEFAULT).copy();
    }

    public HeightLimits copy() {
        return new HeightLimits(this.minY, this.maxY, false);
    }

    public boolean isVanilla() {
        return this.minY == DEFAULT.minY && this.maxY == DEFAULT.maxY;
    }

    public int getHeight() {
        return this.maxY - this.minY;
    }
}
