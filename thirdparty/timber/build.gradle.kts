// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
plugins {
  kotlin("jvm")
}

kotlin {
  explicitApi()
  jvmToolchain(libs.versions.jdk.get().toInt())
}
