plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.compose.nav.graph) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.gradle.versions)
}

// `./gradlew dependencyUpdates` (see chatapp-dependency-updater agent / `/update-deps`) — reports
// available updates for every dependency in one pass instead of hand-checking each Maven repo.
// Only stable releases count as an "update"; a current alpha/beta/rc line stays pinned to newer
// pre-releases of the same line so the report doesn't nag about those separately.
tasks.named<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask>("dependencyUpdates") {
    rejectVersionIf {
        val stableKeywords = listOf("RELEASE", "FINAL", "GA")
        val isStableCandidate = stableKeywords.any { candidate.version.uppercase().contains(it) } ||
            Regex("^[0-9,.v-]+(-r)?$").matches(candidate.version)
        val isStableCurrent = stableKeywords.any { currentVersion.uppercase().contains(it) } ||
            Regex("^[0-9,.v-]+(-r)?$").matches(currentVersion)
        isStableCurrent && !isStableCandidate
    }
    checkForGradleUpdate = true
    outputFormatter = "plain,json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "report"
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
