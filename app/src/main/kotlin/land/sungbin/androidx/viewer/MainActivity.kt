// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.lifecycle.lifecycleScope
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import land.sungbin.androidx.fetcher.AndroidxRepository
import land.sungbin.androidx.fetcher.AndroidxRepositoryReader
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.fetcher.GitHubAuthorizationContext
import land.sungbin.androidx.fetcher.RemoteCachingContext
import land.sungbin.androidx.viewer.components.GHContentTopBar
import land.sungbin.androidx.viewer.components.GHContentTreeScreen
import land.sungbin.androidx.viewer.components.GHLoadingScreen
import land.sungbin.androidx.viewer.components.GHSettingSheetContent
import land.sungbin.androidx.fetcher.GitHubAuthenticateException
import land.sungbin.androidx.viewer.preferences.PreferencesKey
import land.sungbin.androidx.viewer.utils.GitHubFetchCachingContext
import land.sungbin.androidx.viewer.utils.GitHubFetchLoggingContext
import land.sungbin.androidx.viewer.utils.TimberLogger
import land.sungbin.androidx.viewer.utils.runSuspendCatching
import timber.log.Timber

class MainActivity : ComponentActivity() {
  private val dataStore by lazy {
    PreferenceDataStoreFactory.create {
      applicationContext.preferencesDataStoreFile("settings")
    }
  }

  private val ghLogin = GitHubLogin()

  private val repoLogger = TimberLogger("AndroidxRepository")
  private val repo = AndroidxRepository(logger = repoLogger)
  private val repoReader = AndroidxRepositoryReader(logger = repoLogger)

  private var ghCachingContext: RemoteCachingContext? by mutableStateOf(null)
  private var ghTokenContext: GitHubAuthorizationContext? by mutableStateOf(null)

  private val ghFetchingContexts
    get() = Dispatchers.IO
      .plus(GitHubFetchLoggingContext)
      .plus(ghCachingContext ?: EmptyCoroutineContext)
      .plus(ghTokenContext ?: EmptyCoroutineContext)

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      var items by remember { mutableStateOf<ImmutableList<GitContent>>(persistentListOf()) }

      var settingSheetVisible by remember { mutableStateOf(false) }
      val settingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

      val snackbarHostState = remember { SnackbarHostState() }

      LaunchedEffect(dataStore) {
        dataStore.data.collect { preferences ->
          val maxCacheSize = preferences[PreferencesKey.WithDefault.maxCacheSize.key]
          val ghToken = preferences[PreferencesKey.ghAccessToken]

          ghCachingContext = GitHubFetchCachingContext(
            context = applicationContext,
            maxSizeInMB = maxCacheSize ?: PreferencesKey.WithDefault.maxCacheSize.default,
          )
          ghTokenContext = GitHubAuthorizationContext(ghToken)
        }
      }

      LaunchedEffect(repo, repoReader, ghCachingContext, ghTokenContext) {
        val cachingContext = ghCachingContext ?: return@LaunchedEffect
        val tokenContext = ghTokenContext ?: return@LaunchedEffect
        launch(Dispatchers.IO + GitHubFetchLoggingContext + cachingContext + tokenContext) {
          ghFetch(
            snackbarHostState = snackbarHostState,
            onSuccess = { contents -> items = contents.toImmutableList() },
          )
        }
      }

      MaterialTheme(colorScheme = dynamicThemeScheme()) {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            GHContentTopBar(
              modifier = Modifier.fillMaxWidth(),
              contents = items,
              canNavigateBack = remember(items) { items.firstOrNull()?.parent != null },
              onBackClick = { items = items.first().parent!! },
              onRefreshClick = {
                val parent = items.first().parent?.toMutableList()?.toImmutableList() // copy original parent
                items = persistentListOf() // make loading screen
                lifecycleScope.launch(ghFetchingContexts) {
                  ghFetch(
                    ref = items.first().parent?.first()?.url?.substringAfterLast('/') ?: AndroidxRepository.HOME_REF,
                    parent = parent,
                    snackbarHostState = snackbarHostState,
                    onSuccess = { contents -> items = contents.toImmutableList() },
                  )
                }
              },
              onSettingClick = { settingSheetVisible = true },
              onCopyClick = { copyGHLink(items) },
            )
          },
          snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
          if (items.isEmpty()) {
            GHLoadingScreen(
              modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            )
          } else {
            GHContentTreeScreen(
              contents = items,
              modifier = Modifier
                .fillMaxSize()
                .padding(padding),
              onContentClick = { content ->
                val parent = items.toMutableList().toImmutableList() // copy original list
                items = persistentListOf() // make loading screen
                lifecycleScope.launch(ghFetchingContexts) {
                  ghFetch(
                    ref = content.url.substringAfterLast('/'),
                    parent = parent,
                    snackbarHostState = snackbarHostState,
                    onSuccess = { contents -> items = contents.toImmutableList() },
                  )
                }
              },
            )
          }
        }

        if (settingSheetVisible) {
          ModalBottomSheet(
            modifier = Modifier.fillMaxWidth(),
            sheetState = settingSheetState,
            onDismissRequest = { settingSheetVisible = false },
            dragHandle = null,
          ) {
            GHSettingSheetContent(
              modifier = Modifier.fillMaxWidth(),
              dataStore = dataStore,
              onLoginToggleClick = {
                lifecycleScope.launch {
                  if (ghTokenContext?.token != null) {
                    ghLogout()
                  } else if (ghLogin.canLogin()) {
                    ghLogin.login(this@MainActivity)
                  } else {
                    snackbarHostState.showSnackbar(getString(R.string.gh_cannot_login))
                  }
                }
              },
            )
          }
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
            preferences[PreferencesKey.ghAccessToken] = token
            preferences[PreferencesKey.ghLoginDate] = System.currentTimeMillis()
          }
        }
      }
    }
  }

  private suspend fun ghFetch(
    snackbarHostState: SnackbarHostState,
    ref: String = AndroidxRepository.HOME_REF,
    parent: ImmutableList<GitContent>? = null,
    noCache: Boolean = false,
    onSuccess: (contents: List<GitContent>) -> Unit,
  ) {
    runSuspendCatching { repo.fetch(ref) }
      .mapCatching { source -> onSuccess(repoReader.read(source, parent, noCache)) }
      .onFailure { exception ->
        Timber.e(exception, "Failed to fetch the content.")

        if (exception is GitHubAuthenticateException) {
          snackbarHostState.showSnackbar(getString(R.string.gh_fetch_failed_authenticate))
        } else {
          snackbarHostState.showSnackbar(exception.message ?: getString(R.string.gh_fetch_failed))
        }
      }
  }

  private suspend fun ghLogout() {
    dataStore.edit { preferences ->
      preferences.remove(PreferencesKey.ghAccessToken)
      preferences.remove(PreferencesKey.ghLoginDate)
    }
  }

  private fun copyGHLink(items: List<GitContent>) {
    val parents = items.first().wholeParentPaths().orEmpty()
    val current = items.first().path

    val clipboard = getSystemService(ClipboardManager::class.java)
    val clip = ClipData.newPlainText("GitHub path", "$parents/$current")

    clipboard.setPrimaryClip(clip)
  }
}

@Composable @ReadOnlyComposable
private fun dynamicThemeScheme(darkTheme: Boolean = isSystemInDarkTheme()): ColorScheme =
  when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> darkColorScheme()
    else -> lightColorScheme()
  }
