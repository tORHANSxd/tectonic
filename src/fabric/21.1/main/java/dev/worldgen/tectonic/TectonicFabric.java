Resoupackage dev.worldgen.tectonic;

import dev.worldgen.lithostitched.api.registry.LithostitchedBuiltInRegistries;
import dev.worldgen.tectonic.command.TectonicCommand;
import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.lithostitched.ConfigLoadPredicate;
import dev.worldgen.tectonic.lithostitched.SetHeightLimitsModifier;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
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
        
        ResourceConditions.register(ConfigResourceCondition.TYPE);

        Tectonic.registerDensityFunctionTypes((name, codec) -> Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, id(name), codec));
        Tectonic.registerPlacementModifierTypes((name, type) -> Registry.register(BuiltInRegistries.PLACEMENT_MODIFIER_TYPE, id(name), type));
        Registry.register(LithostitchedBuiltInRegistries.MODIFIER_TYPE, id("set_height_limits"), SetHeightLimitsModifier.CODEC);
        Registry.register(LithostitchedBuiltInRegistries.LOAD_PREDICATE_TYPE, id("config"), ConfigLoadPredicate.CODEC);

        if (Tectonic.CONFIG.getState().general.modEnabled) {
            ResourceManagerHelper.registerBuiltinResourcePack(
                id("tectonic"),
                FabricLoader.getInstance().getModContainer("tectonic").get(),
                Component.literal("Tectonic"),
                ResourcePackActivationType.ALWAYS_ENABLED
            );
        }
    }
}