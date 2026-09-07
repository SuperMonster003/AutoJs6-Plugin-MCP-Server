import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.mcp.server"
val buildTypeDebug = "debug"
val buildTypeRelease = "release"

// ---------------------------------------------------------------------------
// Host protocol AARs are consumed only from libs/ and are pinned by locks/host-api-aars.lock.
// The build refuses missing files, debug artifacts, placeholder hashes, and digest mismatches.
// ---------------------------------------------------------------------------

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Properties.requiredValue(key: String): String =
    getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        ?: error("Missing required lock value: $key")

fun File.loadUniqueLock(): Properties {
    val lock = Properties()
    useLines(Charsets.UTF_8) { lines ->
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) {
                return@forEachIndexed
            }
            val separator = trimmed.indexOf('=')
            require(separator > 0) { "Malformed lock line ${index + 1} in $name" }
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            require(!lock.containsKey(key)) { "Duplicate lock key in $name: $key" }
            lock.setProperty(key, value)
        }
    }
    return lock
}

val sha256Pattern = Regex("[0-9a-f]{64}")
val hostApiLockFile = rootProject.file("locks/host-api-aars.lock")
require(hostApiLockFile.isFile) {
    "Missing host API lock: ${hostApiLockFile.relativeTo(rootProject.projectDir)}"
}
val hostApiLock = hostApiLockFile.loadUniqueLock()
val hostApiIds = listOf("common-plugin-api")
val expectedHostApiLockKeys = setOf("format") + hostApiIds.flatMap { id -> listOf("$id.file", "$id.sha256") }
require(hostApiLock.stringPropertyNames() == expectedHostApiLockKeys) {
    "Host API AAR lock must contain exactly these keys: ${expectedHostApiLockKeys.sorted()}"
}
require(hostApiLock.requiredValue("format") == "1") {
    "Unsupported host API AAR lock format"
}

fun lockedHostApiAar(id: String): File {
    val fileName = hostApiLock.requiredValue("$id.file")
    val expectedSha256 = hostApiLock.requiredValue("$id.sha256").lowercase()
    require(fileName == File(fileName).name && fileName.endsWith(".aar")) {
        "Invalid $id.file in ${hostApiLockFile.name}"
    }
    require(!fileName.endsWith("-debug.aar")) {
        "Debug AARs are forbidden: $fileName"
    }
    require(sha256Pattern.matches(expectedSha256)) {
        "Replace $id.sha256 with the audited release AAR SHA-256 before Gradle configuration"
    }
    val artifact = rootProject.file("libs/$fileName")
    require(artifact.isFile) {
        "Missing locked host API AAR: ${artifact.relativeTo(rootProject.projectDir)}"
    }
    val actualSha256 = artifact.sha256()
    require(actualSha256 == expectedSha256) {
        "SHA-256 mismatch for $fileName: expected $expectedSha256, actual $actualSha256"
    }
    return artifact
}

val commonPluginApiAar = lockedHostApiAar("common-plugin-api")

android {
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_engine", "mcp-server")
        resValue("string", "plugin_id", "mcp-server")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))
    }

    lint {
        abortOnError = true
        // Product text intentionally uses ASCII punctuation in every locale.
        disable += "TypographyEllipsis"
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        resValues = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    // No ABI splits on purpose: the plugin ships Kotlin bytecode and resources only, so every
    // device installs the same single APK and getInfo() reports supportedAbis = emptyArray().
    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    implementation(files(commonPluginApiAar))

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.ext.junit)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register<Copy>("appendDigestToReleasedFiles") {
        description = "Appends CRC32 digest to the released MCP Server APK file"
        dependsOn("assembleRelease")

        val ext = utils.FILE_EXTENSION_APK
        val src = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease")
        val dst = file("$rootDir/${buildTypeRelease}s")
        val releaseSigningReady = signs.isValid
        val expectedApkNames = listOf(
            "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}.$ext".lowercase(),
        )

        from(src)
        into(dst)
        include("*.$ext")
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.FAIL

        doFirst {
            require(releaseSigningReady) {
                "Release signing is not configured (sign.properties); refusing to collect an unsigned APK"
            }
            val actualApkNames = src.get().asFile.listFiles { file -> file.extension == ext }
                .orEmpty()
                .map { it.name }
                .sorted()
            require(actualApkNames == expectedApkNames) {
                "Unexpected release APK set: expected $expectedApkNames, actual $actualApkNames"
            }
        }

        eachFile {
            val digest = utils.digestCRC32(file)
            relativePath = RelativePath(true, "${name.removeSuffix(".$ext")}-$digest.$ext")
        }

        doLast { println("Destination: $dst") }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}
