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
  public val authorizationToken: String? = null,
  public val logLevel: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BASIC,
  public val cache: AndroidxRepositoryCache? = null,
  private val base: HttpUrl = "https://api.github.com".toHttpUrl(),
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private var logger: Timber.Tree = Timber.Forest

  @TestOnly internal fun useLogger(timber: Timber.Tree): AndroidxRepository =
    apply { this.logger = timber }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  public suspend fun fetchTrees(ref: String = HOME_REF, noCache: Boolean = false): BufferedSource {
    val cache = cache.takeUnless { noCache }

    val candidateCache = cache?.getCachedSource(ref)
    if (candidateCache != null) {
      Timber.d("Fetched the repository from cache: $ref")
      return candidateCache.buffer()
    }

    val url = base.newBuilder()
      .addPathSegments("repos/androidx/androidx/git/trees")
      .addPathSegment(ref)
      .build()
    val response = withContext(dispatcher) { client().newCall(request(url)).executeAsync() }

    if (!response.isSuccessful) {
      logger.e("Failed to fetch the repository: $response")
      GitHubAuthenticateException.parse(response)?.let { throw it }
      throw IOException(response.message)
    }

    return response.body.source().also { source ->
      val caching = cache?.upsertSource(ref, source.apply { request(Long.MAX_VALUE) }.buffer.snapshot())
      if (caching == true)
        logger.d("Caching the repository: $ref")
      else if (caching == false)
        logger.e("Failed to cache the repository: $ref")
    }
  }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  internal suspend fun readBlobContent(url: String, noCache: Boolean = false): BufferedSource {
    val cache = cache.takeUnless { noCache }
    val cacheRef = url.substringAfterLast('/')

    val candidateCache = cache?.getCachedSource(cacheRef)
    if (candidateCache != null) {
      Timber.d("Fetched the blob from cache: $url")
      return candidateCache.buffer()
    }

    val response = withContext(dispatcher) { client().newCall(request(url.toHttpUrl())).executeAsync() }

    if (!response.isSuccessful) {
      logger.e("Failed to fetch the blob: $response")
      GitHubAuthenticateException.parse(response)?.let { throw it }
      throw IOException(response.message)
    }

    return response.body.source().also { source ->
      val caching = cache?.upsertSource(cacheRef, source.apply { request(Long.MAX_VALUE) }.buffer.snapshot())
      if (caching == true)
        logger.d("Caching the blob: $url")
      else if (caching == false)
        logger.e("Failed to cache the blob: $url")
    }
  }

  private fun client(): OkHttpClient {
    val httpLogging = run {
      if (logLevel > HttpLoggingInterceptor.Level.NONE) {
        HttpLoggingInterceptor(logger::d)
          .apply { redactHeader(GITHUB_AUTHORIZATION_HEADER) }
          .setLevel(logLevel)
      } else {
        null
      }
    }

    return OkHttpClient.Builder()
      .apply { httpLogging?.let(::addInterceptor) }
      .build()
  }

  private fun request(url: HttpUrl): Request =
    Request.Builder()
      .url(url)
      .addHeader("X-GitHub-Api-Version", "2022-11-28")
      .apply { if (authorizationToken != null) addHeader(GITHUB_AUTHORIZATION_HEADER, "Bearer $authorizationToken") }
      .build()

  public companion object {
    public const val HOME_REF: String = "androidx-main"
    private const val GITHUB_AUTHORIZATION_HEADER = "Authorization"
  }
}
