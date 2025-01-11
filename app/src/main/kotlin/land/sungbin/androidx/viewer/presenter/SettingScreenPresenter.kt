// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.presenter

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.node.Ref
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import land.sungbin.androidx.viewer.GitHubLogin
import land.sungbin.androidx.viewer.R
import land.sungbin.androidx.viewer.screen.LocalSnackbarHost
import land.sungbin.androidx.viewer.screen.SettingScreen
import land.sungbin.androidx.viewer.screen.SettingScreen.Event.ToggleGitHubLogin
import land.sungbin.androidx.viewer.screen.SettingScreen.Event.UpdatePreferences
import land.sungbin.androidx.viewer.util.PreferenceDefaults
import land.sungbin.androidx.viewer.util.PreferencesKey
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@Inject class SettingScreenPresenter(
  private val ghLogin: GitHubLogin,
  @Assisted private val dataStore: DataStore<Preferences>,
  @Assisted private val host: Activity,
) : Presenter<SettingScreen.State> {
  @Composable override fun present(): SettingScreen.State {
    val scope = rememberCoroutineScope()
    val snackbarHost = LocalSnackbarHost.current

    var fontSize by rememberRetained { mutableIntStateOf(PreferenceDefaults.FontSize) }
    var maxCacheSize by rememberRetained { mutableLongStateOf(PreferenceDefaults.MaxCacheSize) }
    var ghLoginDate by rememberRetained { mutableLongStateOf(GitHubLogin.LOGOUT_FLAG) }
    val ghToken = rememberRetained { Ref<String>() }

    DisposableEffect(dataStore) {
      scope.launch(Dispatchers.IO) {
        dataStore.data.collect { preferences ->
          fontSize = preferences[PreferencesKey.FontSize] ?: PreferenceDefaults.FontSize
          maxCacheSize = preferences[PreferencesKey.MaxCacheSize] ?: PreferenceDefaults.MaxCacheSize
          ghLoginDate = preferences[PreferencesKey.GHLoginDate] ?: GitHubLogin.LOGOUT_FLAG
          ghToken.value = preferences[PreferencesKey.GHAccessToken]
        }
      }

      onDispose {
        scope.launch(Dispatchers.IO) {
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
          ghLoginDate = event.ghLoginDate ?: ghLoginDate
        }
        is ToggleGitHubLogin -> {
          if (ghToken.value == null) {
            if (ghLogin.canLogin()) {
              ghLogin.login(host)
            } else {
              scope.launch {
                snackbarHost.showSnackbar(host.getString(R.string.gh_cannot_login))
              }
            }
          } else {
            scope.launch(Dispatchers.IO) {
              dataStore.edit { preferences ->
                preferences.remove(PreferencesKey.GHAccessToken)
                preferences.remove(PreferencesKey.GHLoginDate)
              }
            }
          }
        }
      }
    }
  }
}
