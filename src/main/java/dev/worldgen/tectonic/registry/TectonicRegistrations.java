package dev.worldgen.tectonic.registry;

import dev.worldgen.lithostitched.api.registry.LithostitchedBuiltInRegistries;
import dev.worldgen.tectonic.lithostitched.ConfigLoadPredicate;
import dev.worldgen.tectonic.lithostitched.SetHeightLimitsModifier;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigClamp;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigConstant;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import dev.worldgen.tectonic.worldgen.densityfunction.Invert;
import dev.worldgen.tectonic.worldgen.placementmodifier.HeightStabilizedCount;
import net.minecraft.core.registries.BuiltInRegistries;

import static dev.worldgen.tectonic.Tectonic.REGISTRAR;

public class TectonicRegistrations {
	public static void init() {
		REGISTRAR.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, "config_clamp", ConfigClamp.DATA_CODEC);
		REGISTRAR.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, "config_constant", ConfigConstant.DATA_CODEC);
		REGISTRAR.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, "config_noise", ConfigNoise.DATA_CODEC);
		REGISTRAR.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, "invert", Invert.DATA_CODEC);
		
		REGISTRAR.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, "height_stabilized_count", HeightStabilizedCount.TYPE);
		
		REGISTRAR.register(LithostitchedBuiltInRegistries.MODIFIER_TYPE, "set_height_limits", SetHeightLimitsModifier.CODEC);
		
		REGISTRAR.register(LithostitchedBuiltInRegistries.LOAD_PREDICATE_TYPE, "config", ConfigLoadPredicate.CODEC);
	}
}
