package dev.worldgen.tectonic;

import com.mojang.serialization.MapCodec;
import dev.worldgen.tectonic.config.ConfigHandler;
import dev.worldgen.tectonic.worldgen.densityfunction.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.msrandom.multiplatform.annotations.Expect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.function.BiConsumer;

public class Tectonic {
    public static final String MOD_ID = "tectonic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    /**
     * Value saved in chunks used for blending between Tectonic versions.
     * <ol>
     *     <li>3.0.0 - 3.0.8, 3.0.10+</li>
     *     <li>3.0.9</li>
     * </ol>
     */
    public static int BLENDING_VERSION = 1;
    public static String BLENDING_KEY = "tectonic:blending_version";
    public static Path FOLDER;

    public static void init(Path folder) {
        FOLDER = folder;
        ConfigHandler.load(folder.resolve("config").resolve("tectonic.json"));
    }

    public static Identifier idVanilla(String name) {
        return Identifier.withDefaultNamespace(name);
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    @Expect
    public static int getBlendingVersion(CompoundTag tag);

    @Expect
    public static boolean canRunCommand(CommandSourceStack stack);

    public static void registerDensityFunctionTypes(BiConsumer<String, MapCodec<? extends DensityFunction>> consumer) {
        consumer.accept("config_clamp", ConfigClamp.DATA_CODEC);
        consumer.accept("config_constant", ConfigConstant.DATA_CODEC);
        consumer.accept("config_noise", ConfigNoise.DATA_CODEC);
        consumer.accept("invert", Invert.DATA_CODEC);
    }

    public static boolean isEnabled() {
        return ConfigHandler.getState().general.modEnabled;
    }

}
