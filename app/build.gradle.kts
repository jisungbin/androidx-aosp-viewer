// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
import java.util.Properties
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag

plugins {
  id("com.android.application")
  kotlin("android")
  kotlin("plugin.compose")
  id("kotlin-parcelize")
  id(libs.plugins.kotlin.ksp.get().pluginId)
  id(libs.plugins.kotlin.poko.get().pluginId)
}

val secrets = Properties().apply {
  val secrets = rootProject.file("secrets.properties")
  if (secrets.exists()) secrets.reader().use(::load)
}

android {
  namespace = "land.sungbin.androidx.viewer"
  compileSdk = 35

  defaultConfig {
    minSdk = 24
    targetSdk = 35

    buildConfigField("String", "GH_ID", "\"${secrets["gh-id"]}\"")
    buildConfigField("String", "GH_SECRET", "\"${secrets["gh-secret"]}\"")
  }

  buildFeatures {
    buildConfig = true
  }

  compileOptions {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jdk.get().toInt())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jdk.get().toInt())
  }

  sourceSets {
    getByName("main").java.srcDir("src/main/kotlin")
  }

  packaging {
    resources {
      excludes.add("**/*.kotlin_builtins")
    }
  }
}

kotlin {
  jvmToolchain(libs.versions.jdk.get().toInt())
  compilerOptions {
    optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
  }
  sourceSets.all {
    languageSettings.enableLanguageFeature("ExplicitBackingFields")
  }
}

ksp {
  // arg("me.tatarka.inject.dumpGraph", "true")
  arg("circuit.codegen.mode", "kotlin_inject_anvil")
}

composeCompiler {
  featureFlags = setOf(
    ComposeFeatureFlag.OptimizeNonSkippingGroups,
    ComposeFeatureFlag.PausableComposition,
  )
}

dependencies {
  implementation(projects.androidxFetcher)
  implementation(projects.thirdparty.timber)

  implementation(libs.androidx.activity)
  implementation(libs.androidx.datastore)

  implementation(libs.compose.activity)
  implementation(libs.compose.material3)
  implementation(libs.compose.lottie)

  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.immutableCollections)

  implementation(libs.circuit)
  implementation(libs.circuit.codegen)
  ksp(libs.circuit.codegen.ksp)

  implementation(libs.kotlininject)
  implementation(libs.kotlininject.anvil)
  implementation(libs.kotlininject.anvil.scopes)
  ksp(libs.kotlininject.ksp)
  ksp(libs.kotlininject.anvil.ksp)

  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.coroutines)
  implementation(libs.okhttp.logging)
  implementation(libs.okio)

  implementation(libs.moshi)
  debugImplementation(libs.leakcanary)

  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.test.assertk)
  testImplementation(libs.test.kotlin.coroutines)
}
