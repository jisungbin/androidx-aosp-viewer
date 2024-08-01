/*
 * Developed by Ji Sungbin 2024.
 *
 * Licensed under the MIT.
 * Please see full license: https://github.com/jisungbin/androidx-aosp-viewer/blob/trunk/LICENSE
 */

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
    maxSize: Long,
    fs: FileSystem = FileSystem.SYSTEM,
  ): RemoteCachingContext? {
    if (maxSize == 0L) return null

    val cache = DiskLruCache(
      fileSystem = fs,
      directory = context.cacheDir.toOkioPath(),
      appVersion = RemoteCachingContext.CACHE_VERSION,
      valueCount = RemoteCachingContext.ENTRY_SIZE,
      maxSize = maxSize,
      taskRunner = RemoteCachingRunner(),
    )
      .also { it.initialize() }

    return RemoteCachingContext(cache)
  }
}
