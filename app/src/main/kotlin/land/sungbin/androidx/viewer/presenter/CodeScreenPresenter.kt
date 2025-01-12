// Copyright 2025 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
@file:Suppress("ComposableNaming")

package land.sungbin.androidx.viewer.presenter

import android.content.Context
import androidx.annotation.NonUiContext
import androidx.compose.material3.SnackbarHostState
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
import land.sungbin.androidx.viewer.screen.CodeScreen
import land.sungbin.androidx.viewer.screen.LocalSnackbarHost
import land.sungbin.androidx.viewer.screen.assignAsBlob
import land.sungbin.androidx.viewer.screen.assignAsTree
import land.sungbin.androidx.viewer.util.PreferenceDefaults
import land.sungbin.androidx.viewer.util.PreferencesKey
import land.sungbin.androidx.viewer.util.runSuspendCatching
import me.tatarka.inject.annotations.Inject
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toOkioPath
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn
import thirdparty.Timber

@SingleIn(AppScope::class)
@Inject class CodeScreenPresenter(private val sharedState: CodeScreen.SharedState) : Presenter<CodeScreen.State> {
  private var hasGHAccessToken = false

  private var repo: AndroidxRepository? = null
  private var repoReader: AndroidxRepositoryReader? = null
  private var snackbarHost: SnackbarHostState? = null

  suspend fun assigningFetch(
    ref: String = AndroidxRepository.HOME_REF,
    parent: GitContent? = null,
    noCache: Boolean = false,
    stringResolver: ((Int) -> String)? = null,
  ) {
    val repo = checkNotNull(repo) { "Repository is not loaded yet." }
    val repoReader = checkNotNull(repoReader) { "Repository reader is not loaded yet." }
    val snackbarHost = checkNotNull(snackbarHost) { "SnackbarHost is not loaded yet." }

    runSuspendCatching { repo.fetchTree(ref, noCache) }
      .mapCatching { source -> sharedState.assignAsTree(repoReader.readTree(source, parent, noCache)) }
      .onFailure { exception ->
        Timber.e(exception, "Failed to fetch the content.")

        if (exception is GitHubAuthenticateException) {
          val message = if (hasGHAccessToken) R.string.gh_fetch_failed_authenticate_expired else R.string.gh_fetch_failed_authenticate_needed
          snackbarHost.showSnackbar(stringResolver?.invoke(message) ?: exception.message)
        } else {
          snackbarHost.showSnackbar(exception.message ?: stringResolver?.invoke(R.string.gh_fetch_failed) ?: "Unknown error.")
        }
      }
  }

  @Composable override fun present(): CodeScreen.State {
    val context = LocalContext.current
    val snackbarHost = LocalSnackbarHost.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
      val (currentRepo, currentRepoReader) = loadRepos(context)
      repo = currentRepo
      repoReader = currentRepoReader
      this@CodeScreenPresenter.snackbarHost = snackbarHost

      assigningFetch(stringResolver = context::getString)
    }

    return CodeScreen.State(item = sharedState.item.value) { event ->
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
            val reader = checkNotNull(repoReader) { "Repository reader is not loaded yet." }
            val raw = reader.readBlob(event.content.url, event.noCache)
            sharedState.assignAsBlob(raw.utf8(), event.content)
          }
        }
        is CodeScreen.Event.ToggleFavorite -> TODO()
      }
    }
  }

  private suspend fun loadRepos(@NonUiContext context: Context): Pair<AndroidxRepository, AndroidxRepositoryReader> {
    val preferences = context.dataStore.data.first()

    val ghAccessToken = preferences[PreferencesKey.GHAccessToken]
    val ghHttpLogLevel = preferences[PreferencesKey.GHHttpLogLevel] ?: PreferenceDefaults.GHHttpLogLevel
    val maxCacheSize = (preferences[PreferencesKey.MaxCacheSize] ?: PreferenceDefaults.MaxCacheSize) * 1000 * 1000 // MB to Byte

    hasGHAccessToken = ghAccessToken != null

    val cache = AndroidxRepositoryCache(
      context.cacheDir.resolve(ANDROIDX_REPO_CACHE_DIR).toOkioPath(),
      maxCacheSize,
    )

    val repo = AndroidxRepository(
      ghAccessToken,
      cache,
      HttpLoggingInterceptor.Level.entries[ghHttpLogLevel],
    )
    val repoReader = AndroidxRepositoryReader(repo)

    return repo to repoReader
  }

  companion object {
    private const val ANDROIDX_REPO_CACHE_DIR = "androidx-repo"
  }
}
