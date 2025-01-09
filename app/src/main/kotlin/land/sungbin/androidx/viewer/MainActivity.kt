// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import land.sungbin.androidx.viewer.util.PreferencesKey
import thirdparty.Timber

class MainActivity : ComponentActivity() {
  private val dataStore by lazy {
    PreferenceDataStoreFactory.create {
      applicationContext.preferencesDataStoreFile("settings")
    }
  }
  private val ghLogin = GitHubLogin()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      MaterialTheme(colorScheme = dynamicThemeScheme()) {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
//            GHContentTopBar(
//              modifier = Modifier.fillMaxWidth(),
//              content = items,
//              onBackClick = { items = items.first().parent!! },
//              onRefresh = {
//                val parent = items.first().parent?.toMutableList()?.toImmutableList() // copy original parent
//                items = persistentListOf() // make loading screen
//                lifecycleScope.launch(ghFetchingContexts) {
//                  ghFetch(
//                    ref = items.first().parent?.first()?.url?.substringAfterLast('/') ?: AndroidxRepository.HOME_REF,
//                    parent = parent,
//                    snackbarHostState = snackbarHostState,
//                    onSuccess = { contents -> items = contents.toImmutableList() },
//                  )
//                }
//              },
//              onSettingClick = { settingSheetVisible = true },
//              onCopy = { copyGHLink(items) },
//            )
          },
        ) { padding ->
          print(padding)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    val redirectUri = intent.data ?: return
    if (redirectUri.scheme == "androidx-aosp-viewer" && redirectUri.host == "github-login") {
      lifecycleScope.launch(Dispatchers.IO) {
        val result = ghLogin.requestAccessTokenFromRedirectUri(redirectUri)
        Timber.d("GitHub AccessToken: %s", result)

        result.getOrNull()?.let { token ->
          dataStore.edit { preferences ->
            preferences[PreferencesKey.GHAccessToken] = token
            preferences[PreferencesKey.GHLoginDate] = System.currentTimeMillis()
          }
        }
      }
    }
  }

//  private suspend fun ghFetch(
//    snackbarHostState: SnackbarHostState,
//    ref: String = AndroidxRepository.HOME_REF,
//    parent: ImmutableList<GitContent>? = null,
//    noCache: Boolean = false,
//    onSuccess: (contents: List<GitContent>) -> Unit,
//  ) {
//    runSuspendCatching { repo.fetch(ref) }
//      .mapCatching { source -> onSuccess(repoReader.read(source, parent, noCache)) }
//      .onFailure { exception ->
//        Timber.e(exception, "Failed to fetch the content.")
//
//        if (exception is GitHubAuthenticateException) {
//          snackbarHostState.showSnackbar(getString(R.string.gh_fetch_failed_authenticate))
//        } else {
//          snackbarHostState.showSnackbar(exception.message ?: getString(R.string.gh_fetch_failed))
//        }
//      }
//  }

//  private fun copyGHLink(items: List<GitContent>) {
//    val parents = items.first().wholeParentPaths().orEmpty()
//    val current = items.first().path
//
//    val clipboard = getSystemService(ClipboardManager::class.java)
//    val clip = ClipData.newPlainText("GitHub path", "$parents/$current")
//
//    clipboard.setPrimaryClip(clip)
//  }
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
