/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import java.io.IOException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import land.sungbin.androidx.viewer.exception.GitHubAuthenticateException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSource
import okio.buffer

public class AndroidxRepository(
  private val base: HttpUrl = "https://api.github.com".toHttpUrl(),
  private val logger: Logger = Logger.Default,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  @Throws(IOException::class, GitHubAuthenticateException::class)
  public suspend fun fetch(ref: String = HOME_REF, cacheRef: String = ref, noCache: Boolean = false): BufferedSource {
    val cache = coroutineContext[RemoteCachingContext]?.takeUnless { noCache }?.takeIf { it.enabled }

    val candidateCache = cache?.getCachedSource(cacheRef)
    if (candidateCache != null) return candidateCache.buffer()

    var httpLogging: HttpLoggingInterceptor? = null

    coroutineContext[RemoteLoggingContext]?.let { loggingContext ->
      if (loggingContext.level > HttpLoggingInterceptor.Level.NONE) {
        httpLogging = HttpLoggingInterceptor { message ->
          logger.debug { message }
        }
          .apply { level = loggingContext.level }
      }
    }

    val token = coroutineContext[GitHubAuthorizationContext]?.token

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
      .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
      .build()
    val response = withContext(ioDispatcher) { client.newCall(request).executeAsync() }

    if (!response.isSuccessful) {
      logger.error { "Failed to fetch the repository: $response" }
      GitHubAuthenticateException.parse(response)?.let { throw it }
      throw IOException(response.message)
    }

    return response.body.source().also { source ->
      if (cache?.putSource(cacheRef, source.buffer.snapshot()) == false) {
        logger.error { "Failed to cache the repository: $ref" }
      }
    }
  }

  public companion object {
    public const val HOME_REF: String = "androidx-main"
  }
}
