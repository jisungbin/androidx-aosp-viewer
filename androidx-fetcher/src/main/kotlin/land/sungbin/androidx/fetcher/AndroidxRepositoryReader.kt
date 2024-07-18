package land.sungbin.androidx.fetcher

import com.squareup.moshi.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import okio.BufferedSource
import okio.ByteString.Companion.decodeBase64

internal class AndroidxRepositoryReader(
  private val logger: Logger,
  private val client: OkHttpClient,
) {
  suspend fun read(source: BufferedSource): List<GitContent> {
    val snapshotForError = source.buffer.snapshot()
    var root: List<GitContent>? = null
    JsonReader.of(source).use { reader ->
      while (reader.hasNext()) {
        when (reader.nextName()) {
          "tree" -> root = readTree(reader)
          "truncated" -> {
            if (reader.nextBoolean()) {
              logger.warn {
                "The repository has too many files to read. " +
                  "Some files may not be included in the list."
              }
            }
          }
          else -> reader.skipValue()
        }
      }
    }
    return root ?: run {
      logger.error {
        "No tree object found in the repository. " +
          "Please check the given source: ${snapshotForError.utf8()}"
      }
      emptyList()
    }
  }

  private suspend fun readTree(reader: JsonReader): List<GitContent> = coroutineScope {
    val contents = mutableListOf<GitContent>()
    reader.beginArray()
    while (reader.hasNext()) {
      var path: String? = null
      var type: String? = null
      var url: String? = null
      var blob: String? = null

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
        logger.warn {
          "Required fields are missing in the tree object. " +
            "(path: $path, type: $type, url: $url)"
        }
        continue
      }

      launch(Dispatchers.Unconfined) {
        if (type == "blob") {
          blob = withContext(Dispatchers.IO) { readBlobContent(url) }
        }

        contents.add(GitContent(path, url, blob))
      }
    }
    reader.endArray()
    contents
  }

  private suspend fun readBlobContent(url: String): String? {
    val request = Request.Builder().url(url).build()
    return client.newCall(request).executeAsync().use { response ->
      var content: String? = null
      var encoding: String? = null

      JsonReader.of(response.body.source()).use { reader ->
        while (reader.hasNext()) {
          when (reader.nextName()) {
            "content" -> content = reader.nextString()
            "encoding" -> encoding = reader.nextString()
            else -> reader.skipValue()
          }
        }
      }

      if (content == null || encoding == null) {
        logger.warn {
          "Required fields are missing in the blob object. " +
            "(content: $content, encoding: $encoding)"
        }
        return null
      }

      if (encoding != "base64") {
        logger.warn { "Unsupported encoding: $encoding" }
        return null
      }

      content.decodeBase64()!!.utf8()
    }
  }
}