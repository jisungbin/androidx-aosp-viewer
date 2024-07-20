package land.sungbin.androidx.fetcher

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.LoggingEventListener

public class AndroidxRepository(
  private val base: HttpUrl = "https://api.github.com".toHttpUrl(),
  private val logger: Logger = Logger.Default,
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  public suspend fun fetch(): List<GitContent> {
    var httpLogging: HttpLoggingInterceptor? = null
    var eventLogging: LoggingEventListener.Factory? = null

    coroutineContext[RemoteLoggingContext]?.let { loggingContext ->
      if (loggingContext.httpLogging > HttpLoggingInterceptor.Level.NONE) {
        httpLogging = HttpLoggingInterceptor { message ->
          logger.debug { message }
        }
          .apply { level = loggingContext.httpLogging }
      }
      if (loggingContext.eventLogging) {
        eventLogging = LoggingEventListener.Factory { message ->
          logger.debug { message }
        }
      }
    }

    val cache = coroutineContext[RemoteCachingContext]?.let { cachingContext ->
      Cache(cachingContext.fs, cachingContext.directory, cachingContext.maxSize)
    }
    val token = coroutineContext[GitHubAuthorizationContext]?.token

    val client = OkHttpClient.Builder()
      .cache(cache)
      .apply { httpLogging?.let(::addInterceptor) }
      .apply { eventLogging?.let(::eventListenerFactory) }
      .build()

    val url = base.newBuilder()
      .addPathSegments("repos/androidx/androidx/git/trees/androidx-main")
      .build()
    val request = Request.Builder()
      .url(url)
      .addHeader("X-GitHub-Api-Version", "2022-11-28")
      .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
      .build()
    val response = withContext(ioDispatcher) { client.newCall(request).executeAsync() }

    if (!response.isSuccessful) {
      logger.error { "Failed to fetch the repository: $response" }
      return emptyList()
    }

    return AndroidxRepositoryReader(logger, client, ioDispatcher).read(response.body.source())
  }
}