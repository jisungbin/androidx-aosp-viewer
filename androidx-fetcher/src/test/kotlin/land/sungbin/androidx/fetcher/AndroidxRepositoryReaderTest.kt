// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.junit5.internal.MockWebServerExtension
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockWebServerExtension::class)
class AndroidxRepositoryReaderTest {
  private lateinit var server: MockWebServer

  private val logger = TestTimberTree()
  private val reader = AndroidxRepositoryReader(dispatcher = UnconfinedTestDispatcher()).useLogger(logger)

  @BeforeTest fun prepare(server: MockWebServer) {
    this.server = server
  }

  @AfterTest fun cleanup() {
    logger.clear()
  }

  @Test fun truncatedTreeMakesWarning() = runTest {
    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "tree": [
          {
            "path": ".github",
            "mode": "040000",
            "type": "tree",
            "sha": "0099cfc294dfce6ff0b96b344aec8ed18221389e",
            "url": "https://api.github.com/repos/androidx/androidx/git/trees/0099cfc294dfce6ff0b96b344aec8ed18221389e"
          }
        ],
        "truncated": true
      }
    """.trimIndent()

    reader.read(Buffer().apply { writeUtf8(source) })

    assertThat(logger.warns).contains(
      "The repository has too many files to read. " +
        "Some files may not be included in the list.",
    )
  }

  @Test fun noRootTreeMakesError() = runTest {
    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "truncated": false
      }
    """.trimIndent()

    reader.read(Buffer().apply { writeUtf8(source) })

