plugins {
        id("fabric-loom") version "1.10-SNAPSHOT"
}

base {
    archivesName = properties["archives_base_name"] as String
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}
dependencies {
// Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")
    modApi("meteordevelopment:baritone:${properties["baritone_version"] as String}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

// Meteor
    modImplementation("meteordevelopment:meteor-client:${project.property("minecraft_version")}-SNAPSHOT")

// XaeroPlus
    modImplementation("maven.modrinth:xaeroplus:2.28.1+fabric-1.21.4")
// XaeroWorldMap
    modImplementation("maven.modrinth:xaeros-world-map:1.39.12_Fabric_1.21.4")
// XaeroMinimap
    modImplementation("maven.modrinth:xaeros-minimap:25.2.10_Fabric_1.21.4")

    // Include these libraries in the jar
    implementation(include("net.lenni0451:LambdaEvents:2.4.2")!!)
    implementation(include("com.github.ben-manes.caffeine:caffeine:3.1.8")!!)
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
            "loader_version" to project.property("loader_version")
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        val licenseSuffix = project.base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_${licenseSuffix}" }
        }
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }
}
loom {
    accessWidenerPath = file("src/main/resources/bep.accesswidener")
}
