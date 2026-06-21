package dev.worldgen.tectonic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.minecraft.resources.RegistryOps;

public record ConfigResourceCondition(String key) implements ResourceCondition {
	public static final MapCodec<ConfigResourceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Codec.STRING.fieldOf("key").forGetter(ConfigResourceCondition::key)
	).apply(instance, ConfigResourceCondition::new));
	public static final ResourceConditionType<ConfigResourceCondition> TYPE = ResourceConditionType.create(Tectonic.id("config"), CODEC);
	
	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}
	
	@Override
	public boolean test(RegistryOps.RegistryInfoLookup lookup) {
		return Tectonic.CONFIG.getState().test(this.key);
	}
}