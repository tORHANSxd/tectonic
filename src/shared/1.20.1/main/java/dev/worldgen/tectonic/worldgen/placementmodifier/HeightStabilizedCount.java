package dev.worldgen.tectonic.worldgen.placementmodifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class HeightStabilizedCount extends PlacementModifier {
    public static final Codec<HeightStabilizedCount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        VerticalAnchor.CODEC.fieldOf("max_y").forGetter(HeightStabilizedCount::maxY),
        ExtraCodecs.POSITIVE_FLOAT.fieldOf("count_per_section").forGetter(HeightStabilizedCount::countPerSection),
        Codec.BOOL.fieldOf("biased_to_bottom").forGetter(HeightStabilizedCount::favorBottom)
    ).apply(instance, HeightStabilizedCount::new));
    public static final PlacementModifierType<HeightStabilizedCount> TYPE = () -> CODEC;

    private final VerticalAnchor maxY;
    private final float countPerSection;
    private final boolean biasedToBottom;

    public HeightStabilizedCount(VerticalAnchor maxY, float countPerSection, boolean biasedToBottom) {
        this.maxY = maxY;
        this.countPerSection = countPerSection;
        this.biasedToBottom = biasedToBottom;
    }

    public VerticalAnchor maxY() {
        return this.maxY;
    }

    public float countPerSection() {
        return this.countPerSection;
    }

    public boolean favorBottom() {
        return this.biasedToBottom;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        int maxY = this.maxY.resolveY(context);
        int minY = context.getMinGenY();
        int count = HeightStabilizedCountMath.countForRange(minY, maxY, this.countPerSection);
        List<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            positions.add(pos.atY(HeightStabilizedCountMath.sampleY(random, minY, maxY, this.biasedToBottom)));
        }
        return positions.stream();
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE;
    }
}

final class HeightStabilizedCountMath {
    private HeightStabilizedCountMath() {
    }

    static int countForRange(int minY, int maxY, float countPerSection) {
        return (int) Math.ceil(((maxY - minY) / 16f) * countPerSection);
    }

    static int sampleY(RandomSource random, int minY, int maxY, boolean biasedToBottom) {
        if (!biasedToBottom) {
            return random.nextIntBetweenInclusive(minY, maxY);
        }
        int span = maxY - minY;
        return minY + random.nextInt(random.nextIntBetweenInclusive(span / 2, span));
    }
}
