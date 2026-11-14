import groovy.json.JsonSlurper

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

val appVersion = providers.gradleProperty("rulecade.version").get()
val (major, minor, patch) = appVersion.split('.').map(String::toInt)
val releaseKeystore = providers.environmentVariable("RELEASE_KEYSTORE").orNull

android {
    namespace = "io.github.rulecade"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.rulecade"
        minSdk = 28
        targetSdk = 36
        versionCode = major * 10_000 + minor * 100 + patch
        versionName = appVersion
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("RELEASE_KEYSTORE_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Turns design/tokens.json into Compose tokens (and a CSS file for local mockups).
abstract class GenerateDesignTokens : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokens: RegularFileProperty

    @get:OutputDirectory
    abstract val kotlinDir: DirectoryProperty

    @get:OutputDirectory
    abstract val resDir: DirectoryProperty

    @get:OutputFile
    abstract val cssFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val root = JsonSlurper().parse(tokens.get().asFile) as Map<*, *>
        val colors = group(root, "color").mapValues { (_, theme) -> values(theme) }
        val type = group(root, "type").mapValues { (_, style) -> values(style) }
        val dimensions = listOf("spacing" to "Spacing", "radius" to "Radius", "size" to "Sizes")

        val kotlin = buildString {
            appendLine("// Generated from design/tokens.json. Do not edit.")
            appendLine("package io.github.rulecade.ui.theme")
            appendLine()
            appendLine("import androidx.compose.ui.graphics.Color")
            appendLine("import androidx.compose.ui.unit.dp")
            for (theme in listOf("light", "dark")) {
                val name = theme.replaceFirstChar(Char::uppercase)
                appendLine()
                appendLine("internal val ${name}Colors = RcColors(")
                colors.getValue(theme).forEach { (key, hex) ->
                    appendLine("    ${camel(key)} = ${color(hex)},")
                }
                appendLine(")")
            }
            appendLine()
            appendLine("internal object StaticColors {")
            colors.getValue("static").forEach { (key, hex) ->
                appendLine("    val ${camel(key)} = ${color(hex)}")
            }
            appendLine("}")
            appendLine()
            appendLine("internal val RulecadeTypography = RcTypography(")
            type.forEach { (key, s) ->
                val args = "size = ${s["size"]}, lineHeight = ${s["line_height"]}, " +
                    "weight = ${s["weight"]}"
                appendLine("    ${camel(key)} = textStyle($args),")
            }
            appendLine(")")
            for ((key, objectName) in dimensions) {
                appendLine()
                appendLine("internal object $objectName {")
                values(root[key]).forEach { (name, dp) ->
                    appendLine("    val ${camel(name)} = $dp.dp")
                }
                appendLine("}")
            }
            appendLine()
            appendLine("internal object Motion {")
            values(root["motion"]).forEach { (name, value) ->
                val literal = if ('.' in value) "${value}f" else value
                appendLine("    const val ${name.uppercase()} = $literal")
            }
            appendLine("}")
        }
        val file = kotlinDir.get().file("io/github/rulecade/ui/theme/Tokens.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(kotlin)

        val css = buildString {
            appendLine("/* Generated from design/tokens.json. Do not edit. */")
            appendLine(":root {")
            colors.getValue("static").forEach { (key, hex) -> appendLine("  --rc_$key: $hex;") }
            for ((key, _) in dimensions) {
                values(root[key]).forEach { (name, px) ->
                    appendLine("  --rc_${key}_$name: ${px}px;")
                }
            }
            type.forEach { (key, s) ->
                val size = "${s["size"]}px/${s["line_height"]}px"
                val font = "${s["weight"]} $size 'Asta Sans', sans-serif"
                appendLine("  --rc_type_$key: $font;")
            }
            appendLine("}")
            for (theme in listOf("light", "dark")) {
                appendLine("[data-theme=$theme] {")
                colors.getValue(theme).forEach { (key, hex) -> appendLine("  --rc_$key: $hex;") }
                appendLine("}")
            }
        }
        cssFile.get().asFile.apply { parentFile.mkdirs() }.writeText(css)

        // Window and splash backgrounds need the theme colors as resources.
        for ((theme, folder) in listOf("light" to "values", "dark" to "values-night")) {
            val xml = buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                appendLine("<resources>")
                colors.getValue(theme).forEach { (key, hex) ->
                    appendLine("    <color name=\"rc_$key\">${androidColor(hex)}</color>")
                }
                appendLine("</resources>")
            }
            resDir.get().file("$folder/rc_colors.xml").asFile
                .apply { parentFile.mkdirs() }
                .writeText(xml)
        }
    }

    private fun group(root: Map<*, *>, key: String) =
        (root[key] as Map<*, *>).entries.associate { (k, v) -> k.toString() to v }

    private fun values(node: Any?) =
        (node as Map<*, *>).entries.associate { (k, v) -> k.toString() to v.toString() }

    private fun camel(name: String) = name.split('_')
        .mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar(Char::uppercase) }
        .joinToString("")

    // "#RRGGBB" or "#RRGGBBAA" (CSS order) -> "AARRGGBB"
    private fun argb(hex: String): String {
        val digits = hex.removePrefix("#").uppercase()
        val alpha = if (digits.length == 8) digits.substring(6) else "FF"
        return alpha + digits.take(6)
    }

    private fun color(hex: String) = "Color(0x${argb(hex)})"

    private fun androidColor(hex: String) = "#${argb(hex)}"
}

val generateDesignTokens = tasks.register<GenerateDesignTokens>("generateDesignTokens") {
    tokens.set(rootProject.layout.projectDirectory.file("design/tokens.json"))
    cssFile.set(layout.buildDirectory.file("design/tokens.css"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateDesignTokens,
            GenerateDesignTokens::kotlinDir
        )
        variant.sources.res?.addGeneratedSourceDirectory(
            generateDesignTokens,
            GenerateDesignTokens::resDir
        )
    }
}

// Content tests read the shipped question files, so a content change must rerun them.
tasks.withType<Test>().configureEach {
    inputs.dir("src/main/assets").withPathSensitivity(PathSensitivity.RELATIVE)
}

ktlint {
    filter {
        exclude { it.file.invariantSeparatorsPath.contains("/build/generated/") }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.pleos.vehicle)
    implementation(libs.pleos.navi)
    testImplementation(libs.junit)
}
