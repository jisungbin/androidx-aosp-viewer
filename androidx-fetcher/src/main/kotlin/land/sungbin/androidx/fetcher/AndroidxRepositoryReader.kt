// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import com.squareup.moshi.JsonReader
import java.io.IOException
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okio.BufferedSource
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.buffer
import org.jetbrains.annotations.TestOnly
import thirdparty.Timber

public class AndroidxRepositoryReader(
  private val cache: AndroidxRepositoryCache? = null,
  private val client: OkHttpClient = OkHttpClient(),
  private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
  private var timber: Timber.Tree = Timber.Forest

  @TestOnly internal fun useLogger(timber: Timber.Tree): AndroidxRepositoryReader =
    apply { this.timber = timber }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  public suspend fun read(
    source: BufferedSource,
    parent: ImmutableList<GitContent>? = null,
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
              timber.w(
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

    return root?.sortedWith(
      compareBy<GitContent> { it.blob == null } // Folders first
        .thenBy { it.path }, // Alphabetical order
    ) ?: run {
      timber.e(
        "No tree object found in the repository. " +
          "Please check the given source: ${snapshotForError.utf8()}",
      )
      emptyList()
    }
  }

  @Throws(IOException::class, GitHubAuthenticateException::class)
  private suspend fun readTree(
    reader: JsonReader,
    parent: ImmutableList<GitContent>?,
    noCache: Boolean,
  ): List<GitContent> = coroutineScope {
    val contentJobs = mutableListOf<Job>()
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
        timber.w(
          "Required fields are missing in the tree object. " +
            "(path: $path, type: $type, url: $url)",
        )
        continue
      }

      contentJobs += launch(Dispatchers.Unconfined) {
        if (type == "blob") blob = { readBlobContent(url, noCache) }
        contents += async(Dispatchers.Unconfined) {
          GitContent(path, url, blob?.let { withContext(dispatcher) { it() } }, parent)
        }
      }
    }
    reader.endArray()

    contentJobs.joinAll()
    contents.awaitAll()
  }

  private suspend fun readBlobContent(url: String, noCache: Boolean): ByteString {
    val cache = cache.takeUnless { noCache }
    val cacheRef = url.substringAfterLast('/')

    val request = Request.Builder().url(url).build()

    val candidateCache = cache?.getCachedSource(cacheRef)
    if (candidateCache != null) return candidateCache.buffer().use { it.readByteString() }

    return client.newCall(request).executeAsync().use { response ->
      if (!response.isSuccessful) {
        timber.e("Failed to fetch the blob: $response")
        GitHubAuthenticateException.parse(response)?.let { throw it }
        throw IOException(response.message)
      }

      val source = response.body.source()
      if (cache?.putSource(cacheRef, source.buffer.snapshot()) == false) {
        timber.e("Failed to cache the blob: $url")
      }

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
        timber.w(
          "The content of the blob is missing. " +
            "Please check the given source: ${source.buffer.snapshot().utf8()}",
        )
        error("The content of the blob is missing.")
      }

      if (encoding != "base64") {
        timber.w("Unsupported encoding: $encoding")
        error("The encoding of the blob is wrong.")
      }

      content.decodeBase64()!!
    }
  }
}
