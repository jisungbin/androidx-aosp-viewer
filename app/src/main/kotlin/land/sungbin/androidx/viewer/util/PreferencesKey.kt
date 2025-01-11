// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.util

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import okhttp3.logging.HttpLoggingInterceptor

object PreferencesKey {
  val GHAccessToken = stringPreferencesKey("gh_access_token")
  val GHLoginDate = longPreferencesKey("gh_login_date")
  val GHHttpLogLevel = intPreferencesKey("gh_http_log_level")

  val FontSize = intPreferencesKey("font_size")
  val MaxCacheSize = longPreferencesKey("max_cache_size") // MB unit
}

@Suppress("ConstPropertyName")
object PreferenceDefaults {
  val GHHttpLogLevel = HttpLoggingInterceptor.Level.BASIC.ordinal
  const val FontSize = 13
  const val MaxCacheSize = 5L
}
