package dev.worldgen.tectonic.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkSerializerClientCompatibilityTest {
    @Test
    void minecraftAndMixinKeepTheServerOnlyReadContract() throws NoSuchMethodException {
        Method target = Arrays.stream(ChunkSerializer.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("read"))
            .filter(method -> method.getParameterCount() == 4)
            .findFirst()
            .orElseThrow();
        Method mixin = ChunkSerializerMixin.class.getDeclaredMethod(
            "tectonic$read",
            ServerLevel.class,
            PoiManager.class,
            ChunkPos.class,
            CompoundTag.class,
            CallbackInfoReturnable.class
        );

        assertEquals(ServerLevel.class, target.getParameterTypes()[0]);
        assertEquals(ServerLevel.class, mixin.getParameterTypes()[0]);
        assertEquals(ProtoChunk.class, target.getReturnType());
    }

    @Test
    void forge1201JarContainsOnlyTheApplicableChunkMixin() throws IOException {
        String jarProperty = System.getProperty("tectonic.forge1201.jar");
        assertNotNull(jarProperty, "Gradle must provide the final Forge 1.20.1 JAR path");

        try (ZipFile jar = new ZipFile(Path.of(jarProperty).toFile())) {
            assertNotNull(jar.getEntry("dev/worldgen/tectonic/mixin/ChunkSerializerMixin.class"));
            assertFalse(jar.stream().anyMatch(entry -> entry.getName().endsWith("SerializableChunkDataMixin.class")));

            ZipEntry mixinConfig = jar.getEntry("tectonic_1.20.1.mixins.json");
            assertNotNull(mixinConfig);
            String json = new String(jar.getInputStream(mixinConfig).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(json.contains("\"ChunkSerializerMixin\""), json);
            assertFalse(json.contains("SerializableChunkDataMixin"), json);
        }
    }
}
