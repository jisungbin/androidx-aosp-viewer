package land.sungbin.androidx.fetcher

import com.squareup.moshi.JsonReader
import okio.BufferedSource

internal class AndroidxRepositoryReader(private val logger: Logger) {
  fun read(source: BufferedSource): List<GitContent> {
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
    return root ?: emptyList()
  }

  private fun readTree(reader: JsonReader): List<GitContent> {
    // TODO
    return emptyList()
  }
}