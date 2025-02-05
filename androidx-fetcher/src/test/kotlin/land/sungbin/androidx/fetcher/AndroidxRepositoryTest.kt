// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.nio.file.Path
import kotlin.test.Ignore
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import land.sungbin.androidx.fetcher.AndroidxRepository.Companion.GITHUB_AUTHORIZATION_HEADER
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.QueueDispatcher
import mockwebserver3.RecordedRequest
import mockwebserver3.junit5.internal.MockWebServerExtension
import okhttp3.HttpUrl
import okhttp3.internal.cache.DiskLruCache
import okhttp3.logging.HttpLoggingInterceptor
import okio.Closeable
import okio.IOException
import okio.Path.Companion.toOkioPath
import okio.buffer
import okio.fakefilesystem.FakeFileSystem
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import thirdparty.TaskFaker

@ExtendWith(MockWebServerExtension::class)
class AndroidxRepositoryTest {
  @Test fun addsAuthorizationHeaderWhenTokenProvided(server: MockWebServer) = runTest {
    var getRequest: RecordedRequest? = null
    val repo = repo(server.url("/"), token = "MyToken")

    server.dispatcher = object : QueueDispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        getRequest = request
        return super.dispatch(request)
      }
    }
    server.enqueue(MockResponse())

    repo.fetchTree()

    assertThat(getRequest!!.headers[GITHUB_AUTHORIZATION_HEADER])
      .isNotNull()
      .isEqualTo("Bearer MyToken")
  }

  @Ignore("How to test this?")
  @Test fun addsHttpLoggingInterceptorWithGivenLogLevel() = Unit

  @Test fun fetchesWithGivenRef(server: MockWebServer) = runTest {
    var getRequest: RecordedRequest? = null
    val repo = repo(server.url("/"), token = "MyToken")

    server.dispatcher = object : QueueDispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        getRequest = request
        return super.dispatch(request)
      }
    }
    server.enqueue(MockResponse())

    repo.fetchTree("my-ref-value")

    assertThat(getRequest!!.path)
      .isNotNull()
      .endsWith("my-ref-value")
  }

  @Test fun cachesAfterSuccessfulFetch(server: MockWebServer, @TempDir path: Path) = runTest {
    val cache = cache(path)
    val repo = repo(server.url("/"), cache = cache)

    server.enqueue(MockResponse(body = "Hello, World!"))

    repo.fetchTree("ref")

    assertThat(cache.getCachedSource("ref"))
      .isNotNull()
      .transform { it.use { source -> source.buffer().readUtf8() } }
      .isEqualTo("Hello, World!")
  }

  @Test fun doesNotCacheAfterFailedFetch(server: MockWebServer, @TempDir path: Path) = runTest {
    val cache = cache(path)
    val repo = repo(server.url("/"), cache = cache)

    server.enqueue(MockResponse(code = HTTP_BAD_REQUEST))

    runCatching { repo.fetchTree("ref") }

    assertThat(cache.getCachedSource("ref")).isNull()
  }

  @Test fun doesNotMakeHttpRequestForCachedFetch(server: MockWebServer, @TempDir path: Path) = runTest {
    var getRequest: RecordedRequest? = null
    val cache = cache(path)
    val repo = repo(server.url("/"), cache = cache)

    server.dispatcher = object : QueueDispatcher() {
      override fun dispatch(request: RecordedRequest): MockResponse {
        getRequest = request
        return super.dispatch(request)
      }
    }
    server.enqueue(MockResponse(body = "my content"))
    server.enqueue(MockResponse(body = "my content"))

    repo.fetchTree("ref")

    assertThat(getRequest, name = "first request").isNotNull()

    getRequest = null
    repo.fetchTree("ref")

    assertThat(getRequest, name = "cached request").isNull()
  }

  @Test fun disablesCacheWhenNoCache(server: MockWebServer, @TempDir path: Path) = runTest {
    val cache = cache(path)
    val repo = repo(server.url("/"), cache = cache)

    server.enqueue(MockResponse())

    repo.fetchTree("ref", noCache = true)

    assertThat(cache.getCachedSource("ref")).isNull()
  }

  @Test fun throwsIOExceptionOnFailedFetch(server: MockWebServer) = runTest {
    val repo = repo(server.url("/"))

    server.enqueue(
      MockResponse.Builder()
        .status("HTTP/1.1 $HTTP_BAD_REQUEST HTTP_BAD_REQUEST")
        .build(),
    )

    assertFailure { repo.fetchTree() }
      .isInstanceOf<IOException>()
      .hasMessage("HTTP_BAD_REQUEST")
  }

  @Test fun throwsGitHubExceptionOnUnauthorizedFetch(server: MockWebServer) = runTest {
    val repo = repo(server.url("/"))

    server.enqueue(
      MockResponse.Builder()
        .status("HTTP/1.1 $HTTP_UNAUTHORIZED HTTP_UNAUTHORIZED")
        .build(),
    )

    assertFailure { repo.fetchTree() }
      .isInstanceOf<GitHubAuthenticateException>()
      .hasMessage("HTTP_UNAUTHORIZED")
  }

  @Test fun throwsGitHubExceptionOnForbiddenFetch(server: MockWebServer) = runTest {
    val repo = repo(server.url("/"))

    server.enqueue(
      MockResponse.Builder()
        .status("HTTP/1.1 $HTTP_FORBIDDEN HTTP_FORBIDDEN")
        .build(),
    )

    assertFailure { repo.fetchTree() }
      .isInstanceOf<GitHubAuthenticateException>()
      .hasMessage("HTTP_FORBIDDEN")
  }

  @Test fun throwsGitHubExceptionOnNotFoundFetch(server: MockWebServer) = runTest {
    val repo = repo(server.url("/"))

    server.enqueue(
      MockResponse.Builder()
        .status("HTTP/1.1 $HTTP_NOT_FOUND HTTP_NOT_FOUND")
        .build(),
    )

    assertFailure { repo.fetchTree() }
      .isInstanceOf<GitHubAuthenticateException>()
      .hasMessage("HTTP_NOT_FOUND")
  }

  private fun TestScope.repo(
    baseUrl: HttpUrl,
    token: String? = null,
    cache: AndroidxRepositoryCache? = null,
    logging: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.NONE,
  ): AndroidxRepository =
    AndroidxRepository(
      authorizationToken = token,
      cache = cache,
      logging = logging,
      baseUrl = baseUrl,
      dispatcher = UnconfinedTestDispatcher(testScheduler),
    )

  private fun cache(path: Path): AndroidxRepositoryCache {
    val fs = FakeFileSystem().also(toCloses::add)
    val taskFaker = TaskFaker().also(toCloses::add)

    return AndroidxRepositoryCache(
      DiskLruCache(
        fileSystem = fs,
        directory = path.toOkioPath(),
        appVersion = 1,
        valueCount = AndroidxRepositoryCache.ENTRY_SIZE,
        maxSize = Long.MAX_VALUE,
        taskRunner = taskFaker.taskRunner,
      ),
    )
      .also(toCloses::add)
  }

  private companion object {
    private val toCloses = mutableListOf<Closeable>()

    @JvmStatic
    @AfterAll fun tearDown() {
      toCloses.forEach(Closeable::close)
      toCloses.clear()
    }
  }
}
