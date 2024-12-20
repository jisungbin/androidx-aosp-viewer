// Copyright 2024 Ji Sungbin
// SPDX-License-Identifier: Apache-2.0
package land.sungbin.androidx.viewer.utils

import android.content.Context
import androidx.annotation.NonUiContext
import androidx.annotation.WorkerThread
import land.sungbin.androidx.fetcher.RemoteCachingContext
import land.sungbin.androidx.fetcher.RemoteCachingRunner
import okhttp3.internal.cache.DiskLruCache
import okio.FileSystem
import okio.Path.Companion.toOkioPath

object GitHubFetchCachingContext {
  @WorkerThread operator fun invoke(
    @NonUiContext context: Context,
    maxSizeInMB: Long,
    fs: FileSystem = FileSystem.SYSTEM,
  ): RemoteCachingContext? {
    if (maxSizeInMB == 0L) return null

    val cache = DiskLruCache(
      fileSystem = fs,
      directory = context.cacheDir.toOkioPath(),
      appVersion = RemoteCachingContext.CACHE_VERSION,
      valueCount = RemoteCachingContext.ENTRY_SIZE,
      maxSize = maxSizeInMB * 1_000_000, // MB to BYTE
      taskRunner = RemoteCachingRunner(),
    )
      .also { it.initialize() }

    return RemoteCachingContext(cache)
  }
}
