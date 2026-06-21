package dev.worldgen.tectonic.platform.neoforge;

//? if neoforge {
import dev.worldgen.tectonic.Tectonic;
import dev.worldgen.tectonic.command.TectonicCommand;
import dev.worldgen.tectonic.registry.TectonicRegistrations;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.nio.file.Path;
import java.util.Optional;

@Mod(Tectonic.MOD_ID)
public class TectonicNeoforge {
    public TectonicNeoforge(IEventBus bus) {
        TectonicRegistrations.register(NeoForgeRegistries.CONDITION_SERIALIZERS, "config", ConfigResourceCondition.CODEC);
        Tectonic.init();
        
        bus.addListener(this::registerEnabledPacks);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerEnabledPacks(final AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA && Tectonic.CONFIG.getState().enabled()) {
            //? if < 26.1 {
            Path resourcePath = ModList.get().getModFileById("tectonic").getFile().findResource("resourcepacks/tectonic");
            
            Pack dataPack = Pack.readMetaAndCreate(
                new PackLocationInfo(
                    resourcePath.getFileName().toString(),
                    Component.literal("Tectonic"),
                    PackSource.BUILT_IN,
                    Optional.empty()
                ),
                new PathPackResources.PathResourcesSupplier(resourcePath),
                PackType.SERVER_DATA,
                new PackSelectionConfig(
                    true,
                    Pack.Position.TOP,
                    false
                )
            );
            
            event.addRepositorySource((packConsumer) -> packConsumer.accept(dataPack));
            //? } else {
            /*event.addPackFinders(
                Tectonic.id("resourcepacks/tectonic"),
                PackType.SERVER_DATA,
                Component.literal("Tectonic"),
                PackSource.BUILT_IN,
                true,
                Pack.Position.TOP
            );
            *///? }
        }
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        TectonicCommand.register(event.getDispatcher());
    }
}
//? }