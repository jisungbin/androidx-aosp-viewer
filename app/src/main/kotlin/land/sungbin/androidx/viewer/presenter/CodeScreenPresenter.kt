// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ComposableNaming")

package land.sungbin.androidx.viewer.presenter

import android.content.Context
import androidx.annotation.NonUiContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.retained.rememberRetained
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import land.sungbin.androidx.fetcher.AndroidxRepository
import land.sungbin.androidx.fetcher.AndroidxRepositoryCache
import land.sungbin.androidx.fetcher.AndroidxRepositoryReader
import land.sungbin.androidx.fetcher.AndroidxRepositoryTree
import land.sungbin.androidx.fetcher.GitHubAuthenticateException
import land.sungbin.androidx.fetcher.GitItem
import land.sungbin.androidx.fetcher.sha
import land.sungbin.androidx.viewer.App
import land.sungbin.androidx.viewer.MainActivity.Companion.dataStore
import land.sungbin.androidx.viewer.R
import land.sungbin.androidx.viewer.presenter.CodeScreenPresenter.Companion.ANDROIDX_REPO_CACHE_DIR
import land.sungbin.androidx.viewer.screen.CodeScreen
import land.sungbin.androidx.viewer.util.PreferenceDefaults
import land.sungbin.androidx.viewer.util.PreferencesKey
import land.sungbin.androidx.viewer.util.StringResolver
import land.sungbin.androidx.viewer.util.runSuspendCatching
import me.tatarka.inject.annotations.Inject
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toOkioPath
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import thirdparty.Timber

@CircuitInject(CodeScreen::class, AppScope::class)
@Inject class CodeScreenPresenter : Presenter<CodeScreen.State> {
  private val repoReader get() = App.preloadedRepoReader
  private val hasGHAccessToken get() = repoReader.repo.hasGHAccessToken

  private suspend fun fetchTree(
    ref: String = AndroidxRepository.HOME_REF,
    noCache: Boolean = false,
    stringResolver: StringResolver? = null,
  ): Result<AndroidxRepositoryTree> =
    runSuspendCatching { repoReader.repo.fetchTree(ref, noCache) }
      .mapCatching { source -> repoReader.readTree(source, noCache) }
      .recoverCatching { exception ->
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

    var gitItem by rememberRetained { mutableStateOf<GitItem>(GitItem.Tree(AndroidxRepositoryTree.Empty, parent = null)) }

    LaunchedEffect(Unit) {
      fetchTree(stringResolver = context::getString)
        .onSuccess { gitItem = GitItem.Tree(it, parent = null) }
        .onFailure { Timber.e(it) }
    }

    return CodeScreen.State(item = gitItem) { event ->
      when (event) {
        is CodeScreen.Event.Init -> gitItem = event.item
        is CodeScreen.Event.Fetch -> {
          scope.launch {
            fetchTree(
              ref = event.sha,
              noCache = event.noCache,
              stringResolver = context::getString,
            )
              .onSuccess { gitItem = GitItem.Tree(it, event.parent) }
              .onFailure { Timber.e(it) }
          }
        }
        is CodeScreen.Event.OpenBlob -> {
          scope.launch {
            runCatching { repoReader.readBlob(event.content.url, event.noCache) }
              .onSuccess { gitItem = GitItem.Blob(it.utf8(), event.content, event.parent) }
              .onFailure { Timber.e(it) }
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