    assertThat(logger.errors).contains(
      "No tree object found in the repository. " +
        "Please check the given source: $source",
    )
  }

  @Test fun incompleteTreeMakesWarning() = runTest {
    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "tree": [
          {
            "path": "tracing",
            "mode": "040000",
            "sha": "319c54d1ca15c9d3e3574d839a1f58b9a49b5e1f"
          }
        ],
        "truncated": false
      }
    """.trimIndent()

    reader.read(Buffer().apply { writeUtf8(source) })

    assertThat(logger.warns).contains(
      "Required fields are missing in the tree object. " +
        "(path: tracing, type: null, url: null)",
    )
  }

  @Test fun incompleteBlobMakesException() = runTest {
    val blobUrl = server.url("blob.txt")

    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) = MockResponse(
        //language=json
        body = """
        {
          "sha": "somesha",
          "node_id": "somenodeid",
          "size": 10000,
          "url": "https://api.github.com/repos/androidx/androidx/git/blobs/somefile",
          "encoding": "base64"
        }
        """.trimIndent(),
      )
    }

    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "tree": [
           {
            "path": "blob",
            "mode": "040000",
            "type": "blob",
            "sha": "0099cfc294dfce6ff0b96b344aec8ed18221389e",
            "url": "$blobUrl"
          }
        ],
        "truncated": false
      }
    """.trimIndent()

    assertFailure { reader.read(Buffer().apply { writeUtf8(source) }) }
      .hasMessage("The content of the blob is missing.")

    assertThat(logger.warns).contains("The content of the blob is missing. Please check the given source: ")
  }

  @Test fun unsupportedBlobMakesException() = runTest {
    val blobUrl = server.url("blob.txt")

    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) = MockResponse(
        //language=json
        body = """
        {
          "sha": "somesha",
          "node_id": "somenodeid",
          "size": 10000,
          "url": "https://api.github.com/repos/androidx/androidx/git/blobs/somefile",
          "content": "",
          "encoding": "utf8"
        }
        """.trimIndent(),
      )
    }

    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "tree": [
           {
            "path": "blob",
            "mode": "040000",
            "type": "blob",
            "sha": "0099cfc294dfce6ff0b96b344aec8ed18221389e",
            "url": "$blobUrl"
          }
        ],
        "truncated": false
      }
    """.trimIndent()

    assertFailure { reader.read(Buffer().apply { writeUtf8(source) }) }
      .hasMessage("The encoding of the blob is wrong.")

    assertThat(logger.warns).contains("Unsupported encoding: utf8")
  }

  @Test fun gitTreeParsedWithTrees() = runTest {
    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "tree": [
          {
            "path": "tracing",
            "mode": "040000",
            "type": "tree",
            "sha": "319c54d1ca15c9d3e3574d839a1f58b9a49b5e1f",
            "url": "https://api.github.com/repos/androidx/androidx/git/trees/319c54d1ca15c9d3e3574d839a1f58b9a49b5e1f"
          },
          {
            "path": "transition",
            "mode": "040000",
            "type": "tree",
            "sha": "2efb04256a8de6b2f54e009ee01162fad848d76c",
            "url": "https://api.github.com/repos/androidx/androidx/git/trees/2efb04256a8de6b2f54e009ee01162fad848d76c"
          }
        ],
        "truncated": false
      }
    """.trimIndent()

    assertThat(reader.read(Buffer().apply { writeUtf8(source) })).containsExactly(
      GitContent(
        path = "tracing",
        url = "https://api.github.com/repos/androidx/androidx/git/trees/319c54d1ca15c9d3e3574d839a1f58b9a49b5e1f",
        blob = null,
      ),
      GitContent(
        path = "transition",
        url = "https://api.github.com/repos/androidx/androidx/git/trees/2efb04256a8de6b2f54e009ee01162fad848d76c",
        blob = null,
      ),
    )
  }

  @Test fun gitTreeParsedWithBlobsInSortedOrder() = runTest {
    val helloBlobUrl = server.url("blob/hello.txt")
    val worldBlobUrl = server.url("blob/world.txt")
    val byeBlobUrl = server.url("blob/bye.txt")
    val friendBlobUrl = server.url("blob/friend.txt")

    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) = when (request.requestUrl) {
        helloBlobUrl -> MockResponse(body = makeBlobJson("Hello!"))
        worldBlobUrl -> MockResponse(body = makeBlobJson("World!"))
        byeBlobUrl -> MockResponse(body = makeBlobJson("Bye!"))
        friendBlobUrl -> MockResponse(body = makeBlobJson("Friend!"))
        else -> MockResponse(code = HTTP_NOT_FOUND)
      }
    }

    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "tree": [
           {
            "path": "hello",
            "mode": "040000",
            "type": "blob",
            "sha": "0099cfc294dfce6ff0b96b344aec8ed18221389e",
            "url": "$helloBlobUrl"
          },
          {
            "path": "world",
            "mode": "100644",
            "type": "blob",
            "sha": "9b7bb83d3f4ceeb7162fcec0b2c6f8ee4fb3bfec",
            "url": "$worldBlobUrl"
          },
          {
            "path": "bye",
            "mode": "100644",
            "type": "blob",
            "sha": "9b7bb83d3f4ceeb7162fcec0b2c6f8ee4fb3bfec",
            "url": "$byeBlobUrl"
          },
          {
            "path": "friend",
            "mode": "100644",
            "type": "blob",
            "sha": "9b7bb83d3f4ceeb7162fcec0b2c6f8ee4fb3bfec",
            "url": "$friendBlobUrl"
          }
        ],
        "truncated": false
      }
    """.trimIndent()

    assertThat(reader.read(Buffer().apply { writeUtf8(source) })).containsExactly(
      GitContent(path = "bye", url = byeBlobUrl.toString(), blob = "Bye!".encodeUtf8()),
      GitContent(path = "friend", url = friendBlobUrl.toString(), blob = "Friend!".encodeUtf8()),
      GitContent(path = "hello", url = helloBlobUrl.toString(), blob = "Hello!".encodeUtf8()),
      GitContent(path = "world", url = worldBlobUrl.toString(), blob = "World!".encodeUtf8()),
    )
  }

  @Test fun gitTreeParsedWithMixedInSortedOrder() = runTest {
    val helloBlobUrl = server.url("blob/hello.txt")
    val worldBlobUrl = server.url("blob/world.txt")
    val byeBlobUrl = server.url("blob/bye.txt")
    val friendBlobUrl = server.url("blob/friend.txt")

    server.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) = when (request.requestUrl) {
        helloBlobUrl -> MockResponse(body = makeBlobJson("Hello!"))
        worldBlobUrl -> MockResponse(body = makeBlobJson("World!"))
        byeBlobUrl -> MockResponse(body = makeBlobJson("Bye!"))
        friendBlobUrl -> MockResponse(body = makeBlobJson("Friend!"))
        else -> MockResponse(code = HTTP_NOT_FOUND)
      }
    }

    @Language("json") val source = """
      {
        "sha": "85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "url": "https://api.github.com/repos/androidx/androidx/git/trees/85d3f8158b2f9b26cc014a5c9c8793b188544d1f",
        "tree": [
           {
            "path": "hello",
            "mode": "040000",
            "type": "blob",
            "sha": "0099cfc294dfce6ff0b96b344aec8ed18221389e",
            "url": "$helloBlobUrl"
          },
          {
            "path": "world",
            "mode": "100644",
            "type": "blob",
            "sha": "9b7bb83d3f4ceeb7162fcec0b2c6f8ee4fb3bfec",
            "url": "$worldBlobUrl"
          },
          {
            "path": "tracing",
            "mode": "040000",
            "type": "tree",
            "sha": "319c54d1ca15c9d3e3574d839a1f58b9a49b5e1f",
            "url": "url"
          },
          {
            "path": "transition",
            "mode": "040000",
            "type": "tree",
            "sha": "2efb04256a8de6b2f54e009ee01162fad848d76c",
            "url": "url"
          },
          {
            "path": "bye",
            "mode": "100644",
            "type": "blob",
            "sha": "9b7bb83d3f4ceeb7162fcec0b2c6f8ee4fb3bfec",
            "url": "$byeBlobUrl"
          },
          {
            "path": "friend",
            "mode": "100644",
            "type": "blob",
            "sha": "9b7bb83d3f4ceeb7162fcec0b2c6f8ee4fb3bfec",
            "url": "$friendBlobUrl"
          }
        ],
        "truncated": false
      }
    """.trimIndent()

    assertThat(reader.read(Buffer().apply { writeUtf8(source) })).containsExactly(
      GitContent(path = "bye", url = byeBlobUrl.toString(), blob = "Bye!".encodeUtf8()),
      GitContent(path = "friend", url = friendBlobUrl.toString(), blob = "Friend!".encodeUtf8()),
      GitContent(path = "hello", url = helloBlobUrl.toString(), blob = "Hello!".encodeUtf8()),
      GitContent(path = "world", url = worldBlobUrl.toString(), blob = "World!".encodeUtf8()),
      GitContent(path = "tracing", url = "url", blob = null),
      GitContent(path = "transition", url = "url", blob = null),
    )
  }

  private fun makeBlobJson(content: String): String {
    val encoded = content.encodeUtf8().base64()
    // language=json
    return """
      {
        "sha": "somesha",
        "node_id": "somenodeid",
        "size": 10000,
        "url": "https://api.github.com/repos/androidx/androidx/git/blobs/somefile",
        "content": "$encoded",
        "encoding": "base64"
      }
    """.trimIndent()
  }
}
