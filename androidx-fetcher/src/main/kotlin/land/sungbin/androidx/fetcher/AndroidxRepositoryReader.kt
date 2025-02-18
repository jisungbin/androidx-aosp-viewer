// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import com.squareup.moshi.JsonReader
import java.io.IOException
import kotlinx.collections.immutable.toImmutableList
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.Source
import okio.buffer
import thirdparty.Timber

public class AndroidxRepositoryReader(public val repo: AndroidxRepository) {
  @Throws(IOException::class, GitHubAuthenticateException::class)
  public fun readTree(source: Source, noCache: Boolean = false): AndroidxRepositoryTree {
    val source = source.buffer()
    val snapshotForError = source.buffer.snapshot()

    var sha: String? = null
    var tree: List<GitContent>? = null
    var truncated = false

    JsonReader.of(source).use { reader ->
      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "sha" -> sha = reader.nextString()
          "tree" -> tree = readTreeContent(reader, noCache)
          "truncated" -> {
            if (reader.nextBoolean()) {
              truncated = true
              Timber.w("The repository has too many files to read. Some files may not be included in the list.")
            }
          }
          else -> reader.skipValue()
        }
      }
      reader.endObject()
    }

    if (tree == null) {
      Timber.e("No tree object found in the repository. Please check the given source: ${snapshotForError.utf8()}")
      return AndroidxRepositoryTree.Empty
    }

    return AndroidxRepositoryTree(
      checkNotNull(sha) { "The SHA of the tree is missing." },
      truncated,
      tree.sortedWith(
        compareBy(GitContent::isFile) // Folders first
          .thenBy(GitContent::path), // Alphabetical order
      )
        .toImmutableList(),
    )
  }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  private fun readTreeContent(reader: JsonReader, noCache: Boolean): List<GitContent> =
    buildList {
      reader.beginArray()
      while (reader.hasNext()) {
        var path: String? = null
        var url: String? = null
        var size: Long? = null

        reader.beginObject()
        while (reader.hasNext()) {
          when (reader.nextName()) {
            "path" -> path = reader.nextString()
            "url" -> url = reader.nextString()
            "size" -> size = reader.nextLong()
            else -> reader.skipValue()
          }
        }
        reader.endObject()

        if (path == null || url == null) {
          Timber.w("Required fields are missing in the tree object. (path: $path, url: $url)")
          continue
        }

        add(GitContent(path, url, size))
      }
      reader.endArray()
    }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  public suspend fun readBlob(url: String, noCache: Boolean = false): ByteString {
    val source = repo.fetchBlob(url, noCache).buffer()

    var content: String? = null
    var encoding: String? = null

    JsonReader.of(source).use { reader ->
      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "content" -> content = reader.nextString()
          "encoding" -> encoding = reader.nextString()
          else -> reader.skipValue()
        }
      }
      reader.endObject()
    }

    if (content == null) {
      Timber.w(
        "The content of the blob is missing. Please check the given source: " +
          source.buffer.snapshot().utf8(),
      )
      error("The content of the blob is missing.")
    }

    if (encoding != "base64") {
      Timber.w("Unsupported encoding: $encoding")
      error("The encoding of the blob is unsupported.")
    }

    return content.decodeBase64()!!
  }
}
