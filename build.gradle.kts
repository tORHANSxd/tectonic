import net.msrandom.minecraftcodev.remapper.task.LoadMappings
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.security.MessageDigest

plugins {
    kotlin("jvm") version "2.1.21"
    id("earth.terrarium.cloche") version "0.13.6"
}

repositories {
    cloche {
        mavenNeoforgedMeta()
        mavenNeoforged()
        mavenForge()
        mavenFabric()
        mavenParchment()
        librariesMinecraft()
        main()
    }
    mavenCentral()
    maven("https://api.modrinth.com/maven")
    maven("https://maven.terraformersmc.com/")
}

group = "dev.worldgen.tectonic"
val communityVersion = "3.0.17-backport.1"
version = communityVersion

cloche {
    mappings {
        official()
    }

    metadata {
        modId = "tectonic"
        name = "Tectonic 1.20.1 Forge - Unofficial Community Backport"
        description = "Unofficial community backport of Tectonic for Minecraft 1.20.1 Forge."
        license = "MIT"
        icon = "pack.png"

        url = "https://github.com/tORHANSxd/tectonic"
        issues = "https://github.com/tORHANSxd/tectonic/issues"
        sources = "https://github.com/tORHANSxd/tectonic"

        author("Apollo")
        contributor("HB Stratos")
        contributor("DawnKiro")
        contributor("Uni")
        contributor("tORHANS (community backport maintainer)")
    }

    common {
        mixins.from(file("src/common/main/tectonic.mixins.json"))

        dependencies {
            compileOnly("org.spongepowered:mixin:0.8.3")
        }

        metadata {
            dependencies {
                dependency {
                    modId = "lithostitched"
                    required = true
                    version("1.4.11")
                }
            }
        }
    }

    val shared1201 = common("shared:1.20.1") {
        mixins.from(file("src/shared/1.20.1/main/tectonic_1.20.1.mixins.json"))
    }
    val shared1211 = common("shared:1.21.1") {
        mixins.from(file("src/shared/1.21.1/main/tectonic_1.21.1.mixins.json"))
    }
    val shared12110 = common("shared:1.21.10") {
        mixins.from(file("src/shared/1.21.10/main/tectonic_1.21.10.mixins.json"))
    }

    fabric("fabric:1.20.1") {
        dependsOn(shared1201)

        loaderVersion = "0.16.13"
        minecraftVersion = "1.20.1"

        dependencies {
            fabricApi("0.92.6")
            modImplementation("maven.modrinth:lithostitched:1.4.11-fabric-1.20")
            modImplementation("com.terraformersmc:modmenu:7.2.2")
        }

        includedClient()
        runs {
            client()
            server()
        }

        metadata {
            entrypoint("main") {
                value = "dev.worldgen.tectonic.TectonicFabric"
            }
            entrypoint("modmenu") {
                value = "dev.worldgen.tectonic.compat.TectonicModMenuCompat"
            }
        }
    }

    fabric("fabric:1.21.1") {
        dependsOn(shared1211)

        loaderVersion = "0.17.3"
        minecraftVersion = "1.21.1"

        dependencies {
            fabricApi("0.116.1")
            modImplementation("maven.modrinth:lithostitched:1.5.0-fabric-1.21.1")
            modImplementation("com.terraformersmc:modmenu:11.0.3")
        }

        includedClient()
        runs {
            client()
            server()
        }

        metadata {
            entrypoint("main") {
                value = "dev.worldgen.tectonic.TectonicFabric"
            }
            entrypoint("modmenu") {
                value = "dev.worldgen.tectonic.compat.TectonicModMenuCompat"
            }
        }
    }

    fabric("fabric:1.21.10") {
        dependsOn(shared12110)

        loaderVersion = "0.17.3"
        minecraftVersion = "1.21.10"

        dependencies {
            fabricApi("0.135.0", "1.21.10")
            modImplementation("maven.modrinth:lithostitched:1.5.1-fabric-1.21.9")
            modImplementation("com.terraformersmc:modmenu:16.0.0-rc.1")
        } //accessWidenFabric1218CommonMinecraft

        includedClient()
        runs {
            client()
            server()
        }

        metadata {
            entrypoint("main") {
                value = "dev.worldgen.tectonic.TectonicFabric"
            }
            entrypoint("modmenu") {
                value = "dev.worldgen.tectonic.compat.TectonicModMenuCompat"
            }
        }
    }

    forge("forge:1.20.1") {
        dependsOn(shared1201)

        loaderVersion = "47.4.0"
        minecraftVersion = "1.20.1"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.4.11-forge-1.20")
        }

        metadata {
            dependency {
                modId = "minecraft"
                required = true
                version {
                    start = "1.20.1"
                    end = "1.20.2"
                    startInclusive = true
                    endExclusive = true
                }
            }
        }

        runs {
            client()
            server()
        }
    }

    neoforge("neoforge:1.21.1") {
        dependsOn(shared1211)

        loaderVersion = "21.1.209"
        minecraftVersion = "1.21.1"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.5.0-neoforge-1.21.1")
        }

        runs {
            client()
            server()
        }
    }

    neoforge("neoforge:1.21.10") {
        dependsOn(shared12110)

        loaderVersion = "21.10.12-beta"
        minecraftVersion = "1.21.10"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.5.1-neoforge-1.21.9")
        }

        runs {
            client()
            server()
        }
    }
}

