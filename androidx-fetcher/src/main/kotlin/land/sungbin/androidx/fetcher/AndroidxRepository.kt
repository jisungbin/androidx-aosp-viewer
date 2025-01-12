// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okhttp3.logging.HttpLoggingInterceptor
import okio.Source
import org.jetbrains.annotations.VisibleForTesting
import thirdparty.Timber

public class AndroidxRepository(
  private val authorizationToken: String? = null,
  private val cache: AndroidxRepositoryCache? = null,
  private val logging: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
  private val baseUrl: HttpUrl = "https://api.github.com".toHttpUrl(),
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  @Throws(IOException::class, GitHubAuthenticateException::class)
  public suspend fun fetchTree(ref: String = HOME_REF, noCache: Boolean = false): Source {
    val cache = cache.takeUnless { noCache }

    val candidateCache = cache?.getCachedSource(ref)
    if (candidateCache != null) {
      Timber.d("Fetched the repository from cache: $ref")
      return candidateCache
    }

    val url = baseUrl.newBuilder()
      .addPathSegments("repos/androidx/androidx/git/trees")
      .addPathSegment(ref)
      .build()
    val response = withContext(dispatcher) {
      client().newCall(request(url)).executeAsync()
    }

    if (!response.isSuccessful) {
      Timber.e("Failed to fetch the repository: $response")
      GitHubAuthenticateException.parse(response)?.let { throw it }
      throw IOException(response.message)
    }

    return response.body.source().also { source ->
      val caching = cache?.putSource(ref, source) ?: return@also
      if (caching)
        Timber.d("Caching the repository: $ref")
      else
        Timber.e("Failed to cache the repository: $ref")
    }
  }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  public suspend fun fetchBlob(url: String, noCache: Boolean = false): Source {
    val cache = cache.takeUnless { noCache }
    val sha = url.substringAfterLast('/')

    val candidateCache = cache?.getCachedSource(sha)
    if (candidateCache != null) {
      Timber.d("Fetched the blob from cache: $url")
      return candidateCache
    }

    val response = withContext(dispatcher) {
      client().newCall(request(url.toHttpUrl())).executeAsync()
    }

    if (!response.isSuccessful) {
      Timber.e("Failed to fetch the blob: $response")
      GitHubAuthenticateException.parse(response)?.let { throw it }
      throw IOException(response.message)
    }

    return response.body.source().also { source ->
      val caching = cache?.putSource(sha, source) ?: return@also
      if (caching)
        Timber.d("Caching the blob: $url")
      else
        Timber.e("Failed to cache the blob: $url")
    }
  }

  private fun client(): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(
        HttpLoggingInterceptor(Timber::d).apply {
          level = logging
          redactHeader(GITHUB_AUTHORIZATION_HEADER)
        },
      )
      .build()

  private fun request(url: HttpUrl): Request =
    Request.Builder()
      .url(url)
      .addHeader("X-GitHub-Api-Version", "2022-11-28")
      .apply {
        if (authorizationToken != null)
          addHeader(GITHUB_AUTHORIZATION_HEADER, "Bearer $authorizationToken")
      }
      .build()

  public companion object {
    public const val HOME_REF: String = "androidx-main"
    @VisibleForTesting internal const val GITHUB_AUTHORIZATION_HEADER = "Authorization"
  }
}
