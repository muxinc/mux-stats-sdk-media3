import java.time.Year
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.mux.android.distribution)
}

android {
  namespace = "com.mux.stats.sdk.muxstats.media3"
  compileSdk = 37

  buildFeatures {
    buildConfig = true
  }

  defaultConfig {
    minSdk = 23

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  flavorDimensions += "media3"
  productFlavors {
    create("At_latest") {
      dimension = "media3"
    }
    // This module does not currently need different src sets for different media3 versions.
    // We still need to declare different flavors so we can create version-specific variants
    create("at_1_0") {
      dimension = "media3"
      minSdk = 19 // minSdk is 19 before 1.4
    }
    create("at_1_1") {
      dimension = "media3"
      minSdk = 19 // minSdk is 19 before 1.4
    }
    create("at_1_2") {
      dimension = "media3"
      minSdk = 19 // minSdk is 19 before 1.4
    }
    create("at_1_3") {
      dimension = "media3"
      minSdk = 19 // minSdk is 19 before 1.4
    }
    create("at_1_4") {
      dimension = "media3"
      minSdk = 21 // minSdk is 21 before 1.9
    }
    create("at_1_5") {
      dimension = "media3"
      minSdk = 21 // minSdk is 21 before 1.9
    }
    create("at_1_6") {
      dimension = "media3"
      minSdk = 21 // minSdk is 21 before 1.9
    }
    create("at_1_8") {
      dimension = "media3"
      minSdk = 21 // minSdk is 21 before 1.9
    }
    create("at_1_9") {
      dimension = "media3"
    }
    create("at_1_10") {
      dimension = "media3"
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
  }
}

muxDistribution {
  devVersion(versionFromCommitHash("dev-"))
  releaseVersion(versionFromTag())
  artifactIds { variant ->
    val media3Variant = variant.productFlavors.first { it.first == "media3" }.second
//    val media3Variant = variant.productFlavors.first { }.name
    if (media3Variant.contains("at_latest", ignoreCase = true)) {
      println(">>>>>>media3Variant: $media3Variant")
      "data-media3-custom"
    } else {
      println(">>>>>media3Variant: $media3Variant")
      "data-media3-custom-$media3Variant"
    }
  }
  groupIds(just("com.mux.stats.sdk.muxstats"))
  publicReleaseIf(releaseIfCmdFlag("publicRelease"))

  // TODO: This is kinda clumsy, packageJavadocs should be a function not a property probably
  packageDocs(releaseIfCmdFlag("publicRelease").call())
  publishIf { it.contains("release", ignoreCase = true) }
  artifactoryConfig {
    contextUrl = "https://muxinc.jfrog.io/artifactory/"
    releaseRepoKey = "default-maven-release-local"
    devRepoKey = "default-maven-local"
  }

  dokkaConfig {
    moduleName = "Mux Data SDK for Media3, Base"
    footer = "(c) " + Year.now().value + " Mux, Inc. Have questions or need help?" +
            " Contact support@mux.com"
  }

  pom {
    description.set("The Mux Data SDK for Google's androidX media3 player")
    inceptionYear.set("2022")
    url.set("https://github.com/muxinc/mux-stats-sdk-media3")
    organization {
      name.set("Mux, Inc")
      url.set("https://www.mux.com")
    }
    developers {
      developer {
        email.set("support@mux.com")
        name.set("The player and sdks team @mux")
        organization.set("Mux, inc")
      }
    }
  }
}

dependencies {
  api(libs.mux.core.android)
  api(libs.mux.core.java)

  //noinspection GradleDependency
  "at_1_0Api"(libs.media3.common.at10)
  //noinspection GradleDependency
  "at_1_1Api"(libs.media3.common.at11)
  //noinspection GradleDependency
  "at_1_2Api"(libs.media3.common.at12)
  //noinspection GradleDependency
  "at_1_3Api"(libs.media3.common.at13)
  //noinspection GradleDependency // benefit from optimistic matching
  "at_1_4Api"(libs.media3.common.at14)
  //noinspection GradleDependency // benefit from optimistic matching
  "at_1_5Api"(libs.media3.common.at15)
  //noinspection GradleDependency // benefit from optimistic matching
  "at_1_6Api"(libs.media3.common.at16)
  //noinspection GradleDependency // benefit from optimistic matching
  "at_1_8Api"(libs.media3.common.at18)
  //noinspection GradleDependency // benefit from optimistic matching
  "at_1_9Api"(libs.media3.common.at19)
  //noinspection GradleDependency // benefit from optimistic matching
  "at_1_10Api"(libs.media3.common.at110)
  //noinspection GradleDependency // benefit from optimistic matching
  "At_latestApi"(libs.media3.common.atLatest)

  implementation(libs.kotlinx.coroutines.android)

  testImplementation(libs.junit)
  testImplementation(libs.androidx.test.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.robolectric)

  androidTestImplementation(libs.androidx.test.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
}
