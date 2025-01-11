// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import com.mayakapps.kache.OkioFileKache
import dev.drewhamilton.poko.Poko
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.ByteString
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.Source
import org.jetbrains.annotations.VisibleForTesting

// OkHttp's built-in cache works based on Cache-Control.
// But I want semi-persistent cache that is independent of Cache-Control.
@Poko public class AndroidxRepositoryCache public constructor(
  public val cache: OkioFileKache,
  @VisibleForTesting internal val fs: FileSystem = FileSystem.SYSTEM,
) : Closeable {
  init {
    require(currentFileSystem() === fs) { "FileSystem must be the same as the one used by the cache." }
  }

  public constructor(
    directory: Path,
    maxSize: Long,
    fs: FileSystem = FileSystem.SYSTEM,
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
  ) : this(
    runBlocking {
      OkioFileKache(directory, maxSize) {
        fileSystem = fs
        creationScope = scope
        cacheVersion = CACHE_VERSION
      }
    },
    fs,
  )

  internal suspend fun getCachedSource(ref: String): Source? =
    cache.get(ref)?.let(fs::source)

  internal suspend fun upsertSource(ref: String, source: ByteString): Boolean {
    val result = cache.put(ref) { path ->
      try {
        fs.write(path) { write(source) }
        true
      } catch (_: Exception) {
        false
      }
    }
    return result != null
  }

  override fun close() {
    runBlocking { cache.close() }
  }

  private fun currentFileSystem(): FileSystem =
    OkioFileKache::class.java
      .getDeclaredField("fileSystem")
      .apply { isAccessible = true }
      .get(cache) as FileSystem

  public companion object {
    public const val CACHE_VERSION: Int = 1
  }
}
