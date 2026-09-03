package dev.worldgen.tectonic.metadata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeModMetadataTest {
    private static final Pattern DEPENDENCY_BLOCK = Pattern.compile(
        "(?ms)^\\[\\[dependencies\\.tectonic]]\\R(?<body>.*?)(?=^\\[\\[|\\z)"
    );

    @Test
    void releaseJarRequiresCompatibleLithostitched() throws IOException {
        String jarProperty = System.getProperty("tectonic.forge1201.jar");
        assertNotNull(jarProperty, "Gradle must provide the remapped Forge 1.20.1 JAR path");
        Path jarPath = Path.of(jarProperty);
        assertTrue(Files.isRegularFile(jarPath), jarPath.toString());

        try (ZipFile jar = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = jar.getEntry("META-INF/mods.toml");
            assertNotNull(entry, "Release JAR must contain META-INF/mods.toml");
            String metadata = new String(jar.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            String lithostitched = findLithostitchedBlock(metadata);

            assertTrue(lithostitched.contains("mandatory = true"), lithostitched);
            assertTrue(lithostitched.contains("type = \"required\""), lithostitched);
            assertTrue(lithostitched.contains("versionRange = \"[1.4.11,)\""), lithostitched);
        }
    }

    private static String findLithostitchedBlock(String metadata) {
        Matcher blocks = DEPENDENCY_BLOCK.matcher(metadata);
        while (blocks.find()) {
            String body = blocks.group("body");
            if (body.contains("modId = \"lithostitched\"")) {
                return body;
            }
        }
        throw new AssertionError("Release JAR must declare the Lithostitched dependency");
    }
}
