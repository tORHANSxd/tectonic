plugins {
    kotlin("jvm") version "2.1.21"
    id("earth.terrarium.cloche") version "0.16.20"
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
    mavenLocal()
    mavenCentral()
    maven("https://api.modrinth.com/maven")
    maven("https://maven.terraformersmc.com/")
}

group = "dev.worldgen.tectonic"
version = "3.0.18"

cloche {
    targets.all {
        mappings {
            official()
            custom(minecraftVersion.map {
                project.dependencies.create(files("mappings/$it.tiny"))
            })
        }
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
                    version("1.4.11")
                }
            }
        }
    }

    val shared1211 = common("shared:1.21.1") {
        mixins.from(file("src/shared/1.21.1/main/tectonic_1.21.1.mixins.json"))
    }
    val shared12111 = common("shared:1.21.11") {
        mixins.from(file("src/shared/1.21.11/main/tectonic_1.21.11.mixins.json"))
    }

    fabric("fabric:1.21.1") {
        dependsOn(shared1211)

        loaderVersion = "0.17.3"
        minecraftVersion = "1.21.1"

        dependencies {
            fabricApi("0.116.1")
            modImplementation("maven.modrinth:lithostitched:1.5.2-fabric-1.21.1")
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

    fabric("fabric:1.21.11") {
        dependsOn(shared12111)

        loaderVersion = "0.18.2"
        minecraftVersion = "1.21.11"

        dependencies {
            fabricApi("0.139.4")
            modImplementation("maven.modrinth:lithostitched:1.5.2+beta2-fabric-1.21.11")
            modImplementation("com.terraformersmc:modmenu:17.0.0-alpha.1")
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

    neoforge("neoforge:1.21.1") {
        dependsOn(shared1211)

        loaderVersion = "21.1.209"
        minecraftVersion = "1.21.1"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.5.2-neoforge-1.21.1")
        }

        runs {
            client()
            server()
        }
    }

    /*neoforge("neoforge:1.21.10") {
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
    }*/
}