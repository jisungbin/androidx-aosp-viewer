// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasClass
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import java.io.IOException
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.nio.file.Path
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import okhttp3.logging.HttpLoggingInterceptor
import okio.Path.Companion.toOkioPath
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MockWebServerExtension::class)
class AndroidxRepositoryTest {
  @field:TempDir private lateinit var tempDir: Path
  private var cache: AndroidxRepositoryCache? = null
  private val fs = FakeFileSystem()

  private lateinit var server: MockWebServer
  private val logger = TestTimberTree()

  @BeforeTest fun prepare(server: MockWebServer) {
    this.server = server
  }

  @AfterTest fun cleanup() {
    cache?.close()
    fs.checkNoOpenFiles()
    logger.clear()
    cache = null
  }

  @Test fun fetchingWithBasicLogEnablesHttpLogging() = runTest {
    val repo = AndroidxRepository(
      logLevel = HttpLoggingInterceptor.Level.BASIC,
      base = server.url("/"),
      dispatcher = UnconfinedTestDispatcher(),
    )
      .useLogger(logger)

    server.enqueue(MockResponse())
    repo.fetchTrees()

    assertThat(logger.debugs)
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")
  }

  @Test fun fetchingWithNoneLogDisablesHttpLogging() = runTest {
    val repo = AndroidxRepository(
      logLevel = HttpLoggingInterceptor.Level.NONE,
      base = server.url("/"),
      dispatcher = UnconfinedTestDispatcher(),
    )
      .useLogger(logger)

    server.enqueue(MockResponse())
    repo.fetchTrees()

    assertThat(logger.debugs).isEmpty()
  }

  @Test fun fetchingWithCacheEnablesHttpCache() = runTest {
    val repo = AndroidxRepository(
      cache = loadCache(),
      base = server.url("/"),
      dispatcher = UnconfinedTestDispatcher(),
    )
      .useLogger(logger)

    server.enqueue(MockResponse(body = "Hello, world!"))
    var response = repo.fetchTrees()

    assertThat(response.use { it.readUtf8() }).isEqualTo("Hello, world!")
    assertThat(logger.debugs, name = "process real call")
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")

    logger.clear()
    server.enqueue(MockResponse(body = "Hello, world! 2"))
    response = repo.fetchTrees()

    assertThat(response.use { it.readUtf8() }).isEqualTo("Hello, world!")
    assertThat(logger.debugs, name = "empty call").isEmpty()
  }

  @Test fun noCacheFetchingWithCacheDisablesHttpCache() = runTest {
    val repo = AndroidxRepository(
      cache = loadCache(),
      base = server.url("/"),
      dispatcher = UnconfinedTestDispatcher(),
    )
      .useLogger(logger)

    server.enqueue(MockResponse())
    repo.fetchTrees()

    assertThat(logger.debugs, name = "first cache call")
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")

    logger.clear()
    server.enqueue(MockResponse())
    repo.fetchTrees(noCache = true)

    assertThat(logger.debugs, name = "second cache call")
      .contains("--> GET ${server.url("/")}repos/androidx/androidx/git/trees/androidx-main")
  }

  @Test fun fetchingWithAuthorizationTokenAddsAuthorizationHeader() = runTest {
    val repo = AndroidxRepository(
      authorizationToken = "token2",
      logLevel = HttpLoggingInterceptor.Level.HEADERS,
      base = server.url("/"),
      dispatcher = UnconfinedTestDispatcher(),
    )
      .useLogger(logger)

    server.enqueue(MockResponse())
    repo.fetchTrees()

    assertThat(logger.debugs).contains("Authorization: ██")
  }

  @Test fun errorFetchinLogsAndThrows() = runTest {
    val repo = AndroidxRepository(
      base = server.url("/"),
      dispatcher = UnconfinedTestDispatcher(),
    )
      .useLogger(logger)

    server.enqueue(MockResponse(code = HTTP_BAD_REQUEST))
    assertFailure { repo.fetchTrees() }.hasClass<IOException>()

    assertThat(logger.errors).contains(
      "Failed to fetch the repository: Response{" +
        "protocol=http/1.1, code=400, " +
        "message=Client Error, " +
        "url=${server.url("/")}repos/androidx/androidx/git/trees/androidx-main" +
        "}",
    )
  }

  @Test fun unauthorizedFetchingThrowsAuthenticateException() = runTest {
    val repo = AndroidxRepository(
      base = server.url("/"),
      dispatcher = UnconfinedTestDispatcher(),
    )
      .useLogger(logger)

    server.enqueue(MockResponse(code = HTTP_UNAUTHORIZED))
    assertFailure { repo.fetchTrees() }.hasClass<GitHubAuthenticateException>()
  }

  private fun TestScope.loadCache(): AndroidxRepositoryCache =
    AndroidxRepositoryCache(
      directory = tempDir.toOkioPath(),
      maxSize = Long.MAX_VALUE,
      fs = fs,
      scope = this,
    )
      .also { cache = it }
}
