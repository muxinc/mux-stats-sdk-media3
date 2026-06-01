import org.jetbrains.dokka.gradle.DokkaMultiModuleTask
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.mux.android.distribution) apply false
  alias(libs.plugins.dokka)
}

allprojects {
  tasks.withType<DokkaTaskPartial>().configureEach {
    dokkaSourceSets.configureEach {
      //includes.from("README.md")
    }
  }
}

tasks.named<DokkaMultiModuleTask>("dokkaHtmlMultiModule").configure {
  outputDirectory.set(layout.buildDirectory.get().asFile.resolve("dokkaOutput"))
}