val forge1201Main = sourceSets.named("forge1201")
val forge1201FinalJar = tasks.named<Jar>("forge1201IncludeJar")
val forge1201UnitTest = sourceSets.create("forge1201UnitTest") {
    java.srcDir("src/forge/1.20.1/test/java")
    resources.srcDir("src/forge/1.20.1/test/resources")
    compileClasspath += forge1201Main.get().output + forge1201Main.get().compileClasspath
    runtimeClasspath += forge1201Main.get().output + forge1201Main.get().runtimeClasspath
}

dependencies {
    add(forge1201UnitTest.implementationConfigurationName, "org.junit.jupiter:junit-jupiter:5.11.4")
    add(forge1201UnitTest.runtimeOnlyConfigurationName, "org.junit.platform:junit-platform-launcher:1.11.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

val java21Launcher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<LoadMappings>().configureEach {
    javaExecutable.set(java21Launcher.map { it.executablePath })
}

tasks.withType<JavaExec>().matching { it.name.startsWith("run") }.configureEach {
    javaLauncher.set(java21Launcher)
    standardInput = System.`in`
}

tasks.withType<Test>().configureEach {
    javaLauncher.set(java21Launcher)
    useJUnitPlatform()
}

listOf("forge1201Jar", "forge1201RemapJar", "forge1201IncludeJar").forEach { taskName ->
    tasks.named<AbstractArchiveTask>(taskName) {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

val upstreamBaseline = "5373b2084e461f83bd6e0b5f2fe943e81bd59700"
val gitCommit = providers.exec {
    commandLine("git", "rev-parse", "--verify", "HEAD")
}.standardOutput.asText.map { output ->
    output.trim().also { commit ->
        check(commit.matches(Regex("[0-9a-f]{40}"))) { "Invalid Git commit: $commit" }
    }
}
val gitDirty = providers.exec {
    commandLine("git", "status", "--porcelain=v1", "--untracked-files=all")
}.standardOutput.asText.map { it.isNotBlank() }

forge1201FinalJar.configure {
    inputs.property("gitCommit", gitCommit)
    inputs.property("gitDirty", gitDirty)
    inputs.property("upstreamBaseline", upstreamBaseline)

    from(layout.projectDirectory.file("LICENSE")) {
        into("META-INF")
    }
    from(layout.projectDirectory.file("NOTICE.md")) {
        into("META-INF")
    }

    doFirst {
        manifest.attributes(
            "Implementation-Title" to "Tectonic 1.20.1 Forge - Unofficial Community Backport",
            "Implementation-Version" to communityVersion,
            "Community-Build" to "true",
            "Official-Build" to "false",
            "Git-Commit" to gitCommit.get(),
            "Git-Dirty" to gitDirty.get().toString(),
            "Upstream-Baseline" to upstreamBaseline,
            "Java-Class-Major" to "65",
        )
    }
}

tasks.register<Test>("forge1201UnitTest") {
    description = "Runs Forge 1.20.1 backport unit tests."
    group = "verification"
    dependsOn(forge1201FinalJar)
    testClassesDirs = forge1201UnitTest.output.classesDirs
    classpath = forge1201UnitTest.runtimeClasspath
    doFirst {
        systemProperty(
            "tectonic.forge1201.jar",
            forge1201FinalJar.get().archiveFile.get().asFile.absolutePath
        )
    }
}

val communityJarName = "tectonic-community-backport-1.20.1-forge-$communityVersion.jar"
val communityJar = layout.buildDirectory.file("libs/$communityJarName")
val communityChecksum = layout.buildDirectory.file("libs/$communityJarName.sha256")

val copyForge1201CommunityJar = tasks.register<Copy>("copyForge1201CommunityJar") {
    description = "Copies the verified Forge 1.20.1 JAR to the explicit community-backport name."
    group = "build"
    dependsOn(forge1201FinalJar)
    from(forge1201FinalJar.flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("libs"))
    rename { communityJarName }
    outputs.file(communityJar)
}

val checksumForge1201CommunityJar = tasks.register("checksumForge1201CommunityJar") {
    description = "Writes the SHA-256 sidecar for the community Forge 1.20.1 candidate."
    group = "verification"
    dependsOn(copyForge1201CommunityJar)
    inputs.file(communityJar)
    outputs.file(communityChecksum)

    doLast {
        val jar = communityJar.get().asFile
        val digest = MessageDigest.getInstance("SHA-256")
        jar.inputStream().buffered().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val checksum = digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
        communityChecksum.get().asFile.writeText("$checksum  ${jar.name}\n", Charsets.UTF_8)
    }
}

tasks.register("assembleForge1201CommunityCandidate") {
    description = "Builds, tests, names, and checksums the local Forge 1.20.1 community candidate."
    group = "build"
    dependsOn("check", checksumForge1201CommunityJar)
}

tasks.named("check") {
    // Cloche's root test source set spans every configured loader/version. This branch verifies
    // the supported target explicitly so `check` does not resolve unrelated Fabric/NeoForge games.
    setDependsOn(listOf("forge1201UnitTest"))
}
