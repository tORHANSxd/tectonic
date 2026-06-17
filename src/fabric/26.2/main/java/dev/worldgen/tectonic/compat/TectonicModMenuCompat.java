package dev.worldgen.tectonic.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.worldgen.apollib.client.gui.ApollibConfigScreen;
import dev.worldgen.tectonic.Tectonic;
import dev.worldgen.tectonic.client.ConfigScreenBuilder;
import dev.worldgen.tectonic.client.gui.PresetSelectorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;

public class TectonicModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ApollibConfigScreen<>(Tectonic.MOD_ID, parent, Tectonic.CONFIG, helper -> {
            helper.addBig(Button.builder(
                helper.text("view_presets"),
                _ -> Minecraft.getInstance().gui.setScreen(new PresetSelectorScreen(helper.screen()))
            ).width(310).build());
            helper.spacer();
            ConfigScreenBuilder.build(helper);
        });
    }
}
