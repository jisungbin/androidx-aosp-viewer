// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import assertk.assertions.prop
import assertk.assertions.single
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.internal.MockWebServerExtension
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.ByteString.Companion.encode
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockWebServerExtension::class)
class AndroidxRepositoryReaderTest {
  @Test fun setsTreeParentToGivenParent() {
    val reader = AndroidxRepositoryReader(repo())
    val parent = GitContent("parent path", "parent url", size = null)

    // language=json
    val json = """
{
  "sha": "aaaaaa",
  "url": "https://example.com/",
  "tree": [
    {
      "path": ".github",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa",
      "url": "https://example.com/"
    },
    {
      "path": ".github2",
      "mode": "00000",
      "type": "tree",
      "sha": "aaaaab",
      "url": "https://example.com/"
    },
    {
      "path": ".github3",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaac",
      "url": "https://example.com/"
    }
  ],
  "truncated": false
}
    """.trimIndent()

    val result = reader.readTree(bufferOf(json), parent)

    assertThat(result.map(GitContent::parent)).containsOnly(parent)
  }

  @Test fun closesSourceAfterParsing() {
    val reader = AndroidxRepositoryReader(repo())

    // language=json
    val json = """
{
  "sha": "aaaaaa",
  "url": "https://example.com/",
  "tree": [
    {
      "path": ".github",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa",
      "url": "https://example.com/"
    }
  ],
  "truncated": false
}
    """.trimIndent()
    val source = bufferOf(json)

    reader.readTree(source)

    assertThat(source.exhausted()).isTrue()
  }

  @Test fun parsesTruncatedTree() {
    val reader = AndroidxRepositoryReader(repo())

    // language=json
    val json = """
{
  "sha": "aaaaaa",
  "url": "https://example.com/",
  "tree": [
    {
      "path": ".github",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa",
      "url": "https://example.com/"
    }
  ],
  "truncated": true
}
    """.trimIndent()

    assertThat(reader.readTree(bufferOf(json)))
      .all {
        prop(AndroidxRepositoryTree::truncated).isTrue()
        single().isEqualTo(GitContent(".github", "https://example.com/", size = null))
      }
  }

  @Test fun parsesEmptyTree() {
    val reader = AndroidxRepositoryReader(repo())

    // language=json
    val json = """
{
  "sha": "aaaaaa",
  "url": "https://example.com/",
  "tree": [],
  "truncated": false
}
    """.trimIndent()

    assertThat(reader.readTree(bufferOf(json))).isEqualTo(AndroidxRepositoryTree.Empty)
  }

  @Test fun parsesNullTree() {
    val reader = AndroidxRepositoryReader(repo())

    // language=json
    val json = """
{
  "sha": "aaaaaa",
  "url": "https://example.com/",
  "truncated": false
}
    """.trimIndent()

    assertThat(reader.readTree(bufferOf(json))).isSameInstanceAs(AndroidxRepositoryTree.Empty)
  }

  @Test fun sortsParsedTreeFoldersFirstAndAlphabetically() {
    val reader = AndroidxRepositoryReader(repo())

    // language=json
    val json = """
{
  "sha": "aaaaaa",
  "url": "https://example.com/",
  "tree": [
    {
      "path": "fileB.txt",
      "mode": "000000",
      "type": "blob",
      "sha": "aaaaad",
      "size": 1,
      "url": "https://example.com/fileB.txt"
    },
     {
      "path": "folderA",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa",
      "url": "https://example.com/folderA"
    },
    {
      "path": "folderB",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaab",
      "url": "https://example.com/folderB"
    },
    {
      "path": "fileA.txt",
      "mode": "000000",
      "type": "blob",
      "sha": "aaaaac",
      "size": 1,
      "url": "https://example.com/fileA.txt"
    }
  ],
  "truncated": false
}
    """.trimIndent()

    val result = reader.readTree(bufferOf(json))

    assertThat(result)
      .containsExactly(
        GitContent("folderA", "https://example.com/folderA", size = null),
        GitContent("folderB", "https://example.com/folderB", size = null),
        GitContent("fileA.txt", "https://example.com/fileA.txt", size = 1),
        GitContent("fileB.txt", "https://example.com/fileB.txt", size = 1),
      )
  }

  @Test fun parsesOnlyContentWithRequiredFields() {
    val reader = AndroidxRepositoryReader(repo())

    // language=json
    val json = """
{
  "sha": "aaaaaa",
  "url": "https://example.com/",
  "tree": [
    {
      "path": "a",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa",
      "url": "https://example.com/a"
    },
    {
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa",
      "url": "https://example.com/b"
    },
    {
      "path": "c",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa"
    },
    {
      "path": "d",
      "mode": "000000",
      "type": "tree",
      "sha": "aaaaaa",
      "url": "https://example.com/d"
    }
  ],
  "truncated": false
}
    """.trimIndent()

    val result = reader.readTree(bufferOf(json))

    assertThat(result)
      .containsExactly(
        GitContent("a", "https://example.com/a", size = null),
        GitContent("d", "https://example.com/d", size = null),
      )
  }

  @Test fun parsesBlobContent(server: MockWebServer) = runTest {
    val reader = AndroidxRepositoryReader(repo(server.url("/")))
    val text = "Hello, world!"

    val json = """
{
  "sha": "a",
  "node_id": "a",
  "size": 1,
  "url": "https://example.com",
  "content": "${text.encode().base64()}",
  "encoding": "base64"
}
    """.trimIndent()

    server.enqueue(MockResponse(body = json))
    val result = reader.readBlob(server.url("/").toString())

    assertThat(result.utf8()).isEqualTo(text)
  }

  @Test fun throwsExceptionWhenBlobDataMissing(server: MockWebServer) = runTest {
    val reader = AndroidxRepositoryReader(repo(server.url("/")))

    // language=json
    val json = """
{
  "sha": "a",
  "node_id": "a",
  "size": 1,
  "url": "https://example.com",
  "encoding": "base64"
}
    """.trimIndent()

    server.enqueue(MockResponse(body = json))

    assertFailure { reader.readBlob(server.url("/").toString()) }
      .isInstanceOf<IllegalStateException>()
      .hasMessage("The content of the blob is missing.")
  }

  @Test fun throwsExceptionWhenBlobNotBase64(server: MockWebServer) = runTest {
    val reader = AndroidxRepositoryReader(repo(server.url("/")))

    // language=json
    val json = """
{
  "sha": "a",
  "node_id": "a",
  "size": 1,
  "url": "https://example.com",
  "content": "a",
  "encoding": "txt"
}
    """.trimIndent()

    server.enqueue(MockResponse(body = json))

    assertFailure { reader.readBlob(server.url("/").toString()) }
      .isInstanceOf<IllegalStateException>()
      .hasMessage("The encoding of the blob is unsupported.")
  }

  private fun repo(): AndroidxRepository =
    AndroidxRepository(
      baseUrl = "https://example.com".toHttpUrl(),
      dispatcher = UnconfinedTestDispatcher(),
    )

  private fun TestScope.repo(baseUrl: HttpUrl): AndroidxRepository =
    AndroidxRepository(
      baseUrl = baseUrl,
      dispatcher = UnconfinedTestDispatcher(testScheduler),
    )
}
