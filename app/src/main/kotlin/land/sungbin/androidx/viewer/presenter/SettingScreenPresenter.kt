// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import land.sungbin.androidx.viewer.GitHubLogin
import land.sungbin.androidx.viewer.screen.SettingScreen
import land.sungbin.androidx.viewer.screen.SettingScreen.Event.ToggleGitHubLogin
import land.sungbin.androidx.viewer.screen.SettingScreen.Event.UpdatePreferences
import land.sungbin.androidx.viewer.util.PreferenceDefaults
import land.sungbin.androidx.viewer.util.PreferencesKey
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import thirdparty.Timber

@Inject class SettingScreenPresenter(
  @Assisted private val dataStore: DataStore<Preferences>,
  private val ghLogin: GitHubLogin,
) : Presenter<SettingScreen.State> {
  @Composable override fun present(): SettingScreen.State {
    val scope = rememberCoroutineScope()

    var fontSize by rememberRetained { mutableIntStateOf(PreferenceDefaults.FontSize) }
    var maxCacheSize by rememberRetained { mutableLongStateOf(PreferenceDefaults.MaxCacheSize) }
    var ghLoginDate by rememberRetained { mutableLongStateOf(GitHubLogin.LOGOUT_FLAG_DATE) }

    DisposableEffect(dataStore) {
      scope.launch {
        dataStore.data.first().let { preferences ->
          fontSize = preferences[PreferencesKey.FontSize] ?: PreferenceDefaults.FontSize
          maxCacheSize = preferences[PreferencesKey.MaxCacheSize] ?: PreferenceDefaults.MaxCacheSize
          ghLoginDate = preferences[PreferencesKey.GHLoginDate] ?: GitHubLogin.LOGOUT_FLAG_DATE
        }
      }

      onDispose {
        scope.launch {
          dataStore.edit { preferences ->
            preferences[PreferencesKey.FontSize] = fontSize
            preferences[PreferencesKey.MaxCacheSize] = maxCacheSize
            preferences[PreferencesKey.GHLoginDate] = ghLoginDate
          }
        }
      }
    }

    return SettingScreen.State(fontSize, maxCacheSize, ghLoginDate) { event ->
      when (event) {
        is UpdatePreferences -> {
          fontSize = event.fontSize ?: fontSize
          maxCacheSize = event.maxCacheSize ?: maxCacheSize
        }
        is ToggleGitHubLogin -> {
          if (ghLoginDate == GitHubLogin.LOGOUT_FLAG_DATE) {
            scope.launch {
              ghLogin.login(event.windowHost)
                .onSuccess { token ->
                  Timber.d("GitHub AccessToken: %s", token)

                  dataStore.edit { preferences ->
                    preferences[PreferencesKey.GHAccessToken] = token
                    ghLoginDate = System.currentTimeMillis()
                  }
                }
                .onFailure { exception ->
                  Timber.e(exception, exception.message)
                }
            }
          } else {
            scope.launch {
              dataStore.edit { preferences ->
                preferences.remove(PreferencesKey.GHAccessToken)
                ghLoginDate = GitHubLogin.LOGOUT_FLAG_DATE
              }
            }
          }
        }
      }
    }
  }
}
