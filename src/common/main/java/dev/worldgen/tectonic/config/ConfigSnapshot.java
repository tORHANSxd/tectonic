package dev.worldgen.tectonic.config;

import dev.worldgen.tectonic.config.state.ConfigState;
import dev.worldgen.tectonic.config.state.object.HeightLimits;
import dev.worldgen.tectonic.config.state.object.NoiseState;

import java.util.Objects;

/** Immutable configuration published to world-generation threads. */
public final class ConfigSnapshot {
    public final General general;
    public final GlobalTerrain globalTerrain;
    public final Continents continents;
    public final Islands islands;
    public final Oceans oceans;
    public final Biomes biomes;
    public final Caves caves;

    private ConfigSnapshot(ConfigState state) {
        this.general = new General(state.general);
        this.globalTerrain = new GlobalTerrain(state.globalTerrain);
        this.continents = new Continents(state.continents);
        this.islands = new Islands(state.islands);
        this.oceans = new Oceans(state.oceans);
        this.biomes = new Biomes(state.biomes);
        this.caves = new Caves(state.caves);
    }

    public static ConfigSnapshot from(ConfigState state) {
        return new ConfigSnapshot(Objects.requireNonNull(state, "state"));
    }

    public double getValue(String option) {
        return switch (option) {
            case "vertical_scale" -> this.globalTerrain.verticalScale;
            case "elevation_boost" -> this.globalTerrain.elevationBoost;
            case "min_y" -> this.globalTerrain.heightLimits.minY;
            case "max_y" -> this.globalTerrain.heightLimits.maxY;
            case "lava_tunnels" -> this.globalTerrain.lavaTunnels ? 1 : 0;

            case "ocean_offset" -> this.continents.oceanOffset;
            case "underground_rivers" -> this.continents.undergroundRivers ? -1 : 0;
            case "flat_terrain_skew" -> this.continents.flatTerrainSkew;
            case "rolling_hills" -> this.continents.rollingHills ? 1 : 0;
            case "jungle_pillars" -> this.continents.junglePillars ? 1 : 0;

            case "ocean_depth" -> this.oceans.oceanDepth;
            case "deep_ocean_depth" -> this.oceans.deepOceanDepth;

            case "depth_cutoff_start" -> this.caves.depthCutoffStart;
            case "depth_cutoff_size" -> this.caves.depthCutoffSize;
            case "cheese_enabled" -> this.caves.cheeseEnabled ? 1 : 0;
            case "cheese_additive" -> this.caves.cheeseAdditive;
            case "noodle_enabled" -> this.caves.noodleEnabled ? 1 : 0;
            case "noodle_additive" -> this.caves.noodleAdditive;

            default -> 0;
        };
    }

    public NoiseState getNoiseState(String option) {
        return switch (option) {
            case "continents" -> new NoiseState(this.continents.continentsScale, 1, 0);
            case "island" -> this.islands.noise.toMutable();
            case "erosion" -> new NoiseState(this.continents.erosionScale, 1, 0);
            case "ridge" -> new NoiseState(this.continents.ridgeScale, 1, 0);
            case "temperature" -> this.biomes.temperature.toMutable();
            case "vegetation" -> this.biomes.vegetation.toMutable();
            default -> throw new IllegalArgumentException("Unknown noise state option");
        };
    }

    public boolean test(String key) {
        return switch (key) {
            case "disable_islands" -> !this.islands.enabled && this.continents.oceanOffset < -0.49;
            case "increased_height" -> this.globalTerrain.heightLimits.maxY > 320;
            case "ultrasmooth" -> this.globalTerrain.ultrasmooth;
            case "remove_frozen_ocean_ice" -> this.oceans.removeFrozenOceanIce;
            case "river_lanterns" -> this.continents.riverLanterns;
            case "river_ice" -> this.continents.riverIce;
            case "no_carvers" -> !this.caves.carversEnabled;
            default -> false;
        };
    }

