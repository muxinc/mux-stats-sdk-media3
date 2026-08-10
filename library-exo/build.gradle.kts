import java.time.Year
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.mux.android.distribution)
}

android {
  namespace = "com.mux.stats.sdk.muxstats.media3_exo"
  compileSdk = 37

  buildFeatures {
    buildConfig = true
  }

  defaultConfig {
    minSdk = 23

    // our deps almost blow the dex limit by themselves, media3 doc/examples all use multidex
    multiDexEnabled = true

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  flavorDimensions += "media3"
  productFlavors {
    // This module does not currently need different src sets for different media3 versions.
    // We still need to declare different flavors so we can create version-specific variants
    create("At_latest") { dimension = "media3" }
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
    create("at_1_11") {
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
    if (media3Variant.contains("at_latest", ignoreCase = true)) {
      "data-media3"
    } else {
      "data-media3-$media3Variant"
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
    moduleName = "Mux Data SDK for Media3, ExoPlayer"
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
  debugImplementation(project(":library"))

  //noinspection GradleDependency
  "at_1_0Api"(libs.media3.exoplayer.at10)
  //noinspection GradleDependency
  "at_1_0CompileOnly"(libs.media3.exoplayerHls.at10)

  //noinspection GradleDependency
  "at_1_1Api"(libs.media3.exoplayer.at11)
  //noinspection GradleDependency
  "at_1_1CompileOnly"(libs.media3.exoplayerHls.at11)

  //noinspection GradleDependency
  "at_1_2Api"(libs.media3.exoplayer.at12)
  //noinspection GradleDependency
  "at_1_2CompileOnly"(libs.media3.exoplayerHls.at12)

  //noinspection GradleDependency
  "at_1_3Api"(libs.media3.exoplayer.at13)
  //noinspection GradleDependency
  "at_1_3CompileOnly"(libs.media3.exoplayerHls.at13)

  //noinspection GradleDependency
  "at_1_4Api"(libs.media3.exoplayer.at14)
  //noinspection GradleDependency
  "at_1_4CompileOnly"(libs.media3.exoplayerHls.at14)

  //noinspection GradleDependency
  "at_1_5Api"(libs.media3.exoplayer.at15)
  //noinspection GradleDependency
  "at_1_5CompileOnly"(libs.media3.exoplayerHls.at15)

  //noinspection GradleDependency
  "at_1_6Api"(libs.media3.exoplayer.at16)
  //noinspection GradleDependency
  "at_1_6CompileOnly"(libs.media3.exoplayerHls.at16)

  //noinspection GradleDependency
  "at_1_8Api"(libs.media3.exoplayer.at18)
  //noinspection GradleDependency
  "at_1_8CompileOnly"(libs.media3.exoplayerHls.at18)

  //noinspection GradleDependency
  "at_1_9Api"(libs.media3.exoplayer.at19)
  //noinspection GradleDependency
  "at_1_9CompileOnly"(libs.media3.exoplayerHls.at19)

  //noinspection GradleDependency
  "at_1_10Api"(libs.media3.exoplayer.at110)
  //noinspection GradleDependency
  "at_1_10CompileOnly"(libs.media3.exoplayerHls.at110)

  //noinspection GradleDependency
  "at_1_11Api"(libs.media3.exoplayer.at111)
  //noinspection GradleDependency
  "at_1_11CompileOnly"(libs.media3.exoplayerHls.at111)

  //noinspection GradleDependency
  "At_latestApi"(libs.media3.exoplayer.atLatest)
  //noinspection GradleDependency
  "At_latestCompileOnly"(libs.media3.exoplayerHls.atLatest)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  androidTestImplementation(libs.androidx.test.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
}

// Release builds need maven coordinates to depend on :library from this project.
//  We only add it to release variants, matched to the same flavor. AGP exposes a
//  per-variant api configuration named "<variantName>Api" (e.g. at_1_4ReleaseApi) —
//  there's 'at_1_4Api'/'releaseApi' but no combined 'at_1_4ReleaseApi' source set, so
//  we target the variant configuration directly. The coordinate is wrapped in a
//  provider so project.version is read lazily, after muxDistribution has set it
//  (onVariants runs before the plugin's afterEvaluate).
androidComponents {
  onVariants { variant ->
    if (variant.buildType == "release") {
      val flavorName = variant.productFlavors.first().second
      val suffix = if (flavorName.contains("at_latest", ignoreCase = true)) {
        "" // 'at_latest' variant has no -at_X_X
      } else {
        "-$flavorName"
      }
      project.dependencies.add(
              "${variant.name}Api",
              project.provider { "com.mux.stats.sdk.muxstats:data-media3-custom$suffix:${project.version}" }
      )
    }
  }
}
