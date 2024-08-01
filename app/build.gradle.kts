/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */
import java.util.Properties

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("plugin.compose")
}

val secrets = Properties().apply {
  val secrets = rootProject.file("secrets.properties")
  if (secrets.exists()) secrets.reader().use(::load)
}

android {
  namespace = "land.sungbin.androidx.viewer"
  compileSdk = 34

  defaultConfig {
    minSdk = 23
    targetSdk = 34

    buildConfigField("String", "GH_ID", "\"${secrets["gh-id"]}\"")
    buildConfigField("String", "GH_SECRET", "\"${secrets["gh-secret"]}\"")
  }

  buildFeatures {
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
  }

  packaging {
    resources {
      excludes.add("**/*.kotlin_builtins")
    }
  }

  lint {
    disable += "ModifierParameter"
  }
}

kotlin {
  compilerOptions {
    optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
  }
  sourceSets.all {
    languageSettings.enableLanguageFeature("ContextReceivers")
    languageSettings.enableLanguageFeature("ExplicitBackingFields")
  }
}

composeCompiler {
  enableStrongSkippingMode = true
  enableNonSkippingGroupOptimization = true
}

dependencies {
  implementation(projects.androidxFetcher)

  implementation(libs.androidx.activity)
  implementation(libs.androidx.datastore)

  implementation(libs.compose.activity)
  implementation(libs.compose.material3)

  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.immutableCollections)

  implementation(platform(libs.okhttp.bom))
  implementation("com.squareup.okhttp3:okhttp")
  implementation("com.squareup.okhttp3:okhttp-coroutines")
  implementation("com.squareup.okhttp3:logging-interceptor")
  implementation(libs.okio)
  implementation(libs.moshi)

  implementation(libs.timber)

  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.test.kotlin.coroutines)
}
