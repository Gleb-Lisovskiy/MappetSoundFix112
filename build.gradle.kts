plugins {
    id("java-library")
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.6"
}

group = "kerilom.mappetsoundfix"
version = "1.0.0"

base {
    archivesName.set("MappetSoundFix")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    withSourcesJar()
}

minecraft {
    mcVersion.set("1.12.2")
    username.set("Developer")

    // Prints useful stack traces when running the dev client.
    extraRunJvmArguments.add("-ea:${project.group}")
}

repositories {
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
    mavenCentral()
}

dependencies {
    /* MixinBooter bundles Mixin 0.8.5 and the ILateMixinLoader interface, which
     * is everything this addon needs to compile against. It is provided by the
     * player at runtime, so it must never be shaded into the jar. */
    compileOnly("maven.modrinth:mixinbooter:8.9")

    testImplementation("junit:junit:4.13.2")
}

tasks.named<ProcessResources>("processResources") {
    val modVersion = project.version.toString()

    inputs.property("modVersion", modVersion)
    filesMatching("mcmod.info") {
        expand(mapOf("modVersion" to modVersion))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all,-options,-path,-processing"))
}

tasks.named<Test>("test") {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to "Mappet Sound Fix",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Kerilom"
        )
    }
}
