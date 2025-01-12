// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("jvm")
}

kotlin {
  jvmToolchain(libs.versions.jdk.get().toInt())
}

dependencies {
  implementation(libs.test.assertk)

  implementation(platform(libs.okhttp.bom))
  implementation(libs.okhttp.core)
}
