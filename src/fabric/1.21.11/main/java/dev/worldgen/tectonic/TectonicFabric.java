package dev.worldgen.tectonic;

import dev.worldgen.lithostitched.api.registry.LithostitchedBuiltInRegistries;
import dev.worldgen.tectonic.command.TectonicCommand;
import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.lithostitched.ConfigLoadPredicate;
import dev.worldgen.tectonic.lithostitched.SetHeightLimitsModifier;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import static dev.worldgen.tectonic.Tectonic.id;

public class TectonicFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Tectonic.init(FabricLoader.getInstance().getGameDir());

        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> TectonicCommand.register(dispatcher));

        Tectonic.registerDensityFunctionTypes((name, codec) -> Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id(name), codec));
        Registry.register(LithostitchedBuiltInRegistries.MODIFIER_TYPE, id("set_height_limits"), SetHeightLimitsModifier.CODEC);
        Registry.register(LithostitchedBuiltInRegistries.LOAD_PREDICATE_TYPE, id("config"), ConfigLoadPredicate.CODEC);

        if (ConfigHandler.getState().general.modEnabled) {
            ResourceLoader.registerBuiltinPack(
                id("tectonic"),
                FabricLoader.getInstance().getModContainer("tectonic").get(),
                Component.literal("Tectonic"),
                PackActivationType.ALWAYS_ENABLED
            );
        }
    }
}