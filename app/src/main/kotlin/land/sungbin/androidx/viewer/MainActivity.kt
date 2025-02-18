// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.slack.circuit.backstack.rememberSaveableBackStack
import com.slack.circuit.foundation.CircuitCompositionLocals
import com.slack.circuit.foundation.NavigableCircuitContent
import com.slack.circuit.foundation.rememberCircuitNavigator
import com.slack.circuit.runtime.screen.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import land.sungbin.androidx.viewer.di.KotlinInjectAppComponent
import land.sungbin.androidx.viewer.di.create
import land.sungbin.androidx.viewer.screen.CodeScreen
import land.sungbin.androidx.viewer.screen.NoteScreen
import land.sungbin.androidx.viewer.screen.SettingScreen
import land.sungbin.androidx.viewer.ui.LocalSnackbarHost

private data class NavigationBarItemData(
  val screen: Screen,
  @StringRes val label: Int,
  @DrawableRes val selectedIcon: Int,
  @DrawableRes val unselectedIcon: Int,
) {
  companion object {
    val Defaults = listOf(
      NavigationBarItemData(
        CodeScreen,
        R.string.screen_code,
        R.drawable.ic_fill_code_block_24,
        R.drawable.ic_outline_code_block_24,
      ),
      NavigationBarItemData(
        NoteScreen,
        R.string.screen_note,
        R.drawable.ic_fill_sticky_note_24,
        R.drawable.ic_outline_sticky_note_24,
      ),
      NavigationBarItemData(
        SettingScreen,
        R.string.screen_setting,
        R.drawable.ic_fill_settings_24,
        R.drawable.ic_outline_settings_24,
      ),
    )
  }
}

class MainActivity : ComponentActivity() {
  private val apps by lazy { KotlinInjectAppComponent::class.create() }
  private val circuit by lazy {
    apps.circuit
      .newBuilder()
      .addPresenter<SettingScreen, SettingScreen.State>(apps.settingScreenPresenter(dataStore))
      .build()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      CircuitCompositionLocals(circuit) {
        val backStack = rememberSaveableBackStack(CodeScreen)
        val navigator = rememberCircuitNavigator(backStack)
        val snackbarHost = remember { SnackbarHostState() }

        LaunchedEffect(Unit) {
          TimberAppTree.exceptions.consumeEach { exception ->
            val message = exception.message ?: exception.cause?.message
            snackbarHost.showSnackbar(message ?: getString(R.string.error_unknown, exception::class.simpleName ?: "null"))
          }
        }

        MaterialTheme(colorScheme = dynamicThemeScheme()) {
          CompositionLocalProvider(
            LocalSnackbarHost provides snackbarHost,
            LocalActivity provides this,
          ) {
            Scaffold(
              modifier = Modifier.fillMaxSize(),
              bottomBar = {
                NavigationBar(modifier = Modifier.fillMaxWidth()) {
                  NavigationBarItemData.Defaults.forEach { item ->
                    val selected = backStack.topRecord?.screen == item.screen
                    NavigationBarItem(
                      selected = selected,
                      onClick = { navigator.goTo(item.screen) },
                      icon = {
                        Icon(
                          painterResource(if (selected) item.selectedIcon else item.unselectedIcon),
                          "${if (selected) "Selected" else "Unselected"} ${item.screen}",
                        )
                      },
                      label = { Text(stringResource(item.label)) },
                    )
                  }
                }
              },
              snackbarHost = { SnackbarHost(snackbarHost) },
            ) { padding ->
              NavigableCircuitContent(
                navigator,
                backStack,
                modifier = Modifier
                  .fillMaxSize()
                  .padding(padding),
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val redirectUri = intent.data ?: return
    if (
      redirectUri.scheme == GitHubLogin.LOGIN_URI_SCHEME &&
      redirectUri.host == GitHubLogin.LOGIN_URI_HOST
    ) {
      lifecycleScope.launch(Dispatchers.IO) {
        apps.ghLogin.resumeWithAccessToken(redirectUri)
      }
    }
  }

  companion object {
    private const val SETTINGS_PREFERENCES_NAME = "settings"

    val Context.dataStore by preferencesDataStore(SETTINGS_PREFERENCES_NAME)
  }
}

@[Composable ReadOnlyComposable]
private fun dynamicThemeScheme(darkTheme: Boolean = isSystemInDarkTheme()): ColorScheme =
  when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> darkColorScheme()
    else -> lightColorScheme()
  }
