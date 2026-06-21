package dev.worldgen.tectonic.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.worldgen.tectonic.config.object.NoiseState;

public class NewConfigState {
	
	public static class Noises {
		public static final NoiseState TEMPERATURE = NoiseState.create();
		public static final NoiseState VEGETATION = NoiseState.create();
		public static final NoiseState CONTINENTS = NoiseState.create(0.13);
		public static final NoiseState EROSION = NoiseState.create();
		public static final NoiseState RIDGES = NoiseState.create();
		public static final NoiseState ISLANDS = NoiseState.create(0.11);
		
		public static final Codec<Noises> CODEC = RecordCodecBuilder.create(i -> i.group(
			NoiseState.codec("temperature", TEMPERATURE).forGetter(n -> n.temperature),
			NoiseState.codec("vegetation", VEGETATION).forGetter(n -> n.vegetation),
			NoiseState.codec("continents", CONTINENTS).forGetter(n -> n.continents),
			NoiseState.codec("erosion", EROSION).forGetter(n -> n.erosion),
			NoiseState.codec("ridges", RIDGES).forGetter(n -> n.ridges),
			NoiseState.codec("islands", ISLANDS).forGetter(n -> n.islands)
		).apply(i, Noises::new));
		
		public NoiseState temperature;
		public NoiseState vegetation;
		public NoiseState continents;
		public NoiseState erosion;
		public NoiseState ridges;
		public NoiseState islands;
		
		public Noises(NoiseState temperature, NoiseState vegetation, NoiseState continents, NoiseState erosion, NoiseState ridges, NoiseState islands) {
			this.temperature = temperature;
			this.vegetation = vegetation;
			this.continents = continents;
			this.erosion = erosion;
			this.ridges = ridges;
			this.islands = islands;
		}
	}
}
