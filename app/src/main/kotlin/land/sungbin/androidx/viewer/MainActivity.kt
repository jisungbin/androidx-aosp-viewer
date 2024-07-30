/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.viewer

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
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.lifecycleScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import land.sungbin.androidx.fetcher.AndroidxRepository
import land.sungbin.androidx.fetcher.AndroidxRepositoryReader
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.viewer.components.GHContentTopBar
import land.sungbin.androidx.viewer.components.GHContentTreeScreen
import land.sungbin.androidx.viewer.components.GHLoadingScreen
import land.sungbin.androidx.viewer.utils.GitHubFetchLoggingContext
import land.sungbin.androidx.viewer.utils.TimberLogger
import okio.FileSystem
import timber.log.Timber

class MainActivity : ComponentActivity() {
  private val ghLogin = GitHubLogin()
  private val accessToken by lazy {
    ghLogin.readAccessTokenFromStorage(applicationContext, FileSystem.SYSTEM)
  }

  private val repoLogger = TimberLogger("AndroidxRepository")
  private val repo by lazy { AndroidxRepository(logger = repoLogger) }
  private val repoReader by lazy { AndroidxRepositoryReader(logger = repoLogger) }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      var items by remember { mutableStateOf<ImmutableList<GitContent>>(persistentListOf()) }

      var settingSheetVisible by remember { mutableStateOf(false) }
      val settingSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

      LaunchedEffect(repo, repoReader) {
        launch(Dispatchers.IO + GitHubFetchLoggingContext) {
          val raw = repo.fetch() ?: return@launch
          items = repoReader.read(raw.source()).toImmutableList()
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
              onSettingClick = { settingSheetVisible = true },
              onCopyClick = {
                val path = items.fold(StringBuilder()) { acc, content ->
                  acc.append(content.path).append('/')
                }.toString()
                copy(path)
              },
            )
          },
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
                lifecycleScope.launch(Dispatchers.IO + GitHubFetchLoggingContext) {
                  val raw = repo.fetch(content.url.substringAfterLast('/')) ?: run {
                    // TODO show error results
                    return@launch
                  }
                  items = repoReader.read(raw.source(), parent).toImmutableList()
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
      lifecycleScope.launch(Dispatchers.IO + GitHubFetchLoggingContext) {
        val token = ghLogin.requestAccessTokenFromRedirectUri(redirectUri)
        Timber.d("GitHub AccessToken: %s", token)

        token.getOrNull()?.let {
          ghLogin.writeAccessTokenToStorage(applicationContext, FileSystem.SYSTEM, it)
        }
      }
    }
  }

  private fun copy(text: String) {
    TODO()
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
