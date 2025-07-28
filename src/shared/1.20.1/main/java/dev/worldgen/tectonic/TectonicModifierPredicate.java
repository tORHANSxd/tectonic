package dev.worldgen.tectonic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.lithostitched.worldgen.modifier.predicate.ModifierPredicate;
import dev.worldgen.tectonic.config.ConfigHandler;

public record TectonicModifierPredicate(String key) implements ModifierPredicate {
    public static final Codec<TectonicModifierPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("key").forGetter(TectonicModifierPredicate::key)
    ).apply(instance, TectonicModifierPredicate::new));

    @Override
    public boolean test() {
        return switch (this.key) {
            case "disable_islands" -> !ConfigHandler.getState().islands.enabled;
            case "increased_height" -> ConfigHandler.getState().globalTerrain.increasedHeight;
            case "ultrasmooth" -> ConfigHandler.getState().globalTerrain.ultrasmooth;
            case "remove_frozen_ocean_ice" -> ConfigHandler.getState().oceans.removeFrozenOceanIce;
            case "river_lanterns" -> ConfigHandler.getState().continents.riverLanterns;
            default -> false;
        };
    }

    @Override
    public Codec<? extends ModifierPredicate> codec() {
        return CODEC;
    }
}
