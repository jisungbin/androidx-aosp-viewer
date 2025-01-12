// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.fetcher

import java.io.IOException
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.concurrent.TaskRunner
import okhttp3.internal.concurrent.TaskRunner.RealBackend
import okio.BufferedSource
import okio.Closeable
import okio.FileSystem
import okio.Path
import okio.Source
import okio.buffer
import org.jetbrains.annotations.VisibleForTesting

// OkHttp's built-in cache works based on Cache-Control.
// But I want semi-persistent cache that is independent of Cache-Control.
@JvmInline public value class AndroidxRepositoryCache @VisibleForTesting internal constructor(
  @VisibleForTesting internal val cache: DiskLruCache,
) : Closeable {
  public constructor(directory: Path, maxSize: Long) :
    this(
      DiskLruCache(
        fileSystem = FileSystem.SYSTEM,
        directory = directory,
        appVersion = CACHE_VERSION,
        valueCount = ENTRY_SIZE,
        maxSize = maxSize,
        taskRunner = taskRunner(),
      ),
    )

  init {
    cache.initialize()
  }

  internal fun getCachedSource(ref: String): Source? =
    cache[ref]?.getSource(ENTRY_INDEX)

  internal fun putSource(ref: String, source: BufferedSource): Boolean {
    val snapshot = source.apply { request(Long.MAX_VALUE) }.buffer.snapshot()
    if (snapshot.size == 0) return false
    var editor: DiskLruCache.Editor? = null

    return try {
      editor = cache.edit(ref) ?: return false
      editor.newSink(ENTRY_INDEX).buffer().use { it.write(snapshot) }
      editor.commit()
      true
    } catch (_: IOException) {
      try {
        editor?.abort()
      } catch (_: IOException) {
      }
      false
    }
  }

  internal fun evictAll(): Boolean =
    try {
      cache.evictAll()
      true
    } catch (_: IOException) {
      false
    }

  override fun close() {
    cache.close()
  }

  public companion object {
    private const val ENTRY_INDEX = 0
    @VisibleForTesting internal const val ENTRY_SIZE: Int = 1
    private const val CACHE_VERSION: Int = 1

    private fun taskRunner(): TaskRunner =
      TaskRunner(
        RealBackend { runnable ->
          Thread(runnable, AndroidxRepositoryCache::class.simpleName!!).apply { isDaemon = true }
        },
      )
  }
}
