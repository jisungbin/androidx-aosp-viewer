/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

sealed class PreferencesKey<T>(val key: Preferences.Key<T>) {
  companion object {
    // TODO is this good for security? Probably NO.
    val ghAccessToken = stringPreferencesKey("gh_access_token")
    val ghLoginDate = longPreferencesKey("gh_login_date")
  }

  class WithDefault<T>(key: Preferences.Key<T>, val default: T) : PreferencesKey<T>(key) {
    companion object {
      private infix fun <T> Preferences.Key<T>.defaults(value: T) = WithDefault(this, value)

      val fontSize = intPreferencesKey("font_size") defaults 13
      val maxCacheSize = longPreferencesKey("max_cache_size") defaults 50_000_000 // 50 MB
    }
  }
}
