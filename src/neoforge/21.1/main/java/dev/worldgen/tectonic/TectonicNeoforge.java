package dev.worldgen.tectonic;

import dev.worldgen.lithostitched.api.registry.LithostitchedRegistries;
import dev.worldgen.tectonic.command.TectonicCommand;
import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.lithostitched.ConfigLoadPredicate;
import dev.worldgen.tectonic.lithostitched.SetHeightLimitsModifier;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigClamp;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigConstant;
import dev.worldgen.tectonic.worldgen.densityfunction.ConfigNoise;
import dev.worldgen.tectonic.worldgen.densityfunction.Invert;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.nio.file.Path;
import java.util.Optional;

import static dev.worldgen.tectonic.Tectonic.id;

@Mod(Tectonic.MOD_ID)
public class TectonicNeoforge {
    public TectonicNeoforge(IEventBus bus) {
        Tectonic.init(FMLPaths.GAMEDIR.get());

        bus.addListener(this::registerAllTheThings);
        bus.addListener(this::registerEnabledPacks);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerAllTheThings(final RegisterEvent event) {
        event.register(Registries.DENSITY_FUNCTION_TYPE, helper -> {
            helper.register(id("config_clamp"), ConfigClamp.CODEC_HOLDER.codec());
            helper.register(id("config_constant"), ConfigConstant.CODEC_HOLDER.codec());
            helper.register(id("config_noise"), ConfigNoise.CODEC_HOLDER.codec());
            helper.register(id("invert"), Invert.CODEC_HOLDER.codec());
        });
        event.register(LithostitchedRegistries.MODIFIER_TYPE, helper -> {
            helper.register(id("set_height_limits"), SetHeightLimitsModifier.CODEC);
        });
        event.register(LithostitchedRegistries.LOAD_PREDICATE_TYPE, helper -> {
            helper.register(id("config"), ConfigLoadPredicate.CODEC);
        });
        event.register(NeoForgeRegistries.CONDITION_SERIALIZERS.key(), helper -> {
            helper.register(id("config"), ConfigResourceCondition.CODEC);
        });
    }

    private void registerEnabledPacks(final AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA && ConfigHandler.getState().general.modEnabled) {
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
        }
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        TectonicCommand.register(event.getDispatcher());
    }
}
