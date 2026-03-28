package dev.worldgen.tectonic.lithostitched;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.worldgen.lithostitched.api.predicate.LoadPredicate;
import dev.worldgen.tectonic.config.ConfigHandler;

public record ConfigLoadPredicate(String key) implements LoadPredicate {
	public static final MapCodec<ConfigLoadPredicate> CODEC = Codec.STRING.fieldOf("key").xmap(ConfigLoadPredicate::new, ConfigLoadPredicate::key);
	
	@Override
	public boolean test() {
		return ConfigHandler.getState().test(key);
	}
	
	@Override
	public MapCodec<ConfigLoadPredicate> codec() {
		return CODEC;
	}
}
