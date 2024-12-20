// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import java.io.IOException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import land.sungbin.androidx.fetcher.thirdparty.TaskFaker
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.QueueDispatcher
import mockwebserver3.junit5.internal.MockWebServerExtension
import okhttp3.internal.cache.DiskLruCache
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toOkioPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

@ExtendWith(MockWebServerExtension::class)
class AndroidxRepositoryTest {
  @field:TempDir private lateinit var tempDir: Path
  private lateinit var cachingContext: RemoteCachingContext
  private val fs = FakeFileSystem()

  private lateinit var server: MockWebServer
  private lateinit var repo: AndroidxRepository
  private val logger = TestLogger()

  private val taskFaker = TaskFaker()
  private val taskRunner = taskFaker.taskRunner

  private fun createCachingContext() =
    RemoteCachingContext(
      cache = DiskLruCache(
        fileSystem = fs,
        directory = tempDir.toOkioPath(),
        appVersion = RemoteCachingContext.CACHE_VERSION,
        valueCount = RemoteCachingContext.ENTRY_SIZE,
        maxSize = Long.MAX_VALUE,
        taskRunner = taskRunner,
      ),
    )
      .also { context ->
        context.cache!!.initialize()
        cachingContext = context
      }

  @BeforeTest fun prepare(server: MockWebServer) {
    this.server = server
    repo = AndroidxRepository(server.url("/"), logger, UnconfinedTestDispatcher())
    repeat(3) { server.enqueue(MockResponse()) }
    createCachingContext()
  }

  @AfterTest fun cleanup() {
    cachingContext.cache!!.close()
    taskFaker.close()
    fs.checkNoOpenFiles()
    logger.clear()
  }

  @Test fun given_remoteLoggingContext_with_level_above_none_when_fetching_enable_http_logging(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(level = HttpLoggingInterceptor.Level.BASIC)
    withContext(loggingContext) { repo.fetch() }

    assertThat(logger.debugs)
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")
  }

  @Test fun given_remoteLoggingContext_with_level_none_when_fetching_disable_http_logging(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(level = HttpLoggingInterceptor.Level.NONE)
    withContext(loggingContext) { repo.fetch() }

    assertThat(logger.debugs).isEmpty()
  }

  @Test fun given_remoteCachingContext_when_fetching_apply_http_cache(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(level = HttpLoggingInterceptor.Level.BASIC)
    withContext(loggingContext + cachingContext) { repo.fetch() }

    assertThat(logger.debugs, name = "process real call")
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")
    logger.clear()

    withContext(loggingContext + cachingContext) { repo.fetch() }

    assertThat(logger.debugs, name = "no real call").isEmpty()
  }

  @Test fun given_remoteCachingContext_when_noCache_fetching_not_apply_http_cache(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(level = HttpLoggingInterceptor.Level.BASIC)
    withContext(loggingContext + cachingContext) { repo.fetch() }

    assertThat(logger.debugs)
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")
    logger.clear()

    withContext(loggingContext + cachingContext) { repo.fetch(noCache = true) }

    assertThat(logger.debugs)
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")
  }

  @Test fun given_githubAuthorizationContext_when_fetching_add_authorization_header(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(level = HttpLoggingInterceptor.Level.HEADERS)
    val authorizationContext = GitHubAuthorizationContext("token")
    withContext(loggingContext + authorizationContext) { repo.fetch() }

    assertThat(logger.debugs).contains("Authorization: Bearer token")
  }

  @Test fun given_api_response_is_not_successful_when_fetching_log_error_and_throws(): Unit = runTest {
    (server.dispatcher as QueueDispatcher).clear()
    server.enqueue(MockResponse(code = HTTP_BAD_REQUEST))
    assertFailure { repo.fetch() }.hasClass<IOException>()

    assertThat(logger.errors).contains(
      "Failed to fetch the repository: Response{" +
        "protocol=http/1.1, code=400, " +
        "message=Client Error, " +
        "url=${server.url("/")}repos/androidx/androidx/git/trees/androidx-main" +
        "}",
    )
  }

  @Test fun throws_AuthenticateException_when_receive_400_errors(): Unit = runTest {
    (server.dispatcher as QueueDispatcher).clear()
    server.enqueue(MockResponse(code = HTTP_UNAUTHORIZED))

    assertFailure { repo.fetch() }.hasClass<GitHubAuthenticateException>()
  }
}
