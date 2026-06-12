import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.mux.stats.muxdatasdkformedia3"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.example.muxdatasdkformedia3"
    //noinspection EditedTargetSdkVersion
    targetSdk = 36
    minSdk = 23
    versionCode = 1
    val commit = providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }
            .standardOutput.asText.get().trim()
    val branch = providers.exec { commandLine("git", "branch", "--show-current") }
            .standardOutput.asText.get().trim()
    versionName = "$branch-$commit"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
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
  }

  sourceSets {
    getByName("At_latest") { kotlin.directories += "src/compatFrom1_3/java" }
    getByName("at_1_0") { kotlin.directories += "src/compatFrom1_0/java" }
    getByName("at_1_1") { kotlin.directories += "src/compatFrom1_0/java" }
    getByName("at_1_2") { kotlin.directories += "src/compatFrom1_0/java" }
    getByName("at_1_3") { kotlin.directories += "src/compatFrom1_3/java" }
    getByName("at_1_4") { kotlin.directories += "src/compatFrom1_3/java" }
    getByName("at_1_5") { kotlin.directories += "src/compatFrom1_3/java" }
    getByName("at_1_6") { kotlin.directories += "src/compatFrom1_3/java" }
    getByName("at_1_8") { kotlin.directories += "src/compatFrom1_3/java" }
    getByName("at_1_9") { kotlin.directories += "src/compatFrom1_3/java" }
    getByName("at_1_10") { kotlin.directories += "src/compatFrom1_3/java" }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  buildFeatures {
    viewBinding = true
    compose = true
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true // only needed if using IMA
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.kotlinComposeCompilerExt.get()
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
  }
}

dependencies {
  implementation(project(":library-ima"))
  implementation(project(":library-exo"))
  implementation(project(":library"))
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)

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
  "At_latestImplementation"(libs.bundles.media3.app.atLatest)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.constraintlayout)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
