plugins {
    kotlin("jvm") version "2.1.21"
    id("earth.terrarium.cloche") version "0.11.20"
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
version = "3.0.10"

cloche {
    mappings {
        official()
    }

    metadata {
        modId = "tectonic"
        name = "Tectonic"
        description = "Terrain shaping brought to new heights, grander and more varied than ever before!"
        license = "MIT"
        icon = "pack.png"

        url = "https://modrinth.com/project/tectonic"
        issues = "https://github.com/Apollounknowndev/tectonic/issues"
        sources = "https://github.com/Apollounknowndev/tectonic"

        author("Apollo")
        contributor("HB Stratos")
        contributor("DawnKiro")
        contributor("Uni")
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
    val shared1218 = common("shared:1.21.8") {
        mixins.from(file("src/shared/1.21.8/main/tectonic_1.21.8.mixins.json"))
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

        loaderVersion = "0.16.13"
        minecraftVersion = "1.21.1"

        dependencies {
            fabricApi("0.116.1")
            modImplementation("maven.modrinth:lithostitched:1.4.11-fabric-1.21")
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

    fabric("fabric:1.21.8") {
        dependsOn(shared1218)

        loaderVersion = "0.16.13"
        minecraftVersion = "1.21.8"

        dependencies {
            fabricApi("0.129.0")
            modImplementation("maven.modrinth:lithostitched:1.4.11-fabric-1.21.6")
            modImplementation("com.terraformersmc:modmenu:15.0.0-beta.3")
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

    forge("forge:1.20.1") {
        dependsOn(shared1201)

        loaderVersion = "47.4.0"
        minecraftVersion = "1.20.1"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.4.11-forge-1.20")
        }

        runs {
            client()
            server()
        }
    }

    neoforge("neoforge:1.21.1") {
        dependsOn(shared1211)

        loaderVersion = "21.1.26"
        minecraftVersion = "1.21.1"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.4.11-neoforge-1.21")
        }

        runs {
            client()
            server()
        }
    }

    neoforge("neoforge:1.21.8") {
        dependsOn(shared1218)

        loaderVersion = "21.8.4-beta"
        minecraftVersion = "1.21.8"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.4.11-neoforge-1.21.6")
        }

        runs {
            client()
            server()
        }
    }
}