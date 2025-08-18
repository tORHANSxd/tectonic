package dev.worldgen.tectonic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.worldgen.lithostitched.mixin.common.RandomStateAccessor;
import dev.worldgen.lithostitched.worldgen.NoiseWiringHelper;
import dev.worldgen.tectonic.Tectonic;
import dev.worldgen.tectonic.config.ConfigHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

import static net.minecraft.commands.Commands.*;

public class TectonicCommand {
    private static final ResourceKey<DensityFunction> EROSION = key("noise/continent/erosion");
    private static final ResourceKey<DensityFunction> REGION_SELECTOR = key("noise/region_selector");
    private static final ResourceKey<DensityFunction> DEPTH_CUTOFF = key("__constants/cave/depth_cutoff");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("tectonic").requires(stack -> stack.hasPermission(2)).executes(TectonicCommand::run)
        );
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = source.getPlayerOrException().blockPosition();

        if (!ConfigHandler.getState().general.modEnabled) {
            failure(source, "Tectonic is not currently enabled.");
            return 0;
        }

        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator)) {
            failure(source, "Tectonic generation is not present here.");
            return 0;
        }

        message(source, Component.literal("Tectonic debug info:"));

        NoiseGeneratorSettings settings = generator.generatorSettings().value();
        RandomState randomState = RandomState.create(settings, level.registryAccess().lookupOrThrow(Registries.NOISE), level.getSeed());
        NoiseWiringHelper helper = new NoiseWiringHelper(
            level.getSeed(),
            settings.useLegacyRandomSource(),
            randomState,
            ((RandomStateAccessor)(Object)randomState).getRandom()
        );
        HolderLookup.RegistryLookup<DensityFunction> registry = level.registryAccess().lookupOrThrow(Registries.DENSITY_FUNCTION);

        message(source, getRegion(
            get(registry.getOrThrow(EROSION), helper, pos),
            get(registry.getOrThrow(REGION_SELECTOR), helper, pos)
        ));

        message(source, Component.translatableWithFallback(
            "command.tectonic.depth_cutoff",
            "Depth cutoff: %s",
            get(registry.getOrThrow(DEPTH_CUTOFF), helper, pos)
        ));


        return 1;
    }

    private static void message(CommandSourceStack source, Component message) {
        source.sendSystemMessage(message);
    }

    private static void failure(CommandSourceStack source, String message) {
        source.sendFailure(Component.literal(message));
    }

    private static double get(Holder<DensityFunction> holder, NoiseWiringHelper helper, BlockPos pos) {
        double d = holder.value().mapAll(helper).compute(new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ()));
        return (double) Math.round(d * 1000) / 1000;
    }

    private static Component getRegion(double erosion, double regionSelector) {
        String iconText;
        String nameText;
        if (erosion < 0) {
            if (regionSelector < 0) {
                iconText = "♣";
                nameText = "Club";
            } else {
                iconText = "♥";
                nameText = "Heart";
            }
        } else {
            if (regionSelector < 0) {
                iconText = "♠";
                nameText = "Spade";
            } else {
                iconText = "♦";
                nameText = "Diamond";
            }
        }
        MutableComponent icon = Component.literal(iconText);
        icon.withStyle(regionSelector < 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.RED);

        MutableComponent name = Component.literal(nameText);

        return Component.translatableWithFallback("command.tectonic.region", "Region: %s %s (Erosion %s / Region Selector %s)", icon, name, erosion, regionSelector);
    }

    private static double round(double erosion) {
        return (double) Math.round(erosion * 1000) / 1000;
    }

    private static ResourceKey<DensityFunction> key(String name) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, Tectonic.id(name));
    }
}
