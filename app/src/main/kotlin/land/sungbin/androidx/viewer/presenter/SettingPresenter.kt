package land.sungbin.androidx.viewer.presenter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.node.Ref
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import land.sungbin.androidx.viewer.GitHubLogin
import land.sungbin.androidx.viewer.MainActivity
import land.sungbin.androidx.viewer.R
import land.sungbin.androidx.viewer.overlay.SnackbarOverlay
import land.sungbin.androidx.viewer.screen.SettingEvent.UpdatePreferences
import land.sungbin.androidx.viewer.screen.SettingEvent.ToggleGitHubLogin
import land.sungbin.androidx.viewer.screen.SettingScreen
import land.sungbin.androidx.viewer.screen.SettingState
import land.sungbin.androidx.viewer.util.PreferenceDefaults
import land.sungbin.androidx.viewer.util.PreferencesKey
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

@SingleIn(AppScope::class)
@Inject class SettingPresenter(
  @Assisted private val dataStore: DataStore<Preferences>,
  @Assisted private val ghLogin: GitHubLogin,
  @Assisted private val host: MainActivity,
) : Presenter<SettingState> {
  @Composable override fun present(): SettingState {
    val scope = rememberCoroutineScope()
    val overlays = LocalOverlayHost.current

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

    return SettingState(fontSize, maxCacheSize, ghLoginDate) { event ->
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
                overlays.show(SnackbarOverlay(host.getString(R.string.gh_cannot_login)))
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
