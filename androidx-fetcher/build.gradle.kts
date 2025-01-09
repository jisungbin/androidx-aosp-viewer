// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("jvm")
  id(libs.plugins.kotlin.ksp.get().pluginId)
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
  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.immutableCollections)

  compileOnly(libs.compose.stableMarker)

  implementation(libs.kotlininject)
  implementation(libs.kotlininject.anvil)
  implementation(libs.kotlininject.anvil.scopes)
  ksp(libs.kotlininject.ksp)
  ksp(libs.kotlininject.anvil.ksp)

  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp.core)
  implementation(libs.okhttp.coroutines)
  implementation(libs.okhttp.logging)

  implementation(libs.moshi)
  implementation(libs.okio)

  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.test.assertk)
  testImplementation(libs.test.kotlin.coroutines)

  testImplementation(platform(libs.okhttp.bom))
  testImplementation(libs.test.okhttp.mockwebserver)
  testImplementation(libs.test.okio.fakefilesystem)
}
