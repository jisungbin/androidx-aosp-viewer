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
import okio.BufferedSource
import okio.buffer
import org.jetbrains.annotations.TestOnly
import thirdparty.Timber

public class AndroidxRepository(
  private val authorizationToken: String? = null,
  private val logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
  private val cache: AndroidxRepositoryCache? = null,
  private val base: HttpUrl = "https://api.github.com".toHttpUrl(),
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private var timber: Timber.Tree = Timber.Forest

  @TestOnly internal fun useLogger(timber: Timber.Tree): AndroidxRepository =
    apply { this.timber = timber }

  public suspend fun fetch(ref: String = HOME_REF, noCache: Boolean = false): BufferedSource {
    val cache = cache.takeUnless { noCache }

    val candidateCache = cache?.getCachedSource(ref)
    if (candidateCache != null) return candidateCache.buffer()

    var httpLogging: HttpLoggingInterceptor? = null
    if (logLevel > HttpLoggingInterceptor.Level.NONE) {
      httpLogging = HttpLoggingInterceptor(timber::d).setLevel(logLevel)
    }

    val client = OkHttpClient.Builder()
      .apply { httpLogging?.let(::addInterceptor) }
      .build()

    val url = base.newBuilder()
      .addPathSegments("repos/androidx/androidx/git/trees")
      .addPathSegment(ref)
      .build()
    val request = Request.Builder()
      .url(url)
      .addHeader("X-GitHub-Api-Version", "2022-11-28")
      .apply { if (authorizationToken != null) addHeader("Authorization", "Bearer $authorizationToken") }
      .build()
    val response = withContext(dispatcher) { client.newCall(request).executeAsync() }

    if (!response.isSuccessful) {
      timber.e("Failed to fetch the repository: $response")
      GitHubAuthenticateException.parse(response)?.let { throw it }
      throw IOException(response.message)
    }

    return response.body.source().also { source ->
      if (cache?.putSource(ref, source.buffer.snapshot()) == false) {
        timber.e("Failed to cache the repository: $ref")
      }
    }
  }

  public companion object {
    public const val HOME_REF: String = "androidx-main"
  }
}
