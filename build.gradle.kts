import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "co.xenastudios"

/**
 * Version identity is derived from git at build time — there are no semantic-version
 * releases. Scheme: <yyyy.MM.dd>+<short-sha>. If git is unavailable (e.g. a source
 * export with no history) we fall back to a stable placeholder so the build never fails.
 */
fun git(vararg args: String): String = runCatching {
    val process = ProcessBuilder(listOf("git") + args)
        .redirectErrorStream(false)
        .start()
    val text = process.inputStream.bufferedReader().use { it.readText() }.trim()
    process.waitFor()
    if (process.exitValue() == 0) text else ""
}.getOrDefault("")

val shortSha: String = git("rev-parse", "--short=8", "HEAD").ifEmpty { "nogit" }
val fullSha: String = git("rev-parse", "HEAD").ifEmpty { "unknown" }
val buildDate: String = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
val buildTimestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())

version = "$buildDate+$shortSha"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    // Paper API — provided by the server at runtime; bundles Adventure + MiniMessage.
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

// Inject the version + commit + build timestamp into plugin.yml and build-info.properties.
tasks.processResources {
    val tokens = mapOf(
        "version" to project.version.toString(),
        "commit" to fullSha,
        "shortCommit" to shortSha,
        "buildTimestamp" to buildTimestamp,
    )
    inputs.properties(tokens)
    filesMatching(listOf("plugin.yml", "build-info.properties")) {
        expand(tokens)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("Limbo")
    // Nothing to relocate today (MiniMessage is provided by Paper) but shadow gives us a
    // single, predictable runnable jar and room to shade later without changing the build.
}

// `./gradlew build` should produce the shaded, runnable jar.
tasks.build {
    dependsOn(tasks.shadowJar)
}

// Avoid emitting the thin jar so only the shaded jar lands in build/libs.
tasks.jar {
    enabled = false
}
