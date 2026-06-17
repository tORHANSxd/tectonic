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
version = "3.0.23"
val lithostitchedVersion = "1.6.3"
val apollibVersion = "1.1.1"

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
            compileOnly("maven.modrinth:apollib:$apollibVersion-fabric-21.1")
            implementation("de.marhali:json5-java:3.0.0")
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

        dependencies {
            compileOnly("maven.modrinth:apollib:$apollibVersion-fabric-21.1")
        }
    }
    val sharedNew = common("shared:26.1") {
        mixins.from(file("src/shared/26.1/main/tectonic.26.1.mixins.json"))

        dependencies {
            compileOnly("maven.modrinth:apollib:$apollibVersion-fabric-26.1")
        }
    }
    val sharedBleedingEdge = common("shared:26.2") {
        mixins.from(file("src/shared/26.2/main/tectonic.26.2.mixins.json"))

        dependencies {
            compileOnly("maven.modrinth:apollib:$apollibVersion-fabric-26.2")
        }
    }

    fabric("fabric:21.1") {
        dependsOn(sharedOld)

        loaderVersion = "0.19.2"
        minecraftVersion = "1.21.1"

        mappings {
            official()
            custom(minecraftVersion.map {
                project.dependencies.create(files("mappings/21.1.tiny"))
            })
        }

        dependencies {
            fabricApi("0.116.1")
            include("maven.modrinth:apollib:$apollibVersion-fabric-21.1")
            modImplementation("maven.modrinth:apollib:$apollibVersion-fabric-21.1")
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

        loaderVersion = "0.19.2"
        minecraftVersion = "26.1"

        dependencies {
            fabricApi("0.144.3")
            include("maven.modrinth:apollib:$apollibVersion-fabric-26.1")
            modImplementation("maven.modrinth:apollib:$apollibVersion-fabric-26.1")
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

    fabric("fabric:26.2") {
        dependsOn(sharedBleedingEdge)

        loaderVersion = "0.19.2"
        minecraftVersion = "26.2"

        dependencies {
            fabricApi("0.152.1")
            include("maven.modrinth:apollib:$apollibVersion-fabric-26.2")
            modImplementation("maven.modrinth:apollib:$apollibVersion-fabric-26.2")
            modImplementation("maven.modrinth:lithostitched:1.7.10-fabric-26.2")

            modImplementation("com.terraformersmc:modmenu:20.0.0-beta.2")
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
                project.dependencies.create(files("mappings/21.1.tiny"))
            })
        }

        dependencies {
            legacyClasspath("maven.modrinth:apollib:$apollibVersion-neoforge-21.1")
            include("maven.modrinth:apollib:$apollibVersion-neoforge-21.1")
            modImplementation("maven.modrinth:apollib:$apollibVersion-neoforge-21.1")
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
            legacyClasspath("maven.modrinth:apollib:$apollibVersion-neoforge-26.1")
            include("maven.modrinth:apollib:$apollibVersion-neoforge-26.1")
            modImplementation("maven.modrinth:apollib:$apollibVersion-neoforge-26.1")
            modImplementation("maven.modrinth:lithostitched:$lithostitchedVersion-neoforge-26.1")
        }

        runs {
            client()
            server()
        }
    }

    neoforge("neoforge:26.2") {
        dependsOn(sharedBleedingEdge)

        loaderVersion = "26.2.0.0-beta"
        minecraftVersion = "26.2"

        dependencies {
            legacyClasspath("maven.modrinth:apollib:$apollibVersion-neoforge-26.2")
            include("maven.modrinth:apollib:$apollibVersion-neoforge-26.2")
            modImplementation("maven.modrinth:apollib:$apollibVersion-neoforge-26.2")
            modImplementation("maven.modrinth:lithostitched:1.7.10-neoforge-26.2")
        }

        runs {
            client()
            server()
        }
    }
}