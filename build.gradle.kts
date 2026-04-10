plugins {
    kotlin("jvm") version "2.1.21"
    id("earth.terrarium.cloche") version "0.18.10"
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
version = "3.0.22"
val lithostitchedVersion = "1.6.3"

cloche {
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
                    version("1.6.0")
                }
            }
        }
    }

    val sharedOld = common("shared:21.1") {
        mixins.from(file("src/shared/21.1/main/tectonic.21.1.mixins.json"))
    }
    val sharedNew = common("shared:26.1") {
        mixins.from(file("src/shared/26.1/main/tectonic.26.1.mixins.json"))
    }

    fabric("fabric:21.1") {
        dependsOn(sharedOld)

        loaderVersion = "0.18.5"
        minecraftVersion = "1.21.1"

        mappings {
            official()
            custom(minecraftVersion.map {
                project.dependencies.create(files("mappings/$it.tiny"))
            })
        }

        dependencies {
            fabricApi("0.116.1")
            modImplementation("maven.modrinth:lithostitched:$lithostitchedVersion-fabric-21.1")
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

    fabric("fabric:26.1") {
        dependsOn(sharedNew)

        loaderVersion = "0.18.5"
        minecraftVersion = "26.1"

        dependencies {
            fabricApi("0.144.3")
            modImplementation("maven.modrinth:lithostitched:1.6.4-fabric-26.1")
            modImplementation("com.terraformersmc:modmenu:18.0.0-alpha.8")
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

    neoforge("neoforge:21.1") {
        dependsOn(sharedOld)

        loaderVersion = "21.1.222"
        minecraftVersion = "1.21.1"

        mappings {
            official()
            custom(minecraftVersion.map {
                project.dependencies.create(files("mappings/$it.tiny"))
            })
        }

        dependencies {
            modImplementation("maven.modrinth:lithostitched:1.6.5-neoforge-21.1")
        }

        runs {
            client()
            server()
        }
    }

    neoforge("neoforge:26.1") {
        dependsOn(sharedNew)

        loaderVersion = "26.1.0.7-beta"
        minecraftVersion = "26.1"

        dependencies {
            modImplementation("maven.modrinth:lithostitched:$lithostitchedVersion-neoforge-26.1")
        }

        runs {
            client()
            server()
        }
    }
}