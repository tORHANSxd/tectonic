package dev.worldgen.tectonic;

import dev.worldgen.lithostitched.registry.LithostitchedBuiltInRegistries;
import dev.worldgen.tectonic.command.TectonicCommand;
import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.lithostitched.SetHeightLimitsModifier;
import dev.worldgen.tectonic.worldgen.densityfunction.fnl.FastNoiseConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import static dev.worldgen.tectonic.Tectonic.id;

public class TectonicFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Tectonic.init(FabricLoader.getInstance().getConfigDir());

        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> TectonicCommand.register(dispatcher));

        ResourceConditions.register(ConfigResourceCondition.TYPE);

        DynamicRegistries.register(TectonicRegistries.FAST_NOISE_CONFIG, FastNoiseConfig.CODEC);
        var registry = FabricRegistryBuilder.createSimple(TectonicRegistries.FAST_NOISE_CONFIG_TYPE).buildAndRegister();
        Tectonic.registerDensityFunctionTypes((name, codec) -> Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id(name), codec));
        Tectonic.registerFastNoiseConfigTypes((name, codec) -> Registry.register(registry, id(name), codec));
        Registry.register(LithostitchedBuiltInRegistries.MODIFIER_TYPE, id("set_height_limits"), SetHeightLimitsModifier.CODEC);

        ServerLifecycleEvents.SERVER_STARTED.register(Tectonic::onServerStarting);

        if (ConfigHandler.getState().general.modEnabled) {
            ResourceManagerHelper.registerBuiltinResourcePack(
                id("tectonic"),
                FabricLoader.getInstance().getModContainer("tectonic").get(),
                Component.literal("Tectonic"),
                ResourcePackActivationType.ALWAYS_ENABLED
            );
        }
    }
}