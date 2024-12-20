// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("jvm")
}

kotlin {
  explicitApi()
  jvmToolchain(libs.versions.jdk.get().toInt())
  compilerOptions {
    optIn.add("okhttp3.ExperimentalOkHttpApi")
  }
  sourceSets.all {
    languageSettings.enableLanguageFeature("ExplicitBackingFields")
  }
}

dependencies {
  implementation(libs.moshi)
  implementation(libs.okio)
  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.immutableCollections)

  compileOnly(libs.compose.stableMarker)

  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.coroutines)
  implementation(libs.okhttp.logging)

  testImplementation(kotlin("test-junit5"))
  testImplementation(platform(libs.okhttp.bom))
  testImplementation(libs.test.okhttp.mockwebserver)
  testImplementation(libs.test.okio.fs)
  testImplementation(libs.test.kotlin.coroutines)
  testImplementation(libs.test.assertk)
}
