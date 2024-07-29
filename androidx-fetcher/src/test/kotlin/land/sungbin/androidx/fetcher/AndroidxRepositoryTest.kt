/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

package land.sungbin.androidx.fetcher

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.nio.file.Path
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.QueueDispatcher
import mockwebserver3.junit5.internal.MockWebServerExtension
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toOkioPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir

@ExtendWith(MockWebServerExtension::class)
class AndroidxRepositoryTest {
  @field:TempDir private lateinit var testPath: Path

  private lateinit var logger: TestLogger
  private lateinit var server: MockWebServer
  private lateinit var repo: AndroidxRepository

  @BeforeTest fun prepare(server: MockWebServer) {
    logger = TestLogger()
    this.server = server
    repo = AndroidxRepository(server.url("/"), logger, UnconfinedTestDispatcher())
    server.enqueue(MockResponse())
  }

  @Test fun given_remoteLoggingContext_with_level_above_none_when_fetching_enable_http_logging(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(
      httpLogging = HttpLoggingInterceptor.Level.BASIC,
      eventLogging = false,
    )
    withContext(loggingContext) { repo.fetch() }

    logger.assert(mustAssertAll = false) {
      debugs has "--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main"
    }

    assertThat(logger.debugs.getOrNull(1).orEmpty()).doesNotContain("cacheMiss")
  }

  @Test fun given_remoteLoggingContext_with_level_none_when_fetching_disable_http_logging(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(
      httpLogging = HttpLoggingInterceptor.Level.NONE,
      eventLogging = false,
    )
    withContext(loggingContext) { repo.fetch() }

    assertThat(logger.debugs).isEmpty()
  }

  @Test fun given_remoteLoggingContext_with_event_logging_when_fetching_enable_event_logging(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(
      httpLogging = HttpLoggingInterceptor.Level.NONE,
      eventLogging = true,
    )
    withContext(loggingContext) { repo.fetch() }

    logger.assert(mustAssertAll = false) {
      debugs has "[0 ms] callStart: Request{method=GET, url=${server.url("/")}repos/androidx/androidx/git/trees/androidx-main, " +
        "headers=[X-GitHub-Api-Version:2022-11-28]}"
      debugs hasNot "--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main"
    }

    assertThat(logger.debugs.getOrNull(1).orEmpty()).doesNotContain("cacheMiss")
  }

  @Test fun given_remoteLoggingContext_with_both_logs_enabled_when_fetching_enable_http_and_event_logging(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(
      httpLogging = HttpLoggingInterceptor.Level.HEADERS,
      eventLogging = true,
    )
    withContext(loggingContext) { repo.fetch() }

    logger.assert(mustAssertAll = false) {
      debugs has "[0 ms] callStart: Request{method=GET, url=${server.url("/")}repos/androidx/androidx/git/trees/androidx-main, " +
        "headers=[X-GitHub-Api-Version:2022-11-28]}"
      debugs has "--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main"
      debugs hasNot "Authorization: Bearer token"
    }

    assertThat(logger.debugs.getOrNull(1).orEmpty()).doesNotContain("cacheMiss")
  }

  @Test fun given_remoteCachingContext_when_fetching_apply_http_cache(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(
      httpLogging = HttpLoggingInterceptor.Level.NONE,
      eventLogging = true,
    )
    val cachingContext = RemoteCachingContext(
      fs = FakeFileSystem(),
      directory = testPath.toOkioPath(),
      maxSize = 1024L,
    )
    withContext(loggingContext + cachingContext) { repo.fetch() }

    logger.assert(mustAssertAll = false) {
      debugs hasNot "--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main"
    }

    assertThat(logger.debugs.getOrNull(1).orEmpty()).contains("cacheMiss")
  }

  @Test fun given_githubAuthorizationContext_when_fetching_add_authorization_header(): Unit = runTest {
    val loggingContext = RemoteLoggingContext(
      httpLogging = HttpLoggingInterceptor.Level.HEADERS,
      eventLogging = false,
    )
    val authorizationContext = GitHubAuthorizationContext("token")
    withContext(loggingContext + authorizationContext) { repo.fetch() }

    logger.assert(mustAssertAll = false) {
      debugs has "Authorization: Bearer token"
    }
  }

  @Test fun given_api_response_is_not_successful_when_fetching_log_error(): Unit = runTest {
    (server.dispatcher as QueueDispatcher).clear()
    server.enqueue(MockResponse(code = HTTP_BAD_REQUEST))
    repo.fetch()

    logger.assert {
      errors has "Failed to fetch the repository: Response{" +
        "protocol=http/1.1, code=400, " +
        "message=Client Error, " +
        "url=${server.url("/")}repos/androidx/androidx/git/trees/androidx-main" +
        "}"
    }
  }
}
