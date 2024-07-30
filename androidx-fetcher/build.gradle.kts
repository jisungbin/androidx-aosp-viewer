/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */
plugins {
  kotlin("jvm")
}

kotlin {
  explicitApi()
  compilerOptions {
    optIn.add("okhttp3.ExperimentalOkHttpApi")
  }
  sourceSets.all {
    languageSettings.enableLanguageFeature("ContextReceivers")
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
  implementation("com.squareup.okhttp3:okhttp")
  implementation("com.squareup.okhttp3:okhttp-coroutines")
  implementation("com.squareup.okhttp3:logging-interceptor")

  implementation(platform(libs.okhttp.bom))
  testImplementation("com.squareup.okhttp3:mockwebserver3-junit5")
  testImplementation(libs.test.okio.fs)
  testImplementation(libs.test.kotlin.coroutines)
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.test.assertk)
}
