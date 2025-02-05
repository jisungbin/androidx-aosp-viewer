// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ComposableNaming")

package land.sungbin.androidx.viewer.presenter

import android.content.Context
import androidx.annotation.NonUiContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import land.sungbin.androidx.fetcher.AndroidxRepository
import land.sungbin.androidx.fetcher.AndroidxRepositoryCache
import land.sungbin.androidx.fetcher.AndroidxRepositoryReader
import land.sungbin.androidx.fetcher.GitContent
import land.sungbin.androidx.fetcher.GitHubAuthenticateException
import land.sungbin.androidx.fetcher.sha
import land.sungbin.androidx.viewer.MainActivity.Companion.dataStore
import land.sungbin.androidx.viewer.R
import land.sungbin.androidx.viewer.presenter.CodeScreenPresenter.Companion.ANDROIDX_REPO_CACHE_DIR
import land.sungbin.androidx.viewer.screen.CodeScreen
import land.sungbin.androidx.viewer.screen.assignAsBlob
import land.sungbin.androidx.viewer.screen.assignAsTree
import land.sungbin.androidx.viewer.util.PreferenceDefaults
import land.sungbin.androidx.viewer.util.PreferencesKey
import land.sungbin.androidx.viewer.util.StringResolver
import land.sungbin.androidx.viewer.util.runSuspendCatching
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toOkioPath
import thirdparty.Timber

@Inject class CodeScreenPresenter(
  @Assisted private val repoReader: AndroidxRepositoryReader,
  private val sharedState: CodeScreen.SharedState,
) : Presenter<CodeScreen.State> {
  private val hasGHAccessToken
    get() = repoReader.repo.hasGHAccessToken

  suspend fun assigningFetch(
    ref: String = AndroidxRepository.HOME_REF,
    parent: GitContent? = null,
    noCache: Boolean = false,
    stringResolver: StringResolver? = null,
  ): Result<Unit> =
    runSuspendCatching { repoReader.repo.fetchTree(ref, noCache) }
      .mapCatching { source -> sharedState.assignAsTree(repoReader.readTree(source, parent, noCache)) }
      .recoverCatching { exception ->
        Timber.e(exception, "Failed to fetch the content.")

        if (exception is GitHubAuthenticateException) {
          val ghMessageRes = if (hasGHAccessToken) R.string.gh_fetch_failed_authenticate_expired else R.string.gh_fetch_failed_authenticate_needed
          val message = stringResolver?.getString(ghMessageRes) ?: exception.message ?: "GitHub authentication failed."
          throw IllegalStateException(message, exception)
        } else {
          val message = exception.message ?: stringResolver?.getString(R.string.gh_fetch_failed) ?: "Failed to fetch the content."
          throw IllegalStateException(message, exception)
        }
      }

  @Composable override fun present(): CodeScreen.State {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
      assigningFetch(stringResolver = context::getString)
    }

    return CodeScreen.State(item = sharedState.state.value) { event ->
      when (event) {
        is CodeScreen.Event.Fetch -> {
          scope.launch {
            assigningFetch(
              ref = event.parent?.sha ?: AndroidxRepository.HOME_REF,
              parent = event.parent,
              noCache = event.noCache,
              stringResolver = context::getString,
            )
          }
        }
        is CodeScreen.Event.OpenBlob -> {
          scope.launch {
            val raw = repoReader.readBlob(event.content.url, event.noCache)
            sharedState.assignAsBlob(raw.utf8(), event.content)
          }
        }
        is CodeScreen.Event.ToggleFavorite -> TODO()
      }
    }
  }

  companion object {
    const val ANDROIDX_REPO_CACHE_DIR = "androidx-repo"
  }
}

suspend fun AndroidxRepositoryReader(@NonUiContext context: Context): AndroidxRepositoryReader {
  val preferences = context.dataStore.data.first()

  val ghAccessToken = preferences[PreferencesKey.GHAccessToken]
  val ghHttpLogLevel = preferences[PreferencesKey.GHHttpLogLevel] ?: PreferenceDefaults.GHHttpLogLevel
  val maxCacheSize = (preferences[PreferencesKey.MaxCacheSize] ?: PreferenceDefaults.MaxCacheSize) * 1000 * 1000 // MB to Byte

  val cache = AndroidxRepositoryCache(
    context.cacheDir.resolve(ANDROIDX_REPO_CACHE_DIR).toOkioPath(),
    maxCacheSize,
  )
  val repo = AndroidxRepository(
    ghAccessToken,
    cache,
    HttpLoggingInterceptor.Level.entries[ghHttpLogLevel],
  )

  return AndroidxRepositoryReader(repo)
}
