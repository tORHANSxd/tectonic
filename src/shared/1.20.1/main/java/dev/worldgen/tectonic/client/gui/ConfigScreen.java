package dev.worldgen.tectonic.client.gui;

import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.config.state.ConfigState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    protected final Screen parent;
    private ConfigState workingState;

    private ConfigList list;

    public ConfigScreen(Screen parent) {
        this(parent, ConfigHandler.copyConfig());
    }

    ConfigScreen(Screen parent, ConfigState workingState) {
        super(text("title"));
        this.parent = parent;
        this.workingState = workingState.copy();
    }

    @Override
    public void init() {
        list = new ConfigList(minecraft, this);
        list.addEntry(Button.builder(ConfigScreen.text("view_presets"), button -> minecraft.setScreen(new PresetSelectorScreen(this))).build());
        list.build(font, workingState);

        this.addWidget(list);

        this.addRenderableWidget(Button.builder(
            CommonComponents.GUI_DONE,
            button -> this.onDone()
        ).pos(width / 2 - 100, height - 28).size(200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        list.render(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);

        guiGraphics.drawCenteredString(this.font, title, width / 2, 12, 0xffffff);
    }

    @Override
    public void renderBackground(GuiGraphics context) {
        this.renderDirtBackground(context);
    }

    private void onDone() {
        if (minecraft.level != null) {
            minecraft.setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    ConfigHandler.save(workingState, false);
                    this.onClose();
                } else {
                    minecraft.setScreen(this);
                }
            }, text("restart_required.title"), text("restart_required.message")));
            return;
        }

        ConfigHandler.save(workingState, true);
        this.onClose();
    }

    void applyPreset(ConfigState state) {
        this.workingState = state.copy();
        this.minecraft.setScreen(this);
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
