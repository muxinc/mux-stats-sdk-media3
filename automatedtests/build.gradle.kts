// testOptions.reportDir/resultsDir are deprecated but still functional; suppress
// so the script compiles while preserving prior behavior.
@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.mux.player.media3"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.mux.player.media3"
    minSdk = 23
    //noinspection EditedTargetSdkVersion
    targetSdk = 37
    versionCode = 1
    versionName = "1.0"
    multiDexEnabled = true
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("boolean", "SHOULD_REPORT_INSTRUMENTATION_TEST_EVENTS_TO_SERVER", "true")
    buildConfigField("String", "INSTRUMENTATION_TEST_ENVIRONMENT_KEY", "\"YOUR_KEY_HERE\"")
  }

  flavorDimensions += "media3"
  productFlavors {
    create("At_latest") { dimension = "media3" }
    create("at_1_0") { dimension = "media3" }
    create("at_1_1") { dimension = "media3" }
    create("at_1_2") { dimension = "media3" }
    create("at_1_3") { dimension = "media3" }
    create("at_1_4") { dimension = "media3" }
    create("at_1_5") { dimension = "media3" }
    create("at_1_6") { dimension = "media3" }
    create("at_1_8") { dimension = "media3" }
    create("at_1_9") { dimension = "media3" }
    create("at_1_10") { dimension = "media3" }
    create("at_1_11") { dimension = "media3" }
  }

  buildFeatures {
    buildConfig = true
  }

  buildTypes {
    debug {
      isMinifyEnabled = false
    }
  }


  testOptions {
    // Changes the directory where Gradle saves test reports. By default, Gradle saves test reports
    // in the path_to_your_project/module_name/build/outputs/reports/ directory.
    // '$rootDir' sets the path relative to the root directory of the current project.
    reportDir = "./automated_test_results/reports"
    // Changes the directory where Gradle saves test results. By default, Gradle saves test results
    // in the path_to_your_project/module_name/build/outputs/test-results/ directory.
    // '$rootDir' sets the path relative to the root directory of the current project.
    resultsDir = "./automated_test_results/results"
  }

  sourceSets {
    getByName("androidTest") {
      // Important, can't get asset file in instrumentation test without this
      assets.directories += "src/main/assets"
    }
  }

  compileOptions {
    isCoreLibraryDesugaringEnabled = true // only needed if using IMA
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

dependencies {
  implementation(fileTree("libs") { include("*.jar") })
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.navigation.fragment)
  implementation(libs.androidx.navigation.ui)

  coreLibraryDesugaring(libs.desugar.jdk.libs)

  //noinspection GradleDependency
  "at_1_0Implementation"(libs.bundles.media3.app.at10)
  //noinspection GradleDependency
  "at_1_1Implementation"(libs.bundles.media3.app.at11)
  //noinspection GradleDependency
  "at_1_2Implementation"(libs.bundles.media3.app.at12)
  //noinspection GradleDependency
  "at_1_3Implementation"(libs.bundles.media3.app.at13)
  //noinspection GradleDependency
  "at_1_4Implementation"(libs.bundles.media3.app.at14)
  //noinspection GradleDependency
  "at_1_5Implementation"(libs.bundles.media3.app.at15)
  //noinspection GradleDependency
  "at_1_6Implementation"(libs.bundles.media3.app.at16)
  //noinspection GradleDependency
  "at_1_8Implementation"(libs.bundles.media3.app.at18)
  //noinspection GradleDependency
  "at_1_9Implementation"(libs.bundles.media3.app.at19)
  //noinspection GradleDependency
  "at_1_10Implementation"(libs.bundles.media3.app.at110)
  "at_1_11Implementation"(libs.bundles.media3.app.at111)
  "At_latestImplementation"(libs.bundles.media3.app.atLatest)

  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.rules)
  // Optional -- Hamcrest library
  androidTestImplementation(libs.hamcrest.library)
  // Optional -- UI testing with Espresso
  androidTestImplementation(libs.androidx.test.espresso.core)
  // Optional -- UI testing with UI Automator
  androidTestImplementation(libs.androidx.test.uiautomator)
  androidTestImplementation(libs.androidx.test.junit)

  api(libs.checker.qual)

  // Automated tests should always test the local module and not the maven dependency.
  implementation(project(":library"))
  implementation(project(":library-exo"))
  implementation(project(":library-ima"))
}