    public static final class General {
        public final boolean modEnabled;
        public final int snowStartOffset;

        private General(ConfigState.General state) {
            this.modEnabled = state.modEnabled;
            this.snowStartOffset = state.snowStartOffset;
        }
    }

    public static final class GlobalTerrain {
        public final double verticalScale;
        public final double elevationBoost;
        public final HeightLimits heightLimits;
        public final boolean lavaTunnels;
        public final boolean ultrasmooth;

        private GlobalTerrain(ConfigState.GlobalTerrain state) {
            this.verticalScale = state.verticalScale;
            this.elevationBoost = state.elevationBoost;
            this.heightLimits = state.heightLimits.copy();
            this.lavaTunnels = state.lavaTunnels;
            this.ultrasmooth = state.ultrasmooth;
        }
    }

    public static final class Continents {
        public final double oceanOffset;
        public final double continentsScale;
        public final double erosionScale;
        public final double ridgeScale;
        public final boolean undergroundRivers;
        public final boolean riverLanterns;
        public final boolean riverIce;
        public final double flatTerrainSkew;
        public final boolean rollingHills;
        public final boolean junglePillars;

        private Continents(ConfigState.Continents state) {
            this.oceanOffset = state.oceanOffset;
            this.continentsScale = state.continentsScale;
            this.erosionScale = state.erosionScale;
            this.ridgeScale = state.ridgeScale;
            this.undergroundRivers = state.undergroundRivers;
            this.riverLanterns = state.riverLanterns;
            this.riverIce = state.riverIce;
            this.flatTerrainSkew = state.flatTerrainSkew;
            this.rollingHills = state.rollingHills;
            this.junglePillars = state.junglePillars;
        }
    }

    public static final class Islands {
        public final boolean enabled;
        public final Noise noise;

        private Islands(ConfigState.Islands state) {
            this.enabled = state.enabled;
            this.noise = new Noise(state.noise);
        }
    }

    public static final class Oceans {
        public final double oceanDepth;
        public final double deepOceanDepth;
        public final int monumentOffset;
        public final boolean removeFrozenOceanIce;

        private Oceans(ConfigState.Oceans state) {
            this.oceanDepth = state.oceanDepth;
            this.deepOceanDepth = state.deepOceanDepth;
            this.monumentOffset = state.monumentOffset;
            this.removeFrozenOceanIce = state.removeFrozenOceanIce;
        }
    }

    public static final class Biomes {
        public final Noise temperature;
        public final Noise vegetation;

        private Biomes(ConfigState.Biomes state) {
            this.temperature = new Noise(state.temperature);
            this.vegetation = new Noise(state.vegetation);
        }
    }

    public static final class Caves {
        public final double depthCutoffStart;
        public final double depthCutoffSize;
        public final boolean cheeseEnabled;
        public final double cheeseAdditive;
        public final boolean noodleEnabled;
        public final double noodleAdditive;
        public final boolean spaghettiEnabled;
        public final boolean carversEnabled;

        private Caves(ConfigState.Caves state) {
            this.depthCutoffStart = state.depthCutoffStart;
            this.depthCutoffSize = state.depthCutoffSize;
            this.cheeseEnabled = state.cheeseEnabled;
            this.cheeseAdditive = state.cheeseAdditive;
            this.noodleEnabled = state.noodleEnabled;
            this.noodleAdditive = state.noodleAdditive;
            this.spaghettiEnabled = state.spaghettiEnabled;
            this.carversEnabled = state.carversEnabled;
        }
    }

    public static final class Noise {
        public final double scale;
        public final double multiplier;
        public final double offset;

        private Noise(NoiseState state) {
            this.scale = state.scale;
            this.multiplier = state.multiplier;
            this.offset = state.offset;
        }

        private NoiseState toMutable() {
            return new NoiseState(this.scale, this.multiplier, this.offset);
        }
    }
}
