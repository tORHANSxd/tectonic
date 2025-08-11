package dev.worldgen.tectonic.client.gui;

import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.config.state.ConfigState;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import static dev.worldgen.tectonic.config.state.ConfigState.Continents.*;
import static dev.worldgen.tectonic.config.state.ConfigState.General.MOD_ENABLED;
import static dev.worldgen.tectonic.config.state.ConfigState.General.SNOW_START_OFFSET;
import static dev.worldgen.tectonic.config.state.ConfigState.GlobalTerrain.*;
import static dev.worldgen.tectonic.config.state.ConfigState.Islands.ENABLED;
import static dev.worldgen.tectonic.config.state.ConfigState.Islands.NOISE;
import static dev.worldgen.tectonic.config.state.ConfigState.Oceans.*;

public class ConfigScreen extends Screen {
    protected final Screen parent;

    private ConfigList list;
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    public ConfigScreen(Screen parent) {
        super(text("title"));
        this.parent = parent;
    }

    @Override
    public void init() {
        ConfigState state = ConfigHandler.getState();

        layout.addTitleHeader(title, font);

        list = layout.addToContents(new ConfigList(minecraft, width, this));
        list.addEntry(Button.builder(ConfigScreen.text("view_presets"), button -> minecraft.setScreen(new PresetSelectorScreen(this))).build());

        list.addCategory("general", font);
        list.addBoolean("mod_enabled", bool -> state.general.modEnabled = bool, state.general.modEnabled, MOD_ENABLED);
        list.addInteger("snow_start_offset", 0, 256, value -> state.general.snowStartOffset = value, state.general.snowStartOffset, SNOW_START_OFFSET);

        list.addCategory("global_terrain", font);
        list.addDouble("vertical_scale", 0.75, 4, 0.005, value -> state.globalTerrain.verticalScale = value, state.globalTerrain.verticalScale, VERTICAL_SCALE);
        list.addBoolean("increased_height", bool -> state.globalTerrain.increasedHeight = bool, state.globalTerrain.increasedHeight, INCREASED_HEIGHT);
        list.addBoolean("lava_tunnels", bool -> state.globalTerrain.lavaTunnels = bool, state.globalTerrain.lavaTunnels, LAVA_TUNNELS);
        list.addBoolean("ultrasmooth", bool -> state.globalTerrain.ultrasmooth = bool, state.globalTerrain.ultrasmooth, ULTRASMOOTH);

        list.addCategory("continents", font);
        list.addDouble("ocean_offset", -2, 0, 0.01, value -> state.continents.oceanOffset = value, state.continents.oceanOffset, OCEAN_OFFSET);
        list.addDouble("continents_scale", 0.01, 1, 0.01, value -> state.continents.continentsScale = value, state.continents.continentsScale, CONTINENTS_SCALE);
        list.addDouble("erosion_scale", 0.01, 1, 0.01, value -> state.continents.erosionScale = value, state.continents.erosionScale, EROSION_SCALE);
        list.addDouble("ridge_scale", 0.01, 2, 0.01, value -> state.continents.ridgeScale = value, state.continents.ridgeScale, RIDGE_SCALE);
        list.addBoolean("underground_rivers", bool -> state.continents.undergroundRivers = bool, state.continents.undergroundRivers, UNDERGROUND_RIVERS);
        list.addBoolean("river_lanterns", bool -> state.continents.riverLanterns = bool, state.continents.riverLanterns, RIVER_LANTERNS);
        list.addDouble("flat_terrain_skew", -1, 1, 0.01, value -> state.continents.flatTerrainSkew = value, state.continents.flatTerrainSkew, FLAT_TERRAIN_SKEW);
        list.addBoolean("rolling_hills", bool -> state.continents.rollingHills = bool, state.continents.rollingHills, ROLLING_HILLS);
        list.addBoolean("jungle_pillars", bool -> state.continents.junglePillars = bool, state.continents.junglePillars, JUNGLE_PILLARS);

        list.addCategory("islands", font);
        list.addBoolean("enabled", bool -> state.islands.enabled = bool,  state.islands.enabled, ENABLED);
        list.addNoise("noise", state.islands.noise);

        list.addCategory("oceans", font);
        list.addDouble("ocean_depth", -0.75, -0.05, 0.01, value -> state.oceans.oceanDepth = value, state.oceans.oceanDepth, OCEAN_DEPTH);
        list.addDouble("deep_ocean_depth", -0.75, -0.05, 0.01, value -> state.oceans.deepOceanDepth = value, state.oceans.deepOceanDepth, DEEP_OCEAN_DEPTH);
        list.addInteger("monument_offset", -60, 0, value -> state.oceans.monumentOffset = value, state.oceans.monumentOffset, MONUMENT_OFFSET);
        list.addBoolean("remove_frozen_ocean_ice", bool -> state.oceans.removeFrozenOceanIce = bool, state.oceans.removeFrozenOceanIce, REMOVE_FROZEN_OCEAN_ICE);

        list.addCategory("biomes", font);
        list.addNoise("temperature", state.biomes.temperature);
        list.addNoise("vegetation", state.biomes.vegetation);


        LinearLayout footer = layout.addToFooter(LinearLayout.horizontal().spacing(8));

        footer.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onDone()).build());

        layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    private void onDone() {
        ConfigHandler.save();
        this.onClose();
    }

    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    public static Component text(String name) {
        return Component.translatable("config.tectonic." + name);
    }

    public static Component option(String name) {
        return text("option." + name);
    }
}