plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.compose.nav.graph) apply false
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(files("$rootDir/detekt.yml"))
    source.setFrom(files("app/src/main/java", "app/src/main/kotlin"))
    baseline = file("$rootDir/detekt-baseline.xml")
    parallel = true
    buildUponDefaultConfig = true
    autoCorrect = false
    ignoredBuildTypes = listOf("release")
}
