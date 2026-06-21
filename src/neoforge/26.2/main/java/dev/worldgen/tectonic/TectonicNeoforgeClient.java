package dev.worldgen.tectonic;

import dev.worldgen.apollib.client.gui.ApollibConfigScreen;
import dev.worldgen.tectonic.client.ConfigScreenBuilder;
import dev.worldgen.tectonic.client.gui.PresetSelectorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Tectonic.MOD_ID, dist = Dist.CLIENT)
public class TectonicNeoforgeClient {
    public TectonicNeoforgeClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) ->
            new ApollibConfigScreen<>(Tectonic.MOD_ID, parent, Tectonic.CONFIG, helper -> {
                helper.addBig(Button.builder(
                    helper.text("view_presets"),
                    _ -> Minecraft.getInstance().gui.setScreen(new PresetSelectorScreen(helper.screen()))
                ).width(310).build());
                helper.spacer();
                ConfigScreenBuilder.build(helper);
            })
        );
    }
}