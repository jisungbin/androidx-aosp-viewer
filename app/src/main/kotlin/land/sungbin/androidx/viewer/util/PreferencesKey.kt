package land.sungbin.androidx.viewer.util

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKey {
  val GHAccessToken = stringPreferencesKey("gh_access_token")
  val GHLoginDate = longPreferencesKey("gh_login_date")

  val FontSize = intPreferencesKey("font_size")
  val MaxCacheSize = longPreferencesKey("max_cache_size") // MB unit
}

@Suppress("ConstPropertyName")
object PreferenceDefaults {
  const val FontSize = 13
  const val MaxCacheSize = 5L
}
