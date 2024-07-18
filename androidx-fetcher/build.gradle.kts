plugins {
  kotlin("jvm")
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(libs.moshi)
  implementation(libs.okio)
  implementation(libs.kotlin.coroutines)
  implementation(libs.kotlin.immutableCollections)

  compileOnly(libs.compose.stableMarker)

  implementation(platform(libs.okhttp.bom))
  implementation("com.squareup.okhttp3:okhttp")
  implementation("com.squareup.okhttp3:logging-interceptor")
  implementation("com.squareup.okhttp3:okhttp-coroutines")

  implementation(platform(libs.okhttp.bom))
  testImplementation("com.squareup.okhttp3:mockwebserver3-junit5")
  testImplementation(libs.test.kotlin.coroutines)
  testImplementation(kotlin("test-junit5"))
  testImplementation(libs.test.assertk)
}