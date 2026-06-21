package dev.worldgen.tectonic.platform.neoforge;

//? if neoforge {
import dev.worldgen.tectonic.Tectonic;
import dev.worldgen.tectonic.command.TectonicCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(Tectonic.MOD_ID)
public class TectonicNeoforge {
    public TectonicNeoforge(IEventBus bus) {
        Tectonic.init();
        
        Tectonic.REGISTRAR.register(NeoForgeRegistries.CONDITION_SERIALIZERS, "config", ConfigResourceCondition.CODEC);

        bus.addListener(this::registerEnabledPacks);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerEnabledPacks(final AddPackFindersEvent event) {
        if (Tectonic.CONFIG.getState().enabled()) {
            event.addPackFinders(
                Tectonic.id("resourcepacks/tectonic"),
                PackType.SERVER_DATA,
                Component.literal("Tectonic"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP
            );
        }
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        TectonicCommand.register(event.getDispatcher());
    }
}
//? }