package dev.worldgen.tectonic;

import com.mojang.serialization.MapCodec;
import dev.worldgen.tectonic.worldgen.densityfunction.fnl.FastNoiseConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public interface TectonicRegistries {
    ResourceKey<Registry<FastNoiseConfig>> FAST_NOISE_CONFIG = ResourceKey.createRegistryKey(Tectonic.id("fast_noise_config"));
    ResourceKey<Registry<MapCodec<? extends FastNoiseConfig>>> FAST_NOISE_CONFIG_TYPE = ResourceKey.createRegistryKey(Tectonic.id("fast_noise_config_type"));
}
