package dev.worldgen.tectonic.metadata;

import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeModMetadataTest {
    private static final Pattern DEPENDENCY_BLOCK = Pattern.compile(
        "(?ms)^\\[\\[dependencies\\.tectonic]]\\R(?<body>.*?)(?=^\\[\\[|\\z)"
    );

    @Test
    void releaseJarUsesCommunityIdentity() throws IOException {
        try (ZipFile jar = openReleaseJar()) {
            String metadata = readTextEntry(jar, "META-INF/mods.toml");

            assertTrue(metadata.contains("license = \"MIT\""), metadata);
            assertTrue(metadata.contains("version = \"3.0.17-backport.1\""), metadata);
            assertTrue(metadata.contains(
                "displayName = \"Tectonic 1.20.1 Forge - Unofficial Community Backport\""
            ), metadata);
            assertTrue(metadata.contains("Unofficial community backport"), metadata);
            assertTrue(metadata.contains("displayURL = \"https://github.com/tORHANSxd/tectonic\""), metadata);
            assertTrue(metadata.contains(
                "issueTrackerURL = \"https://github.com/tORHANSxd/tectonic/issues\""
            ), metadata);
            assertTrue(metadata.contains("authors = \"Apollo\""), metadata);
            assertTrue(metadata.contains("HB Stratos"), metadata);
            assertTrue(metadata.contains("DawnKiro"), metadata);
            assertTrue(metadata.contains("Uni"), metadata);
            assertTrue(metadata.contains("tORHANS (community backport maintainer)"), metadata);
        }
    }

    @Test
    void releaseJarRequiresCompatibleRuntime() throws IOException {
        try (ZipFile jar = openReleaseJar()) {
            String metadata = readTextEntry(jar, "META-INF/mods.toml");
            String lithostitched = findDependencyBlock(metadata, "lithostitched");
            String minecraft = findDependencyBlock(metadata, "minecraft");

            assertTrue(lithostitched.contains("mandatory = true"), lithostitched);
            assertTrue(lithostitched.contains("type = \"required\""), lithostitched);
            assertTrue(lithostitched.contains("versionRange = \"[1.4.11,)\""), lithostitched);
            assertTrue(minecraft.contains("mandatory = true"), minecraft);
            assertTrue(minecraft.contains("type = \"required\""), minecraft);
            assertTrue(minecraft.contains("versionRange = \"[1.20.1,1.20.2)\""), minecraft);
        }
    }

    @Test
    void releaseJarEmbedsLegalAndProvenanceMetadata() throws IOException {
        try (ZipFile jar = openReleaseJar()) {
            assertTrue(readTextEntry(jar, "META-INF/LICENSE").startsWith("MIT License"));
            assertTrue(readTextEntry(jar, "META-INF/NOTICE.md").contains("unofficial community backport"));

            Manifest manifest = new Manifest(jar.getInputStream(requiredEntry(jar, "META-INF/MANIFEST.MF")));
            Attributes attributes = manifest.getMainAttributes();
            assertEquals("3.0.17-backport.1", attributes.getValue("Implementation-Version"));
            assertEquals("true", attributes.getValue("Community-Build"));
            assertEquals("false", attributes.getValue("Official-Build"));
            assertEquals("5373b2084e461f83bd6e0b5f2fe943e81bd59700", attributes.getValue("Upstream-Baseline"));
            assertEquals("65", attributes.getValue("Java-Class-Major"));
            assertTrue(attributes.getValue("Git-Commit").matches("[0-9a-f]{40}"));
            assertTrue(attributes.getValue("Git-Dirty").matches("true|false"));
        }
    }

    @Test
    void everyTectonicClassTargetsJava21() throws IOException {
        int classCount = 0;
        try (ZipFile jar = openReleaseJar()) {
            Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("dev/worldgen/tectonic/") || !entry.getName().endsWith(".class")) {
                    continue;
                }

                classCount++;
                try (DataInputStream input = new DataInputStream(jar.getInputStream(entry))) {
                    assertEquals(0xCAFEBABE, input.readInt(), entry.getName());
                    input.readUnsignedShort();
                    assertEquals(65, input.readUnsignedShort(), entry.getName());
                }
            }
        }
        assertTrue(classCount > 0, "Release JAR must contain Tectonic classes");
    }

    private static ZipFile openReleaseJar() throws IOException {
        String jarProperty = System.getProperty("tectonic.forge1201.jar");
        assertNotNull(jarProperty, "Gradle must provide the final Forge 1.20.1 JAR path");
        Path jarPath = Path.of(jarProperty);
        assertTrue(Files.isRegularFile(jarPath), jarPath.toString());
        return new ZipFile(jarPath.toFile());
    }

    private static String readTextEntry(ZipFile jar, String name) throws IOException {
        return new String(jar.getInputStream(requiredEntry(jar, name)).readAllBytes(), StandardCharsets.UTF_8);
    }

    private static ZipEntry requiredEntry(ZipFile jar, String name) {
        ZipEntry entry = jar.getEntry(name);
        assertNotNull(entry, "Release JAR must contain " + name);
        return entry;
    }

    private static String findDependencyBlock(String metadata, String modId) {
        Matcher blocks = DEPENDENCY_BLOCK.matcher(metadata);
        while (blocks.find()) {
            String body = blocks.group("body");
            if (body.contains("modId = \"" + modId + "\"")) {
                return body;
            }
        }
        throw new AssertionError("Release JAR must declare the " + modId + " dependency");
    }
}
