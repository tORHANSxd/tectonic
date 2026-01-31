package dev.worldgen.tectonic.worldgen.densityfunction.fnl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.tectonic.worldgen.densityfunction.fnl.FastNoiseLite.CellularDistanceFunction;
import net.minecraft.util.StringRepresentable;

public class CellularNoiseType extends FastNoiseConfig {
    public static final MapCodec<CellularNoiseType> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.FLOAT.fieldOf("frequency").forGetter(FastNoiseConfig::frequency),
        Codec.INT.optionalFieldOf("salt", 0).forGetter(FastNoiseConfig::salt),
        StringRepresentable.fromValues(DistanceFunction::values).fieldOf("distance_function").forGetter(CellularNoiseType::distanceFunction),
        StringRepresentable.fromValues(ReturnType::values).fieldOf("return_type").forGetter(CellularNoiseType::returnType),
        Codec.floatRange(-1f, 1f).fieldOf("jitter").forGetter(CellularNoiseType::jitter)
    ).apply(instance, CellularNoiseType::new));

    private final DistanceFunction distanceFunction;
    private final ReturnType returnType;
    private final float jitter;

    CellularNoiseType(float frequency, int salt, DistanceFunction distanceFunction, ReturnType returnType, float jitter) {
        super(frequency, salt);
        this.distanceFunction = distanceFunction;
        this.returnType = returnType;
        this.jitter = jitter;

        fnl.SetNoiseType(FastNoiseLite.NoiseType.Cellular);
        fnl.SetCellularDistanceFunction(distanceFunction.internal);
        fnl.SetCellularReturnType(returnType.internal);
        fnl.SetCellularJitter(jitter);
    }

    public DistanceFunction distanceFunction() {
        return distanceFunction;
    }

    private ReturnType returnType() {
        return returnType;
    }

    private float jitter() {
        return jitter;
    }

    @Override
    public MapCodec<CellularNoiseType> getCodec() {
        return CODEC;
    }

    public enum DistanceFunction implements StringRepresentable {
        EUCLIDEAN("euclidean", CellularDistanceFunction.Euclidean),
        EUCLIDEAN_SQUARED("euclidean_squared", CellularDistanceFunction.EuclideanSq),
        MANHATTAN("manhattan", CellularDistanceFunction.Manhattan),
        HYBRID("hybrid", CellularDistanceFunction.Hybrid);

        private final String id;
        private final CellularDistanceFunction internal;

        DistanceFunction(String id, CellularDistanceFunction internal) {
            this.id = id;
            this.internal = internal;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }

    public enum ReturnType implements StringRepresentable {
        CELL_VALUE("cell_value", FastNoiseLite.CellularReturnType.CellValue),
        DISTANCE("distance", FastNoiseLite.CellularReturnType.Distance),
        DISTANCE_2("distance_2", FastNoiseLite.CellularReturnType.Distance2),
        DISTANCE_2_ADD("distance_2_add", FastNoiseLite.CellularReturnType.Distance2Add),
        DISTANCE_2_SUB("distance_2_sub", FastNoiseLite.CellularReturnType.Distance2Sub),
        DISTANCE_2_MUL("distance_2_mul", FastNoiseLite.CellularReturnType.Distance2Mul),
        DISTANCE_2_DIV("distance_2_div", FastNoiseLite.CellularReturnType.Distance2Div);

        private final String id;
        private final FastNoiseLite.CellularReturnType internal;

        ReturnType(String id, FastNoiseLite.CellularReturnType internal) {
            this.id = id;
            this.internal = internal;
        }

        @Override
        public String getSerializedName() {
            return id;
        }
    }
}
