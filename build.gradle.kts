import org.apache.commons.lang3.StringUtils

plugins {
    id("dev.isxander.modstitch.base") version "0.5.15-unstable"
    id("dev.isxander.modstitch.publishing") version "0.5.15-unstable"
}

fun prop(name: String, consumer: (prop: String) -> Unit) {
    (findProperty(name) as? String?)
        ?.let(consumer)
}

val modVersion = "${property("mod_version")}"

val minecraft = property("deps.minecraft") as String;

val isFabric = modstitch.isLoom
val isNeoforge = modstitch.isModDevGradleRegular
val isForge = modstitch.isModDevGradleLegacy
val isForgeLike = modstitch.isModDevGradle

val loader = when {
    modstitch.isLoom -> "fabric"
    modstitch.isModDevGradle -> "neoforge"
    else -> error("Unknown loader")
}

modstitch {
    minecraftVersion = minecraft

    // Alternatively use stonecutter.eval if you have a lot of versions to target.
    // https://stonecutter.kikugie.dev/stonecutter/guide/setup#checking-versions
    javaTarget = when (minecraft) {
        "1.20.1" -> 17
        else -> 21
    }

    // If parchment doesnt exist for a version yet you can safely
    // omit the "deps.parchment" property from your versioned gradle.properties
    parchment {
        prop("deps.parchment") { mappingsVersion = it }
    }

    // This metadata is used to fill out the information inside
    // the metadata files found in the templates folder.
    metadata {
        modId = "tectonic"
        modName = "Tectonic"
        modVersion = "${property("mod_version")}"
        modGroup = "dev.worldgen"

        fun <K, V> MapProperty<K, V>.populate(block: MapProperty<K, V>.() -> Unit) {
            block()
        }

        replacementProperties.populate {
            // You can put any other replacement properties/metadata here that
            // modstitch doesn't initially support. Some examples below.
            put("mod_issue_tracker", "https://github.com/Apollounknowndev/tectonic/issues")
        }
    }

    // Fabric Loom (Fabric)
    loom {
        // It's not recommended to store the Fabric Loader version in properties.
        // Make sure its up to date.
        fabricLoaderVersion = "0.16.13"

        // Configure loom like normal in this block.
        configureLoom {

        }
    }

    // ModDevGradle (NeoForge, Forge, Forgelike)
    moddevgradle {
        enable {
            prop("deps.forge") { forgeVersion = it }
            prop("deps.neoform") { neoFormVersion = it }
            prop("deps.neoforge") { neoForgeVersion = it }
            prop("deps.mcp") { mcpVersion = it }
        }

        // Configures client and server runs for MDG, it is not done by default
        defaultRuns()

        // This block configures the `neoforge` extension that MDG exposes by default,
        // you can configure MDG like normal from here
        configureNeoforge {
            runs.all {
                disableIdeRun()
            }
        }
    }

    mixin {
        // You do not need to specify mixins in any mods.json/toml file if this is set to
        // true, it will automatically be generated.
        addMixinsToModManifest = true

        configs.register("tectonic")

        if (minecraft == "1.20.1") configs.register("tectonic_1.20.1")
        if (minecraft == "1.21.1") configs.register("tectonic_1.21.1")
        if (minecraft == "1.21.6") configs.register("tectonic_1.21.6")

        // Most of the time you wont ever need loader specific mixins.
        // If you do, simply make the mixin file and add it like so for the respective loader:
        // if (isLoom) configs.register("examplemod-fabric")
        // if (isModDevGradleRegular) configs.register("examplemod-neoforge")
        // if (isModDevGradleLegacy) configs.register("examplemod-forge")
    }
}

// Stonecutter constants for mod loaders.
// See https://stonecutter.kikugie.dev/stonecutter/guide/comments#condition-constants
var constraint: String = name.split("-")[1]
stonecutter {
    consts(
        "fabric" to constraint.equals("fabric"),
        "neoforge" to constraint.equals("neoforge"),
        "forge" to constraint.equals("forge"),
        "vanilla" to constraint.equals("vanilla")
    )
}

// All dependencies should be specified through modstitch's proxy configuration.
// Wondering where the "repositories" block is? Go to "stonecutter.gradle.kts"
// If you want to create proxy configurations for more source sets, such as client source sets,
// use the modstitch.createProxyConfigurations(sourceSets["client"]) function.
dependencies {
    modstitchModImplementation("maven.modrinth:lithostitched:${property("deps.lithostitched")}")
    if (isFabric) {
        modstitchModImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
        modstitchModImplementation("com.terraformersmc:modmenu:${property("deps.mod_menu")}")
    }

    //modstitchModImplementation("maven.modrinth:terralith:${property("deps.terralith")}")
    if (minecraft != "1.20.1") {
        //modstitchModImplementation("maven.modrinth:clifftree:${property("deps.clifftree")}")
    }
}

tasks {
    modstitch.finalJarTask {
        archiveVersion.set("$modVersion-$loader-$minecraft")
    }
}

publishMods {
    changelog = """
        A changelog for sure!
    """.trimIndent()
    type = BETA
    modLoaders.add(loader)
    file = modstitch.finalJarTask.flatMap { it.archiveFile }
    displayName = "v%s ~ %s %s".format(property("mod_version"), StringUtils.capitalize(loader), property("deps.minecraft"))

    dryRun = false

    modrinth {
        accessToken.set(providers.environmentVariable("TOKEN_MR"))
        projectId.set("lWDHr9jE")

        if (minecraft == "1.20.1") minecraftVersions.add("1.20.1")
        if (minecraft == "1.21.1") minecraftVersions.add("1.21.1")
        if (minecraft == "1.21.6") minecraftVersions.add("1.21.6")

        if (isFabric) requires("fabric-api")
        requires("lithostitched")
        optional("worldgen-patches")
        incompatible("continents")
    }

    curseforge {
        accessToken.set(providers.environmentVariable("TOKEN_CF"))
        projectId.set("686836")

        if (minecraft == "1.20.1") minecraftVersions.add("1.20.1")
        if (minecraft == "1.21.1") minecraftVersions.add("1.21.1")
        if (minecraft == "1.21.6") minecraftVersions.add("1.21.6")

        if (isFabric) requires("fabric-api")
        requires("lithostitched")
        optional("worldgen-patches")
        incompatible("continents")
    }
}