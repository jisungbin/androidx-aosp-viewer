// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import com.squareup.moshi.JsonReader
import java.io.IOException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import org.jetbrains.annotations.TestOnly
import thirdparty.Timber

public class AndroidxRepositoryReader(private val repo: AndroidxRepository) {
  private var logger: Timber.Tree = Timber.Forest

  @TestOnly internal fun useLogger(timber: Timber.Tree): AndroidxRepositoryReader =
    apply { this.logger = timber }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  public suspend fun read(
    source: BufferedSource,
    parent: GitContent? = null,
    noCache: Boolean = false,
  ): List<GitContent> {
    val snapshotForError = source.buffer.snapshot()
    var root: List<GitContent>? = null

    JsonReader.of(source).use { reader ->
      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "tree" -> root = readTree(reader, parent, noCache)
          "truncated" -> {
            if (reader.nextBoolean()) {
              logger.w(
                "The repository has too many files to read. " +
                  "Some files may not be included in the list.",
              )
            }
          }
          else -> reader.skipValue()
        }
      }
      reader.endObject()
    }

    if (root == null) {
      logger.e(
        "No tree object found in the repository. " +
          "Please check the given source: ${snapshotForError.utf8()}",
      )
      return emptyList()
    }

    return root.sortedWith(
      compareBy(GitContent::isDirectory) // Folders first
        .thenBy(GitContent::path), // Alphabetical order
    )
  }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  private suspend fun readTree(
    reader: JsonReader,
    parent: GitContent?,
    noCache: Boolean,
  ): List<GitContent> = coroutineScope {
    val contents = mutableListOf<Deferred<GitContent>>()

    reader.beginArray()
    while (reader.hasNext()) {
      var path: String? = null
      var type: String? = null
      var url: String? = null
      var blob: (suspend () -> ByteString?)? = null

      reader.beginObject()
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "path" -> path = reader.nextString()
          "type" -> type = reader.nextString()
          "url" -> url = reader.nextString()
          else -> reader.skipValue()
        }
      }
      reader.endObject()

      if (path == null || type == null || url == null) {
        logger.w(
          "Required fields are missing in the tree object. " +
            "(path: $path, type: $type, url: $url)",
        )
        continue
      }

      if (type == "blob") blob = { readBlobContent(url, noCache) }
      contents += async(Dispatchers.Unconfined) {
        GitContent(path, url, blob?.invoke(), parent)
      }
    }
    reader.endArray()

    contents.awaitAll()
  }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  private suspend fun readBlobContent(url: String, noCache: Boolean): ByteString {
    val source = repo.readBlobContent(url, noCache)

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
      logger.w(
        "The content of the blob is missing. " +
          "Please check the given source: ${source.buffer.snapshot().utf8()}",
      )
      error("The content of the blob is missing.")
    }

    if (encoding != "base64") {
      logger.w("Unsupported encoding: $encoding")
      error("The encoding of the blob is wrong.")
    }

    return content.decodeBase64()!!
  }
}
